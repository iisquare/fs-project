package com.iisquare.fs.web.govern.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.elasticsearch.util.QueryUtil;
import com.iisquare.fs.base.jpa.core.SQLBatchCallback;
import com.iisquare.fs.base.jpa.helper.SQLHelper;
import com.iisquare.fs.base.web.sse.MaintainEmitter;
import com.iisquare.fs.base.web.util.CronUtil;
import com.iisquare.fs.web.core.rbac.MaintainControllerBase;
import com.iisquare.fs.web.govern.elasticsearch.MetaES;
import com.iisquare.fs.web.govern.entity.Model;
import com.iisquare.fs.web.govern.entity.ModelColumn;
import com.iisquare.fs.web.govern.entity.ModelRelation;
import com.iisquare.fs.web.govern.neo4j.MetaBloodNode;
import com.iisquare.fs.web.govern.neo4j.MetaBloodRelation;
import com.iisquare.fs.web.govern.neo4j.MetaInfluenceNode;
import com.iisquare.fs.web.govern.neo4j.MetaInfluenceRelation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@RequestMapping("/maintain")
@RestController
public class MaintainController extends MaintainControllerBase {

    @Autowired
    MetaES metaES;
    @Autowired
    MetaBloodNode metaBloodNode;
    @Autowired
    MetaBloodRelation metaBloodRelation;
    @Autowired
    MetaInfluenceNode metaInfluenceNode;
    @Autowired
    MetaInfluenceRelation metaInfluenceRelation;
    @Autowired
    EntityManager entityManager;

    @GetMapping("/schema")
    public String schemaAction(@RequestParam Map<String, Object> param) {
        return metaES.create(false);
    }

    @PostMapping("/reindexES")
    public SseEmitter reindexESStreamAction(@RequestBody Map<String, Object> param,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        MaintainEmitter emitter = new MaintainEmitter(request, response, 0L);
        return emitter.async(() -> reindexES(emitter, param));
    }

    @PostMapping("/reindexNeo4j")
    public SseEmitter reindexNeo4jStreamAction(@RequestBody Map<String, Object> param,
                                               HttpServletRequest request,
                                               HttpServletResponse response) {
        MaintainEmitter emitter = new MaintainEmitter(request, response, 0L);
        return emitter.async(() -> reindexNeo4j(emitter, param));
    }

    private void reindexES(MaintainEmitter emitter, Map<String, Object> param) {
        try {
            long time = DPUtil.parseLong(param.get("time"), System.currentTimeMillis());
            emitter.plan(DPUtil.buildMap(
                    "model", "重建模型索引",
                    "column", "重建字段索引",
                    "clean", "清理历史索引"
            ));
            emitter.start("开始重建元数据检索索引", "model");
            emitter.step("重建模型索引", "model", 10);
            SQLHelper.build(entityManager, Model.class).batch(new SQLBatchCallback() {
                @Override
                public boolean execute(ArrayNode rows) throws Exception {
                    ArrayNode sources = metaES.formatModel(rows, time);
                    List<String> result = metaES.add(sources);
                    emitter.log("模型索引批次 " + executeCount + " 完成", "model", 25, "info", executeCount, 0);
                    emitter.progress(25, DPUtil.stringify(result), "model");
                    return true;
                }
            });
            emitter.step("重建字段索引", "column", 40);
            SQLHelper.build(entityManager, ModelColumn.class).batch(new SQLBatchCallback() {
                @Override
                public boolean execute(ArrayNode rows) throws Exception {
                    ArrayNode sources = metaES.formatModelColumn(rows, time);
                    List<String> result = metaES.add(sources);
                    emitter.log("字段索引批次 " + executeCount + " 完成", "column", 70, "info", executeCount, 0);
                    emitter.progress(70, DPUtil.stringify(result), "column");
                    return true;
                }
            });
            emitter.step("清理历史索引", "clean", 90);
            long total = metaES.deleteByQuery(QueryUtil.mustNot(QueryUtil.term("time", time)));
            emitter.log("已清理历史索引 " + total + " 条", "clean", 95, "success");
            emitter.result(0, "元数据检索索引重建完成", param);
        } catch (Exception e) {
            emitter.error(500, "元数据检索索引重建失败", e.getMessage());
        }
    }

