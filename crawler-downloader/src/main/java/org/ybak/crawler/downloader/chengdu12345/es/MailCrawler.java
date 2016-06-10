package org.ybak.crawler.downloader.chengdu12345.es;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ybak.crawler.downloader.chengdu12345.HtmlParser;
import org.ybak.crawler.downloader.util.HtmlUtil;
import org.ybak.crawler.persistence.service.MailService;
import org.ybak.crawler.persistence.vo.Mail;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Service
public class MailCrawler {

    private static final Logger logger = LoggerFactory.getLogger(MailCrawler.class);

    static String urlPrefix = "http://12345.chengdu.gov.cn/";
    public static CountDownLatch tasks;

    public static Set<Integer> failedNumbers = new HashSet<Integer>();

    @Autowired
    public MailService mailService;

    public void crawAll() {
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(8);
        int start = 1, end = 720;
        tasks = new CountDownLatch(end);
        for (int i = start; i <= end; i++) {
            final int number = i;
            fixedThreadPool.execute(() -> {
                try {
                    crawPage(number);
                } finally {
                    tasks.countDown();
                }
            });
        }
        try {
            tasks.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        fixedThreadPool.shutdownNow();
        logger.info("" + failedNumbers);
    }

    public void crawPage(int number) {
        try {
            String pageUrl = urlPrefix + "moreMail?page=" + number;
            logger.info("开始抓取：" + number);
            String html = HtmlUtil.getURLBody(pageUrl);
            Document doc = Jsoup.parse(html);
            Elements elements = doc.select("div.left5 ul li.f12px");
            List<Mail> pageMails = new ArrayList<>();
            for (Element element : elements) {
                String url = urlPrefix + element.select("a").attr("href");

                Mail mail = crawSingleMail(element, url);
                pageMails.add(mail);
                logger.info("结束抓取：" + number + ", 抓取成功, 剩余任务：" + (tasks.getCount() - 1));
            }
            mailService.save(pageMails);
        } catch (Exception e) {
            e.printStackTrace();
            failedNumbers.add(number);
            logger.info("结束抓取：" + number + ", status=" + e.getMessage() + ", 剩余任务：" + (tasks.getCount() - 1));
        } finally {
        }
    }

    /**
     * craw current page, and decide should craw next page
     *
     * @param number
     * @return should continue
     */
    public boolean updatePage(int number) {
        boolean shouldContinue = false;
        try {
            String pageUrl = urlPrefix + "moreMail?page=" + number;
            String html = HtmlUtil.getURLBody(pageUrl);
            Document doc = Jsoup.parse(html);
            Elements elements = doc.select("div.left5 ul li.f12px");

            for (Element element : elements) {
                String url = urlPrefix + element.select("a").attr("href");
                Mail newMail = crawSingleMail(element, url);
                Mail oldMail = mailService.queryMailByURL(url);
                if(oldMail != null){
                    boolean resultUpdated = !StringUtils.equals(oldMail.result, newMail.result);
                    if(resultUpdated){
                        shouldContinue = true;
                        newMail.id = oldMail.id;
                        mailService.update(newMail);
                    }
                }else{
                    mailService.save(Arrays.asList(newMail));
                    shouldContinue = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return shouldContinue;
    }

    private Mail crawSingleMail(Element element, String url) throws IOException {
        Elements divs = element.select("div");
        String title = divs.get(0).text();
        String sender = divs.get(1).text();
        String receiveUnit = divs.get(2).text();
        String status = divs.get(3).text();
        String category = divs.get(4).text();
        String views = divs.get(5).text();

        String contentHtml = HtmlUtil.getURLBody(url);
        Document contentDoc = Jsoup.parse(contentHtml);
        String content = HtmlParser.getContent(contentDoc);
        String handleResult = HtmlParser.getResult(contentDoc);
        Date publishDate = HtmlParser.getPublishTime(contentDoc);
        return new Mail(url, sender, title, receiveUnit, status, category, Integer.parseInt(views), publishDate, content, handleResult);
    }

    public Mail updateMail(String id, String url) {
        try {
            String contentHtml = HtmlUtil.getURLBody(url);
            Document contentDoc = Jsoup.parse(contentHtml);
            Mail mail = new Mail(id);
            mail.result = HtmlParser.getResult(contentDoc);
            mailService.update(mail);
            return mail;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}