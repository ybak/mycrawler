#  SpringBoot集成Docker上Redis服务

在我的应用中, 希望能使用一些redis的特性:比如zset这样的数据结构,如果能方便的在开发环境中使用起来就好了. 
如何集成呢? 这里依然使用Docker和SpringBoot来帮忙.
1. 通过使用docker, 我们就能快速的部署好redis服务. 
2. 而通过使用SpringBoot,我们能快速的把redis集成进我们的服务,并能使用Spring提供的模板方法,方便的调用redis的API.

### 使用Docker快速部署Redis服务
在docker-compose.yml添加以下配置即可:
```
redis_master:
    image: redis:3.2
    ports:
      - "6379:6379"
  
search_web:
    links:
      - redis_master
```
通过使用links,我们可以方便的另一docker容器中通过host方式调用redis容器提供的服务, 而不用绑定具体的ip.

### 使用SpringBoot集成Redis
这里继续使用注解的方式的声明Spring组件:
```java
@SpringBootApplication
public class Booter{
    @Bean
    StringRedisTemplate template(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
```
并在spring配置application.properties中添加服务地址:
```
spring.redis.host=192.168.99.100
```
然后就可以愉快的在服务中和redis玩耍了:
```java

@Autowired
private StringRedisTemplate redisTemplate;

Boolean locked = redisTemplate.opsForValue().setIfAbsent("lock_key", "1");
if (locked) {
    redisTemplate.expire("lock_key", 1, TimeUnit.MINUTES);
}

```

Enjoy.

参考:
[Spring Messaging-Redis Guide][1]
[DockerHub-Redis][2]

[1]: https://spring.io/guides/gs/messaging-redis/
[2]: https://hub.docker.com/_/redis/