# 1. 虚拟机环境设置

```shell
# 查询是否安装 jdk
rpm -qa | grep jdk
rpm -qa | grep java

# 卸载安装的 jdk
yum -y remove java*

# 安装JDK
chmod -R 777 jdk-8u65-linux-x64.rpm
rpm -ivh jdk-8u65-linux-x64.rp

# 安装Mysql
yum install mysql* 
yum install mariadb-server
```

# 2. 数据库备份上传

将本地数据库备份用于在虚拟机部署：

```mysql
mysqldump –uroot –proot –databases miaosha > miaosha.sql
```

上传数据文件到虚拟机并恢复数据：

```mysql
mysql –uroot –proot < miaosha.sql
```

# 3. 应用程序部署

1）在本地项目根目录下使用`mvn clean package`打包生成`miaosha.jar`文件  

2）将jar包服务上传到服务端上并编写外挂的`application.properties`配置文件  

3）编写`deploy.sh`文件启动对应的项目  

```shell
nohup java -Xms2048m -Xmx2048m -XX:NewSize=1024m -XX:MaxNewSize=1024m -jar miaosha.jar
--spring.config.addition-location=/var/www/miaosha/application.properties
```

参数说明:

- `nohup`:以非停止方式运行程序，这样即便控制台退出了程序也不会停止
- `java`:java命令启动，设置jvm初始和最大内存为2048m，2个g大小，设置jvm中初始新生代和最大新生代大小为1024m，设置成一样的目的是为减少扩展jvm内存池过程中向操作系统索要内存分配的消耗，
- `-–spring.config.addtion-location`=指定外挂的配置文件地址  

4）后台启动应用程序

```shell
deploy.sh &
```



