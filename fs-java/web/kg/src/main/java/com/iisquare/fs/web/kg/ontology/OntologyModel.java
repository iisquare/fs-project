package com.iisquare.fs.web.kg.ontology;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 本体模型
 *
 * 将服务端保存的本体配置解析为实体、关系、属性的定义，作为图数据库数据管理的元数据。
 *
 * 支持的配置格式：
 * 1. 画布格式（前端 X6 设计器产物）
 * {
 *     "cells": [
 *         {"id": "n1", "shape": "kg-node", "data": {"name": "人员", "label": "Person",
 *             "primaryField": "id", "captionField": "name", "fields": [{"name": "id", "title": "标识", "type": "String"}]}},
 *         {"id": "r1", "shape": "flow-edge", "data": {"name": "任职", "label": "WORKS_FOR", "fields": []},
 *             "source": {"cell": "n1"}, "target": {"cell": "n2"}}
 *     ]
 * }
 * 2. 结构化格式（可直接由接口组装）
 * {
 *     "entities": [{"id": "person", "name": "人员", "label": "Person", "primaryField": "id", "captionField": "name", "fields": []}],
 *     "relationships": [{"id": "works", "name": "任职", "label": "WORKS_FOR", "source": "person", "target": "company", "fields": []}]
 * }
 */
public class OntologyModel {

    public static final String SHAPE_ENTITY = "kg-node";
    public static final String SHAPE_RELATIONSHIP = "flow-edge";
    public static final String SHAPE_EDGE = "edge";
    public static final Pattern SAFE_NAME = Pattern.compile("^[A-Za-z][A-Za-z0-9_]*$");

    /**
     * 属性（字段）定义
     */
    @Getter
    @Setter
    public static class Field {
        private String name = ""; // 字段名称，对应图数据库的属性名
        private String title = ""; // 显示名称
        private String type = ""; // 数据类型
        private String comment = ""; // 注释
        private boolean required; // 数据保存时必填
        private boolean display; // 是否在画布中展示（作为节点卡片的默认展示字段）

        public ObjectNode toJson() {
            ObjectNode node = DPUtil.objectNode();
            node.put("name", name);
            node.put("title", title);
            node.put("type", type);
            node.put("comment", comment);
            node.put("required", required);
            node.put("display", display);
            return node;
        }
    }

    /**
     * 实体定义，对应图数据库中的节点标签
     */
    @Getter
    @Setter
    public static class Entity {
        private String id = ""; // 画布元素标识或结构化标识
        private String code = ""; // 编码
        private String name = ""; // 显示名称
        private String label = ""; // 主标签，决定实体身份与数据管理范围
        private List<String> labels = new ArrayList<>(); // 完整标签集合，首个为主标签
        private String description = "";
        private String icon = "";
        private String color = "";
        private String primaryField = ""; // 主键字段
        private String captionField = ""; // 标题字段
        private boolean extendable; // 是否允许保存本体未声明的属性
        private boolean extendableLabels; // 是否允许数据扩展标签
        private List<Field> fields = new ArrayList<>();

        public String key() {
            if (!DPUtil.empty(label)) return label;
            if (!DPUtil.empty(code)) return code;
            return id;
        }

        public Field field(String name) {
            if (DPUtil.empty(name)) return null;
            for (Field field : fields) {
                if (name.equals(field.getName())) return field;
            }
            return null;
        }

        public boolean hasField(String name) {
            return null != field(name);
        }

        public ObjectNode toJson() {
            ObjectNode node = DPUtil.objectNode();
            node.put("id", id);
            node.put("code", code);
            node.put("name", name);
            node.put("label", label);
            ArrayNode labelsNode = node.putArray("labels");
            for (String item : labels) labelsNode.add(item);
            node.put("description", description);
            node.put("icon", icon);
            node.put("color", color);
            node.put("primaryField", primaryField);
            node.put("captionField", captionField);
            node.put("extendable", extendable);
            node.put("extendableLabels", extendableLabels);
            ArrayNode array = node.putArray("fields");
            for (Field field : fields) array.add(field.toJson());
            return node;
        }
    }

