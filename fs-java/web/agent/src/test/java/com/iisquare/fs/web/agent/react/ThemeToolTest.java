package com.iisquare.fs.web.agent.react;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ThemeToolTest {

    @Test
    public void reusesFieldsAlreadyLoadedByThemeInfo() {
        ObjectNode info = themeInfo(true, true);
        StubThemeTool tool = new StubThemeTool(info);
        Assert.assertEquals("id", tool.columns(info.at("/datasets/0")).at("/0/name").asText());

        ObjectNode dictionary = tool.dictionary(1);

        Assert.assertEquals(0, tool.batchCalls);
        Assert.assertEquals("orders", dictionary.at("/datasets/0/name").asText());
        Assert.assertEquals("id", dictionary.at("/datasets/0/columns/0/name").asText());
        Assert.assertEquals("customers", dictionary.at("/datasets/1/name").asText());
        Assert.assertEquals("customer_id", dictionary.at("/datasets/1/columns/0/name").asText());
    }

    @Test
    public void batchLoadsOnlyDatasetsWithoutFields() {
        StubThemeTool tool = new StubThemeTool(themeInfo(true, false));

        ObjectNode dictionary = tool.dictionary(1);

        Assert.assertEquals(1, tool.batchCalls);
        Assert.assertEquals(Set.of(2), tool.batchIds);
        Assert.assertEquals("id", dictionary.at("/datasets/0/columns/0/name").asText());
        Assert.assertEquals("customer_id", dictionary.at("/datasets/1/columns/0/name").asText());
    }

    @Test
    public void queryLoadsThemeInfoWithoutRepeatingFieldDictionary() {
        StubThemeTool tool = new StubThemeTool(themeInfo(true, true));
        ObjectNode item = DPUtil.objectNode().put("themeId", 1);
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("sql", "select date_format(created_at, '%Y-%m') as month from orders");

        JsonNode result = (JsonNode) tool.invoke(null, item, args,
                new LinkedHashMap<>(), new LinkedHashMap<>());

        Assert.assertEquals(1, tool.infoCalls);
        Assert.assertEquals(1, tool.queryDataCalls);
        Assert.assertEquals(0, tool.batchCalls);
        Assert.assertEquals("销售主题", result.at("/themeName").asText());
        Assert.assertEquals("month", result.at("/data/columns/0/name").asText());
        Assert.assertFalse(result.has("datasets"));
        Assert.assertFalse(result.has("description"));
    }

    @Test
    public void definitionsUseBasicThemeInfoOnly() {
        StubThemeTool tool = new StubThemeTool(themeInfo(true, true));
        ObjectNode item = DPUtil.objectNode().put("themeId", 1).put("name", "sales");

        tool.definitions(DPUtil.arrayNode().add(item));

        Assert.assertEquals(0, tool.infoCalls);
        Assert.assertEquals(1, tool.basicInfoCalls);
    }

    private ObjectNode themeInfo(boolean firstFields, boolean secondFields) {
        ObjectNode info = DPUtil.objectNode();
        info.put("id", 1);
        info.put("name", "销售主题");
        info.put("description", "销售分析");
        info.put("status", 1);
        info.putObject("content").putArray("datasetIds").add(1).add(2);
        var datasets = info.putArray("datasets");
        datasets.add(dataset(1, "orders", firstFields ? "id" : ""));
        datasets.add(dataset(2, "customers", secondFields ? "customer_id" : ""));
        return info;
    }

    private ObjectNode dataset(int id, String name, String field) {
        ObjectNode dataset = DPUtil.objectNode();
        dataset.put("id", id);
        dataset.put("name", name);
        dataset.put("description", name + " description");
        var fields = dataset.putArray("fields");
        if (!DPUtil.empty(field)) {
            fields.addObject().put("name", field).put("title", field)
                    .put("type", "string").put("comment", field + " comment");
        }
        return dataset;
    }

    private static class StubThemeTool extends ThemeTool {
        private final JsonNode info;
        private int batchCalls;
        private int infoCalls;
        private int basicInfoCalls;
        private int queryDataCalls;
        private Set<Integer> batchIds = new LinkedHashSet<>();

        private StubThemeTool(JsonNode info) {
            this.info = info;
        }

        @Override
        protected JsonNode info(int themeId) {
            infoCalls++;
            return info;
        }

        @Override
        protected Map<Integer, JsonNode> infos(Set<Integer> ids) {
            basicInfoCalls++;
            Map<Integer, JsonNode> result = new LinkedHashMap<>();
            for (Integer id : ids) result.put(id, info);
            return result;
        }

        @Override
        protected JsonNode queryData(int themeId, String themeName, String sql, int limit) {
            queryDataCalls++;
            ObjectNode data = DPUtil.objectNode();
            data.putArray("columns").addObject().put("name", "month").put("type", "varchar");
            data.putArray("rows").addObject().put("month", "2026-11");
            return data;
        }

        @Override
        protected Map<String, JsonNode> columns(Set<Integer> ids) {
            batchCalls++;
            batchIds = new LinkedHashSet<>(ids);
            ObjectNode data = DPUtil.objectNode();
            var rows = data.putArray("rows");
            for (Integer id : ids) {
                ObjectNode row = rows.addObject();
                row.put("id", id);
                row.put("name", id == 2 ? "customers" : "orders");
                row.put("description", "batch " + id);
                row.putArray("columns").addObject()
                        .put("name", id == 2 ? "customer_id" : "id")
                        .put("title", id == 2 ? "customer_id" : "id")
                        .put("type", "string")
                        .put("comment", "");
            }
            Map<String, JsonNode> result = new java.util.LinkedHashMap<>();
            for (JsonNode row : rows) result.put(row.at("/id").asText(""), row);
            return result;
        }
    }
}
