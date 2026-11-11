package com.iisquare.fs.web.spider.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.CodeUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.HttpUtil;
import com.iisquare.fs.web.spider.core.ZooJob;
import com.iisquare.fs.web.spider.core.RedisTask;
import com.iisquare.fs.web.spider.entity.ProxyChannel;
import com.iisquare.fs.web.spider.entity.ProxyGroup;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ProxyLeaseService {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Pattern IP_PORT = Pattern.compile(
            "^\\s*(?:(http|https|socks5)://)?(?:([^:@\\s]+):([^@\\s]*)@)?(\\[[^\\]]+\\]|[^:\\s]+):(\\d+)(?::([^:\\s]+):([^\\s]+))?\\s*$",
            Pattern.CASE_INSENSITIVE);
    private final Map<Integer, CachedResources> resourceCache = new ConcurrentHashMap<>();

    @Autowired
    ProxyGroupService groupService;
    @Autowired
    ProxyChannelService channelService;
    @Autowired
    NodeService nodeService;

    public StringRedisTemplate redis() {
        return nodeService.redis();
    }

    /**
     * 为内部采集任务申请代理。模板未配置代理分组时返回 mode=none，不阻断采集链路。
     */
    public Map<String, Object> acquire(ZooJob job, RedisTask task, String nodeId) {
        JsonNode template = null == job ? DPUtil.objectNode() : job.getTemplate();
        int groupId = template.at("/proxyGroupId").asInt(0);
        if (groupId < 1) {
            ObjectNode data = DPUtil.objectNode();
            data.put("mode", "none");
            data.putObject("proxy");
            data.putObject("state");
            return ApiUtil.result(0, null, data);
        }
        ProxyGroup group = groupService.info(groupId);
        if (null == group || !Objects.equals(1, group.getStatus())) {
            return ApiUtil.result(1013, "代理分组不存在或已禁用", group);
        }
        String proxyMode = normalizeMode(template.at("/proxyMode").asText("rotate"));
        String stateMode = normalizeStateMode(template.at("/stateMode").asText("none"));
        String stateKey = "";
        if (!"none".equals(stateMode)) {
            String scope = DPUtil.trim(template.at("/stateKey").asText("default"));
            Object keySource = switch (stateMode) {
                case "task" -> job.getId() + ":" + task.taskId(job);
                default -> job.getId() + ":" + scope;
            };
            stateKey = CodeUtil.md5(DPUtil.parseString(keySource));
        } else if ("sticky".equals(proxyMode)) {
            stateKey = CodeUtil.md5(job.getId() + ":" + group.getCode());
        }
        int defaultStateTtl = null == group.getStickySeconds() ? 1800 : group.getStickySeconds();
        int stateTtl = Math.max(60, template.at("/stateTtl").asInt(defaultStateTtl));
        boolean sticky = "sticky".equals(proxyMode) || !"none".equals(stateMode);
        return acquire(group, stateKey, nodeId, task.getId(), sticky,
                !"none".equals(stateMode), stateTtl);
    }

    /**
     * 供分组对外接口申请代理。accessKey 由控制器校验，stateKey 为空且 sticky=true 时按 clientId 保持粘性。
     */
    public Map<String, Object> acquire(ProxyGroup group, String stateKey, String clientId, String taskId,
                                       boolean sticky, boolean stateful, int stateTtl) {
        if (null == group || !Objects.equals(1, group.getStatus())) {
            return ApiUtil.result(1013, "代理分组不存在或已禁用", group);
        }
        ProxyChannel channel = groupService.channel(group);
        if (null == channel || !Objects.equals(1, channel.getStatus())) {
            return ApiUtil.result(1014, "代理渠道不存在或已禁用", null);
        }
        String normalizedStateKey = DPUtil.trim(stateKey);
        if (sticky && DPUtil.empty(normalizedStateKey)) {
            normalizedStateKey = CodeUtil.md5(group.getCode() + ":" + DPUtil.parseString(clientId));
        }
        String activeKey = activeKey(group.getId());
        long now = System.currentTimeMillis();
        redis().opsForZSet().removeRangeByScore(activeKey, 0, now);
        if (null != group.getConcurrency() && group.getConcurrency() > 0) {
            Long active = redis().opsForZSet().zCard(activeKey);
            if (null != active && active >= group.getConcurrency()) {
                metricFailure(group, channel, "concurrency");
                return ApiUtil.result(1015, "代理分组活动租约已达上限", group);
            }
        }
        long leaseSeconds = Math.max(30, null == group.getLeaseSeconds() ? 300 : group.getLeaseSeconds());
        long stickySeconds = Math.max(leaseSeconds, null == group.getStickySeconds() ? 1800 : group.getStickySeconds());
        long stateSeconds = Math.max(60, stateTtl > 0 ? stateTtl : (int) stickySeconds);
        String stateLockKey = !stateful || DPUtil.empty(normalizedStateKey) ? ""
                : stateLockKey(group.getId(), normalizedStateKey);
        String stateLockValue = UUID.randomUUID().toString();
        if (!DPUtil.empty(stateLockKey)) {
            Boolean locked = redis().opsForValue().setIfAbsent(
                    stateLockKey, stateLockValue, leaseSeconds, TimeUnit.SECONDS);
            if (!Boolean.TRUE.equals(locked)) {
                metricFailure(group, channel, "state_busy");
                return ApiUtil.result(1018, "状态作用域正在被其他采集任务使用", group);
            }
        }

        SessionBinding binding = null;
        if (sticky && !DPUtil.empty(normalizedStateKey)) {
            binding = readSession(group, normalizedStateKey);
        }
        Resource resource = null == binding ? null : binding.getResource();
        if (null != binding && group.getMaxUses() != null && group.getMaxUses() > 0
                && binding.getUses() >= group.getMaxUses()) {
            resource = null;
        }
        boolean reuse = null != resource && resource.getExpireAt() > now;
        if (null == resource || resource.getExpireAt() <= now) {
            try {
                List<Resource> resources = resources(group, channel);
                resource = select(group, channel, resources);
            } catch (Exception e) {
                releaseStateLock(stateLockKey, stateLockValue);
                log.warn("extract proxy resource failed, group={}", group.getCode(), e);
                metricFailure(group, channel, "extract");
                return ApiUtil.result(1016, "申请代理资源失败：" + e.getMessage(), group);
            }
        }
        if (null == resource) {
            releaseStateLock(stateLockKey, stateLockValue);
            metricFailure(group, channel, "empty");
            return ApiUtil.result(1017, "代理分组暂无可分配资源", group);
        }

        JsonNode savedState = null == binding ? null : binding.getState();
        ObjectNode state = savedState instanceof ObjectNode object ? object : DPUtil.objectNode();
        ObjectNode session = DPUtil.objectNode();
        session.put("groupId", group.getId());
        session.put("channelId", channel.getId());
        session.put("stateKey", normalizedStateKey);
        session.put("expiresAt", now + stickySeconds * 1000);
        session.put("uses", reuse ? binding.getUses() + 1 : 1);
        session.put("failureCount", reuse ? binding.getFailureCount() : 0);
        session.replace("resource", resource.json());
        session.set("state", state);
        String sessionKey = DPUtil.empty(normalizedStateKey) ? null
                : sessionKey(group.getId(), normalizedStateKey);
        if (null != sessionKey) {
            redis().opsForValue().set(sessionKey, DPUtil.stringify(session),
                    Math.max(stickySeconds, stateSeconds), TimeUnit.SECONDS);
        }

        String leaseId = UUID.randomUUID().toString();
        ObjectNode lease = DPUtil.objectNode();
        lease.put("leaseId", leaseId);
        lease.put("groupId", group.getId());
        lease.put("groupCode", group.getCode());
        lease.put("channelId", channel.getId());
        lease.put("nodeId", DPUtil.parseString(clientId));
        lease.put("taskId", DPUtil.parseString(taskId));
        lease.put("stateKey", normalizedStateKey);
        lease.put("sessionKey", null == sessionKey ? "" : sessionKey);
        lease.put("stateLockKey", stateLockKey);
        lease.put("stateLockValue", stateLockValue);
        lease.put("createdAt", now);
        lease.put("leaseExpireAt", now + leaseSeconds * 1000);
        lease.put("sticky", sticky);
        lease.put("uses", 0);
        lease.replace("resource", resource.json());
        redis().opsForValue().set(leaseKey(leaseId), DPUtil.stringify(lease),
                Math.max(leaseSeconds, stateSeconds), TimeUnit.SECONDS);
        redis().opsForZSet().add(activeKey, leaseId, now + leaseSeconds * 1000);
        redis().expire(activeKey, Math.max(leaseSeconds, stateSeconds) + 60, TimeUnit.SECONDS);
        metricAcquire(group, channel, resource, !DPUtil.empty(stateLockKey));

        ObjectNode data = DPUtil.objectNode();
        data.put("leaseId", leaseId);
        data.put("mode", sticky ? "sticky" : "rotate");
        data.put("stateKey", normalizedStateKey);
        data.put("leaseExpireAt", now + leaseSeconds * 1000);
        data.put("stateExpireAt", now + stateSeconds * 1000);
        data.putObject("group").put("id", group.getId()).put("code", group.getCode()).put("name", group.getName());
        ObjectNode proxy = data.putObject("proxy");
        proxy.put("protocol", resource.getProtocol());
        proxy.put("host", resource.getHost());
        proxy.put("port", resource.getPort());
        proxy.put("username", resource.getUsername());
        proxy.put("password", resource.getPassword());
        proxy.put("connectTimeout", null == group.getConnectTimeout() ? 10000 : group.getConnectTimeout());
        proxy.put("socketTimeout", null == group.getSocketTimeout() ? 30000 : group.getSocketTimeout());
        proxy.put("expireAt", resource.getExpireAt());
        proxy.put("resourceKey", resource.key());
        ObjectNode headers = data.putObject("headers");
        if (!DPUtil.empty(resource.getUsername())) {
            String token = resource.getUsername() + ":" + DPUtil.parseString(resource.getPassword());
            headers.put("Proxy-Authorization", "Basic " + Base64.getEncoder()
                    .encodeToString(token.getBytes(StandardCharsets.UTF_8)));
        }
        data.set("state", state);
        return ApiUtil.result(0, null, data);
    }

    public Map<String, Object> keepalive(String leaseId) {
        return keepalive(leaseId, null);
    }

    public Map<String, Object> keepalive(String leaseId, Integer expectedGroupId) {
        ObjectNode lease = lease(leaseId);
        if (null == lease) return ApiUtil.result(1404, "代理租约不存在或已过期", null);
        if (null != expectedGroupId && expectedGroupId != lease.at("/groupId").asInt()) {
            return ApiUtil.result(1404, "代理租约不属于当前分组", null);
        }
        long now = System.currentTimeMillis();
        ProxyGroup group = groupService.info(lease.at("/groupId").asInt());
        long seconds = Math.max(30, null == group || null == group.getLeaseSeconds() ? 300 : group.getLeaseSeconds());
        lease.put("leaseExpireAt", now + seconds * 1000);
        redis().opsForValue().set(leaseKey(leaseId), DPUtil.stringify(lease), seconds,
                TimeUnit.SECONDS);
        redis().opsForZSet().add(activeKey(lease.at("/groupId").asInt()), leaseId, now + seconds * 1000);
        String sessionKey = lease.at("/sessionKey").asText("");
        if (!DPUtil.empty(sessionKey)) {
            JsonNode current = DPUtil.parseJSON(redis().opsForValue().get(sessionKey));
            if (null != current && current.isObject()) {
                ObjectNode session = (ObjectNode) current;
                int stickySeconds = Math.max(60,
                        null == group || null == group.getStickySeconds() ? 1800 : group.getStickySeconds());
                session.put("expiresAt", now + stickySeconds * 1000L);
                redis().opsForValue().set(sessionKey, DPUtil.stringify(session), stickySeconds, TimeUnit.SECONDS);
            }
        }
        String stateLockKey = lease.at("/stateLockKey").asText("");
        if (!DPUtil.empty(stateLockKey)) redis().expire(stateLockKey, seconds, TimeUnit.SECONDS);
        return ApiUtil.result(0, null, lease);
    }

    public Map<String, Object> release(String leaseId, boolean keepState) {
        return release(leaseId, keepState, null, null, false);
    }

    public Map<String, Object> release(String leaseId, boolean keepState, Integer expectedGroupId) {
        return release(leaseId, keepState, expectedGroupId, null, false);
    }

    public Map<String, Object> release(String leaseId, boolean keepState, Integer expectedGroupId,
                                       JsonNode state, boolean success) {
        ObjectNode lease = lease(leaseId);
        if (null == lease) return ApiUtil.result(0, null, false);
        if (null != expectedGroupId && expectedGroupId != lease.at("/groupId").asInt()) {
            return ApiUtil.result(1404, "代理租约不属于当前分组", null);
        }
        ProxyGroup group = groupService.info(lease.at("/groupId").asInt());
        String sessionKey = lease.at("/sessionKey").asText("");
        if (keepState && null != state && state.isObject() && !DPUtil.empty(sessionKey)) {
            JsonNode currentNode = DPUtil.parseJSON(redis().opsForValue().get(sessionKey));
            if (null != currentNode && currentNode.isObject()) {
                ObjectNode session = (ObjectNode) currentNode;
                session.set("state", state);
                session.put("updatedAt", System.currentTimeMillis());
                int seconds = Math.max(60, null == group || null == group.getStickySeconds()
                        ? 1800 : group.getStickySeconds());
                redis().opsForValue().set(sessionKey, DPUtil.stringify(session),
                        seconds, TimeUnit.SECONDS);
            }
        }
        ProxyChannel channel = null == group ? null : groupService.channel(group);
        Resource resource = resource(lease.at("/resource"));
        if (null != group && null != channel && null != resource) {
            metricComplete(group, channel, resource, success, 0);
        }
        finishLease(lease, keepState);
        return ApiUtil.result(0, null, true);
    }

    /**
     * 采集端上报任务结果时同步状态、释放/保留租约，并刷新统计。
     */
    public void complete(JsonNode proxyData, int status, long coast, boolean success) {
        if (null == proxyData || !proxyData.isObject()) return;
        String leaseId = proxyData.at("/leaseId").asText("");
        if (DPUtil.empty(leaseId)) return;
        ObjectNode lease = lease(leaseId);
        if (null == lease) return;
        long now = System.currentTimeMillis();
        JsonNode state = proxyData.at("/state");
        String sessionKey = lease.at("/sessionKey").asText("");
        ProxyGroup group = groupService.info(lease.at("/groupId").asInt());
        boolean keepState = !"rotate".equals(proxyData.at("/mode").asText("rotate"));
        if (!DPUtil.empty(sessionKey) && null != state && state.isObject()) {
            String value = redis().opsForValue().get(sessionKey);
            JsonNode session = DPUtil.parseJSON(value);
            if (null != session && session.isObject()) {
                ObjectNode object = (ObjectNode) session;
                object.set("state", state);
                object.put("updatedAt", now);
                int seconds = Math.max(60, null == group || null == group.getStickySeconds()
                        ? 1800 : group.getStickySeconds());
                int failureCount = success ? 0 : object.at("/failureCount").asInt(0) + 1;
                object.put("failureCount", failureCount);
                int failureThreshold = null == group || null == group.getFailureThreshold()
                        ? 3 : group.getFailureThreshold();
                if (failureThreshold > 0 && failureCount >= failureThreshold) {
                    object.putObject("resource"); // 保留状态，下次申请时重新绑定资源
                }
                redis().opsForValue().set(sessionKey, DPUtil.stringify(object), seconds, TimeUnit.SECONDS);
                keepState = true;
            }
        }
        ProxyChannel channel = null == group ? null : groupService.channel(group);
        Resource resource = resource(lease.at("/resource"));
        if (null != group && null != channel && null != resource) {
            metricComplete(group, channel, resource, success, coast);
        }
        finishLease(lease, keepState);
    }

    private void finishLease(ObjectNode lease, boolean keepState) {
        String leaseId = lease.at("/leaseId").asText("");
        int groupId = lease.at("/groupId").asInt();
        redis().delete(leaseKey(leaseId));
        redis().opsForZSet().remove(activeKey(groupId), leaseId);
        releaseStateLock(lease.at("/stateLockKey").asText(""), lease.at("/stateLockValue").asText(""));
        if (!keepState) {
            String sessionKey = lease.at("/sessionKey").asText("");
            if (!DPUtil.empty(sessionKey)) redis().delete(sessionKey);
        }
    }

    public ObjectNode lease(String leaseId) {
        if (DPUtil.empty(leaseId)) return null;
        JsonNode node = DPUtil.parseJSON(redis().opsForValue().get(leaseKey(leaseId)));
        return null != node && node.isObject() ? (ObjectNode) node : null;
    }

    public long activeCount(ProxyGroup group) {
        if (null == group) return 0;
        String key = activeKey(group.getId());
        redis().opsForZSet().removeRangeByScore(key, 0, System.currentTimeMillis());
        Long count = redis().opsForZSet().zCard(key);
        return null == count ? 0 : count;
    }

    /**
     * 测试渠道提取配置，仅返回脱敏后的资源样本，不创建租约。
     */
    public Map<String, Object> probe(ProxyChannel channel, int batchSize, String protocol, String region) {
        if (null == channel) return ApiUtil.result(1404, "代理渠道不存在", null);
        ProxyGroup group = new ProxyGroup();
        group.setId(0);
        group.setChannelId(channel.getId());
        group.setCode("probe");
        group.setBatchSize(Math.max(1, batchSize));
        group.setProtocol(DPUtil.empty(protocol) ? channel.getProtocol() : protocol);
        group.setRegion(DPUtil.parseString(region));
        try {
            List<Resource> resources = resources(group, channel);
            ArrayNode rows = DPUtil.arrayNode();
            for (int i = 0; i < Math.min(10, resources.size()); i++) {
                Resource resource = resources.get(i);
                ObjectNode item = rows.addObject();
                item.put("protocol", resource.getProtocol());
                item.put("host", resource.getHost());
                item.put("port", resource.getPort());
                item.put("expireAt", resource.getExpireAt());
                item.put("hasCredentials", !DPUtil.empty(resource.getUsername()));
            }
            ObjectNode data = DPUtil.objectNode();
            data.put("count", resources.size());
            data.put("sampled", rows.size());
            data.set("resources", rows);
            return ApiUtil.result(0, null, data);
        } catch (Exception e) {
            return ApiUtil.result(1016, "测试代理提取失败：" + e.getMessage(), null);
        } finally {
            resourceCache.remove(0);
        }
    }

    /**
     * dimension 支持 group、channel、protocol、resource、status。
     * date 为空时统计当天，group/channel 参数用于过滤。
     */
    public Map<String, Object> stats(String dimension, String date, Integer groupId, Integer channelId) {
        String dimensionName = DPUtil.empty(dimension) ? "group" : dimension.trim().toLowerCase(Locale.ROOT);
        List<String> supported = Arrays.asList("group", "channel", "protocol", "resource", "status");
        if (!supported.contains(dimensionName)) {
            return ApiUtil.result(1001, "统计维度不支持", dimensionName);
        }
        String day = DPUtil.empty(date) ? LocalDate.now().format(DAY_FORMAT)
                : date.replace("-", "").trim();
        if ("status".equals(dimensionName)) {
            return ApiUtil.result(0, null, statusStats());
        }
        Set<ZSetOperations.TypedTuple<String>> tuples = redis().opsForZSet()
                .reverseRangeWithScores(indexKey(day, dimensionName), 0, 999);
        List<ObjectNode> rows = new ArrayList<>();
        if (null != tuples) {
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                String id = tuple.getValue();
                if (DPUtil.empty(id)) continue;
                ObjectNode metric = metric(id, day, dimensionName);
                if (null == metric) continue;
                if ("group".equals(dimensionName) && null != groupId && groupId > 0
                        && groupId != metric.at("/id").asInt()) continue;
                if ("channel".equals(dimensionName) && null != channelId && channelId > 0
                        && channelId != metric.at("/id").asInt()) continue;
                if ("resource".equals(dimensionName)) {
                    int ownerGroup = metric.at("/groupId").asInt();
                    if (null != groupId && groupId > 0 && ownerGroup != groupId) continue;
                    if (null != channelId && channelId > 0 && channelId != metric.at("/channelId").asInt()) continue;
                }
                rows.add(metric);
            }
        }
        rows.sort((left, right) -> Long.compare(right.at("/lastTime").asLong(), left.at("/lastTime").asLong()));
        return ApiUtil.result(0, null, rows);
    }

    private ObjectNode statusStats() {
        ObjectNode result = DPUtil.objectNode();
        List<ProxyGroup> groups = groupService.all();
        int enabled = 0;
        int disabled = 0;
        for (ProxyGroup group : groups) {
            if (Objects.equals(1, group.getStatus())) enabled++;
            else disabled++;
        }
        result.put("time", System.currentTimeMillis());
        result.put("totalGroups", groups.size());
        result.put("enabled", enabled);
        result.put("disabled", disabled);
        result.put("active", totalActive());
        return result;
    }

    private long totalActive() {
        Set<String> keys = redis().keys(nodeService.keyPrefix() + "proxy:active:*");
        long total = 0;
        if (null != keys) {
            for (String key : keys) {
                redis().opsForZSet().removeRangeByScore(key, 0, System.currentTimeMillis());
                Long count = redis().opsForZSet().zCard(key);
                total += null == count ? 0 : count;
            }
        }
        return total;
    }

    private ObjectNode metric(String id, String day, String dimension) {
        String key = metricKey(day, dimension, id);
        Map<Object, Object> values = redis().opsForHash().entries(key);
        if (values.isEmpty()) return null;
        long total = parseLong(values.get("total"));
        long success = parseLong(values.get("success"));
        long failure = parseLong(values.get("failure"));
        long latencyTotal = parseLong(values.get("latencyTotal"));
        long latencyCount = parseLong(values.get("latencyCount"));
        ObjectNode node = DPUtil.objectNode();
        String[] parts = id.split("\\|", -1);
        if ("group".equals(dimension)) {
            ProxyGroup group = groupService.info(DPUtil.parseInt(id));
            node.put("id", DPUtil.parseInt(id));
            node.put("code", null == group ? "" : group.getCode());
            node.put("name", null == group ? id : group.getName());
            node.put("status", null == group ? 2 : group.getStatus());
            node.put("channelId", null == group ? 0 : group.getChannelId());
        } else if ("channel".equals(dimension)) {
            ProxyChannel channel = channelService.info(DPUtil.parseInt(id));
            node.put("id", DPUtil.parseInt(id));
            node.put("name", null == channel ? id : channel.getName());
            node.put("adapter", null == channel ? "" : channel.getAdapter());
            node.put("status", null == channel ? 2 : channel.getStatus());
        } else if ("protocol".equals(dimension)) {
            node.put("id", id);
            node.put("name", id.toUpperCase(Locale.ROOT));
        } else if ("resource".equals(dimension)) {
            node.put("id", id);
            node.put("host", parts.length > 0 ? parts[0] : "");
            node.put("protocol", parts.length > 1 ? parts[1] : "");
            node.put("groupId", parts.length > 2 ? DPUtil.parseInt(parts[2]) : 0);
            node.put("channelId", parts.length > 3 ? DPUtil.parseInt(parts[3]) : 0);
        }
        node.put("total", total);
        node.put("success", success);
        node.put("failure", failure);
        node.put("active", parseLong(values.get("active")));
        node.put("stateful", parseLong(values.get("stateful")));
        node.put("successRate", total < 1 ? 0D : Math.round(success * 10000D / total) / 100D);
        node.put("avgLatency", latencyCount < 1 ? 0D : Math.round(latencyTotal * 1D / latencyCount));
        node.put("maxLatency", parseLong(values.get("maxLatency")));
        node.put("lastTime", parseLong(values.get("lastTime")));
        node.put("lastError", DPUtil.parseString(values.get("lastError")));
        return node;
    }

    private void metricAcquire(ProxyGroup group, ProxyChannel channel, Resource resource, boolean stateful) {
        ObjectNode changes = DPUtil.objectNode();
        changes.put("total", 1);
        changes.put("active", 1);
        if (stateful) changes.put("stateful", 1);
        metricUpdate(group, channel, resource, changes);
    }

    private void metricComplete(ProxyGroup group, ProxyChannel channel, Resource resource,
                                boolean success, long coast) {
        ObjectNode changes = DPUtil.objectNode();
        changes.put(success ? "success" : "failure", 1);
        changes.put("active", -1);
        changes.put("latencyTotal", Math.max(0, coast));
        changes.put("latencyCount", 1);
        metricUpdate(group, channel, resource, changes);
    }

    private void metricUpdate(ProxyGroup group, ProxyChannel channel, Resource resource, ObjectNode changes) {
        String day = LocalDate.now().format(DAY_FORMAT);
        ObjectNode dimensions = DPUtil.objectNode();
        dimensions.put("group", String.valueOf(group.getId()));
        dimensions.put("channel", String.valueOf(channel.getId()));
        dimensions.put("protocol", resource.getProtocol());
        dimensions.put("resource", resource.getHost() + "|" + resource.getProtocol() + "|"
                + group.getId() + "|" + channel.getId());
        for (Map.Entry<String, JsonNode> entry : dimensions.properties()) {
            String key = metricKey(day, entry.getKey(), entry.getValue().asText());
            for (Map.Entry<String, JsonNode> change : changes.properties()) {
                long delta = change.getValue().asLong();
                if (delta == 0) continue;
                redis().opsForHash().increment(key, change.getKey(), delta);
            }
            Object active = redis().opsForHash().get(key, "active");
            if (parseLong(active) < 0) redis().opsForHash().put(key, "active", "0");
            if (changes.has("success")) redis().opsForHash().delete(key, "lastError");
            if (changes.has("latencyTotal")) {
                long latency = changes.at("/latencyTotal").asLong();
                Object maxLatency = redis().opsForHash().get(key, "maxLatency");
                if (latency > parseLong(maxLatency)) {
                    redis().opsForHash().put(key, "maxLatency", String.valueOf(latency));
                }
            }
            redis().opsForHash().put(key, "lastTime", String.valueOf(System.currentTimeMillis()));
            redis().expire(key, 7, TimeUnit.DAYS);
            redis().opsForZSet().add(indexKey(day, entry.getKey()), entry.getValue().asText(),
                    System.currentTimeMillis());
            redis().expire(indexKey(day, entry.getKey()), 8, TimeUnit.DAYS);
        }
    }

    private void metricFailure(ProxyGroup group, ProxyChannel channel, String reason) {
        String day = LocalDate.now().format(DAY_FORMAT);
        for (Map.Entry<String, String> dimension : Map.of(
                "group", String.valueOf(group.getId()),
                "channel", String.valueOf(channel.getId())).entrySet()) {
            String key = metricKey(day, dimension.getKey(), dimension.getValue());
            redis().opsForHash().increment(key, "total", 1);
            redis().opsForHash().increment(key, "failure", 1);
            redis().opsForHash().put(key, "lastTime", String.valueOf(System.currentTimeMillis()));
            redis().opsForHash().put(key, "lastError", reason);
            redis().expire(key, 7, TimeUnit.DAYS);
            redis().opsForZSet().add(indexKey(day, dimension.getKey()), dimension.getValue(),
                    System.currentTimeMillis());
            redis().expire(indexKey(day, dimension.getKey()), 8, TimeUnit.DAYS);
        }
    }

    private List<Resource> resources(ProxyGroup group, ProxyChannel channel) throws Exception {
        CachedResources cached = resourceCache.get(group.getId());
        long now = System.currentTimeMillis();
        int cacheSeconds = "static".equals(channel.getAdapter()) ? 60 : 10;
        if (null != cached && now - cached.getTime() < cacheSeconds * 1000L) {
            return cached.getResources();
        }
        JsonNode config = DPUtil.parseJSON(channel.getConfig(), j -> DPUtil.objectNode());
        JsonNode credentials = DPUtil.parseJSON(channel.getCredentials(), j -> DPUtil.objectNode());
        List<Resource> resources = "static".equals(channel.getAdapter())
                ? staticResources(config, channel)
                : remoteResources(group, channel, config, credentials);
        if (resources.isEmpty()) throw new IllegalStateException("渠道未返回代理资源");
        resourceCache.put(group.getId(), new CachedResources(resources, now));
        return resources;
    }

    private List<Resource> staticResources(JsonNode config, ProxyChannel channel) {
        List<Resource> resources = new ArrayList<>();
        JsonNode values = config.at("/resources");
        if (values.isTextual()) {
            for (String value : values.asText("").split("[\\r\\n,]+")) addResource(resources, value, channel, null);
        } else if (values.isArray()) {
            for (JsonNode value : values) {
                if (value.isTextual()) addResource(resources, value.asText(), channel, null);
                else addResource(resources, "", channel, value);
            }
        }
        return resources;
    }

    private List<Resource> remoteResources(ProxyGroup group, ProxyChannel channel, JsonNode config,
                                           JsonNode credentials) {
        String endpoint = channel.getEndpoint();
        if (DPUtil.empty(endpoint)) throw new IllegalStateException("代理渠道未配置提取地址");
        Map<String, String> params = stringMap(config.at("/params"));
        for (Map.Entry<String, String> entry : params.entrySet()) {
            params.put(entry.getKey(), resolve(entry.getValue(), group, channel, credentials));
        }
        Map<String, String> headers = stringMap(config.at("/headers"));
        JsonNode credentialHeaders = credentials.at("/headers");
        if (credentialHeaders.isObject()) headers.putAll(stringMap(credentialHeaders));
        endpoint = resolve(endpoint, group, channel, credentials);
        String method = config.at("/method").asText("GET").toUpperCase(Locale.ROOT);
        String body = null;
        if ("POST".equals(method)) {
            JsonNode bodyNode = config.at("/body");
            if (bodyNode.isObject()) body = resolve(DPUtil.stringify(bodyNode), group, channel, credentials);
            else body = resolve(config.at("/body").asText(""), group, channel, credentials);
            if (DPUtil.empty(body)) body = DPUtil.stringify(params);
            if (!headers.containsKey("Content-Type")) headers.put("Content-Type", "application/json");
            return parseResponse(HttpUtil.post(endpoint, null, body, headers, "UTF-8"),
                    group, channel, config, credentials);
        }
        return parseResponse(HttpUtil.get(endpoint, params, headers, "UTF-8"),
                group, channel, config, credentials);
    }

    private List<Resource> parseResponse(String response, ProxyGroup group, ProxyChannel channel,
                                         JsonNode config, JsonNode credentials) {
        if (DPUtil.empty(response)) throw new IllegalStateException("渠道接口无响应");
        String responseType = config.at("/responseType").asText(
                DPUtil.empty(config.at("/listPath").asText()) ? "text" : "json");
        List<Resource> resources = new ArrayList<>();
        if ("text".equalsIgnoreCase(responseType)) {
            String separator = config.at("/textSeparator").asText("\\r?\\n");
            for (String item : response.split(separator)) addResource(resources, item, channel, credentials);
            return resources;
        }
        JsonNode json = DPUtil.parseJSON(response);
        if (null == json) throw new IllegalStateException("渠道响应不是合法 JSON");
        String listPath = config.at("/listPath").asText(defaultListPath(channel.getAdapter()));
        JsonNode list = listPath.startsWith("/") ? json.at(listPath) : json.get(listPath);
        if (null == list || list.isMissingNode()) list = json;
        if (list.isArray()) {
            for (JsonNode item : list) {
                if (item.isTextual()) addResource(resources, item.asText(), channel, credentials);
                else addResource(resources, "", channel, item, config, credentials);
            }
        } else if (list.isTextual()) {
            for (String item : list.asText().split("[\\r\\n,]+")) addResource(resources, item, channel, credentials);
        } else {
            addResource(resources, "", channel, list, config, credentials);
        }
        return resources;
    }

    private void addResource(List<Resource> result, String value, ProxyChannel channel, JsonNode source) {
        addResource(result, value, channel, source, DPUtil.objectNode(), DPUtil.objectNode());
    }

    private void addResource(List<Resource> result, String value, ProxyChannel channel, JsonNode source,
                             JsonNode config, JsonNode credentials) {
        String text = DPUtil.trim(value);
        if (null != source && source.isObject()) {
            String host = source.at("/" + config.at("/hostField").asText("ip")).asText("");
            String port = source.at("/" + config.at("/portField").asText("port")).asText("");
            String username = source.at("/" + config.at("/usernameField").asText("username")).asText("");
            String password = source.at("/" + config.at("/passwordField").asText("password")).asText("");
            String protocol = source.at("/" + config.at("/protocolField").asText("protocol")).asText("");
            if (!DPUtil.empty(host) && !DPUtil.empty(port)) {
                text = (!DPUtil.empty(protocol) ? protocol : channel.getProtocol()) + "://"
                        + (!DPUtil.empty(username) ? username + ":" + password + "@" : "")
                        + host + ":" + port;
            }
        }
        if (DPUtil.empty(text)) return;
        java.util.regex.Matcher matcher = IP_PORT.matcher(text);
        if (!matcher.matches()) return;
        Resource resource = new Resource();
        resource.setProtocol(DPUtil.empty(matcher.group(1))
                ? channel.getProtocol() : matcher.group(1).toLowerCase(Locale.ROOT));
        String username = DPUtil.empty(matcher.group(2)) ? matcher.group(6) : matcher.group(2);
        String password = DPUtil.empty(matcher.group(3)) ? matcher.group(7) : matcher.group(3);
        resource.setUsername(DPUtil.empty(username) ? credentials.at("/username").asText("") : username);
        resource.setPassword(DPUtil.empty(password) ? credentials.at("/password").asText("") : password);
        resource.setHost(matcher.group(4));
        resource.setPort(DPUtil.parseInt(matcher.group(5)));
        int expireSeconds = config.at("/expireSeconds").asInt(1800);
        long expireAt = System.currentTimeMillis() + Math.max(60, expireSeconds) * 1000L;
        String expireField = config.at("/expireField").asText("");
        String expireValue = null == source || DPUtil.empty(expireField) ? "" : source.at("/" + expireField).asText("");
        if (!DPUtil.empty(expireValue) && expireValue.matches("\\d+")) {
            long parsed = Long.parseLong(expireValue);
            if (parsed > 10_000_000_000L) expireAt = parsed;
            else if (parsed > 1_000_000_000L) expireAt = parsed * 1000L;
            else if (parsed > 0) expireAt = System.currentTimeMillis() + parsed * 1000L;
        }
        resource.setExpireAt(expireAt);
        result.add(resource);
    }

    private Resource select(ProxyGroup group, ProxyChannel channel, List<Resource> resources) {
        if (resources.isEmpty()) return null;
        int index;
        if ("random".equals(group.getStrategy())) {
            index = ThreadLocalRandom.current().nextInt(resources.size());
        } else if ("least_used".equals(group.getStrategy())) {
            index = 0;
            long minimum = Long.MAX_VALUE;
            for (int i = 0; i < resources.size(); i++) {
                String resourceId = resources.get(i).getHost() + "|" + resources.get(i).getProtocol()
                        + "|" + group.getId() + "|" + channel.getId();
                Object active = redis().opsForHash().get(metricKey(LocalDate.now().format(DAY_FORMAT),
                        "resource", resourceId), "active");
                long count = parseLong(active);
                if (count < minimum) {
                    minimum = count;
                    index = i;
                }
            }
        } else {
            Long sequence = redis().opsForValue().increment(
                    nodeService.keyPrefix() + "proxy:cursor:" + group.getId());
            index = (int) Math.floorMod(null == sequence ? 0 : sequence, resources.size());
        }
        return resources.get(index);
    }

    private SessionBinding readSession(ProxyGroup group, String stateKey) {
        String value = redis().opsForValue().get(sessionKey(group.getId(), stateKey));
        JsonNode node = DPUtil.parseJSON(value);
        if (null == node || !node.isObject()) return null;
        if (node.at("/expiresAt").asLong() <= System.currentTimeMillis()) return null;
        JsonNode resource = node.at("/resource");
        SessionBinding binding = new SessionBinding();
        binding.setResource(null == resource || !resource.isObject() ? null : resource(resource));
        binding.setState(node.at("/state"));
        binding.setUses(node.at("/uses").asInt(0));
        binding.setFailureCount(node.at("/failureCount").asInt(0));
        return binding;
    }

    private Resource resource(JsonNode node) {
        if (null == node || !node.isObject()) return null;
        Resource resource = new Resource();
        resource.setHost(node.at("/host").asText(""));
        resource.setPort(node.at("/port").asInt());
        resource.setProtocol(node.at("/protocol").asText("http"));
        resource.setUsername(node.at("/username").asText(""));
        resource.setPassword(node.at("/password").asText(""));
        resource.setExpireAt(node.at("/expireAt").asLong(0));
        return DPUtil.empty(resource.getHost()) || resource.getPort() < 1 ? null : resource;
    }

    private Map<String, String> stringMap(JsonNode node) {
        Map<String, String> result = new LinkedHashMap<>();
        if (null != node && node.isObject()) {
            node.fields().forEachRemaining(entry -> result.put(entry.getKey(), entry.getValue().asText("")));
        }
        return result;
    }

    private String resolve(String value, ProxyGroup group, ProxyChannel channel, JsonNode credentials) {
        if (DPUtil.empty(value)) return value;
        Map<String, String> replacements = new LinkedHashMap<>();
        replacements.put("count", String.valueOf(null == group.getBatchSize() ? 1 : group.getBatchSize()));
        replacements.put("protocol", DPUtil.empty(group.getProtocol()) ? channel.getProtocol() : group.getProtocol());
        replacements.put("region", DPUtil.parseString(group.getRegion()));
        replacements.put("token", credentials.at("/token").asText(""));
        replacements.put("username", credentials.at("/username").asText(""));
        replacements.put("password", credentials.at("/password").asText(""));
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            value = value.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return value;
    }

    private String defaultListPath(String adapter) {
        return switch (adapter) {
            case "kuaidaili", "zhima" -> "/data/proxy_list";
            case "juliang", "ipidea" -> "/data/list";
            default -> "/data/list";
        };
    }

    private String normalizeMode(String value) {
        return "sticky".equalsIgnoreCase(value) ? "sticky" : "rotate";
    }

    private String normalizeStateMode(String value) {
        if ("task".equalsIgnoreCase(value)) return "task";
        if ("template".equalsIgnoreCase(value)) return "template";
        return "none";
    }

    private String leaseKey(String leaseId) {
        return nodeService.keyPrefix() + "proxy:lease:" + leaseId;
    }

    private String activeKey(int groupId) {
        return nodeService.keyPrefix() + "proxy:active:" + groupId;
    }

    private String sessionKey(int groupId, String stateKey) {
        return nodeService.keyPrefix() + "proxy:session:" + groupId + ":" + stateKey;
    }

    private String stateLockKey(int groupId, String stateKey) {
        return nodeService.keyPrefix() + "proxy:state-lock:" + groupId + ":" + stateKey;
    }

    private void releaseStateLock(String key, String value) {
        if (DPUtil.empty(key)) return;
        if (DPUtil.parseString(value).equals(redis().opsForValue().get(key))) {
            redis().delete(key);
        }
    }

    private String metricKey(String day, String dimension, String id) {
        String safe = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(id.getBytes(StandardCharsets.UTF_8));
        return nodeService.keyPrefix() + "proxy:metric:" + day + ":" + dimension + ":" + safe;
    }

    private String indexKey(String day, String dimension) {
        return nodeService.keyPrefix() + "proxy:metric:" + day + ":index:" + dimension;
    }

    private long parseLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        return DPUtil.parseLong(DPUtil.parseString(value));
    }

    @Data
    private static class Resource {
        private String protocol;
        private String host;
        private int port;
        private String username;
        private String password;
        private long expireAt;

        public String key() {
            return protocol + "://" + host + ":" + port;
        }

        public ObjectNode json() {
            ObjectNode node = DPUtil.objectNode();
            node.put("protocol", protocol);
            node.put("host", host);
            node.put("port", port);
            node.put("username", username);
            node.put("password", password);
            node.put("expireAt", expireAt);
            return node;
        }
    }

    @Data
    @AllArgsConstructor
    private static class CachedResources {
        private List<Resource> resources;
        private long time;
    }

    @Data
    private static class SessionBinding {
        private Resource resource;
        private JsonNode state;
        private int uses;
        private int failureCount;
    }

}
