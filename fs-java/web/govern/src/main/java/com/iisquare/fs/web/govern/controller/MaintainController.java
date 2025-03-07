package com.iisquare.fs.web.govern.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.jpa.core.SQLBatchCallback;
import com.iisquare.fs.base.jpa.helper.SQLHelper;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import com.iisquare.fs.base.web.util.CronUtil;
import com.iisquare.fs.web.govern.elasticsearch.MetaES;
import com.iisquare.fs.web.govern.entity.Model;
import com.iisquare.fs.web.govern.entity.ModelColumn;
import com.iisquare.fs.web.govern.entity.ModelRelation;
import com.iisquare.fs.web.govern.neo4j.MetaBloodNode;
import com.iisquare.fs.web.govern.neo4j.MetaBloodRelation;
import com.iisquare.fs.web.govern.neo4j.MetaInfluenceNode;
import com.iisquare.fs.web.govern.neo4j.MetaInfluenceRelation;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@RequestMapping("/maintain")
@RestController
public class MaintainController extends ControllerBase {

    @Autowired
    private MetaES metaES;
    @Autowired
    private MetaBloodNode metaBloodNode;
    @Autowired
    private MetaBloodRelation metaBloodRelation;
    @Autowired
    private MetaInfluenceNode metaInfluenceNode;
    @Autowired
    private MetaInfluenceRelation metaInfluenceRelation;
    @Autowired
    private EntityManager entityManager;

    @GetMapping("/schema")
    public String schemaAction(@RequestParam Map<String, Object> param) {
        String result = metaES.create(false);
        return result;
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
            long total = metaES.deleteByQuery(QueryBuilders.boolQuery().mustNot(
                    QueryBuilders.termsQuery("time", new long[]{time})));
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
