package com.iisquare.fs.flink.plugins.olap.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.flink.flow.BatchNode;
import com.iisquare.fs.flink.flow.Node;
import com.iisquare.fs.flink.flow.StreamNode;
import com.iisquare.fs.flink.plugins.core.node.SQLTransformNode;
import org.apache.flink.api.common.functions.GroupReduceFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.java.BatchTableEnvironment;
import org.apache.flink.types.Row;
import org.apache.flink.util.Collector;

import java.text.SimpleDateFormat;
import java.util.*;

@SuppressWarnings("unchecked")
public class AnalysisNode extends Node {

    private final String SP = "___";
    private final String TN = "_result_";

    @Override
    public JsonNode run() throws Exception {
        return DPUtil.parseJSON(this.config);
    }

    @Override
    protected StreamNode stream() {
        return null;
    }

    private static void groupSortAndLimit(Iterable<Map<String, Object>> values, Map<String, String> group, String sort, int limit, ArrayNode result) {
        Set<Map.Entry<String, String>> entrySet = group.entrySet();
        Map<String, List<JsonNode>> groupMap = new LinkedHashMap<>();
        for (Map<String, Object> item : values) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : entrySet) {
                sb.append(item.get(entry.getKey()));
            }
            String key = sb.toString();
            List<JsonNode> list;
            if (groupMap.containsKey(key)) {
                list = groupMap.get(key);
                list.add(DPUtil.convertJSON(item));
            } else {
                list = new LinkedList<>();
                list.add(DPUtil.convertJSON(item));
                groupMap.put(key, list);
            }
        }
        for (Map.Entry<String, List<JsonNode>> entry : groupMap.entrySet()) {
            List<JsonNode> list = entry.getValue();
            for (String sortStr : DPUtil.explode(DPUtil.trim(DPUtil.parseString(sort)))) {
                String[] sortParams = DPUtil.explode(sortStr, " ");
                String field = sortParams[0];
                int dir = sortParams.length > 1 && sortParams[1].toLowerCase().equals("desc") ? -1 : 1;
                Collections.sort(list, new Comparator<JsonNode>() {
                    @Override
                    public int compare(JsonNode o1, JsonNode o2) {
                        JsonNode f1 = o1.findPath(field);
                        JsonNode f2 = o2.findPath(field);
                        if (f1.isNumber()) {
                            Double v1 = DPUtil.parseDouble(f1);
                            Double v2 = DPUtil.parseDouble(f2);
                            return v1.compareTo(v2) * dir;
                        } else {
                            return f1.asText().compareTo(f2.asText()) * dir;
                        }
                    }
                });
            }
            int count = 0;
            for (JsonNode node : list) {
                if (limit == 0 || count++ < limit) {
                    result.add(node);
                }
            }
        }
    }

    private Map<String, String> table(BatchTableEnvironment envTable, Set<Node> nodes, JsonNode tables) {
        Map<String, String> result = new LinkedHashMap<>();
        Map<Integer, Node> warehouses = new LinkedHashMap<>();
        for (Node node : nodes) {
            int id = DPUtil.parseJSON(node.getConfig()).get("warehouse").asInt();
            warehouses.put(id, node);
        }
        Iterator<JsonNode> iterator = tables.elements();
        while (iterator.hasNext()) {
            JsonNode table = iterator.next();
            String alias = table.get("alias").asText();
            if (DPUtil.empty(alias)) alias = table.get("id").asText();
            Node node = warehouses.get(table.get("from").asInt());
            DataSet<Row> data = node.getBatch().result().map(SQLTransformNode.map2row()).returns(node.getTypeInfo());
            Set<String> columns = new LinkedHashSet<>();
            Iterator<JsonNode> elements = table.get("columns").elements();
            while (elements.hasNext()) {
                JsonNode element = elements.next();
                if (element.get("checked").asBoolean()) {
                    columns.add(element.get("field").asText());
                }
            }
            if (columns.size() < 1) {
                envTable.registerDataSet(alias, data);
            } else {
                envTable.registerDataSet(alias, data, DPUtil.implode(",", columns.toArray(new String[columns.size()])));
            }
            result.put(table.get("id").asText(), alias);
        }
        return result;
    }

    private String where(JsonNode where, String glue) {
        if (null == where) return null;
        List<String> result = new ArrayList<>();
        Iterator<JsonNode> iterator = where.elements();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            String type = item.get("type").asText();
            switch (type) {
                case "RELATION":
                    String children = where(item.get("children"), item.get("value").asText());
                    if (null != children) result.add(children);
                    break;
                case "FILTER":
                    String left = DPUtil.trim(item.get("left").asText());
                    if (left.length() < 1) left = "''";
                    String right = DPUtil.trim(item.get("right").asText());
                    if (right.length() < 1) right = "''";
                    String operation = item.get("value").asText();
                    switch (operation) {
                        case "EQUAL":
                            result.add(left + "=" + right);
                            break;
                        case "NOT_EQUAL":
                            result.add(left + "<>" + right);
                            break;
                        case "LESS_THAN":
                            result.add(left + "<" + right);
                            break;
                        case "LESS_THAN_OR_EQUAL":
                            result.add(left + "<=" + right);
                            break;
                        case "GREATER_THAN":
                            result.add(left + ">" + right);
                            break;
                        case "GREATER_THAN_OR_EQUAL":
                            result.add(left + ">=" + right);
                            break;
                        case "IS_NULL":
                            result.add(left + " IS NULL");
                            break;
                        case "IS_NOT_NULL":
                            result.add(left + " IS NOT NULL");
                            break;
                        case "IN":
                            result.add(left + " IN (" + right + ")");
                            break;
                        case "NOT_IN":
                            result.add(left + " NOT IN (" + right + ")");
                            break;
                        default:
                            throw new RuntimeException("Filter Expression Operation [" + operation + "] is not supported");
                    }
                    break;
                default:
                    throw new RuntimeException("Filter Expression Type [" + type + "] is not supported");
            }
        }
        if (result.size() > 0) {
            glue = null == glue ? " AND " : (" " + glue + " ");
            return "(" + DPUtil.implode(glue, result.toArray(new String[result.size()])) + ")";
        }
        return null;
    }

    private void relation(List<String> list, String tableId, Map<String, String> tables, JsonNode relations) {
        // 查找该节点的关联记录
        List<JsonNode> lines = new ArrayList<>();
        Iterator<JsonNode> iterator = relations.elements();
        while (iterator.hasNext()) {
            ObjectNode relation = (ObjectNode) iterator.next();
            if (relation.has("isReady")) continue; // 忽略已处理节点
            if (tableId.equals(relation.get("targetId").asText())) { // 交换方向
                relation.put("targetId", relation.get("sourceId").asText());
                relation.put("sourceId", tableId);
                switch (relation.get("operation").asText()) {
                    case "LeftJoin":relation.put("operation", "RightJoin");break;
                    case "RightJoin":relation.put("operation", "LeftJoin");break;
                }
            }
            if (tableId.equals(relation.get("sourceId").asText())) {
                relation.put("isReady", true);
                lines.add(relation);
            }
        }
        // 关联目标节点
        for (JsonNode line : lines) {
            String operation = line.get("operation").asText();
            String targetId = line.get("targetId").asText();
            String where = null;
            switch (operation) {
                case "InnerJoin":
                    list.add("INNER JOIN " + tables.get(targetId));
                    where = where(line.get("filter"), null);
                    break;
                case "LeftJoin":
                    list.add("LEFT JOIN " + tables.get(targetId));
                    where = where(line.get("filter"), null);
                    break;
                case "RightJoin":
                    list.add("RIGHT JOIN " + tables.get(targetId));
                    where = where(line.get("filter"), null);
                    break;
                case "FullJoin":
                    list.add("FULL OUTER JOIN " + tables.get(targetId));
                    where = where(line.get("filter"), null);
                    break;
                case "DistinctUnion":
                    list.add("UNION " + tables.get(targetId));
                    break;
                case "FullUnion":
                    list.add("UNION ALL " + tables.get(targetId));
                    break;
                default:
                    throw new RuntimeException("Operation [" + operation + "] in " + tableId + " is not supported");
            }
            if (null != where) list.add("ON " + where);
            relation(list, targetId, tables, relations); // 深度优先进行遍历
        }
    }

    private String sql(BatchTableEnvironment envTable, Map<String, String> tables, JsonNode content) throws Exception {
        // 生成查询字段
        Set<String> select = new LinkedHashSet<>();
        tables.forEach((String key, String table) -> {
            String[] columns = envTable.scan(table).getSchema().getColumnNames();
            for (String column : columns) {
                select.add(table + "." + column + " AS " + table + SP + column);
            }
        });
        // 任意选择一个节点
        String tableId = tables.keySet().iterator().next();
        // 递归关联节点
        List<String> relation = new ArrayList<>();
        relation(relation, tableId, tables, content.get("relations"));
        // 生成查询条件
        String where = where(content.get("filterExpression"), null);
        // 组合SQL语句
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(DPUtil.implode(",", select.toArray(new String[select.size()])));
        sql.append(" FROM ").append(tables.get(tableId));
        if (relation.size() > 0) {
            sql.append(" ").append(DPUtil.implode(" ", relation.toArray(new String[relation.size()])));
        }
        if (null != where) {
            sql.append(" WHERE ").append(where);
        }
        return sql.toString();
    }

    private ObjectNode columns(JsonNode columns) {
        ObjectNode result = DPUtil.objectNode();
        Iterator<JsonNode> iterator = columns.elements();
        while (iterator.hasNext()) {
            ObjectNode item = (ObjectNode) iterator.next();
            item.put("name", item.get("name").asText().replaceAll("\\.", SP));
            result.replace(DPUtil.trim(item.get("alias").asText()), item);
        }
        return result;
    }

    private LinkedHashMap<String, String> as(LinkedHashMap<String, String> properties) {
        LinkedHashMap<String, String> result = new LinkedHashMap();
        properties.forEach((String key, String value) -> {
            result.put(key, value + " AS " + key);
        });
        return result;
    }

    private LinkedHashMap fields(JsonNode fields, JsonNode columns) throws Exception {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        Iterator<JsonNode> elements = fields.elements();
        while (elements.hasNext()) {
            String field = elements.next().asText();
            String name = columns.get(field).get("name").asText();
            String type = columns.get(field).get("type").asText();
            String value = columns.get(field).get("value").asText();
            switch (type) {
                case "java.custom.Calculation":
                    switch (value) {
                        case "SUM": case "COUNT": case "MAX": case "MIN": case "AVG":
                            result.put(field, value + "(" + name + ")");
                            break;
                        case "COUNT_DISTINCT":
                            result.put(field, "COUNT(DISTINCT " + name + ")");
                            break;
                        default:
                            throw new Exception("Calculation [" + value + "] for [" + field + "] not supported");
                    }
                    break;
                case "java.custom.State":
                    throw new Exception("Filed type java.custom.State not supported yet");
                default:
                    result.put(field, name);
            }
        }
        return result;
    }

    private DataSet<JsonNode> group(BatchTableEnvironment envTable, JsonNode grades, LinkedHashMap<String, String> parent) throws Exception {
        DataSet<JsonNode> result = null;
        Iterator<JsonNode> iterator = grades.elements();
        while (iterator.hasNext()) {
            ObjectNode grade = (ObjectNode) iterator.next();
            // 排序
            String sort = DPUtil.trim(grade.get("sort").asText());
            // 条数限制
            int limit = grade.get("limit").asInt();
            StringBuilder sql = new StringBuilder();
            sql.append(grade.get("sql").asText());
            if (!DPUtil.empty(sort)) sql.append(" order by ").append(sort);
            if (limit > 0) sql.append(" limit ").append(limit);
            Table table;
            try {
                table = envTable.sqlQuery(sql.toString());
            } catch (Exception e) {
                throw new Exception("Group SQL Error:" + sql.toString(), e);
            }
            // 分组
            LinkedHashMap<String, String> group = new LinkedHashMap<>();
            group.putAll(parent);
            String rowkey = DPUtil.trim(grade.get("rowkey").asText());
            String[] columnNames = DPUtil.empty(rowkey) ? table.getSchema().getColumnNames() : DPUtil.explode(rowkey);
            group.putAll(DPUtil.buildMap(columnNames, columnNames, String.class, String.class));
            grade.replace("primaries", DPUtil.convertJSON(columnNames));
            ArrayList<String> columns = new ArrayList<>();
            columns.addAll(Arrays.asList(table.getSchema().getColumnNames()));
            columns.removeAll(parent.keySet());
            columns.removeAll(Arrays.asList(columnNames));
            grade.replace("columns", DPUtil.convertJSON(columns));
            // 获取子集
            DataSet<JsonNode> children = group(envTable, grade.get("children"), group);
            String gradeId = grade.get("id").asText();
            // 获取数据
            DataSet<JsonNode> dataSet = envTable.toDataSet(table, Row.class).map(SQLTransformNode.row2map(SQLTransformNode.info(table))).reduceGroup(new GroupReduceFunction<Map<String, Object>, JsonNode>() {
                @Override
                public void reduce(Iterable<Map<String, Object>> values, Collector<JsonNode> out) throws Exception {
                    ObjectNode result = DPUtil.objectNode();
                    result.put("gradeId", gradeId);
                    ArrayNode list = result.putArray("list");
                    for (Map<String, Object> value : values) {
                        ObjectNode item = (ObjectNode) DPUtil.convertJSON(value);
                        ArrayNode groups = item.putArray("group");
                        for (String key : group.keySet()) groups.add(key);
                        list.add(item);
                    }
                    out.collect(result);
                }
            });
            result = result == null ? dataSet : result.union(dataSet);
            if (children != null) result = result.union(children);
        }
        return result;
    }

    private DataSet<JsonNode> group(BatchTableEnvironment envTable, JsonNode columns, JsonNode grades, LinkedHashMap<String, String> parent) throws Exception {
        DataSet<JsonNode> result = null;
        Iterator<JsonNode> iterator = grades.elements();
        while (iterator.hasNext()) {
            JsonNode grade = iterator.next();
            // 提取主键
            LinkedHashMap<String, String> primaries = fields(grade.get("primaries"), columns);
            // 提取属性
            LinkedHashMap<String, String> properties = new LinkedHashMap<>();
            properties.putAll(as(parent)); // 将上级主键加入属性列表
            properties.putAll(as(primaries)); // 将主键加入属性列表
            properties.putAll(as(fields(grade.get("columns"), columns))); // 加入属性
            // 排序
            String sort = DPUtil.trim(grade.get("sort").asText());
            // 条数限制
            int limit = grade.get("limit").asInt();
            // 分组
            LinkedHashMap<String, String> group = new LinkedHashMap<>();
            group.putAll(parent);
            group.putAll(primaries);
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ").append(DPUtil.implode(",", properties.values().toArray(new String[properties.size()])))
                .append(" FROM ").append(TN).append(" GROUP BY ")
                .append(DPUtil.implode(",", group.values().toArray(new String[group.size()])));
            Table table;
            try {
                table = envTable.sqlQuery(sql.toString());
            } catch (Exception e) {
                throw new Exception("Group SQL Error:" + sql.toString(), e);
            }
            // 获取子集
            DataSet<JsonNode> children = group(envTable, columns, grade.get("children"), group);
            String gradeId = grade.get("id").asText();
            // 获取数据
            DataSet<JsonNode> dataSet = envTable.toDataSet(table, Row.class).map(SQLTransformNode.row2map(SQLTransformNode.info(table))).reduceGroup(new GroupReduceFunction<Map<String, Object>, JsonNode>() {
                @Override
                public void reduce(Iterable<Map<String, Object>> values, Collector<JsonNode> out) throws Exception {
                    ObjectNode result = DPUtil.objectNode();
                    result.put("gradeId", gradeId);
                    ArrayNode arrayNode = result.putArray("list");
                    //分组排序取topN
                    groupSortAndLimit(values, parent, sort, limit, arrayNode);
                    for (JsonNode node : arrayNode) {
                        ArrayNode groups = ((ObjectNode) node).putArray("group");
                        for (Object object : group.keySet().toArray()) {
                            groups.add(DPUtil.convertJSON(object));
                        }
                    }
                    out.collect(result);
                }
            });
            result = result == null ? dataSet : result.union(dataSet);
            if (children != null) result = result.union(children);
        }
        return result;
    }

    @Override
    protected BatchNode batch() {
        return new BatchNode() {
            @Override
            public DataSet<Map<String, Object>> run(JsonNode config) throws Exception {
                BatchTableEnvironment envTable = TableEnvironment.getTableEnvironment(environment());
                String type = config.get("info").get("type").asText();
                JsonNode content = DPUtil.parseJSON(config.get("info").get("content").asText());
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                dateFormat.setTimeZone(TimeZone.getTimeZone(config.get("timezone").asText()));
                String id = config.get("id").asText().replace("OLAPAnalysis", "") + "-" + dateFormat.format(new Date());
                String contentStr = DPUtil.stringify(content);
                String flowStr = DPUtil.stringify(current.getFlow());
                String settingStr = config.get("info").get("setting").asText();
                Map<String, String> tables = table(envTable, source, content.get("tables"));
                JsonNode columns = content.get("columns"), grades = content.get("grades");
                DataSet<JsonNode> group = null;
                switch (type) {
                    case "sql":
                        columns = DPUtil.arrayNode();
                        group = group(envTable, grades, new LinkedHashMap<>());
                        break;
                    default:
                        String sql = sql(envTable, tables, content);
                        try {
                            envTable.registerTable(TN, envTable.sqlQuery(sql));
                        } catch (Exception e) {
                            throw new Exception("SQL Query Failed:" + sql, e);
                        }
                        group = group(envTable, columns(columns), grades, new LinkedHashMap<>());
                }
                String columnsStr = DPUtil.stringify(columns), gradesStr = DPUtil.stringify(grades);
                return group.reduceGroup(new GroupReduceFunction<JsonNode, Map<String, Object>>() {
                    @Override
                    public void reduce(Iterable<JsonNode> values, Collector<Map<String, Object>> out) throws Exception {
                        JsonNode content = DPUtil.parseJSON(contentStr);
                        if (content == null || !content.has("grades")) {
                            return;
                        }
                        ObjectNode gradeResult = DPUtil.objectNode();
                        for (JsonNode item : values) {
                            gradeResult.set(item.get("gradeId").asText(), item.get("list"));
                        }
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("_id", id);
                        map.put("log:type", type);
                        map.put("log:columns", columnsStr);
                        map.put("log:grades", gradesStr);
                        map.put("log:flow", flowStr);
                        map.put("log:content", DPUtil.stringify(result(content.get("grades"), gradeResult)));
                        JsonNode setting = DPUtil.parseJSON(settingStr);
                        if (setting != null && setting.has("ttl")) {
                            long ttl = setting.get("ttl").asLong();
                            if (ttl > 0) {
                                map.put("_ttl", ttl);
                            }
                        }
                        out.collect(map);
                    }

                    // 组合数据
                    private ArrayNode result(JsonNode grades, JsonNode gradesResult) {
                        ArrayNode result = DPUtil.arrayNode();
                        Iterator<JsonNode> iterator = grades.elements();
                        // 遍历当前层级分组配置
                        while (iterator.hasNext()) {
                            ArrayNode gradeResult = DPUtil.arrayNode();
                            JsonNode grade = iterator.next();
                            Iterator<JsonNode> gradeIterator = gradesResult.findPath(grade.get("id").asText()).elements();
                            // 递归获取子节点数据
                            ArrayNode childrenResult = result(grade.get("children"), gradesResult);
                            // 遍历当前层级节点数据
                            while (gradeIterator.hasNext()) {
                                ObjectNode gradeItem = (ObjectNode) gradeIterator.next();
                                ArrayNode gradeChildren = gradeItem.putArray("children");
                                Iterator<JsonNode> childrenIterator = childrenResult.elements();
                                while (childrenIterator.hasNext()) {
                                    ArrayNode gradeChild = DPUtil.arrayNode();
                                    Iterator<JsonNode> childIterator = childrenIterator.next().elements();
                                    while (childIterator.hasNext()) {
                                        ObjectNode childItem = DPUtil.objectNode();
                                        childItem.setAll((ObjectNode) childIterator.next());
                                        Iterator<JsonNode> groupIterator = gradeItem.get("group").elements();
                                        boolean isParent = true;
                                        while (groupIterator.hasNext()) {
                                            String field = groupIterator.next().asText();
                                            // 判断子节点是否属于当前层级节点
                                            if (!childItem.get(field).asText().equals(gradeItem.get(field).asText())) {
                                                isParent = false;
                                                break;
                                            }
                                            // 移除子节点中的父节点字段
                                            childItem.remove(field);
                                        }
                                        if (isParent) {
                                            gradeChild.add(childItem);
                                        }
                                    }
                                    gradeChildren.add(gradeChild);
                                }
                                gradeItem.remove("group");
                                gradeResult.add(gradeItem);
                            }
                            result.add(gradeResult);
                        }
                        return result;
                    }
                });
            }
        };
    }
}
