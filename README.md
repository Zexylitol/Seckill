# Seckill
## 简介

通过SpringBoot快速搭建前后端分离的电商基础秒杀项目。在秒杀基础项目中完成用户otp注册、登录、查看、商品列表、进入商品详情以及倒计时秒杀下单的基本流程。再基于基础秒杀项目，优化并实现高性能的秒杀系统。

## 开发环境

### 后端

- JDK1.8
- Maven3
- SpringBoot2.0.5
- MySQL5.6
- Mybatis

### 前端

- Metronic模板

> [Metronic 6.0](http://metronic.kp7.cn/) 是一套精美的响应式后台管理模板，基于强大的 Twitter Bootstrap 3.3.4 框架实现。

获取otp:

<center><img src="https://ss.im5i.com/2021/06/22/SGnx5.png" alt="SGnx5.png" border="0" /></center>

注册界面：

<center><img src="https://ss.im5i.com/2021/06/22/SG3w6.png" alt="SG3w6.png" border="0" /></center>

登录界面：

<center><img src="https://ss.im5i.com/2021/06/22/SGZoU.png" alt="SGZoU.png" border="0" /></center>

商品列表：

<center><img src="https://ss.im5i.com/2021/06/22/SGiOw.png" alt="SGiOw.png" border="0" /></center>

秒杀下单：

<center><img src="https://ss.im5i.com/2021/06/22/SG8XJ.png" alt="SG8XJ.png" border="0" /></center>

创建商品：

<center><img src="https://ss.im5i.com/2021/06/22/SGeSn.png" alt="SGeSn.png" border="0" /></center>

## 架构图

<center><img src="https://ss.im5i.com/2021/06/23/SyFr7.png" alt="SyFr7.png" border="0" /></center>

## 启动说明

前提：部署至本地Linux虚拟机，并部署Nginx OpenResty

| 启动             | 说明                                                         |
| :--------------- | :----------------------------------------------------------- |
| 启动redis-server |                                                              |
| 启动nginx        | cd /usr/local/openresty/nginx<br/>sbin/nginx -c conf/nginx.conf |
| 启动程序         | cd /var/www/miaosha<br/>./deploy.sh &                        |
| 启动RocketMQ     | #Start Name Server<br/>nohup sh bin/mqnamesrv &<br/>#Start Broker<br/>nohup sh bin/mqbroker -n localhost:9876 &<br/>#创建topic<br/>./bin/mqadmin updateTopic -n localhost:9876 -t stock -c DefaultCluster |

## 本地Linux虚拟机部署

- [本地Linux虚拟机部署](md/本地Linux虚拟机部署.md)

## 性能优化

- [容量问题优化](md/容量问题优化.md)
- [分布式：nginx反向代理与分布式会话管理](md/分布式：nginx反向代理与分布式会话管理.md)
- [多级缓存](md/多级缓存.md)
- [交易泄压](md/交易泄压.md)
- [库存数据最终一致性保证](md/库存数据最终一致性保证.md)
- [流量削峰](md/流量削峰.md)
- [防刷限流](md/防刷限流.md)