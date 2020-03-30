package com.iisquare.fs.flink.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.flink.util.FlinkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Runner {

    private Event event;

    public Runner(Event event) {
        this.event = event;
    }

    private boolean process(Map<String, Node> nodeMap) throws Exception {
        // 查找入度为零的全部节点
        List<Node> list = new ArrayList<>();
        for (Map.Entry<String, Node> entry : nodeMap.entrySet()) {
            Node node = entry.getValue();
            if(node.isReady()) continue;
            Set<Node> set = node.getSource();
            boolean sourceReady = true;
            for (Node source : set) {
                if(source.isReady()) continue;
                sourceReady = false;
                break;
            }
            if(sourceReady) {
                list.add(node);
            }
        }
        // 执行任务
        for (Node node : list) {
            String nodeId = node.getId();
            event.onNodeStart(node);
            Object result = node.process();
            event.onNodeEnd(node, result);
            if(null != result) return false;
            node.setReady(true);
        }
        return !list.isEmpty();
    }

    public JobExecutionResult execute(String name, JsonNode flow) throws Exception {
        ExecutionEnvironment batch = null;
        StreamExecutionEnvironment stream = null;
        // 配置环境参数
        String type = flow.get("type").asText();
        int parallelism = 1;
        switch (type) {
            case "batch":
                batch = ExecutionEnvironment.getExecutionEnvironment();
                batch.setRestartStrategy(restartStrategyConfiguration(flow.get("restart")));
                parallelism = batch.getParallelism();
                break;
            case "stream":
                stream = StreamExecutionEnvironment.getExecutionEnvironment();
                parallelism = stream.getParallelism();
                if(flow.has("checkpoint")) {
                    JsonNode checkpoint = flow.get("checkpoint");
                    long interval = checkpoint.get("interval").asLong();
                    CheckpointingMode mode = null;
                    if(checkpoint.has("mode")) {
                        switch (checkpoint.get("mode").asText()) {
                            case "AT_LEAST_ONCE":
                                mode = CheckpointingMode.AT_LEAST_ONCE;
                                break;
                            case "EXACTLY_ONCE":
                                mode = CheckpointingMode.EXACTLY_ONCE;
                                break;
                        }
                    }
                    if(null == mode) {
                        stream.enableCheckpointing(interval);
                    } else {
                        stream.enableCheckpointing(interval, mode);
                    }
                }
                stream.setRestartStrategy(restartStrategyConfiguration(flow.get("restart")));
                break;
            default:
                return null;
        }
        // 解析节点
        Iterator<JsonNode> it = flow.get("nodes").elements();
        Map<String, Node> nodes = new LinkedHashMap<>();
        while (it.hasNext()) {
            JsonNode item = it.next();
            Map<String, TypeInformation> infoType = new LinkedHashMap<>();
            Map<String, String> fieldReflect = new LinkedHashMap<>();
            Iterator<JsonNode> iterator = item.get("returns").elements();
            while (iterator.hasNext()) {
                JsonNode ret = iterator.next();
                String field = ret.get("field").asText();
                infoType.put(field, FlinkUtil.convertType(ret.get("classname").asText()));
                String reflect = ret.has("from") ? ret.get("from").asText() : field;
                fieldReflect.put(field, reflect);
            }
            List<URL> urls = new ArrayList<>();
            Iterator<JsonNode> itplg = flow.get("plugins").get(item.get("plugin").asText()).elements();
            while (itplg.hasNext()) {
                urls.add(new URL(itplg.next().asText()));
            }
            Node node;
            if(urls.size() > 0) {
                ClassLoader classLoader = new ClassLoader(urls.toArray(new URL[urls.size()]), getClass().getClassLoader());
                node = (Node) classLoader.loadClass(item.get("classname").asText()).newInstance();
            } else {
                node = (Node) Class.forName(item.get("classname").asText()).newInstance();
            }
            String id = item.get("id").asText();
            node.setFlow(flow);
            node.setEnvironment(type);
            node.setBatchEnvironment(batch);
            node.setStreamEnvironment(stream);
            node.setId(id);
            node.setConfig(DPUtil.stringify(item));
            node.setTypeInfo(new RowTypeInfo(
                infoType.values().toArray(new TypeInformation[infoType.size()]),
                infoType.keySet().toArray(new String[infoType.size()])));
            node.setFieldReflect(fieldReflect);
            int parallel = item.has("parallelism") ? DPUtil.parseInt(item.get("parallelism").asText()) : parallelism;
            if(parallel < 1) parallel = parallelism;
            node.setParallelism(parallel);
            node.setProperties(DPUtil.objectNode());
            nodes.put(id, node);
        }
        // 解析连线
        it = flow.get("connections").elements();
        while (it.hasNext()) {
            JsonNode item = it.next();
            Node source = nodes.get(item.get("sourceId").asText());
            Node target = nodes.get(item.get("targetId").asText());
            source.getTarget().add(target);
            target.getSource().add(source);
        }
        // 查找入度为零的节点并执行
        while(process(nodes)) {}
        // 返回执行结果
        switch (type) {
            case "batch":
                return batch.execute(name);
            case "stream":
                return stream.execute(name);
                default:
                    return null;
        }
    }

    private RestartStrategies.RestartStrategyConfiguration restartStrategyConfiguration(JsonNode restart) {
        if(null == restart) return RestartStrategies.noRestart();
        if(restart.has("fixedDelay")) {
            restart = restart.get("fixedDelay");
            return RestartStrategies.fixedDelayRestart(
                restart.get("restartAttempts").asInt(), restart.get("delayBetweenAttempts").asLong());
        } else if(restart.has("failureRate")) {
            restart = restart.get("failureRate");
            return RestartStrategies.failureRateRestart(
                restart.get("failureRate").asInt(),
                Time.of(restart.get("failureInterval").asLong(), TimeUnit.MILLISECONDS),
                Time.of(restart.get("delayInterval").asLong(), TimeUnit.MILLISECONDS));
        } else {
            return RestartStrategies.fallBackRestart();
        }
    }

}
