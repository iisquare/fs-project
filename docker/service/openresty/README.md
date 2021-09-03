# OpenResty

## 使用说明
- 配置域名解析
```
# buffered messages send to kafka err: not found topic
# buffered messages send to kafka err: no resolver defined to resolve
cat /etc/resolv.conf
vi /usr/local/openresty/nginx/conf/nginx.conf
http {
  resolver 127.0.0.11;
}
```
- 快速构建
```
sudo docker-compose stop openresty
sudo docker-compose rm -f openresty
sudo docker-compose build openresty
sudo rm -rf /data/runtime/openresty
sudo docker-compose up -d openresty
sudo docker-compose exec openresty /bin/bash
tail -f /usr/local/openresty/nginx/logs/error.log
```
- 日志测试
```
sudo docker-compose exec kafka /bin/bash
kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic fs-access-log
```

## 参考
- [lua-resty-kafka](https://github.com/doujiang24/lua-resty-kafka)
- [docker-openresty](https://github.com/openresty/docker-openresty/blob/master/centos7/Dockerfile)
- [OpenResty + Lua + Kafka 实现日志收集系统以及部署过程中遇到的坑](https://www.cnblogs.com/gxyandwmm/p/11298912.html)
- [lua-resty-kafka模块使用](https://blog.csdn.net/liuxiao723846/article/details/107213643)
