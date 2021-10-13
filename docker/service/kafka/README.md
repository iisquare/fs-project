# Kafka

## 注意事项

### KAFKA_LISTENERS

- PLAINTEXT://:9092，默认采用主机名，确保客户端存在主机名到IP地址的映射。
- PLAINTEXT://[ip]:9092，采用固定IP访问。
- PLAINTEXT://[hostname]:9092，采用域名解析，确保客户端可正常解析对应域名。

