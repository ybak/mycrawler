package org.ybak.crawler.persistence.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.ybak.crawler.persistence.repo.MailRepository;
import org.ybak.crawler.persistence.vo.Mail;

/**
 * Created by happy on 2016/5/22.
 */
@Component("mailService")
@Transactional
public class MailService {

    @Autowired
    private MailRepository mailRepository;

    public Page<Mail> search(String keyword, Pageable pageable) {
        return mailRepository.search("%" + keyword + "%", pageable);
    }
}
