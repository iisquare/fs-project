package com.iisquare.fs.web.kg.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 图数据库结构定义
 *
 * 索引与约束的统一描述，作为接口参数、登记存储、结构对账的公共模型。
 * 该模型不受本体定义限制，可描述图数据库中的任意索引与约束。
 */
public class SchemaDefinition {

    public static final String KIND_INDEX = "INDEX";
    public static final String KIND_CONSTRAINT = "CONSTRAINT";
    public static final String TYPE_NODE = "NODE";
    public static final String TYPE_REL = "REL";

    public static final Set<String> INDEX_TYPES = new LinkedHashSet<>(
            Arrays.asList("RANGE", "TEXT", "POINT", "LOOKUP"));
    public static final Set<String> CONSTRAINT_TYPES = new LinkedHashSet<>(
            Arrays.asList("UNIQUE", "NOT_NULL", "KEY", "RELATIONSHIP_KEY", "TYPE"));
    public static final Set<String> ENTERPRISE_TYPES = new LinkedHashSet<>(
            Arrays.asList("NOT_NULL", "KEY", "RELATIONSHIP_KEY", "TYPE"));
    public static final Set<String> PROPERTY_TYPES = new LinkedHashSet<>(
            Arrays.asList("STRING", "BOOLEAN", "INTEGER", "FLOAT", "DATE",
                    "LOCAL TIME", "ZONED TIME", "LOCAL DATETIME", "ZONED DATETIME", "DURATION", "POINT"));

    private String kind = KIND_INDEX;
    private String name = "";
    private String ontologyType = TYPE_NODE;
    private String subType = "";
    private List<String> labels = new ArrayList<>();
    private List<String> fields = new ArrayList<>();
    private String propertyType = "";
    private int ontologyId = 0;

    /**
     * 解析接口参数，兼容历史参数名（indexType、constraintType）
     */
    public static SchemaDefinition parse(Map<String, Object> param) {
        SchemaDefinition definition = new SchemaDefinition();
        String indexType = DPUtil.parseString(param.get("indexType")).toUpperCase();
        String constraintType = DPUtil.parseString(param.get("constraintType")).toUpperCase();
        String kind = DPUtil.parseString(param.get("kind")).toUpperCase();
        if (DPUtil.empty(kind)) kind = DPUtil.empty(constraintType) ? KIND_INDEX : KIND_CONSTRAINT;
        definition.setKind(kind);
        definition.setName(DPUtil.parseString(param.get("name")));
        String subType = DPUtil.parseString(param.get("subType")).toUpperCase();
        if (DPUtil.empty(subType)) subType = KIND_CONSTRAINT.equals(kind) ? constraintType : indexType;
        if (DPUtil.empty(subType)) subType = KIND_CONSTRAINT.equals(kind) ? "UNIQUE" : "RANGE";
        definition.setSubType(subType);
        definition.setOntologyType(ontologyType(param.get("ontologyType")));
        definition.setLabels(labels(param.get("labels"), param.get("label")));
        definition.setFields(fieldList(param.get("fields")));
        definition.setPropertyType(DPUtil.parseString(param.get("propertyType")).toUpperCase());
        definition.setOntologyId(DPUtil.parseInt(param.get("ontologyId")));
        return definition;
    }

    /**
     * 解析登记表或方案中的结构化定义
     */
    public static SchemaDefinition parse(JsonNode node) {
        SchemaDefinition definition = new SchemaDefinition();
        if (null == node || node.isNull() || node.isMissingNode()) return definition;
        definition.setKind(DPUtil.parseString(node.at("/kind").asText("INDEX")).toUpperCase());
        definition.setName(node.at("/name").asText(""));
        definition.setSubType(node.at("/subType").asText("").toUpperCase());
        definition.setOntologyType(ontologyType(node.at("/ontologyType").asText("")));
        definition.setLabels(labels(node.at("/labels"), node.at("/label")));
        definition.setFields(fieldList(node.at("/fields")));
        definition.setPropertyType(node.at("/propertyType").asText("").toUpperCase());
        definition.setOntologyId(node.at("/ontologyId").asInt(0));
        return definition;
    }

