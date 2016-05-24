package org.ybak.crawler.downloader.util;

import com.alibaba.fastjson.JSON;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import static org.elasticsearch.index.query.QueryBuilders.*;

import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.ybak.crawler.persistence.vo.Mail;

import java.io.IOException;
import java.net.InetAddress;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Created by isaac on 16/5/23.
 */
public class ElasticSearchUtil {

    public static Client client;

    static {
        try {
            client = TransportClient.builder().build()
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        SearchRequestBuilder request = client.prepareSearch("chengdu12345")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(multiMatchQuery("红牌楼", "title", "content", "result").type(MatchQueryBuilder.Type.PHRASE)) // Query
                .addSort("createDate", SortOrder.DESC)
                .setFrom(0).setSize(60).setExplain(true);
        System.out.println(request.toString());
        SearchResponse response = request.execute().actionGet();
        System.out.println(response.getHits().totalHits());
    }

    public static void indexMails(Iterable<Mail> mails) {
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        for (Mail mail : mails) {
            addMailIndexRequest(bulkRequest, mail);
        }
        BulkResponse bulkResponse = bulkRequest.get();
        System.out.println(JSON.toJSONString(bulkResponse));
    }

    private static void addMailIndexRequest(BulkRequestBuilder bulkRequest, Mail mail) {
        try {
            bulkRequest.add(client.prepareIndex("chengdu12345", "mail", "" + mail.id)
                            .setSource(jsonBuilder()
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
}
