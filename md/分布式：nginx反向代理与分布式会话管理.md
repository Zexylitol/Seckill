# 1. nginx反向代理负载均衡

## 1.1 单机容量问题，水平扩展

- 表象：单机CPU使用率增高，memory占用增加，网络带宽使用增加
- 使用到的Linux命令：`top`
  - cpu us : 用户空间的cpu使用情况(用户层代码)
  - cpu sy : 内核空间的cpu使用情况(系统调用)
  - load average : 1, 5, 15分钟load平均值，跟着核数系数,  0代表正常，1代表打满，1+代表等待阻塞
  - memory: free空闲内存, used使用内存

### 1.1.1 改进前的部署结构

<center><img src="https://ss.im5i.com/2021/07/04/SMqXD.png" alt="SMqXD.png" border="0" /></center>

### 1.1.2 改进后的部署结构

- 数据库远程开放端口连接

  ```mysql
  # 授权任何一个域名用户访问root账号并且给予所有权限（但是要输入正确的root密码）
  mysql > grant all privileges on *.* to root@'%' identified by 'root';
  # 刷新
  mysql > flush privileges;
  ```

  

<center><img src="https://ss.im5i.com/2021/07/04/SMvDj.png" alt="SMvDj.png" border="0" /></center>

## 1.2 部署Nginx OpenResty

### 1.2.1 简介

