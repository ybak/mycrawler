package org.ybak.crawler.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ybak.crawler.downloader.chengdu12345.es.MailCrawler;
import org.ybak.crawler.persistence.service.MailService;
import org.ybak.crawler.persistence.vo.Mail;

import java.util.Map;

@Controller
public class WelcomeController {

    @Autowired
    private MailService mailService;
    @Autowired
    private MailCrawler mailCrawler;

    @RequestMapping("/search")
    @ResponseBody
    public PageImpl<Map<String, Object>> search(String keyword) {
        Pageable query = new PageRequest(0, 100);
        return mailService.search(keyword, query);
    }

    @RequestMapping("/update")
    @ResponseBody
    public Mail update(String id, String url) {
        return mailCrawler.updateMail(id, url);
    }

    @MessageMapping("/hello")
    public String greeting() throws Exception {
        return "ok";
    }

}