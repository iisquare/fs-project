package com.iisquare.fs.web.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.agent.dao.ProjectShareDao;
import com.iisquare.fs.web.agent.dao.ProjectVersionDao;
import com.iisquare.fs.web.agent.entity.ProjectVersion;
import com.iisquare.fs.web.agent.mvc.Configuration;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 分享项目的版本记录（管理端维护 + 客户端读取）。
 *
 * 一条版本 = 一次发布的项目配置快照：版本号（name）、版本说明（description）与配置原文（content）。
 * 配置原文只在详情接口下发，列表行里只给大小，避免列表页把大字段一并拖出来。
 */
@Service
public class ProjectVersionService extends JPAServiceBase {

    @Autowired
    ProjectVersionDao versionDao;
    @Autowired
    ProjectShareDao projectDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;

    /** 单个版本的项目配置文本上限：配置本身很小，给足余量的同时防住异常大的提交 */
    public static final int MAX_CONTENT_SIZE = 256 * 1024;

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("status", "asc");
        return sorts;
    }

    public Map<Integer, String> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "停用");
        return status;
    }

    public ProjectVersion info(Integer id) {
        return info(versionDao, id);
    }

    /** 管理端详情：带项目配置原文 */
    public Map<String, Object> info(Map<?, ?> param) {
        ProjectVersion info = info(DPUtil.parseInt(param.get("id")));
        if (null == info) return ApiUtil.result(1404, "信息不存在", null);
        // 带项目配置原文的完整记录（列表走 strip 去掉原文，这里按 id 单独取）
        ObjectNode node = (ObjectNode) DPUtil.firstNode(DPUtil.toArrayNode(info));
        node.put("contentSize", contentSize(DPUtil.parseString(info.getContent())));
        return ApiUtil.result(0, null, node);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(versionDao, param, (root, query, cb) -> {
            SpecificationHelper<ProjectVersion> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntGTZero("projectId");
            helper.equalWithIntNotEmpty("status").like("name");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("id")), sorts().keySet());
        JsonNode rows = strip(ApiUtil.rows(result));
        if (!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if (!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        if (!DPUtil.empty(args.get("withProjectInfo"))) {
            fillInfo(projectDao, rows, "projectId");
        }
        return result;
    }

    /**
     * 列表行去掉配置原文，只留大小：原文可能有几十 KB，翻页一次带上十几行没有意义，
     * 需要看内容时按 id 取详情。
     */
    public JsonNode strip(JsonNode rows) {
        if (null == rows) return null;
        if (rows.isObject()) {
            stripRow((ObjectNode) rows);
            return rows;
        }
        for (JsonNode row : rows) {
            if (!row.isObject()) continue;
            stripRow((ObjectNode) row);
        }
        return rows;
    }

    protected void stripRow(ObjectNode node) {
        node.put("contentSize", contentSize(node.at("/content").asText("")));
        node.remove("content");
    }

    /**
     * 配置原文的大小（UTF-8 字节）：列表里的「配置大小」按它显示。
     * 用字节而不是字符数，否则中文配置会被明显低估，页面上的 KB / MB 也就不准了。
     */
    public static int contentSize(String content) {
        if (DPUtil.empty(content)) return 0;
        return content.getBytes(StandardCharsets.UTF_8).length;
    }

    /** 管理端修改：只改版本号、状态与版本说明，配置原文以发布内容为准，不在这里改写 */
    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        if (id <= 0) return ApiUtil.result(1001, "版本ID异常", id);
        ProjectVersion info = info(id);
        if (null == info) return ApiUtil.result(404, null, id);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) return ApiUtil.result(1002, "版本号异常", name);
        int status = DPUtil.parseInt(param.get("status"));
        if (!status().containsKey(status)) return ApiUtil.result(1003, "状态异常", status);
        info.setName(name);
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(versionDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, DPUtil.toJSON(info, ObjectNode.class));
    }

    public Map<String, Object> remove(List<Integer> ids) {
        return remove(versionDao, ids);
    }

    /** 删除分享项目时连版本一并清理 */
    public void deleteByProjectIds(List<Integer> projectIds) {
        removeByParentId(versionDao, "projectId", projectIds);
    }

}
