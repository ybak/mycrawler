package org.ybak.crawler.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ybak.crawler.downloader.cdgh.PlanCrawler;
import org.ybak.crawler.persistence.service.PlanService;
import org.ybak.crawler.persistence.vo.Plan;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Controller
@RequestMapping(value = "/cdgh")
@MessageMapping(value = "/cdgh")
public class PlanCrawController {

    public static final String LOCK_KEY = "local:craw:cdgh:increase";
    @Autowired
    private SimpMessagingTemplate msgTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private PlanService planService;
    @Autowired
    private PlanCrawler planCrawler;

    @RequestMapping("/search")
    @ResponseBody
    public PageImpl<Map<String, Object>> search(String keyword) {
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "1");
        if (locked) {
            redisTemplate.expire(LOCK_KEY, 5, TimeUnit.SECONDS);
        } else {
            throw new RuntimeException("访问太频繁");
        }

        Pageable query = new PageRequest(0, 100);
        return planService.search(keyword, query);
    }

    @MessageMapping("/craw/start")
    public String increaseCraw(String jobId) throws Exception {
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "1");
        if (locked) {
            redisTemplate.expire(LOCK_KEY, 1, TimeUnit.MINUTES);
        } else {
            return "locked";
        }

        new Thread() {
            @Override
            public void run() {
                planService.initIndexIfAbsent();
                BoundedExecutor executor = new BoundedExecutor(5);
                AtomicBoolean shouldContinue = new AtomicBoolean(true);
                CrawProgress progress = new CrawProgress(10000);

                for (int i = 0; i < 10000; i++) {//最大1000页邮件
                    int page = i;

                    executor.submitTask(() -> {
                        shouldContinue.set(planCrawler.updatePage(page + 1));
                        progress.current = page;
                        msgTemplate.convertAndSend("/topic/progress/cdgh/" + jobId, progress);
                    });
//                    if (!shouldContinue.get()) {
//                        break;
//                    }
                }
                progress.current = 500;
                msgTemplate.convertAndSend("/topic/progress/cdgh/" + jobId, progress);
            }
        }.start();

        return "ok";
    }

}