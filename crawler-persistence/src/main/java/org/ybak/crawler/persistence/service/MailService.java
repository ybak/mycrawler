package org.ybak.crawler.persistence.service;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.ybak.crawler.persistence.repo.MailRepository;
import org.ybak.crawler.persistence.util.ElasticSearchUtil;
import org.ybak.crawler.persistence.vo.Mail;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by happy on 2016/5/22.
 */
@Component("mailService")
public class MailService {

//    @Autowired
//    private MailRepository mailRepository;

    private ElasticSearchUtil elasticSearchUtil;

    @Value("${spring.data.elasticsearch.cluster-nodes}")
    private String nodes;

    @PostConstruct
    public void init() {
        elasticSearchUtil = new ElasticSearchUtil(nodes);
    }

    public PageImpl<Map<String, Object>> search(String keyword, Pageable pageable) {
        SearchHits searchHits = elasticSearchUtil.searchByKeyword(keyword, pageable.getOffset(), pageable.getPageSize());
        List<Map<String, Object>> result = new ArrayList<>();

        for (SearchHit hit : searchHits.getHits()) {
            Map<String, Object> source = hit.getSource();
            source.put("id", hit.getId());
            result.add(source);
        }

        return new PageImpl<Map<String, Object>>(result, pageable, searchHits.getTotalHits());
    }

    public void save(List<Mail> mails) {
        elasticSearchUtil.indexMails(mails);
    }

    public void update(Mail mail) {
        elasticSearchUtil.updateMail(mail);
    }
}