    /**
     * 由 SHOW INDEXES 结果构造
     */
    public static SchemaDefinition fromIndexRow(JsonNode row) {
        SchemaDefinition definition = new SchemaDefinition();
        definition.setKind(KIND_INDEX);
        definition.setName(row.at("/name").asText(""));
        definition.setSubType(DPUtil.parseString(row.at("/type").asText("RANGE")).toUpperCase());
        definition.setOntologyType(ontologyType(row.at("/entityType").asText("")));
        definition.setLabels(textList(row.at("/labelsOrTypes")));
        definition.setFields(textList(row.at("/properties")));
        return definition;
    }

    /**
     * 由 SHOW CONSTRAINTS 结果构造
     */
    public static SchemaDefinition fromConstraintRow(JsonNode row) {
        SchemaDefinition definition = new SchemaDefinition();
        definition.setKind(KIND_CONSTRAINT);
        definition.setName(row.at("/name").asText(""));
        definition.setSubType(constraintSubType(row.at("/type").asText("")));
        definition.setOntologyType(ontologyType(row.at("/entityType").asText("")));
        definition.setLabels(textList(row.at("/labelsOrTypes")));
        definition.setFields(textList(row.at("/properties")));
        definition.setPropertyType(DPUtil.parseString(row.at("/propertyType").asText("")).toUpperCase());
        return definition;
    }

    /**
     * 约束类型归一化，屏蔽不同版本的命名差异
     */
    public static String constraintSubType(String type) {
        String value = DPUtil.parseString(type).toUpperCase().replace("_", " ");
        if (value.contains("KEY")) return value.startsWith("REL") ? "RELATIONSHIP_KEY" : "KEY";
        if (value.contains("UNIQUE")) return "UNIQUE";
        if (value.contains("EXIST")) return "NOT_NULL";
        if (value.contains("TYPE")) return "TYPE";
        return value.replace(" ", "_");
    }

    public static String ontologyType(Object value) {
        String type = DPUtil.parseString(value).toUpperCase();
        if (type.startsWith("REL")) return TYPE_REL;
        return TYPE_NODE;
    }

    public static List<String> fieldList(Object object) {
        List<String> fields = new ArrayList<>();
        JsonNode node = DPUtil.toJSON(object);
        if (null == node || node.isNull() || node.isMissingNode()) return fields;
        if (node.isArray()) {
            for (JsonNode item : node) {
                String name = DPUtil.trim(item.asText(""));
                if (!DPUtil.empty(name)) fields.add(name);
            }
            return fields;
        }
        for (String name : DPUtil.parseStringList(node.asText(""))) {
            name = DPUtil.trim(name);
            if (!DPUtil.empty(name)) fields.add(name);
        }
        return fields;
    }

    /**
     * 标签集合解析，支持数组与"A:B"字符串形式
     */
    public static List<String> labels(JsonNode array, JsonNode text) {
        List<String> result = textList(array);
        if (result.isEmpty()) {
            String value = null == text || text.isNull() || text.isMissingNode() ? "" : text.asText("");
            for (String item : DPUtil.parseString(value).split(":")) {
                item = DPUtil.trim(item);
                if (!DPUtil.empty(item)) result.add(item);
            }
        }
        return result;
    }

    public static List<String> labels(Object array, Object text) {
        List<String> result = fieldList(array);
        if (result.isEmpty() && !DPUtil.empty(text)) {
            for (String item : DPUtil.parseString(text).split(":")) {
                item = DPUtil.trim(item);
                if (!DPUtil.empty(item)) result.add(item);
            }
        }
        return result;
    }

    protected static String firstText(JsonNode array) {
        if (null != array && array.isArray() && !array.isEmpty()) return array.get(0).asText("");
        return "";
    }

