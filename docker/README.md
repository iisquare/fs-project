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
```
