# MySQL

## 解决方案
- 配置不生效，检查文件权限，确认配置被正常加载
```
mysql: [Warning] World-writable config file '/etc/mysql/conf.d/mysql.cnf' is ignored.
```
- 共享目录无法修改文件权限
```
mysqld: Cannot change permissions of the file 'ca.pem' (OS errno 1 - Operation not permitted)
```
