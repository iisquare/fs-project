package com.iisquare.fs.web.bi.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Kafka 配置：servers、securityProtocol、saslMechanism、saslJaasConfig、connectTimeout。
 */
public class KafkaConnector extends DatasourceConnector<AdminClient> {

    public KafkaConnector(String type, JsonNode config) {
        super(type, config);
    }

    @Override
    public String summary() {
        return servers();
    }

    public String servers() {
        String servers = config.at("/servers").asText("");
        if (DPUtil.empty(servers)) servers = config.at("/bootstrap").asText("");
        return DPUtil.trim(servers);
    }

    public Properties properties() {
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, servers());
        properties.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG,
                String.valueOf(config.at("/connectTimeout").asInt(5000)));
        String securityProtocol = config.at("/securityProtocol").asText("");
        if (!DPUtil.empty(securityProtocol)) {
            properties.put("security.protocol", securityProtocol);
        }
        String saslMechanism = config.at("/saslMechanism").asText("");
        if (!DPUtil.empty(saslMechanism)) {
            properties.put("sasl.mechanism", saslMechanism);
        }
        String saslJaasConfig = config.at("/saslJaasConfig").asText("");
        if (!DPUtil.empty(saslJaasConfig)) {
            properties.put("sasl.jaas.config", saslJaasConfig);
        }
        return properties;
    }

    @Override
    public AdminClient open() {
        return AdminClient.create(properties());
    }

    @Override
    public void close(AdminClient client) {
        if (client != null) client.close();
    }

    @Override
    public Map<String, Object> test() {
        AdminClient client = null;
        try {
            if (DPUtil.empty(servers())) return ApiUtil.result(1001, "Kafka服务地址不能为空", servers());
            client = open();
            int timeout = Math.max(1000, config.at("/connectTimeout").asInt(5000));
            String clusterId = client.describeCluster().clusterId().get(timeout, TimeUnit.MILLISECONDS);
            return ApiUtil.result(0, "连接成功", DPUtil.buildMap("servers", servers(), "clusterId", clusterId));
        } catch (Exception e) {
            return ApiUtil.result(1500, "连接失败", e.getMessage());
        } finally {
            close(client);
        }
    }
}
