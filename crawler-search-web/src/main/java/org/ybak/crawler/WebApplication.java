package org.ybak.crawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//@SpringBootApplication

@SpringBootApplication(scanBasePackages = {
        "org.ybak.crawler.persistence.service",
        "org.ybak.crawler.downloader.chengdu12345.es",
        "org.ybak.crawler.web"
})

//@EnableJpaRepositories("org.ybak.crawler.persistence.repo")
//@EntityScan("org.ybak.crawler.persistence.vo")
public class WebApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(WebApplication.class, args);
    }

}