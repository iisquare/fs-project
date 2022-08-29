# Trino

## 使用说明

### 安装配置

- 登录用户

输入任意用户名，密码留空即可

- 配置文件

默认在/etc/trino中，若需要覆盖，可在docker-compose.yml文件中单独挂载。

### 常用命令
- CLI
```
sudo docker-compose exec trino /usr/lib/trino/bin/trino-cli --catalog mysql --schema fs_project
```
- 帮助
```
help;
```
- 数据源
```
show catalogs;
use mysql.default;
show schemas;
show tables;
```

### Superset集成
- 驱动
```
pip install sqlalchemy-trino
```
- 连接
```
trino://{username}:{password}@{hostname}:{port}/{catalog}
trino://admin@trino:8080/mysql
```

## 参考
- [Trino documentation](https://trino.io/docs/current/index.html)
- [Presto安装部署详细说明](https://blog.csdn.net/jsbylibo/article/details/107821214)
