# Docker
项目依赖的基础服务，适用于单机临时测试运行，请勿在线上环境使用。

## Compose
- Start
```bash
docker-compose up -d {container-name}
docker-compose up -d
```
- Stop
```bash
docker-compose stop {container-name}
docker-compose stop
```
- Build
```bash
docker-compose build {container-name}
```
- Log
```bash
docker-compose logs {container-name}
docker-compose logs -f {container-name}
```
- Delete
```bash
docker-compose rm {container-name}
docker-compose down
```

## Docker
- Exec
```bash
docker exec -it {container-id} /bin/bash
docker run -it --entrypoint /bin/bash name:version
```
- Start Policy
```
docker run --restart=always
docker update --restart=always <CONTAINER ID>
```
- Info
```
docker inspect [OPTIONS] {NAME|ID}
```

## Best Practice
- network
```
bridge，桥接网络，以桥接模式连接到宿主机，默认方式；
host，宿主网络，即与宿主机共用网络；
none，表示无网络，容器将无法联网。
```
- privileged
```
true，container内的root拥有真正的root权限。
false，container内的root只是外部的一个普通用户权限。
```
- [run older base images](https://github.com/microsoft/WSL/issues/4694)
```
%userprofile%\.wslconfig

[wsl2]
kernelCommandLine = vsyscall=emulate
```
- volumes short syntax
```
[SOURCE:]TARGET[:MODE]
ro for read-only
rw for read-write (default)
```

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
- [Compose file version 3 reference](https://docs.docker.com/compose/compose-file/compose-file-v3/)
- [为容器设置启动时要执行的命令和参数](https://kubernetes.io/zh/docs/tasks/inject-data-application/define-command-argument-container/)
- [docker-compose建立容器之间的连接关系](https://www.jianshu.com/p/1e80c2866a9d)
- [Docker run reference VOLUME (shared filesystems)](https://docs.docker.com/engine/reference/run/#volume-shared-filesystems)
- [Segmentation fault when run old debian containers if docker host is debian10(buster)](https://stackoverflow.com/questions/57807835/segmentation-fault-when-run-old-debian-containers-if-docker-host-is-debian10bus)
- [Enable vsyscall=emulate in the kernel config to run older base images such as Centos 6](https://github.com/microsoft/WSL/issues/4694)