    protected static List<String> textList(JsonNode array) {
        List<String> values = new ArrayList<>();
        if (null == array || !array.isArray()) return values;
        for (JsonNode item : array) values.add(item.asText(""));
        return values;
    }

    /**
     * 名称校验
     *
     * 名称可能来自图数据库中已存在的手工结构（允许中文等字符），因此只拦截会破坏语句的字符。
     */
    public String validateName() {
        if (DPUtil.empty(name)) return "名称不能为空";
        if (name.length() > 128) return "名称长度不能超过128个字符";
        if (name.contains("`")) return "名称不能包含反引号";
        if (name.contains("\n") || name.contains("\r") || name.contains("\t")) {
            return "名称不能包含换行或制表符";
        }
        return null;
    }

    /**
     * 校验定义，返回错误信息，校验通过返回null
     */
    public String validate() {
        if (!KIND_INDEX.equals(kind) && !KIND_CONSTRAINT.equals(kind)) return "结构类型不合法";
        String nameMessage = validateName();
        if (null != nameMessage) return nameMessage;
        if (KIND_INDEX.equals(kind)) {
            if (!INDEX_TYPES.contains(subType)) return "索引类型不合法";
            if ("LOOKUP".equals(subType)) return null;
            String labelMessage = validateLabels();
            if (null != labelMessage) return labelMessage;
            if (fields.isEmpty()) return "字段不能为空";
            return validateFields();
        }
        if (!CONSTRAINT_TYPES.contains(subType)) return "约束类型不合法";
        String labelMessage = validateLabels();
        if (null != labelMessage) return labelMessage;
        if (fields.isEmpty()) return "字段不能为空";
        String message = validateFields();
        if (null != message) return message;
        switch (subType) {
            case "UNIQUE":
                return null;
            case "NOT_NULL":
                return fields.size() > 1 ? "属性存在性约束仅支持单个字段" : null;
            case "KEY":
                return TYPE_NODE.equals(ontologyType) ? null : "键约束仅支持节点";
            case "RELATIONSHIP_KEY":
                return TYPE_REL.equals(ontologyType) ? null : "关系键约束仅支持关系";
            case "TYPE":
                if (fields.size() > 1) return "属性类型约束仅支持单个字段";
                if (DPUtil.empty(propertyType)) propertyType = "STRING";
                return PROPERTY_TYPES.contains(propertyType) ? null : "属性类型不合法";
            default:
                return "约束类型不合法";
        }
    }

    protected String validateFields() {
        Set<String> names = new LinkedHashSet<>();
        for (String field : fields) {
            if (!DPUtil.isMatcher("^[A-Za-z0-9_]+$", field)) {
                return String.format("字段[%s]只能由字母、数字、下划线组成", field);
            }
            if (!names.add(field)) return String.format("字段[%s]重复", field);
        }
        return null;
    }

    protected String validateLabels() {
        if (labels.isEmpty()) return "标签不能为空";
        Set<String> names = new LinkedHashSet<>();
        for (String label : labels) {
            if (!DPUtil.isMatcher("^[A-Za-z][A-Za-z0-9_]*$", label)) {
                return String.format("标签[%s]只能由字母、数字、下划线组成且以字母开头", label);
            }
            if (!names.add(label)) return String.format("标签[%s]重复", label);
        }
        return null;
    }

