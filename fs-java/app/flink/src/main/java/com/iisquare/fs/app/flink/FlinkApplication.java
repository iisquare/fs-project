package com.iisquare.fs.app.flink;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.flink.core.FlinkRunner;
import com.iisquare.fs.base.dag.DAGCore;
import com.iisquare.fs.base.dag.core.DAGNode;
import com.iisquare.fs.base.dag.util.DAGUtil;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Flink 作业入口。
 *
 * Flink 2.x 已移除 DataSet API，流批统一基于 DataStream API，通过 RuntimeExecutionMode 切换：
 * batch-BATCH，stream-STREAMING。
 */
public class FlinkApplication {

    public static void main(String... args) throws Exception {
        JsonNode diagram = DAGUtil.loadDiagram(args[0]);
        String model = diagram.at("/model").asText("");
        if (!"batch".equals(model) && !"stream".equals(model)) {
            throw new RuntimeException("处理模式仅支持batch与stream，当前model=" + model);
        }
        Map<String, Class<? extends DAGNode<?>>> nodes = new LinkedHashMap<>();
        nodes.putAll(DAGUtil.scanNodes(
                FlinkApplication.class.getPackage().getName() + ".node",
                DAGCore.class.getPackage().getName() + ".node",
                DAGCore.class.getPackage().getName() + ".config"));
        // 批处理与流处理节点一并纳入，具体语义由执行模式决定
        nodes.putAll(DAGUtil.scanNodes(FlinkApplication.class.getPackage().getName() + ".batch"));
        nodes.putAll(DAGUtil.scanNodes(FlinkApplication.class.getPackage().getName() + ".stream"));
        StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();
        environment.setRuntimeMode(RuntimeExecutionMode.valueOf(model.toUpperCase()));
        FlinkRunner runner = new FlinkRunner(environment, diagram, nodes);
        runner.execute();
        String jobName = diagram.at("/name").asText(FlinkApplication.class.getSimpleName());
        runner.stream().execute(jobName);
    }

}
