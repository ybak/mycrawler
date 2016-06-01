# 市长信箱邮件查询服务

鉴于我的八卦特质,总想了解下周边的一些投诉信息. 而成都的[市长信箱](http://12345.chengdu.gov.cn)是一个绝好的信息来源.所以我构建了这个应用, 提供市长信箱的内容抓取与检索页面. 

该应用使用SpringBoot构建, 一些服务的部署依赖于Docker.

-------------------

## 概览

#### 信件格式:
来信情况 |   张三
:------ | :---
来信标题|	 生活困扰
来信内容| 尊敬市长你好我们有十三户污水到我处无法排走，使我处蚊虫飞舞，...
办理结果| 郫县(2016-05-20 11:31:10)： 来信人： 您好！ 来信收悉。...

#### JPA版架构:
![这里写图片描述](http://img.blog.csdn.net/20160522222805031)

#### ES版架构:
![这里写图片描述](http://img.blog.csdn.net/20160523103600807)

#### SpringBoot代码多模块组织:
父模块(声明此工程的spring-boot的版本)
pom.xml
``` xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>1.3.5.RELEASE</version>
</parent>
```
crawler-downloader:抓取模块
: pom.xml

crawler-persistence:存储模块(使用spring的jpa实现orm)
: pom.xml
``` xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```
crawler-search-web:页面模块(使用spring的thymeleaf实现mvc和rest api)
: pom.xml
``` xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

接下来,分别介绍各个模块的细节:

-------------------
### crawler-persistence:存储模块

根据信件内容的格式,设计存储信件的表结构如下:
![这里写图片描述](http://img.blog.csdn.net/20160522225147687)

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

 . 该模块功能非常简单:

1. 提供一个静态页面
2. 提供一个搜索API

这里使用Spring MVC来提供实现:
``` java
@Controller
public class WelcomeController {
    @Autowired
    private MailService mailService;
    @RequestMapping("/")
    public String welcome() {
        return "welcome";//提供静态页面
    }
    @RequestMapping("/search")
    @ResponseBody
    public Page<Mail> search(String keyword) {
        Pageable query = new PageRequest(0, 100);//提供一个搜索API
        return mailService.search(keyword, query);
    }
}
```
有了Controller和页面, 剩下的工作就是利用Spring Boot来启动工程了. 使用Spring Boot启动应用非常方便, 只需几行代码:
``` java
@SpringBootApplication(scanBasePackages = {
        "org.ybak.crawler.persistence.service",
        "org.ybak.crawler.web"
})
@EnableJpaRepositories("org.ybak.crawler.persistence.repo")
@EntityScan("org.ybak.crawler.persistence.vo")
public class WebApplication {
    public static void main(String[] args) throws Exception {
        SpringApplication.run(WebApplication.class, args);
    }
}
```
有了Spring Boot的启动类, 只用运行main就可以启动应用了. 最后的页面是这样的:
![这里写图片描述](http://img.blog.csdn.net/20160523095022471)