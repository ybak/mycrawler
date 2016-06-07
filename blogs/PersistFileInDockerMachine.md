#解决Docker Machine中的容器文件不能持久的问题


目前Mac下使用docker的主流方式还是使用Docker machine. 
Docker machine需要借用Virtual Box虚拟器启动一个Linux宿主机, 才能在上面启动多个Docker容器.

当我们在docker容器中运行服务时,经常会有需要将服务数据持久化的场景.比如运行Elasticsearch集群时, 需要将索引数据持久保存到磁盘. 

那具体保存到哪里呢?容器的无状态特性决定了我们不应该将数据保存在容器中, 因为容器一旦重启, 文件数据就会丢失.一般应该使用volume参数,通过挂载外部文件系统到Docker容器中来保存数据.

当我在Mac下使用Docker开发时, 遇到这种场景, 自然就想到了将服务数据保存到Linux宿主机上. 结果很不辛, 使用Docker Machine创建的Linux宿主机的文件系统几乎也是不能持久的, Linux重启后, 之前写入的文件都会丢失. 

不过好在使用Docker Machine创建的Linux宿主机上面,自动挂载了Mac上的用户目录到Linux宿主机上. 如下:
```
Linux: /Users   --->   Mac  : /Users
```
我们挂载文件系统时, 将应用的数据保存到/Users不久可以了吗?

然而现实还是那么残酷. 当我使用按照这个方式启动Elasticsearch时:
``` shell
docker run -v "/Users/isaac/work/data":/usr/share/elasticsearch/data elasticsearch:2.3.3
```
却得到了以下错误:
``` java
Exception in thread "main" java.lang.IllegalStateException: Unable to access 'path.data' (/usr/share/elasticsearch/data/elasticsearch)
Likely root cause: java.nio.file.AccessDeniedException: /usr/share/elasticsearch/data/elasticsearch
	at sun.nio.fs.UnixException.translateToIOException(UnixException.java:84)
	at sun.nio.fs.UnixException.rethrowAsIOException(UnixException.java:102)
	at sun.nio.fs.UnixException.rethrowAsIOException(UnixException.java:107)
	at sun.nio.fs.UnixFileSystemProvider.createDirectory(UnixFileSystemProvider.java:384)
	at java.nio.file.Files.createDirectory(Files.java:674)
	at java.nio.file.Files.createAndCheckIsDirectory(Files.java:781)
	at java.nio.file.Files.createDirectories(Files.java:767)
	at org.elasticsearch.bootstrap.Security.ensureDirectoryExists(Security.java:337)
	at org.elasticsearch.bootstrap.Security.addPath(Security.java:314)
	at org.elasticsearch.bootstrap.Security.addFilePermissions(Security.java:259)
	at org.elasticsearch.bootstrap.Security.createPermissions(Security.java:212)
	at org.elasticsearch.bootstrap.Security.configure(Security.java:118)
	at org.elasticsearch.bootstrap.Bootstrap.setupSecurity(Bootstrap.java:196)
	at org.elasticsearch.bootstrap.Bootstrap.setup(Bootstrap.java:167)
	at org.elasticsearch.bootstrap.Bootstrap.init(Bootstrap.java:270)
	at org.elasticsearch.bootstrap.Elasticsearch.main(Elasticsearch.java:35)

```

经过分析, 文件操作失败是因为用户权限的问题.
查看Linux宿主机的用户信息:
``` shell
docker-machine ssh default
cat /etc/passwd
```
得到以下内容:

```
root:x:0:0:root:/root:/bin/sh
lp:x:7:7:lp:/var/spool/lpd:/bin/sh
nobody:x:65534:65534:nobody:/nonexistent:/bin/false
tc:x:1001:50:Linux User,,,:/home/tc:/bin/sh
docker:x:1000:50:Linux User,,,:/home/docker:/bin/sh
```

所以通过指定启动Docker容器的用户, 可以解决此问题:
``` shell
docker run -u 1000 -v "/Users/isaac/work/data":/usr/share/elasticsearch/data elasticsearch:2.3.3 
```
其中:-u 1000 代表使用id为1000的docker用户来启动应用

在docker-compose.yml中指定启动用户为docker用户:
``` yml
elasticsearch1:
    image: elasticsearch:2.3.3
    command: "elasticsearch -Des.cluster.name=elasticsearch -Des.discovery.zen.ping.unicast.hosts=elasticsearch_master"
    links:
      - elasticsearch_master
    volumes:
      - /Users/isaac/work/data:/usr/share/elasticsearch/data
    user: "1000"

```

ok,启动服务后, 数据终于持久不丢失了.
