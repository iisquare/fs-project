# Ceph

## 使用说明
### Monitor
- 启动mon服务
```
sudo docker-compose up -d ceph-monitor
```
#### Manager
- 启动mgr服务
```
sudo docker-compose up -d ceph-manager
```
- 启用控制面板
```
sudo docker ps
sudo docker exec -it docker_ceph-manager_1 /bin/bash
ceph mgr module enable dashboard
ceph dashboard create-self-signed-cert
ceph dashboard set-login-credentials admin admin888
ceph config set mgr mgr/dashboard/server_addr 0.0.0.0
ceph config set mgr mgr/dashboard/server_port 16789 # default is 8443
ceph config set mgr mgr/dashboard/ssl false
ceph mgr services
sudo docker restart docker_ceph-manager_1
sudo docker exec docker_ceph-manager_1 ceph mgr services
```
### Storage
- 分发授权文件
```
sudo docker exec docker_ceph-monitor_1 ceph auth get client.bootstrap-osd -o /var/lib/ceph/bootstrap-osd/ceph.keyring
```
- Linux创建虚拟磁盘
```
sudo fdisk -l
sudo dd if=/dev/zero of=./runtime/ceph/device.img bs=1M count=10240
sudo mkfs.xfs -f ./runtime/ceph/device.img
# sudo fsck.xfs ./runtime/ceph/device.img
sudo xfs_repair ./runtime/ceph/device.img
sudo losetup -Pf --show ./runtime/ceph/device.img
sudo fdisk -l
sudo kpartx /dev/loop0
mkdir -p ./runtime/ceph/files
sudo mount /dev/loop0 ./runtime/ceph/files
ls -alh ./runtime/ceph/files
```
- Linux卸载虚拟磁盘
```
sudo umount ./runtime/ceph/files
rm -rf ./runtime/ceph/files
sudo kpartx -dv /dev/loop0
sudo losetup -d /dev/loop0
sudo rm -f runtime/ceph/device.img
```
- 启动osd服务
```
sudo docker-compose up -d ceph-storage
```

## 参考
- [Ceph介绍及原理架构分享](https://www.jianshu.com/p/cc3ece850433)
- [ceph-deploy部署ceph集群](https://www.kancloud.cn/willseecloud/ceph/1788301)
- [使用docker快速部署Ceph集群 arm64 or x86](https://www.jianshu.com/p/ff3be28a1015)
- [Linux创建、挂载、格式化虚拟磁盘](https://blog.csdn.net/pkgfs/article/details/8498667)
