# prometheus

### 使用说明
- 各exporter通过内部域名直接访问，不再导出端口至宿主机访问。


## 常见问题

### 时间不同步

```
Warning: Error fetching server time: Detected 32.834999799728394 seconds time difference between your browser and the server. Prometheus relies on accurate time and time drift might cause unexpected query results.
sudo docker-compose exec prometheus date
# WSL时间同步
sudo apt install ntpdate
sudo ntpdate time.windows.com
```


### 参考
- [redis_exporter](https://github.com/oliver006/redis_exporter)
