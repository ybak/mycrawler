package org.ybak.crawler.downloader.cdgh;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ybak.crawler.downloader.util.HtmlUtil;
import org.ybak.crawler.persistence.service.PlanService;
import org.ybak.crawler.persistence.vo.Plan;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;


@Service
public class PlanCrawler {

    private static final Logger logger = LoggerFactory.getLogger(PlanCrawler.class);

    static String urlPrefix = "http://www.cdgh.gov.cn/ghgs/gggs/";

    @Autowired
    public PlanService planService;

    /**
     * craw current page, and decide should craw next page
     *
     * @param number
     * @return should continue
     */
    public boolean updatePage(int number) {
        boolean shouldContinue = false;
        try {
            String pageUrl = urlPrefix + number + ".htm";
            Plan oldMail = planService.queryPlanByURL(pageUrl);

            if (oldMail == null) {
                Plan newMail = crawSinglePlan(pageUrl);
                planService.save(Arrays.asList(newMail));
                shouldContinue = true;
            }
        } catch (Exception e) {
            logger.error("抓取失败", e.getMessage());
        }
        return shouldContinue;
    }

    private Plan crawSinglePlan(String url) throws IOException {
        String html = HtmlUtil.getURLBody(url);
        Document doc = Jsoup.parse(html);
        String title = ContentProcessor.getTitle(doc);
        Date publishTime = ContentProcessor.getPublishTime(doc);
        return new Plan(url, title, publishTime);
    }

}