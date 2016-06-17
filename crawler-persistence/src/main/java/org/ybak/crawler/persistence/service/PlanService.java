package org.ybak.crawler.persistence.service;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.ybak.crawler.persistence.util.ElasticSearchUtil;
import org.ybak.crawler.persistence.util.PlanElasticSearchUtil;
import org.ybak.crawler.persistence.vo.Mail;
import org.ybak.crawler.persistence.vo.Plan;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by happy on 2016/5/22.
 */
@Component("planService")
public class PlanService {

//    @Autowired
//    private MailRepository mailRepository;

    private PlanElasticSearchUtil planEsUtil;

    @Value("${spring.data.elasticsearch.cluster-nodes}")
    private String nodes;

    @PostConstruct
    public void init() {
        planEsUtil = new PlanElasticSearchUtil(nodes);
    }

    public PageImpl<Map<String, Object>> search(String keyword, Pageable pageable) {
        SearchHits searchHits = planEsUtil.searchByKeyword("cdgh", new String[]{"title"}, keyword, pageable.getOffset(), pageable.getPageSize());
        List<Map<String, Object>> result = new ArrayList<>();

        for (SearchHit hit : searchHits.getHits()) {
            Map<String, Object> source = hit.getSource();
            source.put("id", hit.getId());
            result.add(source);
        }

        return new PageImpl(result, pageable, searchHits.getTotalHits());
    }

    public void save(List<Plan> plans) {
        planEsUtil.indexPlans(plans);
    }

    public Plan queryPlanByURL(String url) {
        SearchHits searchHits = planEsUtil.searchByURL(url);
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            Map<String, Object> source = hit.getSource();
            Plan plan = new Plan();
            plan.url = (String) source.get("url");
            plan.title = (String) source.get("title");
            plan.id = hit.getId();
            return plan;
        }
        return null;
    }

    public void initIndexIfAbsent() {
        planEsUtil.createIndexIfAbsend();
    }
}
