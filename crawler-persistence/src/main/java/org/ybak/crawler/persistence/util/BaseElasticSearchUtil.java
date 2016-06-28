package org.ybak.crawler.persistence.util;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;

import java.net.InetAddress;

/**
 * Created by ybak on 16/6/23.
 */
public class BaseElasticSearchUtil {
    public TransportClient client;

    public BaseElasticSearchUtil(String nodesStr) {
        if (nodesStr != null) {
            init(nodesStr);
        } else {
            init("localhost:9300");
        }
    }

    protected void init(String nodesStr) {
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
}