    /**
     * 关系定义，对应图数据库中的关系类型
     */
    @Getter
    @Setter
    public static class Relationship {
        private String id = "";
        private String code = "";
        private String name = "";
        private String label = ""; // 关系类型标签
        private String description = "";
        private String source = ""; // 起点实体标识
        private String target = ""; // 终点实体标识
        private List<String> mergeFields = new ArrayList<>(); // 关系键，用于区分同一对实体之间的多条同类关系
        private boolean cascadeDelete; // 删除实体时是否级联删除该关系
        private List<Field> fields = new ArrayList<>();
        private Entity sourceEntity;
        private Entity targetEntity;

        public String key() {
            if (!DPUtil.empty(label)) return label;
            if (!DPUtil.empty(code)) return code;
            return id;
        }

        public Field field(String name) {
            if (DPUtil.empty(name)) return null;
            for (Field field : fields) {
                if (name.equals(field.getName())) return field;
            }
            return null;
        }

        public boolean hasField(String name) {
            return null != field(name);
        }

        public ObjectNode toJson() {
            ObjectNode node = DPUtil.objectNode();
            node.put("id", id);
            node.put("code", code);
            node.put("name", name);
            node.put("label", label);
            node.put("description", description);
            node.put("source", source);
            node.put("target", target);
            node.put("sourceLabel", null == sourceEntity ? "" : sourceEntity.getLabel());
            node.put("targetLabel", null == targetEntity ? "" : targetEntity.getLabel());
            ArrayNode merges = node.putArray("mergeFields");
            for (String field : mergeFields) merges.add(field);
            node.put("cascadeDelete", cascadeDelete);
            ArrayNode array = node.putArray("fields");
            for (Field field : fields) array.add(field.toJson());
            return node;
        }
    }

    private List<Entity> entities = new ArrayList<>();
    private List<Relationship> relationships = new ArrayList<>();
    private List<String> issues = new ArrayList<>();
    private String format = "canvas";

    public static OntologyModel parse(JsonNode content) {
        OntologyModel model = new OntologyModel();
        if (null == content || content.isNull() || content.isMissingNode()) {
            model.issues.add("本体配置为空");
            return model;
        }
        if (content.has("entities") || content.has("relationships")) {
            model.format = "structured";
            model.parseStructured(content);
        } else {
            model.parseCanvas(content);
        }
        model.validate();
        return model;
    }

    /**
     * 使用已解析的定义构造模型，用于规范化存储的读取
     */
    public static OntologyModel build(List<Entity> entities, List<Relationship> relationships) {
        OntologyModel model = new OntologyModel();
        model.format = "definition";
        if (null != entities) model.entities.addAll(entities);
        if (null != relationships) model.relationships.addAll(relationships);
        Map<String, Entity> byId = new LinkedHashMap<>();
        for (Entity entity : model.entities) {
            if (!DPUtil.empty(entity.getId())) byId.put(entity.getId(), entity);
        }
        model.resolve(byId);
        model.validate();
        return model;
    }

    public boolean valid() {
        return issues.isEmpty();
    }

    /**
     * 按标识查找实体定义，支持元素标识、编码、标签
     */
    public Entity entity(String key) {
        if (DPUtil.empty(key)) return null;
        for (Entity entity : entities) {
            if (key.equals(entity.getId()) || key.equals(entity.getCode()) || key.equals(entity.getLabel())) {
                return entity;
            }
        }
        for (Entity entity : entities) {
            if (key.equalsIgnoreCase(entity.getCode()) || key.equalsIgnoreCase(entity.getLabel())) {
                return entity;
            }
        }
        return null;
    }

