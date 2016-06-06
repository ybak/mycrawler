package org.ybak.crawler.downloader.chengdu12345.es;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;
import org.ybak.crawler.downloader.chengdu12345.HtmlParser;
import org.ybak.crawler.downloader.util.HtmlUtil;
import org.ybak.crawler.persistence.service.MailService;
import org.ybak.crawler.persistence.vo.Mail;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



@Service
public class MailCrawler {

    static String urlPrefix = "http://12345.chengdu.gov.cn/";
    public static CountDownLatch tasks;

    public static Set<Integer> failedNumbers = new HashSet<Integer>();

    @Autowired
    MailService mailService;

    public void craw(int number) {
        String pageUrl = urlPrefix + "moreMail?page=" + number;
        System.out.println("开始抓取：" + number);
        try {
            String html = HtmlUtil.getURLBody(pageUrl);
            Document doc = Jsoup.parse(html);
            Elements elements = doc.select("div.left5 ul li.f12px");
            List<Mail> pageMails = new ArrayList<>();
            for (Element element : elements) {
                String url = urlPrefix + element.select("a").attr("href");

                Mail mail = crawSingleMail(element, url);
                pageMails.add(mail);
                System.out.println("结束抓取：" + number + ", 抓取成功, 剩余任务：" + (tasks.getCount() - 1));
            }
            mailService.save(pageMails);
        } catch (Exception e) {
            e.printStackTrace();
            failedNumbers.add(number);
            System.out.println("结束抓取：" + number + ", status=" + e.getMessage() + ", 剩余任务：" + (tasks.getCount() - 1));
        } finally {
        }
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