    private void reindexNeo4j(MaintainEmitter emitter, Map<String, Object> param) {
        try {
            emitter.plan(DPUtil.buildMap(
                    "index", "初始化图索引",
                    "model", "重建模型节点",
                    "column", "重建字段节点",
                    "relation", "重建关系",
                    "clean", "清理历史数据"
            ));
            emitter.start("开始重建元数据关系图", "index");
            metaBloodNode.unique();
            metaBloodNode.index("catalog");
            metaBloodNode.index("time");
            metaBloodRelation.index("time");
            metaInfluenceNode.unique();
            metaInfluenceNode.index("catalog");
            metaInfluenceNode.index("time");
            metaInfluenceRelation.index("time");
            emitter.step("图索引初始化完成", "model", 10);
            long time = DPUtil.parseLong(param.get("time"), System.currentTimeMillis());
            SQLHelper.build(entityManager, Model.class).where("type!='catalog'").batch(new SQLBatchCallback() {
                @Override
                public boolean execute(ArrayNode rows) throws Exception {
                    Iterator<JsonNode> iterator = rows.iterator();
                    while (iterator.hasNext()) {
                        metaBloodNode.save(metaBloodNode.formatModel(iterator.next(), time));
                    }
                    emitter.progress(25, "模型节点批次 " + executeCount + " 完成", "model");
                    return true;
                }
            });
            emitter.step("重建字段节点", "column", 40);
            SQLHelper.build(entityManager, ModelColumn.class).batch(new SQLBatchCallback() {
                @Override
                public boolean execute(ArrayNode rows) throws Exception {
                    Iterator<JsonNode> iterator = rows.iterator();
                    while (iterator.hasNext()) {
                        metaInfluenceNode.save(metaInfluenceNode.formatColumn(iterator.next(), time));
                    }
                    emitter.progress(65, "字段节点批次 " + executeCount + " 完成", "column");
                    return true;
                }
            });
            emitter.step("重建模型关系", "relation", 75);
            List<String> exclude = Arrays.asList("time");
            SQLHelper.build(entityManager, ModelRelation.class).batch(new SQLBatchCallback() {
                @Override
                public boolean execute(ArrayNode rows) throws Exception {
                    Iterator<JsonNode> iterator = rows.iterator();
                    while (iterator.hasNext()) {
                        JsonNode row = iterator.next();
                        metaBloodRelation.save(metaBloodRelation.formatModelRelation(row, time), null, exclude);
                        metaInfluenceRelation.save(metaInfluenceRelation.formatColumnRelation(row, time), null, exclude);
                    }
                    emitter.progress(90, "关系批次 " + executeCount + " 完成", "relation");
                    return true;
                }
            });
            emitter.step("清理历史图数据", "clean", 95);
            long total = metaBloodNode.deleteWithTime(time) + metaBloodRelation.deleteWithTime(time);
            total += metaInfluenceNode.deleteWithTime(time) + metaInfluenceRelation.deleteWithTime(time);
            emitter.log("已清理历史图数据 " + total + " 条", "clean", 98, "success");
            emitter.result(0, "元数据关系图重建完成", param);
        } catch (Exception e) {
            emitter.error(500, "元数据关系图重建失败", e.getMessage());
        }
    }