    /**
     * 按标识查找关系定义，支持元素标识、编码、标签
     */
    public Relationship relationship(String key) {
        if (DPUtil.empty(key)) return null;
        for (Relationship relationship : relationships) {
            if (key.equals(relationship.getId()) || key.equals(relationship.getCode()) || key.equals(relationship.getLabel())) {
                return relationship;
            }
        }
        for (Relationship relationship : relationships) {
            if (key.equalsIgnoreCase(relationship.getCode()) || key.equalsIgnoreCase(relationship.getLabel())) {
                return relationship;
            }
        }
        return null;
    }

    public ObjectNode toJson() {
        ObjectNode node = DPUtil.objectNode();
        node.put("format", format);
        node.put("valid", valid());
        ArrayNode entityArray = node.putArray("entities");
        for (Entity entity : entities) entityArray.add(entity.toJson());
        ArrayNode relationshipArray = node.putArray("relationships");
        for (Relationship relationship : relationships) relationshipArray.add(relationship.toJson());
        node.set("issues", DPUtil.toJSON(issues));
        return node;
    }

    protected void parseStructured(JsonNode content) {
        Map<String, Entity> byId = new LinkedHashMap<>();
        JsonNode entityNodes = content.at("/entities");
        if (entityNodes.isArray()) {
            for (JsonNode item : entityNodes) {
                Entity entity = new Entity();
                entity.setId(item.at("/id").asText(""));
                entity.setCode(item.at("/code").asText(item.at("/label").asText("")));
                entity.setName(item.at("/name").asText(""));
                entity.setLabel(item.at("/label").asText(item.at("/code").asText("")));
                entity.setLabels(parseNames(item.at("/labels")));
                entity.setDescription(item.at("/description").asText(""));
                entity.setIcon(item.at("/icon").asText(""));
                entity.setColor(item.at("/color").asText(""));
                entity.setPrimaryField(item.at("/primaryField").asText(""));
                entity.setCaptionField(item.at("/captionField").asText(""));
                entity.setExtendable(item.at("/extendable").asBoolean(false));
                entity.setExtendableLabels(item.at("/extendableLabels").asBoolean(false));
                entity.setFields(parseFields(item.at("/fields")));
                entities.add(entity);
                if (!DPUtil.empty(entity.getId())) byId.put(entity.getId(), entity);
            }
        }
        JsonNode relationshipNodes = content.at("/relationships");
        if (relationshipNodes.isArray()) {
            for (JsonNode item : relationshipNodes) {
                Relationship relationship = new Relationship();
                relationship.setId(item.at("/id").asText(""));
                relationship.setCode(item.at("/code").asText(item.at("/label").asText("")));
                relationship.setName(item.at("/name").asText(""));
                relationship.setLabel(item.at("/label").asText(item.at("/code").asText("")));
                relationship.setDescription(item.at("/description").asText(""));
                relationship.setSource(item.at("/source").asText(""));
                relationship.setTarget(item.at("/target").asText(""));
                relationship.setMergeFields(parseNames(item.at("/mergeFields")));
                relationship.setCascadeDelete(item.at("/cascadeDelete").asBoolean(false));
                relationship.setFields(parseFields(item.at("/fields")));
                relationships.add(relationship);
            }
        }
        resolve(byId);
    }

