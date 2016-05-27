# 市长信箱邮件查询服务: 在Docker上安装Elasticsearch作为一个服务

在上一章,我完成了Elasticsearch替换Mysql的工作. 按照之前的计划, 现在是该把ES迁移到Docker的时候了.

为什么要迁移到Docker? 
  1. 为后续展示Elasticsearch的弹性做准备, 使用docker可以方便的部署多节点.
  2. docker很火.
   
-------------------

我开发机是Mac Pro, 要在mac上使用docker, 需要安装[docker-machine](https://docs.docker.com/machine/overview/).

docker-machine的原理就是在mac上安装一台Linux虚拟机(VisualBox),作为Docker容器的宿主机, 以此来在不同平台(Mac,Windows)支持docker容器的创建. 
安装完成后, 执行以下命令, 就可以运行docker了.
``` shell
docker-machine start
eval "$(docker-machine env default)"
```
通过执行:docker-machine ip 命令可以得到docker宿主机的ip. 后续访问ES服务将需要访问此IP, 一般为:192.168.99.100

Docker Hub官方有elasticsearch的镜像. 我们本地拉取运行即可
``` shell
docker run -p 9200:9200 -p 9300:9300 -v "$home/.esdata/node1":/usr/share/elasticsearch/data elasticsearch:2.3.3 -Des.node.name="node1"
```
参数说明:
-p 宿主机端口:docker容器端口
-v "本地挂载路径":docker容器目录路径
-Des.node.name es节点名称

这里使用
-p将docker容器内部ES服务端口(开发机不能直接访问)映射到docker宿主机端口上(开发机可以直接访问),也就是: 192.168.99.100:9200和192.168.99.100:9300
-v本地挂载路径映射的目的是为了持久化ES的数据, 否则一旦docker容器重启,ES中的数据将会丢失.

然后在浏览器访问: http://192.168.99.100:9200/_cluster/health?pretty=true
可以从中看到ES的状态, 其中"number_of_nodes"的值为1,说明ES集群只有一个节点.

使用Docker可以方便的将单节点ES扩展为多节点ES集群.

这里我使用[docker-compose](https://docs.docker.com/compose/overview/)来完成这个任务.
编写docker-compose.yml文件:
``` yml
elasticsearch_master:
    image: elasticsearch:2.3.3
    command: "elasticsearch -Des.cluster.name=elasticsearch -Des.node.master=true"
    ports:
      - "9200:9200"
      - "9300:9300"
    volumes:
      - /.esdata/node1:/usr/share/elasticsearch/data

elasticsearch1:
    image: elasticsearch:2.3.3
    command: "elasticsearch -Des.cluster.name=elasticsearch -Des.discovery.zen.ping.unicast.hosts=elasticsearch_master"
    links:
      - elasticsearch_master
    volumes:
      - /.esdata/node2:/usr/share/elasticsearch/data
      
elasticsearch2:
    image: elasticsearch:2.3.3
    command: "elasticsearch -Des.cluster.name=elasticsearch -Des.discovery.zen.ping.unicast.hosts=elasticsearch_master"
    links:
      - elasticsearch_master
    volumes:
      - /.esdata/node3:/usr/share/elasticsearch/data
```
参数说明:
-Des.discovery.zen.ping.unicast.hosts=elasticsearch_master 指定ES的集群发现机制为查询elasticsearch_master主节点.(多播发现机制不被推荐用在生产环境)

集群启动了3个ES节点, 其中一个为主节点, 对外暴露服务端口. 启动集群只需要在docker-compose.yml的当前目录执行"docker-compose up"即可.

参考资料:
http://www.tuicool.com/articles/AnIVJn
http://stackoverflow.com/questions/28632977/elasticsearch-in-docker-container-cluster
https://docs.docker.com/compose/overview/