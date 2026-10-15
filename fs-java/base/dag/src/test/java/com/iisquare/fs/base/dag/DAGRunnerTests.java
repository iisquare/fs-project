package com.iisquare.fs.base.dag;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGConfig;
import com.iisquare.fs.base.dag.core.DAGExecution;
import com.iisquare.fs.base.dag.core.DAGNode;
import com.iisquare.fs.base.dag.core.DAGRunner;
import com.iisquare.fs.base.dag.core.DAGSink;
import com.iisquare.fs.base.dag.core.DAGType;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DAGRunner 调度行为回归测试。
 */
public class DAGRunnerTests {

    /**
     * 计数配置节点：配置节点应在 configure 阶段执行，且一次运行只执行一次
     */
    public static class CountConfigNode extends DAGConfig {

        static final AtomicInteger PROCESSED = new AtomicInteger(0);

        @Override
        public JsonNode process() {
            PROCESSED.incrementAndGet();
            return DPUtil.objectNode().put("flag", true);
        }
    }

    /**
     * 采集输出节点：记录 configure 阶段收到的上游配置，供断言使用
     */
    public static class CollectSinkNode extends DAGSink<Void> {

        static volatile JsonNode received = null;

        @Override
        public DAGType<Void> outputType() {
            return DAGType.of(Void.class, "Void");
        }

        @Override
        public boolean configure(JsonNode... configs) {
            super.configure(configs);
            received = configs.length > 0 ? configs[0] : null;
            return true;
        }

        @Override
        public Void process() {
            return null;
        }
    }

    private static class TestRunner extends DAGRunner {

        TestRunner(JsonNode diagram, Map<String, Class<? extends DAGNode<?>>> nodes) {
            super(diagram, nodes);
        }
    }

    /**
     * 配置节点既不应在运行阶段被重复执行，也不应出现在执行报告的数据节点中
     */
    @Test
    public void configNodeProcessedOnce() {
        CountConfigNode.PROCESSED.set(0);
        CollectSinkNode.received = null;
        JsonNode diagram = DPUtil.parseJSON("{\"nodes\":["
                + "{\"id\":\"c1\",\"type\":\"CountConfig\",\"name\":\"c1\",\"options\":{}},"
                + "{\"id\":\"s1\",\"type\":\"CollectSink\",\"name\":\"s1\",\"options\":{}}"
                + "],\"edges\":[{\"source\":\"c1\",\"target\":\"s1\"}]}");
        Map<String, Class<? extends DAGNode<?>>> nodes = new LinkedHashMap<>();
        nodes.put("CountConfig", CountConfigNode.class);
        nodes.put("CollectSink", CollectSinkNode.class);
        DAGExecution execution = new TestRunner(diagram, nodes).execute();
        Assert.assertEquals("配置节点应只执行一次", 1, CountConfigNode.PROCESSED.get());
        Assert.assertTrue("执行应成功", execution.succeed());
        Assert.assertNotNull("下游应收到配置节点输出", CollectSinkNode.received);
        Assert.assertTrue("下游应收到配置节点输出", CollectSinkNode.received.at("/flag").asBoolean());
        Assert.assertNull("配置节点不应出现在执行报告的数据节点中", execution.item("c1"));
    }

}