    protected void parseCanvas(JsonNode content) {
        Map<String, Entity> byId = new LinkedHashMap<>();
        JsonNode cells = content.at("/cells");
        if (!cells.isArray()) return;
        for (JsonNode cell : cells) {
            String shape = cell.at("/shape").asText("");
            JsonNode data = cell.at("/data");
            if (SHAPE_ENTITY.equals(shape)) {
                Entity entity = new Entity();
                entity.setId(cell.at("/id").asText(""));
                entity.setCode(data.at("/code").asText(""));
                entity.setName(data.at("/name").asText(""));
                entity.setLabel(data.at("/label").asText(""));
                entity.setLabels(parseNames(data.at("/labels")));
                entity.setDescription(data.at("/description").asText(""));
                entity.setIcon(data.at("/icon").asText(""));
                entity.setColor(data.at("/color").asText(""));
                entity.setPrimaryField(data.at("/primaryField").asText(""));
                entity.setCaptionField(data.at("/captionField").asText(""));
                entity.setExtendable(data.at("/extendable").asBoolean(false));
                entity.setExtendableLabels(data.at("/extendableLabels").asBoolean(false));
                entity.setFields(parseFields(data.at("/fields")));
                entities.add(entity);
                if (!DPUtil.empty(entity.getId())) byId.put(entity.getId(), entity);
            } else if (SHAPE_RELATIONSHIP.equals(shape) || SHAPE_EDGE.equals(shape)) {
                Relationship relationship = new Relationship();
                relationship.setId(cell.at("/id").asText(""));
                relationship.setCode(data.at("/code").asText(""));
                relationship.setName(data.at("/name").asText(""));
                relationship.setLabel(data.at("/label").asText(""));
                relationship.setDescription(data.at("/description").asText(""));
                relationship.setSource(cell.at("/source/cell").asText(""));
                relationship.setTarget(cell.at("/target/cell").asText(""));
                relationship.setMergeFields(parseNames(data.at("/mergeFields")));
                relationship.setCascadeDelete(data.at("/cascadeDelete").asBoolean(false));
                relationship.setFields(parseFields(data.at("/fields")));
                relationships.add(relationship);
            }
        }
        resolve(byId);
    }

    protected List<Field> parseFields(JsonNode array) {
        List<Field> fields = new ArrayList<>();
        if (null == array || !array.isArray()) return fields;
        for (JsonNode item : array) {
            Field field = new Field();
            field.setName(DPUtil.trim(item.at("/name").asText("")));
            field.setTitle(item.at("/title").asText(""));
            field.setType(item.at("/type").asText(""));
            field.setComment(item.at("/comment").asText(""));
            field.setRequired(item.at("/required").asBoolean(false));
            field.setDisplay(item.at("/display").asBoolean(false));
            fields.add(field);
        }
        return fields;
    }

    protected List<String> parseNames(JsonNode array) {
        List<String> names = new ArrayList<>();
        if (null == array || !array.isArray()) return names;
        for (JsonNode item : array) {
            String name = DPUtil.trim(item.asText(""));
            if (!DPUtil.empty(name)) names.add(name);
        }
        return names;
    }

    /**
     * 解析关系两端的实体引用
     */
    protected void resolve(Map<String, Entity> byId) {
        for (Relationship relationship : relationships) {
            relationship.setSourceEntity(lookup(byId, relationship.getSource()));
            relationship.setTargetEntity(lookup(byId, relationship.getTarget()));
        }
    }

    protected Entity lookup(Map<String, Entity> byId, String key) {
        if (DPUtil.empty(key)) return null;
        Entity entity = byId.get(key);
        if (null != entity) return entity;
        return entity(key);
    }

