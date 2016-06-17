package org.ybak.crawler.persistence.util;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ybak.crawler.persistence.vo.Mail;
import org.ybak.crawler.persistence.vo.Plan;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by isaac on 16/5/23.
 */
public class PlanElasticSearchUtil {

    private static final Logger logger = LoggerFactory.getLogger(PlanElasticSearchUtil.class);

    public TransportClient client;

    public PlanElasticSearchUtil(String nodesStr) {
        if (nodesStr != null) {
            init(nodesStr);
        } else {
            init("localhost:9300");
        }
    }

    private void init(String nodesStr) {
        String[] split = nodesStr.split(";");
        try {

            client = TransportClient.builder().build();
            for (String s : split) {
                String[] node = s.split(":");
                client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(node[0]), Integer.valueOf(node[1])));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public SearchHits searchByKeyword(String index, String[] fields, String keyword, int from, int size) {
        SearchRequestBuilder request = client.prepareSearch(index)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
        if (StringUtils.isNotEmpty(keyword)) {
            request.setQuery(QueryBuilders.multiMatchQuery(keyword, fields)
                    .type(MatchQueryBuilder.Type.PHRASE));//完全匹配
        }
        request.addSort("createDate", SortOrder.DESC)
                .setFrom(from).setSize(size).setExplain(true);

        SearchResponse response = request.execute().actionGet();
        return response.getHits();
    }

    public SearchHits searchByURL(String url) {
        SearchRequestBuilder request = client.prepareSearch("cdgh")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.matchPhraseQuery("url", url))
                .addSort("createDate", SortOrder.DESC);

        SearchResponse response = request.execute().actionGet();
        return response.getHits();
    }

    public void indexPlans(Iterable<Plan> plans) {
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        for (Plan plan : plans) {
            addPlanIndexRequest(bulkRequest, plan);
        }
        BulkResponse bulkResponse = bulkRequest.get();
        logger.info(JSON.toJSONString(bulkResponse));
    }

    private void addPlanIndexRequest(BulkRequestBuilder bulkRequest, Plan plan) {
        try {
            bulkRequest.add(client.prepareIndex("cdgh", "plan")
                            .setSource(XContentFactory.jsonBuilder()
                                    .startObject()
                                    .field("createDate", plan.createDate)
                                    .field("title", plan.title)
                                    .field("url", plan.url)
                                    .endObject())
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateMail(Mail mail) {
        try {
            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.index("cdgh");
            updateRequest.type("mail");
            updateRequest.id(mail.id);

            updateRequest.doc(XContentFactory.jsonBuilder()
                    .startObject()
                    .field("result", mail.result)
//                    .field("status", mail.status)
                    .endObject());

            UpdateResponse updateResponse = client.update(updateRequest).get();
            System.out.println(JSON.toJSONString(updateResponse));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void createIndexIfAbsend() {
        try {
            searchByKeyword("cdgh", new String[]{"title"}, "test", 0, 1);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            Plan plan = new Plan("url", "title", DateUtils.addYears(new Date(), -10));
            indexPlans(Arrays.asList(plan));
        }
    }
}
