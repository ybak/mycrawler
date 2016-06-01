# 市长信箱邮件查询服务: 将SpringBoot应用部署到Docker

在上一章, 我完成了将ES部署到Docker的工作. SpringBoot和Docker都具有能快速启动的特性, 因此SpringBoot+Docker是一对用来部署微服务的黄金搭档. 在计划中, 基于SpringBoot的web应用也将部署到Docker之上, 那我们就开始行动吧.

----------------------------------------------------------------------------------------------------------------------------------------------------------------------

将SpringBoot部署到Docker上,需要执行以下步骤:
1. 保证SpringBoot打包后的可执行jar/war能正常启动
2. 在SpringBoot应用中编写Dockerfile镜像的生成规则和启动规则,并部署镜像
3. 在Docker中启动SpringBoot应用

## 保证SpringBoot打包后的可执行jar/war能正常启动
我的crawler-search-web工程默认打包出来的war文件, 直接使用java -jar的命令来启动的话会报错. 因为war包还不是一个可执行jar/war. 要让SpringBoot工程打出的包成为一个可执行jar/war,需要使用spring-boot-maven-plugin插件, 对包的内容进行修改,才能成为可执行的jar.要使用此插件, 只需在pom.xml添加以下内容:
```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
</plugin>
```
验证SpringBoot能作为可执行jar正常启动:
```shell
mvn clean install
java -jar target/crawler-search-web-1.0-SNAPSHOT.war
```
执行java -jar命令后, 如果控制台没有异常, 那说明新打的war包可以作为一个可执行war部署到docker了.

### 异常情况

**多模块的SpringBoot无法启动,提示"No qualifying bean of type ..."**

可能是SpringBoot的bug(版本1.3.5): 通过自动扫描方式构建的bean, 如果没有处于启动类所在的包下面, 会无法构建.
比如我的服务类:MailService 所在的包:org.ybak.crawler.persistence.service 就没有在启动类WebApplication所在的包:org.ybak.crawler.web 下面, 启动时就会报错.
解决办法是将WebApplication移动到org.ybak.crawler包下面.

## 在SpringBoot应用中编写Dockerfile镜像的生成规则和启动规则,并部署镜像

这里使用docker-maven-plugin来进行镜像的生成规则和启动规则的设置,并通过这个插件将镜像部署到本地.docker-maven-plugin的配置如下:
```xml
<plugin>
    <groupId>com.spotify</groupId>
    <artifactId>docker-maven-plugin</artifactId>
    <version>0.4.10</version>
    <configuration>
        <imageName>${docker.image.prefix}/${project.artifactId}</imageName>
        <baseImage>frolvlad/alpine-oraclejdk8:slim</baseImage>
        <cmd>sh -c 'touch /${project.build.finalName}.war'</cmd>
        <entryPoint>["java","-Djava.security.egd=file:/dev/./urandom","-jar","/${project.build.finalName}.war"]</entryPoint>
        <resources>
            <resource>
                <targetPath>/</targetPath>
                <directory>${project.build.directory}</directory>
                <include>${project.build.finalName}.war</include>
            </resource>
        </resources>
    </configuration>
</plugin>
```
参数说明:
imageName: 生成的Docker镜像名称
baseImage: 同Dockerfile的FROM参数
cmd: 同Dockerfile的RUN参数
entryPoint: 同Dockerfile的ENTRYPOINT参数
resources: 生成docker镜像的资源文件

docker-maven-plugin也支持引入Dockerfile的方式进行镜像配置.这样的方式配置灵活性更高.具体方式可以参见[官方文档](https://github.com/spotify/docker-maven-plugin).
配置完后, 执行:
```shell
mvn package docker:build
```
即可部署镜像.

## 在Docker中启动SpringBoot应用

部署完镜像后,执行以下命令即可启动应用:
```shell
docker run -p 8080:8080 -t ybak/crawler-search-web
```
参数说明:
-p: 端口映射, 同ES在Docker中部署一样, SpringBoot应用在Docker中部署后,我们也不能直接访问到服务, 需要做Docker容器端口映射到Docker宿主机上的端口 
-t: 模拟一个tty窗口, 可中断程序执行

执行完后, 可以看到SpringBoot的启动日志, 任务完成.

参考:
https://spring.io/guides/gs/spring-boot-docker/