    /**
     * 校验本体定义，返回问题列表
     */
    public List<String> validate() {
        issues = new ArrayList<>();
        Map<String, Entity> labels = new LinkedHashMap<>();
        Map<String, Entity> combinations = new LinkedHashMap<>();
        for (Entity entity : entities) {
            normalizeLabels(entity);
            String label = entity.getLabel();
            if (DPUtil.empty(label)) {
                issues.add("存在未设置标签的实体");
            } else if (!SAFE_NAME.matcher(label).matches()) {
                issues.add(String.format("实体标签[%s]只能由字母、数字、下划线组成且以字母开头", label));
            } else if (null != labels.put(label, entity)) {
                issues.add(String.format("实体标签[%s]重复", label));
            }
            for (String item : entity.getLabels()) {
                if (item.equals(label)) continue;
                if (!SAFE_NAME.matcher(item).matches()) {
                    issues.add(String.format("实体[%s]的附加标签[%s]只能由字母、数字、下划线组成且以字母开头", entity.key(), item));
                }
            }
            if (!entity.getLabels().isEmpty()) {
                List<String> sorted = new ArrayList<>(entity.getLabels());
                Collections.sort(sorted);
                String combination = DPUtil.implode(":", sorted.toArray(new String[0]));
                if (null != combinations.put(combination, entity)) {
                    issues.add(String.format("实体标签组合[%s]重复", combination));
                }
            }
            if (DPUtil.empty(entity.getCode())) entity.setCode(label);
            if (DPUtil.empty(entity.getName())) entity.setName(entity.getCode());
            validateFields(String.format("实体[%s]", entity.key()), entity.getFields());
            if (DPUtil.empty(entity.getPrimaryField())) {
                issues.add(String.format("实体[%s]未设置主键字段", entity.key()));
            } else if (!entity.hasField(entity.getPrimaryField())) {
                issues.add(String.format("实体[%s]主键字段[%s]未在字段列表中定义", entity.key(), entity.getPrimaryField()));
            }
            if (!DPUtil.empty(entity.getCaptionField()) && !entity.hasField(entity.getCaptionField())) {
                issues.add(String.format("实体[%s]标题字段[%s]未在字段列表中定义", entity.key(), entity.getCaptionField()));
            }
        }
        for (Entity entity : entities) {
            for (String item : entity.getLabels()) {
                if (item.equals(entity.getLabel())) continue;
                Entity owner = labels.get(item);
                if (null != owner && owner != entity) {
                    issues.add(String.format("提示：实体[%s]的附加标签[%s]同时是实体[%s]的主标签，相关数据会同时出现在两个实体的列表中",
                            entity.key(), item, owner.key()));
                }
            }
        }
        Map<String, Relationship> types = new LinkedHashMap<>();
        for (Relationship relationship : relationships) {
            String label = DPUtil.trim(relationship.getLabel());
            relationship.setLabel(label);
            if (DPUtil.empty(label)) {
                issues.add("存在未设置标签的关系");
            } else if (!SAFE_NAME.matcher(label).matches()) {
                issues.add(String.format("关系标签[%s]只能由字母、数字、下划线组成且以字母开头", label));
            } else if (null != types.put(label, relationship)) {
                issues.add(String.format("关系标签[%s]重复", label));
            }
            if (DPUtil.empty(relationship.getCode())) relationship.setCode(label);
            if (DPUtil.empty(relationship.getName())) relationship.setName(relationship.getCode());
            validateFields(String.format("关系[%s]", relationship.key()), relationship.getFields());
            if (null == relationship.getSourceEntity()) {
                issues.add(String.format("关系[%s]起点实体[%s]不存在", relationship.key(), relationship.getSource()));
            }
            if (null == relationship.getTargetEntity()) {
                issues.add(String.format("关系[%s]终点实体[%s]不存在", relationship.key(), relationship.getTarget()));
            }
        }
        return issues;
    }

    protected void validateFields(String prefix, List<Field> fields) {
        Map<String, Field> names = new LinkedHashMap<>();
        for (Field field : fields) {
            String name = DPUtil.trim(field.getName());
            field.setName(name);
            if (DPUtil.empty(name)) {
                issues.add(String.format("%s存在未命名的字段", prefix));
            } else if (!SAFE_NAME.matcher(name).matches()) {
                issues.add(String.format("%s字段[%s]只能由字母、数字、下划线组成且以字母开头", prefix, name));
            } else if (null != names.put(name, field)) {
                issues.add(String.format("%s字段[%s]重复", prefix, name));
            }
            if (DPUtil.empty(field.getTitle())) field.setTitle(field.getName());
        }
    }

