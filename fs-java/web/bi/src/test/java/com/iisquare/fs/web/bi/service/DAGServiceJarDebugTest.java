package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DAGServiceJarDebugTest {

    @Test
    public void debugReplacesJarUriWithTemporaryUrlFromJarId() {
        JarCatalogService jars = mock(JarCatalogService.class);
        DAGService service = new DAGService();
        service.jarCatalogService = jars;
        ObjectNode frame = frame("dag_1_jar", "读取订单", "7", "http://old/download?id=7");
        when(jars.temporaryUrl("7", 21600000)).thenReturn("http://files/order.jar?expire=21600000");

        String error = service.resolveJars(frame, 21600000);

        Assert.assertNull(error);
        Assert.assertEquals("http://files/order.jar?expire=21600000",
                frame.at("/nodes/0/options/jarURI").asText());
        Assert.assertEquals("7", frame.at("/nodes/0/options/jarId").asText());
        verify(jars).temporaryUrl("7", 21600000);
    }

    @Test
    public void debugReportsMissingJarWithoutChangingStableUri() {
        JarCatalogService jars = mock(JarCatalogService.class);
        DAGService service = new DAGService();
        service.jarCatalogService = jars;
        ObjectNode frame = frame("dag_1_jar", "读取订单", "7", "http://old/download?id=7");
        when(jars.temporaryUrl("7", 21600000)).thenReturn(null);

        String error = service.resolveJars(frame, 21600000);

        Assert.assertEquals("节点“读取订单”引用的 JAR 不存在、已删除或无法生成临时授权链接", error);
        Assert.assertEquals("http://old/download?id=7", frame.at("/nodes/0/options/jarURI").asText());
    }

    @Test
    public void localClasspathJarKeepsEmptyUri() {
        JarCatalogService jars = mock(JarCatalogService.class);
        DAGService service = new DAGService();
        service.jarCatalogService = jars;
        ObjectNode frame = frame("dag_1_jar", "本地作业", "", "");

        String error = service.resolveJars(frame, 21600000);

        Assert.assertNull(error);
        Assert.assertEquals("", frame.at("/nodes/0/options/jarURI").asText());
    }

    private ObjectNode frame(String id, String name, String jarId, String jarURI) {
        ObjectNode frame = DPUtil.objectNode();
        ArrayNode nodes = frame.putArray("nodes");
        ObjectNode node = nodes.addObject();
        node.put("id", id).put("type", "Jar").put("name", name);
        ObjectNode options = node.putObject("options");
        options.put("jarId", jarId).put("jarURI", jarURI).put("pkgClass", "com.example.JobNode");
        return frame;
    }
}
