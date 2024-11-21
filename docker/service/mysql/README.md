# MySQL

## 如何使用

### 查看日志
```
show binary logs;
show master logs;
show master status;
show binlog events in 'mysql-bin.000005';
```

### 删除日志
```
show variables like 'expire_log_days';
set global expire_log_days=3;  // 过期删除
reset master; // 删除master的binlog
reset slave; // 删除slave的中继日志
purge master logs before '2016-10-20 16:25:00';// 删除指定日期前的日志索引中binlog日志文件
purge master logs to 'binlog.000002';// 删除指定日志文件
```

### 解析日志
```
mysqlbinlog /var/lib/mysql/mysql-bin.000003
--no-defaults：不要读取任何选项文件。
--database=dbname：只列出该数据库下的行数据，但无法过滤Rows_query_event。
--base64-output=decode-rows -vv：显示具体SQL语句。
--skip-gtids=true：忽略GTID显示。
--start-datetime='2022-07-09 00:00:00'：时间范围，起始时间。
--stop-datetime='2022-07-11 00:00:00'：时间范围，结束时间。
```

### 备份还原
```
mysqldump -h127.0.0.1 -P3306 -uroot -p --databases test --tables t1 t2 > /path/to/dump.sql
# --all-databases, 导出包括系统数据库在内的所有数据库
# -d, 只导出表结构不导表数据
# -t, 只导出表数据不导表结构
mysql> source /path/to/dump.sql
```

## 解决方案
- 配置不生效，检查文件权限，确认配置被正常加载
```
mysql: [Warning] World-writable config file '/etc/mysql/conf.d/mysql.cnf' is ignored.
```
- 共享目录无法修改文件权限
```
mysqld: Cannot change permissions of the file 'ca.pem' (OS errno 1 - Operation not permitted)
```
- 低版本客户端无法支撑导入导出数据
```
mysqldump: Got error: 2059: Authentication plugin 'caching_sha2_password' cannot be loaded
sudo docker-compose exec mysql mysql -h 127.0.0.1 -p
select version();
show variables like 'default_authentication_plugin';
select host,user,plugin from mysql.user;
ALTER USER 'root'@'localhost' IDENTIFIED BY 'admin888' PASSWORD EXPIRE NEVER; # 修改加密规则 
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'admin888'; # 更新一下用户的密码 
FLUSH PRIVILEGES; # 刷新权限
```
