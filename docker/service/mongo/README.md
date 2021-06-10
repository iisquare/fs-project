# mongo

## WARNING (Windows & OS X)
- 创建软连接
```
mkdir /data/mongo
ln -s /data/mongo ./runtime/mongo
```
- 查看软连接
```
ls -lh ./runtime/mongo
```
- 修改软连接
```
ln –snf /data/mongo ./runtime/mongo
```
- 删除软连接
```
rm -rf ./runtime/mongo
```

### 参考
- [关于widows系统使用docker部署mongo时报错：Operation not permitted](https://blog.csdn.net/qq506930427/article/details/99658808)
