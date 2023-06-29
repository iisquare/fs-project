# Superset

## 使用说明

### 安装步骤

- 拉取配置文件
```
https://github.com/apache/superset/blob/1.5.3/superset/config.py
```
- 修改config.py配置参数
```
# The SQLAlchemy connection string.
# SQLALCHEMY_DATABASE_URI = "sqlite:///" + os.path.join(DATA_DIR, "superset.db")
SQLALCHEMY_DATABASE_URI = 'mysql://root:admin888@mysql/superset'
# SQLALCHEMY_DATABASE_URI = 'postgresql://root:password@localhost/myapp'
```
- 查看挂载配置
```
sudo docker-compose exec superset cat /app/superset/config.py|grep SQLALCHEMY_DATABASE_URI
```
- 创建用户
```
sudo docker-compose exec superset superset fab create-admin \
              --username admin \
              --firstname Superset \
              --lastname Admin \
              --email admin@superset.com \
              --password admin888
```
- 迁移数据到最新版本
```
sudo docker-compose exec superset superset db upgrade
```
- 导入测试数据
```
sudo docker-compose exec superset superset load_examples
```
- 初始化角色信息
```
sudo docker-compose exec superset superset init
```

## 参考
- [Install Database Drivers](https://superset.apache.org/docs/databases/installing-database-drivers)
