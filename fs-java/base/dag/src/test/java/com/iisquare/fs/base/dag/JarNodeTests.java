package com.iisquare.fs.base.dag;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGConfig;
import com.iisquare.fs.base.dag.core.DAGException;
import com.iisquare.fs.base.dag.core.DAGSink;
import com.iisquare.fs.base.dag.core.DAGSource;
import com.iisquare.fs.base.dag.core.DAGTransform;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.node.JarNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * JAR 作业节点本地类路径加载回归测试。
 */
public class JarNodeTests {

    public static class LocalTransformNode extends DAGTransform<String> {

        @Override
        public DAGType<String> outputType() {
            return DAGType.of(String.class);
        }

        @Override
        public String process() {
            return "local-classpath";
        }
    }

    public static class LocalSourceNode extends DAGSource<String> {

        @Override
        public DAGType<String> outputType() {
            return DAGType.of(String.class);
        }

        @Override
        public String process() {
            return "local-source";
        }
    }

    public static class LocalSinkNode extends DAGSink<String> {

        @Override
        public DAGType<String> outputType() {
            return DAGType.of(String.class);
        }

        @Override
        public String process() {
            return "local-sink";
        }
    }

    public static class LocalConfigNode extends DAGConfig {

        @Override
        public com.fasterxml.jackson.databind.JsonNode process() {
            return DPUtil.objectNode();
        }
    }

    @Test
    public void loadFromContextClasspathWithoutJar() throws Exception {
        assertLocalExecution(LocalTransformNode.class, "local-classpath");
        assertLocalExecution(LocalSourceNode.class, "local-source");
        assertLocalExecution(LocalSinkNode.class, "local-sink");
    }

    @Test
    public void rejectUnsupportedConfigNode() {
        JarNode node = localNode(LocalConfigNode.class);
        Assert.assertTrue(node.configure());
        Assert.assertThrows(DAGException.class, node::process);
    }

    private void assertLocalExecution(Class<?> nodeClass, String expected) throws Exception {
        JarNode node = localNode(nodeClass);
        Assert.assertTrue(node.configure());
        Assert.assertEquals(expected, node.process());
    }

    private JarNode localNode(Class<?> nodeClass) {
        ObjectNode options = DPUtil.objectNode();
        options.put("localMode", true);
        options.put("jarId", "");
        options.put("jarURI", "");
        options.put("pkgClass", nodeClass.getName());
        JarNode node = new JarNode();
        node.setOptions(options);
        return node;
    }
}
