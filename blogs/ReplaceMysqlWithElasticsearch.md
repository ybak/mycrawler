# 市长信箱邮件查询服务: 使用Elasticsearch 替代 Mysql

我在上一篇文章中实现了一个基于Springboot构建的web应用: 市长信箱邮件查询服务. 应用将邮件信息抓取后保存在Mysql中,用以提供给搜索Web使用.Mysql虽然集成简单，能快速实现功能, 但like查询性能一般, 尤其数据量大了之后就必须考虑使用搜索引擎. 所以这次我把存储从Mysql替换为Elasticsearch(ES).

----------------------------------------------------------------------------------------------------------------------------------------------------------------------

Elasticsearch提供了两种方式API来进行调用: Rest API与Java Native. Java Native的执行效率更高, 并且与当前项目集成更方便，所以我这里选择了Java native Api. 引入Java native Api,只需要在根pom.xml添加依赖:
``` xml
<dependency>
    <groupId>org.elasticsearch</groupId>
    <artifactId>elasticsearch</artifactId>
    <version>2.3.3</version>
</dependency>
``` 

使用ES替换Mysql,需要考虑这两方面:
 1. 存储: 将Mysql的insert插入数据改为ES的添加文档操作.
 2. 查询: 替换Mysql的查询sql语句,改为ES的搜索操作.

## 存储:
  由于我们的邮件结构简单，没有内嵌其他复杂对象, 所以从mysql转换为ES的文档非常自然. 只需调用ES的添加文档API即可:
``` java
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
```
在以上代码中,我使用了批量添加(bulkRequest)API, 以减少API调用次数.

## 查询: 
  之前Mysql中邮件的查询sql如下:
``` sql
select m from cheng12345 m where m.title like ? or m.content like ? or m.result like ? order by create_date desc limit ?,?
```
  用ES来实现SQL对应效果的代码也非常简单:
``` java
public static SearchHits searchByKeyword(String keyword, int from, int size) {
    SearchRequestBuilder request = client.prepareSearch("chengdu12345")
            .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
            .setQuery(QueryBuilders.multiMatchQuery(keyword, "title", "content", "result")
            .type(MatchQueryBuilder.Type.PHRASE))//完全匹配
            .addSort("createDate", SortOrder.DESC)
            .setFrom(from).setSize(size).setExplain(true);

    SearchResponse response = request.execute().actionGet();
    return response.getHits();
}
```  
  这里需要注意的是,查询类型我选择的是短语查询(Type.PHRASE), 它能保证搜索时输入的短语不被拆分, 否则我输入"红牌楼"查询时,可能返回一堆包含"红楼"的邮件列表, 这明显不是我想要的结果.
  
  另外, Springdata同样提供了对ES的[支持](http://docs.spring.io/spring-data/elasticsearch/docs/current/reference/html/), 它像对JPA一样提供了基于Repository模板方法支持,利用它能简化不少对ES操作的代码.
