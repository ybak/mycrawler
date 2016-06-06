package org.ybak.crawler.downloader.chengdu12345;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.ybak.crawler.downloader.chengdu12345.es.MailCrawler;

import javax.annotation.PostConstruct;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by happy on 2016/6/6.
 */
@SpringBootApplication
public class CrawlerStart {

    @Autowired
    MailCrawler crawler;

    public static void main(String[] args) throws Exception {
        SpringApplication.run(MailCrawler.class, args);
    }


    @PostConstruct
    public void init() throws Exception {
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(8);
        int start = 1, end = 720;
        crawler.tasks = new CountDownLatch(end);
        for (int i = start; i <= end; i++) {
            final int number = i;
            fixedThreadPool.execute(() -> {
                try {
                    crawler.craw(number);
                } finally {
                    crawler.tasks.countDown();
                }
            });
        }
        crawler.tasks.await();
        fixedThreadPool.shutdownNow();
        System.out.println(crawler.failedNumbers);
    }
}
