package com.iisquare.fs.web.kg.service;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.kg.dao.OntologyEntityDao;
import com.iisquare.fs.web.kg.dao.OntologyEntityFieldDao;
import com.iisquare.fs.web.kg.dao.OntologyEntityLabelDao;
import com.iisquare.fs.web.kg.dao.OntologyRelationshipDao;
import com.iisquare.fs.web.kg.dao.OntologyRelationshipFieldDao;
import com.iisquare.fs.web.kg.entity.OntologyEntity;
import com.iisquare.fs.web.kg.entity.OntologyEntityField;
import com.iisquare.fs.web.kg.entity.OntologyEntityLabel;
import com.iisquare.fs.web.kg.entity.OntologyRelationship;
import com.iisquare.fs.web.kg.entity.OntologyRelationshipField;
import com.iisquare.fs.web.kg.ontology.OntologyModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 本体定义的规范化存储
 *
 * 实体、关系、属性以独立数据表保存，画布布局仍保留在本体的content字段。
 * 规范化存储是数据管理与结构方案生成的权威定义来源。
 */
@Service
public class OntologyDefinitionService {

    @Autowired
    OntologyEntityDao ontologyEntityDao;
    @Autowired
    OntologyEntityFieldDao ontologyEntityFieldDao;
    @Autowired
    OntologyEntityLabelDao ontologyEntityLabelDao;
    @Autowired
    OntologyRelationshipDao ontologyRelationshipDao;
    @Autowired
    OntologyRelationshipFieldDao ontologyRelationshipFieldDao;

    /**
     * 读取规范化定义
     */
    public OntologyModel load(Integer ontologyId) {
        List<OntologyEntity> entities = ontologyEntityDao.findAllByOntologyIdOrderBySortAscIdAsc(ontologyId);
        List<OntologyRelationship> relationships = ontologyRelationshipDao.findAllByOntologyIdOrderBySortAscIdAsc(ontologyId);
        // 批量读取标签与字段，避免逐实体查询
        List<Integer> entityIds = new ArrayList<>();
        for (OntologyEntity row : entities) entityIds.add(row.getId());
        List<Integer> relationshipIds = new ArrayList<>();
        for (OntologyRelationship row : relationships) relationshipIds.add(row.getId());
        Map<Integer, List<OntologyEntityLabel>> labelGroups = new LinkedHashMap<>();
        Map<Integer, List<OntologyEntityField>> entityFieldGroups = new LinkedHashMap<>();
        Map<Integer, List<OntologyRelationshipField>> relationshipFieldGroups = new LinkedHashMap<>();
        if (!entityIds.isEmpty()) {
            for (OntologyEntityLabel item : ontologyEntityLabelDao.findAllByEntityIdInOrderBySortAscIdAsc(entityIds)) {
                labelGroups.computeIfAbsent(item.getEntityId(), key -> new ArrayList<>()).add(item);
            }
            for (OntologyEntityField item : ontologyEntityFieldDao.findAllByEntityIdInOrderBySortAscIdAsc(entityIds)) {
                entityFieldGroups.computeIfAbsent(item.getEntityId(), key -> new ArrayList<>()).add(item);
            }
        }
        if (!relationshipIds.isEmpty()) {
            for (OntologyRelationshipField item : ontologyRelationshipFieldDao.findAllByRelationshipIdInOrderBySortAscIdAsc(relationshipIds)) {
                relationshipFieldGroups.computeIfAbsent(item.getRelationshipId(), key -> new ArrayList<>()).add(item);
            }
        }
        Map<Integer, String> labels = new LinkedHashMap<>();
        List<OntologyModel.Entity> entityModels = new ArrayList<>();
        for (OntologyEntity row : entities) {
            OntologyModel.Entity entity = new OntologyModel.Entity();
            entity.setId(String.valueOf(row.getId()));
            entity.setCode(row.getCode());
            entity.setName(row.getName());
            entity.setLabel(row.getLabel());
            entity.setDescription(row.getDescription());
            entity.setIcon(row.getIcon());
            entity.setColor(row.getColor());
            entity.setPrimaryField(row.getPrimaryField());
            entity.setCaptionField(row.getCaptionField());
            entity.setExtendable(1 == DPUtil.parseInt(row.getExtendable()));
            entity.setExtendableLabels(1 == DPUtil.parseInt(row.getExtendableLabels()));
            List<String> entityLabels = new ArrayList<>();
            for (OntologyEntityLabel item : labelGroups.getOrDefault(row.getId(), new ArrayList<>())) {
                if (!DPUtil.empty(item.getLabel())) entityLabels.add(item.getLabel());
            }
            if (entityLabels.isEmpty() && !DPUtil.empty(row.getLabel())) entityLabels.add(row.getLabel());
            entity.setLabels(entityLabels);
            for (OntologyEntityField field : entityFieldGroups.getOrDefault(row.getId(), new ArrayList<>())) {
                entity.getFields().add(field(field.getName(), field.getTitle(), field.getType(),
                        field.getRequiredFlag(), field.getDisplayFlag(), field.getComment()));
            }
            entityModels.add(entity);
            labels.put(row.getId(), row.getLabel());
        }
        List<OntologyModel.Relationship> relationshipModels = new ArrayList<>();
        for (OntologyRelationship row : relationships) {
            OntologyModel.Relationship relationship = new OntologyModel.Relationship();
            relationship.setId(String.valueOf(row.getId()));
            relationship.setCode(row.getCode());
            relationship.setName(row.getName());
            relationship.setLabel(row.getLabel());
            relationship.setDescription(row.getDescription());
            relationship.setSource(labels.getOrDefault(row.getSourceEntityId(), ""));
            relationship.setTarget(labels.getOrDefault(row.getTargetEntityId(), ""));
            relationship.setMergeFields(DPUtil.parseStringList(row.getMergeFields()));
            relationship.setCascadeDelete(1 == DPUtil.parseInt(row.getCascadeDelete()));
            for (OntologyRelationshipField field : relationshipFieldGroups.getOrDefault(row.getId(), new ArrayList<>())) {
                relationship.getFields().add(field(field.getName(), field.getTitle(), field.getType(),
                        field.getRequiredFlag(), field.getDisplayFlag(), field.getComment()));
            }
            relationshipModels.add(relationship);
        }
        return OntologyModel.build(entityModels, relationshipModels);
    }

