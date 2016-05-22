# SpringBoot 开发小记

一直想用SpringBoot做个微服务,练练手, 为后续部署到docker打下基础. 今天比较空闲, 就开始把部分想法落地了.
https://github.com/ybak/mycrawler

-------------------

## 概览

用来练手的demo是一个市长信箱的内容抓取与检索页面. 鉴于我的八卦特质,总想了解下周边的一些投诉信息. 而成都的[市长信箱](http://12345.chengdu.gov.cn)是一个绝好的信息来源.
#### 信件格式:
来信情况 |   张三
:------ | ---
来信标题|	生活困扰
来信内容| 尊敬市长你好我们有十三户污水到我处无法排走，使我处蚊虫飞舞，臭气难问，而且渗透到地下，我们用钢管井抽水吃，水受到污染无法引用，望市长为我们百姓作做，我们是移民到在里无衣无靠只有靠政府。
办理结果| 郫县(2016-05-20 11:31:10)： 来信人： 您好！ 来信收悉。感谢您对政府工作的关心与支持。您所反映的问题回复如下： 针对您反映的问题，4月26日，唐元镇驻福昌村包片领导、走村干部联系县移民办工作人员同问题反映人一起到现场进行了实地查看，并根据实际，提出处理意见：对流经蔡联有家后面的污水沟进行整治，出口并入化粪池，并将化粪池进行清淘。其所有工程，严格按照移民办的要求程序进行。 欢迎多提宝贵意见！ 此复。 郫县县长公开电话办公室 2016年5月20日

这个demo的主要功能有:

 1. 从市长信箱抓取所有的市民投诉并保存
 2. 提供按关键字检索的web页面来检索感兴趣的投诉信息

按照循序渐进的原则, 先实现只实现基本功能, 不考虑性能, 后续再进行优化. 
Mysql的提供了基本的模糊匹配功能, 且SpringBoot中,能方便的集成JPA. 
使用Mysql保存抓取信息, 并提供给Web应用查询, 是很容易实现的. 所以该demo的第一版技术设计如下:
![这里写图片描述](http://img.blog.csdn.net/20160522222805031)
SpringBoot的代码使用maven的多模块组织:

crawler-downloader:抓取模块
: pom.xml

crawler-persistence:存储模块
: pom.xml
``` xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```
crawler-search-web:页面模块
: pom.xml
``` xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```
pom.xml
``` xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>1.3.5.RELEASE</version>
</parent>
```

接下来,分别介绍各个模块的细节:

-------------------
### crawler-persistence:存储模块

根据信件内容的格式,设计存储信件的表结构如下:
![这里写图片描述](http://img.blog.csdn.net/20160522225147687)

#### 抓取邮件信息是的DB操作
这里我使用的[rxjava-jdbc](https://github.com/davidmoten/rxjava-jdbc)来进行数据的插入.相比JPA, [rxjava-jdbc](https://github.com/davidmoten/rxjava-jdbc)如果做基础的查询和插入操作使用起来很方便.
``` java
// 查询邮件详情url
Iterable<Tuple2<Integer, String>> results = db
.select("select id, url from chengdu12345 limit ?,?").parameters(i * 50, 50)
.getAs(Integer.class, String.class).toBlocking().toIterable();
//插入邮件记录
int updates = db.update("insert into chengdu12345(url, title, sender, accept_unit, status, category, views, create_date) values (?,?,?,?,?,?,?,?)")
.parameters(url, title, sender, receiveUnit, status, category, views, publishDate)
.execute();
```
#### WEB展示的DB操作
查询数据库的邮件信息时, 会涉及到分页, 模糊匹配, 这个时候rxjava-jdbc显的有些力不从心了. 而spring-data的大量的模板方法,会让查询代码简化. 所以这里我使用了spring-data-jpa的方式来进行查询.

``` java
@Table(name = "chengdu12345")
@NamedQuery(name = "Mail.search",
        query = "select m from Mail m where m.title like ?1 or m.content like ?1 or m.result like ?1")
public class Mail implements Serializable {...}
......
public interface MailRepository extends Repository<Mail, Long> {
    Page<Mail> search(String keyword, Pageable pageable);
}
......
public class MailService {
    @Autowired
    private MailRepository mailRepository;

    public Page<Mail> search(String keyword, Pageable pageable) {
        return mailRepository.search("%" + keyword + "%", pageable);
    }
}
```
MailService 的search方法,只需传入Pageable 实例, spring-data将自动为我们处理好分页的逻辑, 非常方便.

-------------------
### crawler-downloader:抓取模块

[市长信箱](http://12345.chengdu.gov.cn)的邮件展示列表中只有邮件标题和邮件详情链接等基础信息,没有邮件正文和处理结果详情. 我的抓取流程是:

1. 遍历所有的邮件列表的分页信息, 将邮件基础信息保存到数据库.
2. 遍历数据库中的所有已保存的邮件基础信息, 取出邮件详情链接, 再对该链接进行抓取, 取得内容进行分析并保存到数据库中.
 
我用来抓取页面的http客户端类库是[okhttp](https://github.com/square/okhttp),
okhttp不但提供了简洁的api, 还在内部建立了url连接池, 在快速抓取页面时, 减少了tcp链接的建立, 提高了速度, 也降低了抓取失败的几率.
``` java
public class HtmlUtil {
    static OkHttpClient client = new OkHttpClient();
    public static String getURLBody(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            response.body().close();
            throw new IllegalArgumentException(response.message());
        }
        return response.body().string();
    }
}
```
页面解析的列库我使用了[Jsoup](http://jsoup.org), Jsoup也可以直接用来抓取页面. 但它没有提供易用的连接池机制. 默认每次抓取都会创建tcp连接. 在快速抓取页面的情况下很容易打开过多的端口,从而造成抓取失败. 但Jsoup的html解析api却是相当的强大. 尤其它的对css selector的支持, 选取dom就像使用jquery一样方便.   
``` java
String html = HtmlUtil.getURLBody(pageUrl);
Document doc = Jsoup.parse(html);
Elements elements = doc.select("div.left5 ul li.f12px");
for (Element element : elements) {
    String url = urlPrefix + element.select("css").attr("href");
......
}
```
-------------------

### crawler-search-web:页面模块


-------------------

### 踩过的坑


##总结