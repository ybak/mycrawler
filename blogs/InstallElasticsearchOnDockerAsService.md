# 市长信箱邮件查询服务: 在Docker上安装Elasticsearch作为一个服务

在上一章,我完成了Elasticsearch替换Mysql的工作. 按照之前的计划, 现在是该把ES迁移到Docker的时候了.

为什么要迁移到Docker? 
  1. 为后续展示Elasticsearch的弹性做准备, 使用docker可以方便的部署多节点.
  2. docker很火.
   
-------------------

我开发机是Mac Pro, 要在mac上使用docker, 需要安装[docker-machine](https://docs.docker.com/machine/overview/).

``` shell
docker-machine start
eval "$(docker-machine env default)"
docker run -d -p 9200:9200 -p 9300:9300 -v "$home/.esdata/node1":/usr/share/elasticsearch/data elasticsearch:2.3.3 -Des.node.name="node1"
```
说明:
-d 后台运行
-p 宿主机端口:docker容器端口
-v "本地挂载路径":docker容器目录路径
-Des.node.name es节点名称

参见: 
https://github.com/docker-library/docs/tree/master/elasticsearch
https://www.elastic.co/blog/docker-networking

尝试使用docker-compose组合集群.
http://www.tuicool.com/articles/AnIVJn


eval "$(docker-machine env default)"

docker run -p 9200:9200 -p 9300:9300 -v "$home/.esdata/node1":/usr/share/elasticsearch/data elasticsearch:2.3.3 -Des.node.name="node1" -Des.discovery.zen.ping.unicast.hosts=192.168.99.100

docker run -p 9201:9200 -p 9301:9300 -v "$home/.esdata/node2":/usr/share/elasticsearch/data elasticsearch:2.3.3 -Des.node.name="node2" -Des.discovery.zen.ping.unicast.hosts=192.168.99.100
