package org.ybak.crawler.persistence.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;
import org.ybak.crawler.persistence.vo.Mail;

public interface MailRepository extends Repository<Mail, Long> {

    Page<Mail> search(String keyword, Pageable pageable);

    Page<Mail> findAll(Pageable page);
}