    /**
     * 创建语句
     */
    public String statement() {
        if (KIND_INDEX.equals(kind)) {
            StringBuilder sb = new StringBuilder("CREATE ");
            if (!"RANGE".equals(subType)) sb.append(subType).append(" ");
            sb.append("INDEX `").append(name).append("` IF NOT EXISTS ");
            if ("LOOKUP".equals(subType)) {
                sb.append(isNode() ? "FOR (n) ON EACH labels(n)" : "FOR ()-[r]-() ON EACH type(r)");
                return sb.toString();
            }
            sb.append(isNode() ? "FOR (nor" + labelPattern() + ") ON (" : "FOR ()-[nor" + labelPattern() + "]-() ON (");
            for (String field : fields) sb.append("nor.`").append(field).append("`, ");
            sb.setLength(sb.length() - 2);
            return sb.append(")").toString();
        }
        String target = isNode() ? "FOR (nor" + labelPattern() + ")" : "FOR ()-[nor" + labelPattern() + "]-()";
        String property = 1 == fields.size() ? "nor.`" + fields.get(0) + "`" : "(" + propertyList() + ")";
        String suffix;
        switch (subType) {
            case "UNIQUE":
                suffix = property + " IS UNIQUE";
                break;
            case "NOT_NULL":
                suffix = property + " IS NOT NULL";
                break;
            case "KEY":
                suffix = property + " IS NODE KEY";
                break;
            case "RELATIONSHIP_KEY":
                suffix = property + " IS RELATIONSHIP KEY";
                break;
            case "TYPE":
                suffix = property + " IS :: " + (DPUtil.empty(propertyType) ? "STRING" : propertyType);
                break;
            default:
                suffix = property;
        }
        return "CREATE CONSTRAINT `" + name + "` IF NOT EXISTS " + target + " REQUIRE " + suffix;
    }

    /**
     * 删除语句
     */
    public String dropStatement() {
        return (KIND_INDEX.equals(kind) ? "DROP INDEX `" : "DROP CONSTRAINT `") + name + "` IF EXISTS";
    }

    /**
     * 结构签名，用于与数据库实际结构比对，与名称无关
     */
    public String signature() {
        String value = "TYPE".equals(subType) ? propertyType : "";
        List<String> sorted = new ArrayList<>(labels);
        Collections.sort(sorted);
        return DPUtil.implode("|", new Object[]{
                kind, subType, ontologyType, DPUtil.implode(":", sorted.toArray(new String[0])),
                DPUtil.implode(",", fields.toArray(new String[0])), value
        });
    }

    public boolean isNode() {
        return !TYPE_REL.equals(ontologyType);
    }

    public boolean enterpriseOnly() {
        return ENTERPRISE_TYPES.contains(subType);
    }

    public String propertyList() {
        List<String> items = new ArrayList<>();
        for (String field : fields) items.add("nor.`" + field + "`");
        return DPUtil.implode(", ", items.toArray(new String[0]));
    }

    /**
     * 标签模式，用于Cypher语句拼接
     */
    public String labelPattern() {
        StringBuilder sb = new StringBuilder();
        for (String label : labels) sb.append(":`").append(label).append("`");
        return sb.toString();
    }

    public ObjectNode toJson() {
        ObjectNode node = DPUtil.objectNode();
        node.put("kind", kind);
        node.put("name", name);
        node.put("ontologyType", ontologyType);
        node.put("subType", subType);
        node.put("label", getLabel());
        ArrayNode labelArray = node.putArray("labels");
        for (String label : labels) labelArray.add(label);
        ArrayNode array = node.putArray("fields");
        for (String field : fields) array.add(field);
        node.put("propertyType", propertyType);
        node.put("ontologyId", ontologyId);
        node.put("statement", statement());
        node.put("enterpriseOnly", enterpriseOnly());
        return node;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOntologyType() {
        return ontologyType;
    }

    public void setOntologyType(String ontologyType) {
        this.ontologyType = ontologyType;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public String getLabel() {
        return labels.isEmpty() ? "" : labels.get(0);
    }

    public void setLabel(String label) {
        this.labels = new ArrayList<>();
        if (!DPUtil.empty(label)) this.labels.add(label);
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = null == labels ? new ArrayList<>() : labels;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public String getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(String propertyType) {
        this.propertyType = propertyType;
    }

    public int getOntologyId() {
        return ontologyId;
    }

    public void setOntologyId(int ontologyId) {
        this.ontologyId = ontologyId;
    }

}