    @GetMapping("/reindexES")
    public void reindexESAction(@RequestParam Map<String, Object> param, HttpServletResponse response) {
        try {
            CronUtil.initialize(response);
            CronUtil.flushStringify(response, param, true);
            long time = DPUtil.parseLong(param.get("time"), System.currentTimeMillis());
            CronUtil.flush(response, "reindex model...", true);
            SQLHelper.build(entityManager, Model.class).batch(new SQLBatchCallback() {
                @Override
                public boolean execute(ArrayNode rows) throws Exception {
                    CronUtil.flushString(response, true, "reindex model step %d", executeCount);
                    ArrayNode sources = metaES.formatModel(rows, time);
                    List<String> result = metaES.add(sources);
                    CronUtil.flushStringify(response, result, true);
                    return true;
                }
            });
            CronUtil.flush(response, "reindex model column...", true);
            SQLHelper.build(entityManager, ModelColumn.class).batch(new SQLBatchCallback() {
                @Override
                public boolean execute(ArrayNode rows) throws Exception {
                    CronUtil.flushString(response, true, "reindex model column step %d", executeCount);
                    ArrayNode sources = metaES.formatModelColumn(rows, time);
                    List<String> result = metaES.add(sources);
                    CronUtil.flushStringify(response, result, true);
                    return true;
                }
            });
            CronUtil.flushString(response, true, "clean with time %d", time);
            long total = metaES.deleteByQuery(QueryUtil.mustNot(QueryUtil.term("time", time)));
            CronUtil.flushString(response, true, "clean es total %d", total);
            CronUtil.flushStringify(response, ApiUtil.result(0, null, param), true);
        } catch (Exception e) {
            CronUtil.flush(response, e, true);
        }
    }

    @GetMapping("/reindexNeo4j")
    public void reindexNeo4jAction(@RequestParam Map<String, Object> param, HttpServletResponse response) {
        try {
            CronUtil.initialize(response);
            CronUtil.flushStringify(response, param, true);
            CronUtil.flush(response, "create neo4j index...", true);
            metaBloodNode.unique();
            metaBloodNode.index("catalog");
            metaBloodNode.index("time");
            metaBloodRelation.index("time");
            metaInfluenceNode.unique();
            metaInfluenceNode.index("catalog");
            metaInfluenceNode.index("time");
            metaInfluenceRelation.index("time");
            long time = DPUtil.parseLong(param.get("time"), System.currentTimeMillis());
            CronUtil.flush(response, "reindex model...", true);
            SQLHelper.build(entityManager, Model.class).where("type!='catalog'").batch(new SQLBatchCallback() {
                @Override
                public boolean execute(ArrayNode rows) throws Exception {
                    CronUtil.flushString(response, true, "reindex model step %d", executeCount);
                    Iterator<JsonNode> iterator = rows.iterator();
                    while (iterator.hasNext()) {
                        JsonNode row = iterator.next();
                        metaBloodNode.save(metaBloodNode.formatModel(row, time));
                    }
                    return true;
                }
            });
            CronUtil.flush(response, "reindex model column...", true);
            SQLHelper.build(entityManager, ModelColumn.class).batch(new SQLBatchCallback() {
                @Override
                public boolean execute(ArrayNode rows) throws Exception {
                    CronUtil.flushString(response, true, "reindex model column step %d", executeCount);
                    Iterator<JsonNode> iterator = rows.iterator();
                    while (iterator.hasNext()) {
                        JsonNode row = iterator.next();
                        metaInfluenceNode.save(metaInfluenceNode.formatColumn(row, time));
                    }
                    return true;
                }
            });
            CronUtil.flush(response, "reindex model relation...", true);
            List<String> exclude = Arrays.asList("time");
            SQLHelper.build(entityManager, ModelRelation.class).batch(new SQLBatchCallback() {
                @Override
                public boolean execute(ArrayNode rows) throws Exception {
                    CronUtil.flushString(response, true, "reindex model relation step %d", executeCount);
                    Iterator<JsonNode> iterator = rows.iterator();
                    while (iterator.hasNext()) {
                        JsonNode row = iterator.next();
                        metaBloodRelation.save(metaBloodRelation.formatModelRelation(row, time), null, exclude);
                        metaInfluenceRelation.save(metaInfluenceRelation.formatColumnRelation(row, time), null, exclude);
                    }
                    return true;
                }
            });
            CronUtil.flushString(response, true, "clean with time %d", time);
            long total = metaBloodNode.deleteWithTime(time) + metaBloodRelation.deleteWithTime(time);
            total += metaInfluenceNode.deleteWithTime(time) + metaInfluenceRelation.deleteWithTime(time);
            CronUtil.flushString(response, true, "clean es total %d", total);
            CronUtil.flushStringify(response, ApiUtil.result(0, null, param), true);
        } catch (Exception e) {
            CronUtil.flush(response, e, true);
        }
    }

}
