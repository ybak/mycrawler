package org.ybak.crawler.downloader.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.ybak.crawler.persistence.repo.MailRepository;
import org.ybak.crawler.persistence.vo.Mail;

import javax.annotation.PostConstruct;

@SpringBootApplication
@EnableJpaRepositories("org.ybak.crawler.persistence.repo")
@EntityScan("org.ybak.crawler.persistence.vo")
public class QuickTest {

    @Autowired
    MailRepository repo;

    @PostConstruct
    public void init(){
        int page1 = 0;

        Page<Mail> all = processPageMail(page1);
        int totalPages = all.getTotalPages();
        for (int i = 1; i < totalPages; i++) {
            processPageMail(i);
        }
    }

    private Page<Mail> processPageMail(int page) {
        Pageable query = new PageRequest(page, 100);
        Page<Mail> mails = repo.findAll(query);
        ElasticSearchUtil.indexMails(mails);
        return mails;
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(QuickTest.class, args);

    }

}