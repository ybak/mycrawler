package org.ybak.crawler.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "org.ybak.crawler.persistence.service",
        "org.ybak.crawler.web"
})
@EnableJpaRepositories("org.ybak.crawler.persistence.repo")
@EntityScan("org.ybak.crawler.persistence.vo")
public class WebApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(WebApplication.class, args);
    }

}