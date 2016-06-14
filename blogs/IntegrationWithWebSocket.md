#使用WebSocket实现增量抓取进度进度条的展示

自从上次全量抓取完所有市长信箱的所有邮件后, 过去了一个多星期,期间又有了很多新的信件产生. 如何抓取这些新邮件呢? 需要开发一个增量抓取的功能才能解决更新邮件的问题. 
我这次把增量抓取的按钮放到页面上,取名为"同步所有邮件".
![图片描述][1]
并在开始抓取后,页面上展示出当前增量抓取的进度.
![图片描述][2]

增量抓取的时间往往会超过10分钟,一般从页面获取当前抓取进度,有两种方式:
 1. 向服务端轮询
 2. 服务端推送
轮询会存在一定的延时, 为了增加通知的实时性,我采用了服务器推送的方式来更新页面的进度. 
Spring4增加了对WebSocket的支持, 并使用[Stomp][3]作为WebSocket之上的子协议, 并提供了stomp.js这样的前端类库, 极大的简化了前后端代码. 所以这里我使用spring-websocket来实现WebSocket. 

虽然websocket能实现浏览器和服务端的双工通信,但使用websocket编程时还是不太方便,和socket编程一样,我们需要编写连接的建立,释放和超时等各种处理逻辑.而我们真正关心的只是处理发布订阅这样的业务逻辑. Spring的Websocket模块的出现正好解决了这个痛点,它屏蔽了处理这些底层的繁杂逻辑,让我们只关心发布订阅相关的业务逻辑.
通过使用stomp协议,我们在前端编写订阅抓取进度通知的代码, 在后端编写发布抓取进度的代码就OK了.

###使用spring-websocket实现浏览器和服务端的发布订阅模式

####在工程中引入spring-websocket
#####添加maven依赖
    ```xml
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-messaging</artifactId>
    </dependency>
    ```

#####在配置代码中声明Spring的websocket的全局配置
需要实现以下逻辑:
 - 开启Websocket功能
 - 定义ws连接入口

    ```java
    @EnableWebSocketMessageBroker//开启Websocket功能
    public class WebApplication extends AbstractWebSocketMessageBrokerConfigurer {
    
        public static void main(String[] args) throws Exception {
            SpringApplication.run(WebApplication.class, args);
        }
    
        @Override
        public void configureMessageBroker(MessageBrokerRegistry config) {
            config.enableSimpleBroker("/topic");
            config.setApplicationDestinationPrefixes("/app");
        }
    
        @Override
        public void registerStompEndpoints(StompEndpointRegistry registry) {
            registry.addEndpoint("/ws").withSockJS();//定义ws连接入口
        }
    
        @Bean
        StringRedisTemplate template(RedisConnectionFactory connectionFactory) {
            return new StringRedisTemplate(connectionFactory);
        }
    }
    ```

####后端编写发布抓取进度的Java代码
#####添加服务端的处理逻辑
需要实现以下逻辑:
 - 定义接收客户端发送开始抓取的消息的入口
 - 多线程增量抓取市长信箱
 - 更新邮件的同时,向客户端发送更新进度

    ```java
     @MessageMapping("/craw/start") //定义接收客户端发送开始抓取的消息的入口
        public String increaseCraw(String jobId) throws Exception {
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "1");
            if (locked) {
                redisTemplate.expire(LOCK_KEY, 1, TimeUnit.MINUTES);
            } else {
                return "locked";
            }
    
            new Thread() {
                @Override
                public void run() {
                    mailService.initIndexIfAbsent();
                    BoundedExecutor executor = new BoundedExecutor(5);
                    AtomicBoolean shouldContinue = new AtomicBoolean(true);
                    CrawProgress progress = new CrawProgress(500);
    
                    for (int i = 0; i < 500; i++) {//最大1000页邮件
                        int page = i;
    
                        executor.submitTask(() -> { //多线程增量抓取市长信箱
                            shouldContinue.set(mailCrawler.updatePage(page + 1));
                            progress.current = page;
                            msgTemplate.convertAndSend("/topic/progress/" + jobId, progress); //向客户端发送更新进度
                        });
                    }
                    progress.current = 500;
                    msgTemplate.convertAndSend("/topic/progress/" + jobId, progress);
                }
            }.start();
    
            return "ok";
        }
    ```

####前端编订阅布抓取进度的JavaScript代码
#####引入Spring提供的前端类库
    ```html
        <script src="sockjs-0.3.4.js"></script> //支持websocket的类库
        <script src="stomp.js"></script> //支持stomp的类库
        <script src="index.js"></script> //业务逻辑代码
    ```
    
#####编写前端业务逻辑
需要编写以下逻辑:
 - 根据之前服务端声明的ws入口,建立ws连接
 - 初始化stomp客户端
 - 建立ws后的业务代码
 - 创建事件监听回调逻辑,收到进度通知后,更新页面
 - 向服务端发送开始抓取的请求

    ```javascript
    function connect() {
        var socket = new SockJS('/ws'), //根据之前服务端声明的ws入口,建立ws连接
            stompClient = Stomp.over(socket); //初始化stomp客户端
        stompClient.connect({}, function (frame) { //建立ws后的业务代码
            var jobId = Date.now();
            stompClient.subscribe('/topic/progress/' + jobId, function (msg) { //创建事件监听回调逻辑,收到进度通知后,更新页面
                console.log('Msg Received: ' + msg);
                var body = JSON.parse(msg.body);
                $('#progressModal .progress-bar').width(body.progress + '%');
                if (body.progress >= 100) {
                    $('#progressModal').modal('hide');
                }
            });
            stompClient.send("/app/craw/start", {}, jobId); //向服务端发送开始抓取的请求
        });
    }
    
    ```

通过这些代码, 我们就能实现服务端向浏览器不断的发送抓取进度的通知了.

 


  [1]: https://sfault-image.b0.upaiyun.com/259/259/2592592253-575fa4bcc0c2b_articlex
  [2]: https://sfault-image.b0.upaiyun.com/131/340/1313400573-575fa50ae710f_articlex
  [3]: http://jmesnil.net/stomp-websocket/doc/