[OpenResty®](http://openresty.org/cn/) 是一个基于 [Nginx](http://openresty.org/cn/nginx.html) 与 Lua 的高性能 Web 平台，其内部集成了大量精良的 Lua 库、第三方模块以及大多数的依赖项。用于方便地搭建能够处理超高并发、扩展性极高的动态 Web 应用、Web 服务和动态网关。

OpenResty 是一个强大的 Web 应用服务器，Web 开发人员可以使用 Lua 脚本语言调动 Nginx 支持的各种 C 以及 Lua 模块，更主要的是在性能方面，OpenResty可以快速构造出足以胜任 10K 以上并发连接响应的超高性能 Web 应用系统。

360，UPYUN，阿里云，新浪，腾讯网，去哪儿网，酷狗音乐等都是 OpenResty 的深度用户。

### 1.2.2 安装openresty

安装前的准备：

必须将 `perl 5.6.1+`, `libpcre`, `libssl`安装在您的电脑之中

```shell
yum install pcre-devel openssl-devel gcc curl
```

安装：

```shell
chmod -R 777 openresty-1.13.6.2.tar.gz 

tar -zxvf openresty-1.13.6.2.tar.gz

cd ./openresty-1.13.6.2/

./configure

make

sudo make install
```

### 1.2.3 启动nginx

```shell
cd /usr/local/openresty/nginx

# 启动
sbin/nginx -c conf/nginx.conf

# 查看nginx是否正常启动
netstat -an | grep 80
```

通过访问80端口查看nginx是否部署成功：ip:port (eg: 192.168.65.130:80)

### 1.2.4 修改前端资源用于部署nginx

- 使用nginx作为web服务器
- 使用nginx作为动静分离服务器
- 使用nginx作为反向代理服务器

<center><img src="https://ss.im5i.com/2021/07/04/SMNgS.png" alt="SMNgS.png" border="0" /></center>

#### 1.2.4.1 前端资源部署

将前端资源文件上传至`/usr/local/openresty/nginx/html`目录下

#### 1.2.4.2 前端资源路由

修改`nginx/conf`

- location节点path：指定url映射key
- location节点内容：index指定默认的访问页

```shell
cd /usr/local/openresty/nginx/conf

vim nginx/conf
        location /resources/ {
            alias   /usr/local/openresty/nginx/html/resources/;
            index  index.html index.htm;
        }
        
cd ../html/
mkdir resources/
mv *.html resources/
mv gethost.js resources/
mv ./static resources/

# 修改配置后，无缝重启nginx
../sbin/nginx -s reload
```

| 页面     | 访问路径                                        |
| :------- | :---------------------------------------------- |
| 获取OTP  | http://192.168.65.130/resources/getotp.html     |
| 注册     | http://192.168.65.130/resources/register.html   |
| 登录     | http://192.168.65.130/resources/login.html      |
| 商品浏览 | http://192.168.65.130/resources/listitem.html   |
| 下单     | http://192.168.65.130/resources/getitem.html    |
| 创建商品 | http://192.168.65.130/resources/createitem.html |



### 1.2.5 配置nginx反向代理

- nginx动静分离服务器

  - location节点path特定resources：静态资源路径
  - location节点其他路径：动态资源用

- nginx做反向代理服务器

  - 设置upstream server

    ```shell
    cd /usr/local/openresty/nginx/conf
    
    vim nginx/conf
            upstream backend_ server{
            	server ip weight=1;
            	server ip weight=1;
            	keepalive 16;
            }
            
    # 重启nginx
    ../sbin/nginx -s reload
    ```

    

  - 设置动态请求location为proxy pass路径

    ```shell
    cd /usr/local/openresty/nginx/conf
    
    vim nginx/conf
    	location / {
    		proxy_pass http://backend_server;
    		#proxy_set_header Host $http_host:$proxy_port;
    		proxy_set_header Host $http_host;
    		proxy_set_header X-Real-IP $remote_addr;
    		proxy_set_header X-Forwarded-For $proxy_add_х_forwarded_for;
    		proxy_http_version 1.1;
    		# http://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_set_header
    		#允许重新定义或追加字段到传递给代理服务器的请求头信息(默认是close)
    		proxy_set_header Connection "";
    	}
    
    # 重启nginx
    ../sbin/nginx -s reload
    ```

    

  - 开启tomcat access log验证

    ```shell
    # 开启tomcat access log验证
    cd /var/www/miaosha
    mkdir tomcat
    chmod -R 777 tomcat/
    vim application.properties
    	server.tomcat.accesslog.enabled=true
    	server.tomcat.accesslog.directory=/var/www/miaosha/tomcat
    	server.tomcat.accesslog.pattern=%h %l %u %t "%r" %s %b %D
    
    # 重启应用程序
    ps -ef | grep java
    kill PID
    ./deploy.sh &
    ```



## 1.3 Nginx高性能原因

### 1.3.1 epoll多路复用

- java BIO模型，阻塞进程式
- linux select模型，变更触发轮询查找，有1024数量上限

<center><img src="https://ss.im5i.com/2021/07/04/SMV3s.png" alt="SMV3s.png" border="0" /></center>

- epoll模型，变更触发回调直接读取，理论上无上限

<center><img src="https://ss.im5i.com/2021/07/04/SMdkQ.png" alt="SMdkQ.png" border="0" /></center>

### 1.3.2 master-worker进程模型

- 每一个worker进程中只有一个线程

<center><img src="https://ss.im5i.com/2021/07/04/SMlf3.png" alt="SMlf3.png" border="0" /></center>

### 1.3.3 协程机制

- 协程是一个比线程更小的内存模型概念，依附于线程的内存模型，不像线程有CPU切换开销，只有内存切换开销，切换开销小
- 遇阻塞即归还执行权，代码同步
- 无需加锁

### 1.3.4 小结

- epoll多路复用：解决IO阻塞回调通知的问题
- master worker：平滑过渡、平滑重启，并且基于worker单线程模型和epoll多路复用的机制完成高效操作
- 协程机制：将用户的请求对应到线程中的某一个协程中，在协程中使用epoll多路复用的机制来完成同步调用

## 1.4 分布式会话

### 1.4.1 会话管理

- 基于cookie传输sessionId: java tomcat容器session实现

  ```java
  //将otp验证码同对应用户的手机号关联, 此处使用HttpSession的方式进行手机号与OTPCode进行绑定
  httpServletRequest.getSession().setAttribute(telphone,otpCode);
  
  //用户登录服务，用来校验用户登录是否合法
  UserModel userModel = userService.validateLoing(telphone,enCodeByMd5(password));
  //将登录的凭证加入到用户的登录成功的session内
  this.httpServletRequest.getSession().setAttribute("IS_LOGIN",true);
  this.httpServletRequest.getSession().setAttribute("LOGIN_USER",userModel);
  
  Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
  if(Objects.isNull(isLogin) || !isLogin.booleanValue()){
  	throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户未登录,不能下单");
  }
  //获取用户登录信息
  UserModel userModel = (UserModel) httpServletRequest.getSession().getAttribute("LOGIN_USER");
  ```

  

- 基于token传输类似sessionId: java代码session实现

### 1.4.2 分布式会话

- 基于cookie传输sessionId: java tomcat容器session实现迁移到redis

  1. 引入redis依赖

     ```xml
     <dependency>
         <groupId>org.springframework.boot</groupId>
         <artifactId>spring-boot-starter-data-redis</artifactId>
     </dependency>
     <dependency>
         <groupId>org.springframework.session</groupId>
         <artifactId>spring-session-data-redis</artifactId>
         <version>2.0.5.RELEASE</version>
     </dependency>
     ```

     

  2. 下载redis

     ```shell
     wget https://download.redis.io/releases/redis-5.0.12.tar.gz
     
     chmod -R 777 redis-5.0.12.tar.gz 
     
     tar -zxvf redis-5.0.12.tar.gz 
     
     cd ./redis-5.0.12/
     
     # 编译
     make
     
     # 安装
     sudo make install
     
     cd ./src
     
     # 启动
     ./redis-server
     
     # 后台启动
     ./redis-server &
     
     # 启动客户端
     ./redis-cli
     ```

  3. 配置stringboot对redis的依赖以及设置jedis连接池

     ```xml
     # 配置springboot对redis的依赖
     spring.redis.host=127.0.0.1
     spring.redis.port=6379
     spring.redis.database=10
     # spring.redis.password=
     
     # 设置jedis连接池
     spring.redis.jedis.pool.max-active=50
     spring.redis.jedis.pool.min-idle=20
     ```

     

  4. `UserModel`实现`Serializable`接口

  5. 使用redis实现session管理

     ```java
     package com.miaoshaproject.config;
     
     import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
     import org.springframework.stereotype.Component;
     
     @Component
     @EnableRedisHttpSession(maxInactiveIntervalInSeconds = 3600)
     public class RedisConfig {
     
     }
     ```

     

- 基于token传输类似sessionId: java代码session实现迁移到redis
  - token： 令牌

  ```java
  // 登录
  //将登录的凭证加入到用户的登录成功的session内
  // 修改：若用户登录验证成功后将对应的登录信息和登录凭证一起存入redis中
  // 生成登录凭证token，UUID
  String uuidToken = UUID.randomUUID().toString();
  uuidToken = uuidToken.replace("-", "");
  // 建立token和用户登录态之间的联系
  redisTemplate.opsForValue().set(uuidToken, userModel);
  redisTemplate.expire(uuidToken, 1, TimeUnit.HOURS);
  // 下发token
  return CommonReturnType.create(uuidToken);
  
  // 下单
  String token = httpServletRequest.getParameterMap().get("token")[0];
  if (StringUtils.isEmpty(token)) {
      throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户未登录,不能下单");
  }
  //获取用户登录信息
  UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
  if (Objects.isNull(userModel)) {
      throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户未登录,不能下单");
  }
  ```

### 1.4.3 重新部署

```shell
mvn clean package

scp target/miaosha-1.0-SNAPSHOT.jar root@192.168.65.130:/var/www/miaosha/

# 杀掉原进程后重启
ps -ef | grep java
kill PID

chmod -R 777 miaosha-1.0-SNAPSHOT.jar

# 后台启动
./deploy.sh &
```

# 2. Jmeter性能压测

| Thread Properties        |      |
| :----------------------- | :--- |
| Number of Threads(users) | 1000 |
| Ramp-up period(seconds)  | 10   |
| LoopCount                | 100  |

| HTTP Request      |                |
| :---------------- | -------------- |
| Protocol[http]    | http           |
| Server Name or IP | 192.168.65.130 |
| Port Number       | 8090           |
| Method            | GET            |
| Path              | /item/get?id=1 |

| Aggregate Report |           |         |        |      |      |      |      |       |        |            |                 |             |
| :--------------: | :-------: | :-----: | :----: | :--: | :--: | :--: | :--: | :---: | :----: | :--------: | :-------------: | :---------: |
|      Label       | # Samples | Average | Median | TP90 | TP95 | TP99 | Min  |  Max  | Error% | Throughput | Received KB/sec | Sent KB/sec |
|   HTTP Request   |  100000   |   582   |  182   | 1629 | 2411 | 4266 |  1   | 14269 |  0.0   | 1443.8/sec |     510.42      |     0.0     |
|      TOTAL       |  100000   |   582   |  182   | 1629 | 2411 | 4266 |  1   | 14269 |  0.0   | 1443.8/sec |     510.42      |     0.0     |