    /**
     * 保存规范化定义，实体与关系的标识保持稳定，已移除的定义同步清理
     */
    @Transactional
    public void save(Integer ontologyId, OntologyModel model, int uid) {
        long time = System.currentTimeMillis();
        Map<String, OntologyEntity> entities = new LinkedHashMap<>();
        for (OntologyEntity row : ontologyEntityDao.findAllByOntologyIdOrderBySortAscIdAsc(ontologyId)) {
            entities.put(row.getLabel(), row);
        }
        Map<String, Integer> entityIds = new LinkedHashMap<>();
        int index = 0;
        for (OntologyModel.Entity item : model.getEntities()) {
            if (DPUtil.empty(item.getLabel()) || entityIds.containsKey(item.getLabel())) continue;
            OntologyEntity row = entities.remove(item.getLabel());
            if (null == row) {
                row = new OntologyEntity();
                row.setOntologyId(ontologyId);
                row.setCreatedTime(time);
                row.setCreatedUid(uid);
            }
            row.setCode(item.getCode());
            row.setName(item.getName());
            row.setLabel(item.getLabel());
            row.setDescription(item.getDescription());
            row.setIcon(item.getIcon());
            row.setColor(item.getColor());
            row.setPrimaryField(item.getPrimaryField());
            row.setCaptionField(item.getCaptionField());
            row.setExtendable(item.isExtendable() ? 1 : 0);
            row.setExtendableLabels(item.isExtendableLabels() ? 1 : 0);
            row.setSort(index++);
            row.setUpdatedTime(time);
            row.setUpdatedUid(uid);
            row = ontologyEntityDao.save(row);
            entityIds.put(row.getLabel(), row.getId());
            ontologyEntityLabelDao.deleteByEntityId(row.getId());
            int labelIndex = 0;
            for (String label : item.getLabels()) {
                OntologyEntityLabel entityLabel = new OntologyEntityLabel();
                entityLabel.setEntityId(row.getId());
                entityLabel.setLabel(label);
                entityLabel.setPrimaryFlag(label.equals(item.getLabel()) ? 1 : 0);
                entityLabel.setSort(labelIndex++);
                ontologyEntityLabelDao.save(entityLabel);
            }
            ontologyEntityFieldDao.deleteByEntityId(row.getId());
            int fieldIndex = 0;
            for (OntologyModel.Field field : item.getFields()) {
                OntologyEntityField entityField = new OntologyEntityField();
                entityField.setEntityId(row.getId());
                entityField.setName(field.getName());
                entityField.setTitle(field.getTitle());
                entityField.setType(field.getType());
                entityField.setRequiredFlag(field.isRequired() ? 1 : 0);
                entityField.setDisplayFlag(field.isDisplay() ? 1 : 0);
                entityField.setComment(field.getComment());
                entityField.setSort(fieldIndex++);
                ontologyEntityFieldDao.save(entityField);
            }
        }
        for (OntologyEntity row : entities.values()) {
            ontologyEntityLabelDao.deleteByEntityId(row.getId());
            ontologyEntityFieldDao.deleteByEntityId(row.getId());
            ontologyEntityDao.delete(row);
        }
        Map<String, OntologyRelationship> relationships = new LinkedHashMap<>();
        for (OntologyRelationship row : ontologyRelationshipDao.findAllByOntologyIdOrderBySortAscIdAsc(ontologyId)) {
            relationships.put(row.getLabel(), row);
        }
        index = 0;
        List<String> types = new ArrayList<>();
        for (OntologyModel.Relationship item : model.getRelationships()) {
            if (DPUtil.empty(item.getLabel()) || types.contains(item.getLabel())) continue;
            types.add(item.getLabel());
            OntologyRelationship row = relationships.remove(item.getLabel());
            if (null == row) {
                row = new OntologyRelationship();
                row.setOntologyId(ontologyId);
                row.setCreatedTime(time);
                row.setCreatedUid(uid);
            }
            row.setCode(item.getCode());
            row.setName(item.getName());
            row.setLabel(item.getLabel());
            row.setDescription(item.getDescription());
            row.setSourceEntityId(null == item.getSourceEntity() ? 0 : entityIds.getOrDefault(item.getSourceEntity().getLabel(), 0));
            row.setTargetEntityId(null == item.getTargetEntity() ? 0 : entityIds.getOrDefault(item.getTargetEntity().getLabel(), 0));
            row.setMergeFields(DPUtil.implode(",", item.getMergeFields().toArray(new String[0])));
            row.setCascadeDelete(item.isCascadeDelete() ? 1 : 0);
            row.setSort(index++);
            row.setUpdatedTime(time);
            row.setUpdatedUid(uid);
            row = ontologyRelationshipDao.save(row);
            ontologyRelationshipFieldDao.deleteByRelationshipId(row.getId());
            int fieldIndex = 0;
            for (OntologyModel.Field field : item.getFields()) {
                OntologyRelationshipField relationshipField = new OntologyRelationshipField();
                relationshipField.setRelationshipId(row.getId());
                relationshipField.setName(field.getName());
                relationshipField.setTitle(field.getTitle());
                relationshipField.setType(field.getType());
                relationshipField.setRequiredFlag(field.isRequired() ? 1 : 0);
                relationshipField.setDisplayFlag(field.isDisplay() ? 1 : 0);
                relationshipField.setComment(field.getComment());
                relationshipField.setSort(fieldIndex++);
                ontologyRelationshipFieldDao.save(relationshipField);
            }
        }
        for (OntologyRelationship row : relationships.values()) {
            ontologyRelationshipFieldDao.deleteByRelationshipId(row.getId());
            ontologyRelationshipDao.delete(row);
        }
    }

    /**
     * 清理本体的全部定义
     */
    @Transactional
    public void remove(Integer ontologyId) {
        for (OntologyEntity row : ontologyEntityDao.findAllByOntologyIdOrderBySortAscIdAsc(ontologyId)) {
            ontologyEntityLabelDao.deleteByEntityId(row.getId());
            ontologyEntityFieldDao.deleteByEntityId(row.getId());
        }
        ontologyEntityDao.deleteByOntologyId(ontologyId);
        for (OntologyRelationship row : ontologyRelationshipDao.findAllByOntologyIdOrderBySortAscIdAsc(ontologyId)) {
            ontologyRelationshipFieldDao.deleteByRelationshipId(row.getId());
        }
        ontologyRelationshipDao.deleteByOntologyId(ontologyId);
    }

    protected OntologyModel.Field field(String name, String title, String type,
                                        Integer required, Integer display, String comment) {
        OntologyModel.Field field = new OntologyModel.Field();
        field.setName(name);
        field.setTitle(title);
        field.setType(type);
        field.setComment(comment);
        field.setRequired(1 == DPUtil.parseInt(required));
        field.setDisplay(1 == DPUtil.parseInt(display));
        return field;
    }

}
