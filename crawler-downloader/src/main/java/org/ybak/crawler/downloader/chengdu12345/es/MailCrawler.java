package org.ybak.crawler.downloader.chengdu12345.es;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.ybak.crawler.downloader.chengdu12345.HtmlParser;
import org.ybak.crawler.downloader.util.HtmlUtil;
import org.ybak.crawler.persistence.repo.MailRepository;
import org.ybak.crawler.persistence.util.ElasticSearchUtil;
import org.ybak.crawler.persistence.vo.Mail;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@SpringBootApplication
@EnableJpaRepositories("org.ybak.crawler.persistence.repo")
@EntityScan("org.ybak.crawler.persistence.vo")
public class MailCrawler {

    static String urlPrefix = "http://12345.chengdu.gov.cn/";
    static CountDownLatch tasks;

    static Set<Integer> failedNumbers = new HashSet<Integer>();

    @Autowired
    MailRepository repo;

    public static void main(String[] args) throws Exception {
        SpringApplication.run(MailCrawler.class, args);
    }

    @PostConstruct
    public void init() throws Exception {
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(8);
        int start = 1, end = 720;
        tasks = new CountDownLatch(end);
        for (int i = start; i <= end; i++) {
            final int number = i;
            fixedThreadPool.execute(() -> {
                try {
                    craw(number);
                } finally {
                    tasks.countDown();
                }
            });
        }
        tasks.await();
        fixedThreadPool.shutdownNow();
        System.out.println(failedNumbers);
    }

    private static void craw(int number) {
        String pageUrl = urlPrefix + "moreMail?page=" + number;
        System.out.println("开始抓取：" + number);
        try {
            String html = HtmlUtil.getURLBody(pageUrl);
            Document doc = Jsoup.parse(html);
            Elements elements = doc.select("div.left5 ul li.f12px");
            List<Mail> pageMails = new ArrayList<>();
            for (Element element : elements) {
                String url = urlPrefix + element.select("a").attr("href");

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
                Date publishDate =  HtmlParser.getPublishTime(contentDoc);
                Mail mail = new Mail(url, sender, title, receiveUnit, status, category, Integer.parseInt(views),  publishDate, content, handleResult);
                pageMails.add(mail);
                System.out.println("结束抓取：" + number + ", 抓取成功, 剩余任务：" + (tasks.getCount() - 1));
            }
            ElasticSearchUtil.indexMails(pageMails);
        } catch (Exception e) {
            e.printStackTrace();
            failedNumbers.add(number);
            System.out.println("结束抓取：" + number + ", status=" + e.getMessage() + ", 剩余任务：" + (tasks.getCount() - 1));
        } finally {
        }
    }

}