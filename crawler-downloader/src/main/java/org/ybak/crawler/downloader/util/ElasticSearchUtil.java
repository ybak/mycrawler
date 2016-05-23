package org.ybak.crawler.downloader.util;

import com.alibaba.fastjson.JSON;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilders;
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
        SearchResponse response = client.prepareSearch("twitter")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.termQuery("content", "红牌楼"))                 // Query
//                .setPostFilter(QueryBuilders.rangeQuery("age").from(12).to(18))     // Filter
                .setFrom(0).setSize(60).setExplain(true)
                .execute()
                .actionGet();
        System.out.println(JSON.toJSONString(response));
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
            bulkRequest.add(client.prepareIndex("twitter", "tweet", ""+mail.id)
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
