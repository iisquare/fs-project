# prometheus-nvidia

### 使用说明
- 运行容器
```
docker run -d --gpus all --rm -p 9400:9400 nvcr.io/nvidia/k8s/dcgm-exporter
```
- 配置监控
```
scrape_configs:
  - job_name: nvidia
    metrics_path: '/metrics'
    static_configs:
      - targets: ["localhost:9400"]
```
- 配置看板
```
id:12239
/usr/local/dcgm/dcgm-exporter-entrypoint.sh -f /etc/dcgm-exporter/default-counters.csv
/etc/dcgm-exporter/1.x-compatibility-metrics.csv
/etc/dcgm-exporter/dcp-metrics-included.csv
```
- 综合实例
```
/opt/dcgm-exporter/etc/default-counters.csv
docker run -v /opt/dcgm-exporter/etc:/etc/dcgm-exporter --privileged=true --cap-add SYS_ADMIN --gpus all --rm -p 9400:9400 nvcr.io/nvidia/k8s/dcgm-exporter
import service/grafana/dashboards/nvidia-dcgm-exporter-dashboard.json to grafna
```

## 常见问题

### 容器驱动异常
- 异常错误
```
docker: Error response from daemon: could not select device driver "" with capabilities: [[gpu]].
```
- 解决办法
```
distribution=$(. /etc/os-release;echo $ID$VERSION_ID)
curl -s -L https://nvidia.github.io/nvidia-docker/gpgkey | sudo apt-key add -
curl -s -L https://nvidia.github.io/nvidia-docker/$distribution/nvidia-docker.list | sudo tee /etc/apt/sources.list.d/nvidia-docker.list

sudo apt-get update && sudo apt-get install -y nvidia-container-toolkit
sudo systemctl restart docker
```

### nvlink/nvswitch throughput metrics
```
FOR GPU <-> PCI (Bps)

DCGM_FI_PROF_PCIE_TX_BYTES
DCGM_FI_PROF_PCIE_RX_BYTES

FOR GPU <-> NVLINK (Bps)

DCGM_FI_PROF_NVLINK_TX_BYTES
DCGM_FI_PROF_NVLINK_RX_BYTES
```

## 参考

- [DCGM Exporter](https://docs.nvidia.com/datacenter/cloud-native/gpu-telemetry/latest/dcgm-exporter.html)
- [can support nvlink/nvswitch throughput metrics](https://github.com/NVIDIA/dcgm-exporter/issues/155)
- [DCGM User Guide / Field Identifiers](https://docs.nvidia.com/datacenter/dcgm/latest/dcgm-api/dcgm-api-field-ids.html)
- [Data Center GPU Manager User Guide / Profiling Metrics](https://docs.nvidia.com/datacenter/dcgm/2.3/dcgm-user-guide/feature-overview.html#profiling)
