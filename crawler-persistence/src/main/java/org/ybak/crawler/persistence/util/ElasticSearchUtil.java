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

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by isaac on 16/5/23.
 */
public class ElasticSearchUtil extends BaseElasticSearchUtil {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchUtil.class);

    public TransportClient client;

    public ElasticSearchUtil(String nodesStr) {
        super(nodesStr);
    }

    public static void main(String[] args) throws Exception {
        int size = 20;
        String keyword = "红牌楼";
        SearchHits result = new ElasticSearchUtil(null).searchByKeyword("chengdu12345", new String[]{"title", "content", "result"}, keyword, 0, 20);
        System.out.println(result);
    }

    public SearchHits searchByURL(String url) {
        SearchRequestBuilder request = client.prepareSearch("chengdu12345")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.matchPhraseQuery("url", url))
                .addSort("createDate", SortOrder.DESC);

        SearchResponse response = request.execute().actionGet();
        return response.getHits();
    }

    public void indexMails(Iterable<Mail> mails) {
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        for (Mail mail : mails) {
            addMailIndexRequest(bulkRequest, mail);
        }
        BulkResponse bulkResponse = bulkRequest.get();
        System.out.println(JSON.toJSONString(bulkResponse));
    }

    private void addMailIndexRequest(BulkRequestBuilder bulkRequest, Mail mail) {
        try {
            bulkRequest.add(client.prepareIndex("chengdu12345", "mail")
                    .setSource(XContentFactory.jsonBuilder()
                            .startObject()
                            .field("content", mail.content)
                            .field("createDate", mail.createDate)
                            .field("acceptUnit", mail.acceptUnit)
                            .field("category", mail.category)
                            .field("result", mail.result)
                            .field("sender", mail.sender)
                            .field("status", mail.status)
                            .field("title", mail.title)
                            .field("url", mail.url)
                            .field("views", mail.views)
                            .endObject())
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateMail(Mail mail) {
        try {
            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.index("chengdu12345");
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
            searchByKeyword("chengdu12345", new String[]{"title", "content", "result"}, "test", 0, 1);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            Mail mail = new Mail("url", "sender", "title", "unit", "status", "category", 1, DateUtils.addYears(new Date(), -10), "content", "result");
            indexMails(Arrays.asList(mail));
        }
    }
}