    /**
     * 标签集合归一化：去重去空，主标签固定在首位
     */
    protected void normalizeLabels(Entity entity) {
        List<String> labels = new ArrayList<>();
        for (String item : entity.getLabels()) {
            item = DPUtil.trim(item);
            if (DPUtil.empty(item) || labels.contains(item)) continue;
            labels.add(item);
        }
        String primary = DPUtil.trim(entity.getLabel());
        if (DPUtil.empty(primary) && !labels.isEmpty()) primary = labels.get(0);
        if (!DPUtil.empty(primary)) {
            labels.remove(primary);
            labels.add(0, primary);
        }
        entity.setLabel(primary);
        entity.setLabels(labels);
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public List<Relationship> getRelationships() {
        return relationships;
    }

    public List<String> getIssues() {
        return issues;
    }

    public String getFormat() {
        return format;
    }

    /**
     * 本体中全部实体标签
     */
    public List<String> entityLabels() {
        List<String> labels = new ArrayList<>();
        for (Entity entity : entities) {
            if (!DPUtil.empty(entity.getLabel())) labels.add(entity.getLabel());
        }
        return labels;
    }

    /**
     * 本体中全部关系类型
     */
    public List<String> relationshipLabels() {
        List<String> labels = new ArrayList<>();
        for (Relationship relationship : relationships) {
            if (!DPUtil.empty(relationship.getLabel())) labels.add(relationship.getLabel());
        }
        return labels;
    }

    /**
     * 转换为画布格式，使结构化定义保存后仍可被前端设计器打开
     */
    public ObjectNode toCanvasJson() {
        ObjectNode content = DPUtil.objectNode();
        ArrayNode cells = content.putArray("cells");
        Map<String, String> references = new LinkedHashMap<>();
        int index = 0;
        for (Entity entity : entities) {
            String id = DPUtil.empty(entity.getId()) ? String.format("node_%d", index) : entity.getId();
            references.put(entity.getId(), id);
            references.put(entity.getCode(), id);
            references.put(entity.getLabel(), id);
            ObjectNode cell = DPUtil.objectNode();
            cell.put("id", id);
            cell.put("shape", SHAPE_ENTITY);
            cell.put("zIndex", index + 1);
            cell.put("x", 60 + (index % 4) * 340);
            cell.put("y", 60 + (index / 4) * 180);
            cell.put("width", 260);
            cell.put("height", 88);
            ObjectNode data = cell.putObject("data");
            data.put("name", entity.getName());
            data.put("code", entity.getCode());
            data.put("label", entity.getLabel());
            ArrayNode labelsNode = data.putArray("labels");
            for (String item : entity.getLabels()) labelsNode.add(item);
            data.put("type", "Node");
            data.put("description", entity.getDescription());
            data.put("icon", entity.getIcon());
            data.put("color", entity.getColor());
            data.put("primaryField", entity.getPrimaryField());
            data.put("captionField", entity.getCaptionField());
            data.put("extendable", entity.isExtendable());
            data.put("extendableLabels", entity.isExtendableLabels());
            ArrayNode fields = data.putArray("fields");
            for (Field field : entity.getFields()) fields.add(field.toJson());
            cells.add(cell);
            index++;
        }
        index = 0;
        for (Relationship relationship : relationships) {
            String id = DPUtil.empty(relationship.getId()) ? String.format("edge_%d", index) : relationship.getId();
            ObjectNode cell = DPUtil.objectNode();
            cell.put("id", id);
            cell.put("shape", SHAPE_RELATIONSHIP);
            cell.put("zIndex", 0);
            ObjectNode data = cell.putObject("data");
            data.put("name", relationship.getName());
            data.put("code", relationship.getCode());
            data.put("label", relationship.getLabel());
            data.put("description", relationship.getDescription());
            ArrayNode merges = data.putArray("mergeFields");
            for (String field : relationship.getMergeFields()) merges.add(field);
            data.put("cascadeDelete", relationship.isCascadeDelete());
            ArrayNode fields = data.putArray("fields");
            for (Field field : relationship.getFields()) fields.add(field.toJson());
            ObjectNode source = cell.putObject("source");
            source.put("cell", references.getOrDefault(relationship.getSource(), ""));
            source.put("port", "right");
            ObjectNode target = cell.putObject("target");
            target.put("cell", references.getOrDefault(relationship.getTarget(), ""));
            target.put("port", "left");
            cells.add(cell);
            index++;
        }
        return content;
    }

}
