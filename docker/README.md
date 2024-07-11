# Docker
项目依赖的基础服务，适用于单机临时测试运行，请勿在线上环境使用。

## 注意事项

### 数据目录请务放置在共享目录下。
- WSL挂载的宿主机目录无法修改权限。
- 可将环境变量DATA_DIR=/data/runtime改为非宿主机目录。
```
mkdir /data/runtime
chmod 777 /data/runtime
```
- 可将数据目录软连接到非共享目录。
```
# 创建软连接
mkdir /data/mongo
ln -s /data/mongo ./runtime/mongo
# 查看软连接
ls -lh ./runtime/mongo
# 修改软连接
ln –snf /data/mongo ./runtime/mongo
# 删除软连接
rm -rf ./runtime/mongo
```

## 参考连接
- [Dockerfile reference](https://docs.docker.com/engine/reference/builder/)
- [Compose file version 3 reference](https://docs.docker.com/compose/compose-file/compose-file-v3/)
- [为容器设置启动时要执行的命令和参数](https://kubernetes.io/zh/docs/tasks/inject-data-application/define-command-argument-container/)
- [docker-compose建立容器之间的连接关系](https://www.jianshu.com/p/1e80c2866a9d)
- [Docker run reference VOLUME (shared filesystems)](https://docs.docker.com/engine/reference/run/#volume-shared-filesystems)
- [Segmentation fault when run old debian containers if docker host is debian10(buster)](https://stackoverflow.com/questions/57807835/segmentation-fault-when-run-old-debian-containers-if-docker-host-is-debian10bus)
- [Enable vsyscall=emulate in the kernel config to run older base images such as Centos 6](https://github.com/microsoft/WSL/issues/4694)
