package org.ybak.crawler.persistence.service;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.ybak.crawler.persistence.repo.MailRepository;
import org.ybak.crawler.persistence.util.ElasticSearchUtil;
import org.ybak.crawler.persistence.vo.Mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by happy on 2016/5/22.
 */
@Component("mailService")
@Transactional
public class MailService {

    @Autowired
    private MailRepository mailRepository;

    public PageImpl<Map<String, Object>> search(String keyword, Pageable pageable) {
        SearchHits searchHits = ElasticSearchUtil.searchByKeyword(20, keyword);
        List<Map<String, Object>> result = new ArrayList<>();

        for (SearchHit hit : searchHits.getHits()) {
            result.add(hit.getSource());
        }

        return new PageImpl<Map<String, Object>>(result, pageable, searchHits.getTotalHits());
    }
}
