# 市长信箱邮件查询服务: 使用Elasticsearch 替代 Mysql

我在上一篇文章中实现一个基于Springboot构建的web应用: 市长信箱邮件查询服务. 应用将邮件信息抓取后,将数据保存在Mysql中,用以提供给搜索Web使用.
使用mysql能快速实现功能, 但使用like查询性能上不太好, 数据量大了就必须考虑使用搜索引擎了. 这次我们把Mysql替换为Elasticsearch.

Elasticsearch, 提供了两种API来



``` json
{
  "query": {
    "multi_match": {
      "query": "红牌楼",
      "fields": [
        "content",
        "result",
        "title"
      ],
      "type": "phrase"
    }
  },
  "sort": [
    {
      "createDate": {
        "order": "desc"
      }
    }
  ],
  "size": 100
}
``` 