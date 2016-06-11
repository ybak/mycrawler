package org.ybak.crawler.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ybak.crawler.downloader.chengdu12345.es.MailCrawler;
import org.ybak.crawler.persistence.service.MailService;
import org.ybak.crawler.persistence.vo.Mail;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class CrawController {

    public static final String LOCK_KEY = "local:craw:increase";
    @Autowired
    private SimpMessagingTemplate msgTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MailService mailService;

    @Autowired
    private MailCrawler mailCrawler;

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
        return mailService.search(keyword, query);
    }

    @RequestMapping("/update")
    @ResponseBody
    public Mail update(String id, String url) {
        return mailCrawler.updateMail(id, url);
    }

    @MessageMapping("/craw/start")
    public String increaseCraw(Message<?> message) throws Exception {
        String sessionId = SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "1");
        if (locked) {
            redisTemplate.expire(LOCK_KEY, 1, TimeUnit.MINUTES);
        } else {
            return "locked";
        }

        new Thread() {
            @Override
            public void run() {
                mailService.initIndexIfAbsent();
                CrawProgress progress = new CrawProgress(1000);
                for (int i = 1; i < 1000; i++) {//最大1000页邮件
                    boolean shouldContinue = mailCrawler.updatePage(i);
                    progress.current = i;
                    msgTemplate.convertAndSend("/topic/progress", progress);
                    if (!shouldContinue) {
                        break;
                    }
                }
                progress.current = 1000;
                msgTemplate.convertAndSend("/topic/progress", progress);
            }
        }.start();

        return "ok";
    }

}