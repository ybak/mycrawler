package org.ybak.crawler.downloader.chengdu12345;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.ybak.crawler.downloader.chengdu12345.es.MailCrawler;

import javax.annotation.PostConstruct;

/**
 * Created by happy on 2016/6/6.
 */
@SpringBootApplication(scanBasePackages = {
        "org.ybak.crawler.persistence.service",
        "org.ybak.crawler.downloader.chengdu12345.es"
})
public class CrawlerStart {

    @Autowired
    MailCrawler crawler;

    public static void main(String[] args) throws Exception {
        SpringApplication.run(CrawlerStart.class, args);
    }

    @PostConstruct
    public void init() throws Exception {
        crawler.crawAll();
    }
}
