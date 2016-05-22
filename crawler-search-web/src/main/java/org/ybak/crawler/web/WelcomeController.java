package org.ybak.crawler.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ybak.crawler.persistence.service.MailService;
import org.ybak.crawler.persistence.vo.Mail;

import java.util.Map;

@Controller
public class WelcomeController {

    @Value("${application.message:Hello World}")
    private String message = "Hello World";

    @Autowired
    private MailService mailService;

    @RequestMapping("/")
    public String welcome(Map<String, Object> model) {
        return "welcome";
    }


    @RequestMapping("/search")
    @ResponseBody
    public Page<Mail> search(String keyword) {
        Pageable query = new PageRequest(0, 100);
        return mailService.search(keyword, query);
    }

}