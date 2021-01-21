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

## 参考连接
- [Compose file version 3 reference](https://docs.docker.com/compose/compose-file/compose-file-v3/)
- [为容器设置启动时要执行的命令和参数](https://kubernetes.io/zh/docs/tasks/inject-data-application/define-command-argument-container/)
- [docker-compose建立容器之间的连接关系](https://www.jianshu.com/p/1e80c2866a9d)
