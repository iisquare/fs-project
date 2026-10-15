package com.iisquare.fs.web.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.agent.dao.ProjectShareDao;
import com.iisquare.fs.web.agent.dao.ProjectVersionDao;
import com.iisquare.fs.web.agent.entity.ProjectShare;
import com.iisquare.fs.web.agent.entity.ProjectVersion;
import com.iisquare.fs.web.agent.mvc.Configuration;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 分享的项目：管理端维护 + 客户端发布与浏览。
 *
 * 项目按「项目 + 版本」两级组织（见 {@link ProjectVersionService}）：
 * 客户端发布一次就追加一条版本，管理端可以停用 / 停用单个版本 / 删除整个项目与版本记录。
 *
 * 权限面：管理端接口走 `agent:projectShare:`，客户端接口走 `agent:bot:`（挂在 BotController 下），
 * 两边的业务实现共用本服务，只是可见范围不同——客户端只看「启用 + 授权命中」的项目与版本，
 * 分享人自己不受该限制（自己的项目、自己的停用版本都能看到与管理）。
 */
@Service
public class ProjectShareService extends JPAServiceBase {

    @Autowired
    ProjectShareDao projectDao;
    @Autowired
    ProjectVersionDao versionDao;
    @Autowired
    ProjectVersionService versionService;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;

    /** 客户端浏览的分页上限：授权角色在内存里过滤，一次不要拉太多 */
    public static final int CLIENT_PAGE_SIZE = 15;
    public static final int CLIENT_PAGE_SIZE_MAX = 60;
    /** 客户端版本记录的分页：一个项目攒下的版本可能很多，按页取，页面按需「加载更多」 */
    public static final int CLIENT_VERSION_PAGE_SIZE = 20;
    public static final int CLIENT_VERSION_PAGE_SIZE_MAX = 100;

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("status", "asc");
        sorts.put("sort", "desc");
        return sorts;
    }

    public Map<Integer, String> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "停用");
        return status;
    }

    public ProjectShare info(Integer id) {
        return info(projectDao, id);
    }

    public Map<String, Object> info(Map<?, ?> param) {
        ProjectShare info = info(DPUtil.parseInt(param.get("id")));
        if (null == info) return ApiUtil.result(1404, "信息不存在", null);
        // 与列表同一套填充：格式化成行数组后补状态文案、用户信息与授权角色，再取第一条返回。
        // 详情要能独立支撑「信息查看」抽屉：授权角色（标签）、版本数、最新版本都在这里补齐，
        // 页面点开就能看到，不必依赖列表行里带了哪些字段（列表面向列表，未必带最新版本）。
        // 注意必须先包成行数组再填充：直接遍历单个对象拿到的是它的字段值，既填不了角色也拼不出版本数。
        ArrayNode rows = DPUtil.toArrayNode(info);
        format(rows);
        fillStatus(rows, status());
        rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        rbacService.fillInfos(rows);
        fillVersions(rows, false, true);
        return ApiUtil.result(0, null, DPUtil.firstNode(rows));
    }

    /* ------------------------------- 管理端 ------------------------------- */

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(projectDao, param, (root, query, cb) -> {
            SpecificationHelper<ProjectShare> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntNotEmpty("status").like("name").equalWithIntGTZero("createdUid");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id")), sorts().keySet());
        JsonNode rows = format(ApiUtil.rows(result));
        if (!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if (!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        if (!DPUtil.empty(args.get("withRoles"))) {
            rbacService.fillInfos(rows);
        }
        if (!DPUtil.empty(args.get("withVersionCount"))) {
            // 版本数量与最新版本一起填：两者共用同一个版本查询条件，一次填充省一趟；
            // 最新版本是否下发由 withLatest 决定（列表页要在行里看到最新版本号时打开）
            fillVersions((ArrayNode) rows, false, !DPUtil.empty(args.get("withLatest")));
        }
        return result;
    }

    /**
     * 行转换：roleIds 以字符串入库，出参统一给数组，页面按标签展示。
     *
     * 列表（行数组）与详情（单个对象）共用这一份转换——单个对象不要再按行遍历：
     * 遍历 ObjectNode 拿到的是它的字段值（id 这种就是 IntNode），既转不了角色也强转不成 ObjectNode。
     */
    public JsonNode format(JsonNode rows) {
        if (null == rows) return null;
        if (rows.isObject()) {
            formatRow((ObjectNode) rows);
            return rows;
        }
        for (JsonNode row : rows) {
            if (!row.isObject()) continue;
            formatRow((ObjectNode) row);
        }
        return rows;
    }

    protected void formatRow(ObjectNode node) {
        node.replace("roleIds", DPUtil.toJSON(DPUtil.parseIntList(node.at("/roleIds").asText(""))));
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) return ApiUtil.result(1001, "项目名称异常", name);
        int status = DPUtil.parseInt(param.get("status"));
        if (!status().containsKey(status)) return ApiUtil.result(1002, "状态异常", status);
        ProjectShare info;
        if (id > 0) {
            if (!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if (null == info) return ApiUtil.result(404, null, id);
        } else {
            if (!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new ProjectShare();
        }
        info.setName(name);
        info.setIcon(DPUtil.parseString(param.get("icon")));
        info.setRoleIds(DPUtil.implode(",", DPUtil.parseIntList(param.get("roleIds"))));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(projectDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    /** 删除分享项目：版本记录一并清理，不留孤儿版本 */
    public Map<String, Object> remove(List<Integer> ids) {
        if (null == ids || ids.isEmpty()) return ApiUtil.result(0, null, 0);
        try {
            versionService.deleteByProjectIds(ids);
        } catch (Exception e) {
            return ApiUtil.result(1500, "删除项目版本失败", e.getMessage());
        }
        return remove(projectDao, ids);
    }

    /* ------------------------------- 客户端 ------------------------------- */

    /** 我分享过的项目：只按分享人过滤，停用与否都返回（本人对自己的项目有完整视图） */
    public Map<String, Object> mine(Map<?, ?> param, HttpServletRequest request) {
        int uid = rbacService.uid(request);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        int page = ValidateUtil.filterInteger(param.get("page"), 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), 1, CLIENT_PAGE_SIZE_MAX, CLIENT_PAGE_SIZE);
        Page<ProjectShare> data = projectDao.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("createdUid"), uid));
            if (!DPUtil.empty(name)) predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            return cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, pageSize, Sort.by(Sort.Order.desc("id"))));
        ArrayNode rows = DPUtil.arrayNode();
        for (ProjectShare share : data.getContent()) {
            rows.add(row(share, uid));
        }
        fillVersions(rows, false, true);
        ObjectNode result = DPUtil.objectNode();
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.set("rows", rows);
        return ApiUtil.result(0, null, result);
    }

    /**
     * 浏览别人分享的项目：只给「已启用 + 授权命中」的项目与已启用的版本。
     *
     * 授权角色是逗号拼接的字符串，SQL 里表达不了「与调用人角色有交集」，因此与技能清单同一口径：
     * 先按条件取回，再在内存里按角色过滤、分页（分享的项目量级与技能相当，内存过滤足够）。
     */
    public Map<String, Object> browse(Map<?, ?> param, HttpServletRequest request) {
        int uid = rbacService.uid(request);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        int page = ValidateUtil.filterInteger(param.get("page"), 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), 1, CLIENT_PAGE_SIZE_MAX, CLIENT_PAGE_SIZE);
        List<ProjectShare> rows = projectDao.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), 1));
            if (!DPUtil.empty(name)) predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id")));
        List<ProjectShare> allowed = new ArrayList<>();
        for (ProjectShare share : rows) {
            if (authorized(share, request)) allowed.add(share);
        }
        int total = allowed.size();
        int from = Math.min((page - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        ArrayNode items = DPUtil.arrayNode();
        for (ProjectShare share : allowed.subList(from, to)) {
            items.add(row(share, uid));
        }
        fillVersions(items, true, true);
        rbacService.fillUserInfo(items, "createdUid");
        ObjectNode result = DPUtil.objectNode();
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", total);
        result.set("rows", items);
        return ApiUtil.result(0, null, result);
    }

    /**
     * 某个分享项目的版本清单：本人的项目连停用版本一起给，别人的项目只给启用的版本。
     *
     * 分页取（每页 20，上限 100）：版本数量可以攒到很多，一次全给既拖慢接口也让页面无从收敛，
     * 出参带上 page / pageSize / total，页面滚动到底或点「加载更多」再取下一页；
     * 另可传 name 按版本号检索（下拉面板里的检索框走这里，不必把全部版本取到前端再过滤）。
     */
    public Map<String, Object> versions(Map<?, ?> param, HttpServletRequest request) {
        int uid = rbacService.uid(request);
        ProjectShare share = info(DPUtil.parseInt(param.get("projectId")));
        if (null == share) return ApiUtil.result(1404, "分享的项目不存在", param.get("projectId"));
        boolean mine = Objects.equals(uid, share.getCreatedUid());
        if (!mine) {
            Map<String, Object> denied = visible(share, request);
            if (!denied.isEmpty()) return denied;
        }
        int page = ValidateUtil.filterInteger(param.get("page"), 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), 1, CLIENT_VERSION_PAGE_SIZE_MAX,
                CLIENT_VERSION_PAGE_SIZE);
        Page<ProjectVersion> data = versionDao.findAll(
                versionSpec(share.getId(), !mine, DPUtil.trim(DPUtil.parseString(param.get("name")))),
                PageRequest.of(page - 1, pageSize, Sort.by(Sort.Order.desc("id"))));
        ArrayNode rows = DPUtil.arrayNode();
        for (ProjectVersion version : data.getContent()) {
            rows.add(versionRow(version));
        }
        rbacService.fillUserInfo(rows, "createdUid");
        ObjectNode result = DPUtil.objectNode();
        result.set("project", row(share, uid));
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.set("rows", rows);
        return ApiUtil.result(0, null, result);
    }

    /** 单个版本的详情：带项目配置原文，客户端据此导出配置文件或创建本地项目 */
    public Map<String, Object> version(Map<?, ?> param, HttpServletRequest request) {
        int uid = rbacService.uid(request);
        ProjectVersion version = versionService.info(DPUtil.parseInt(param.get("id")));
        if (null == version) return ApiUtil.result(1404, "版本不存在", param.get("id"));
        ProjectShare share = info(version.getProjectId());
        if (null == share) return ApiUtil.result(1404, "分享的项目不存在", version.getProjectId());
        boolean mine = Objects.equals(uid, share.getCreatedUid());
        if (!mine) {
            Map<String, Object> denied = visible(share, request);
            if (!denied.isEmpty()) return denied;
            if (null == version.getStatus() || 1 != version.getStatus()) {
                return ApiUtil.result(1404, "该版本已停用", version.getId());
            }
        }
        ObjectNode item = versionRow(version);
        JsonNode content = DPUtil.parseJSON(DPUtil.parseString(version.getContent()));
        item.set("content", null != content && content.isObject() ? content : DPUtil.objectNode());
        ObjectNode result = DPUtil.objectNode();
        result.set("project", row(share, uid));
        result.set("version", item);
        return ApiUtil.result(0, null, result);
    }

    /**
     * 发布一个版本：projectId 为 0 时新建分享项目（要求给名称），否则在指定项目下追加版本。
     *
     * 只有分享人能往自己的项目里追加版本；项目名称、图标、描述随本次发布同步
     * （分享一个改过名的本地项目后，列表里看到的就是新名字；留空则保持原值）。
     */
    @Transactional
    public Map<String, Object> publish(Map<?, ?> param, HttpServletRequest request) {
        int uid = rbacService.uid(request);
        String version = DPUtil.trim(DPUtil.parseString(param.get("version")));
        if (DPUtil.empty(version)) return ApiUtil.result(1001, "版本号不能为空", version);
        if (version.length() > 128) return ApiUtil.result(1002, "版本号过长（上限 128 字符）", version);
        String content = content(param.get("content"));
        if (null == content) return ApiUtil.result(1003, "项目配置不能为空，且必须是合法的 JSON 对象", null);
        if (content.length() > ProjectVersionService.MAX_CONTENT_SIZE) {
            return ApiUtil.result(1004,
                    "项目配置过大（上限 " + (ProjectVersionService.MAX_CONTENT_SIZE / 1024) + "KB）", content.length());
        }
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        int projectId = DPUtil.parseInt(param.get("projectId"));
        ProjectShare project;
        if (projectId > 0) {
            project = info(projectId);
            if (null == project) return ApiUtil.result(1404, "分享的项目不存在", projectId);
            if (!Objects.equals(uid, project.getCreatedUid())) {
                return ApiUtil.result(9403, "只能往自己分享的项目里追加版本", projectId);
            }
        } else {
            if (DPUtil.empty(name)) return ApiUtil.result(1005, "项目名称不能为空", name);
            project = ProjectShare.builder()
                    .name(name)
                    .icon(DPUtil.parseString(param.get("icon")))
                    .roleIds("")
                    .sort(0)
                    .status(1)
                    .description(DPUtil.parseString(param.get("description")))
                    .build();
            project = save(projectDao, project, uid);
        }
        // 版本号在同一个分享项目下不重复：版本号是使用者对照配置的依据，
        // 两条 v1.0 谁也说不清哪条是哪条；要修正已发布的版本就发新版本号（旧版本可停用或删除）
        Integer shareId = project.getId();
        long exists = versionDao.count((root, query, cb) -> cb.and(
                cb.equal(root.get("projectId"), shareId), cb.equal(root.get("name"), version)));
        if (exists > 0) {
            return ApiUtil.result(1006, "该分享项目下已存在版本号「" + version + "」，请换一个版本号", version);
        }
        // 已有项目：名称 / 图标 / 描述按本次发布同步，留空保持原值（想清空由管理端来改）
        if (projectId > 0) {
            boolean changed = false;
            if (!DPUtil.empty(name) && !name.equals(DPUtil.parseString(project.getName()))) {
                project.setName(name);
                changed = true;
            }
            String icon = DPUtil.parseString(param.get("icon"));
            if (!DPUtil.empty(icon) && !icon.equals(DPUtil.parseString(project.getIcon()))) {
                project.setIcon(icon);
                changed = true;
            }
            String description = DPUtil.parseString(param.get("description"));
            if (!DPUtil.empty(description) && !description.equals(DPUtil.parseString(project.getDescription()))) {
                project.setDescription(description);
                changed = true;
            }
            if (changed) project = save(projectDao, project, uid);
        }
        ProjectVersion record = ProjectVersion.builder()
                .projectId(project.getId())
                .name(version)
                .content(content)
                .status(1)
                .description(DPUtil.parseString(param.get("versionDescription")))
                .build();
        record = save(versionDao, record, uid);
        ObjectNode result = DPUtil.objectNode();
        result.set("project", row(project, uid));
        result.set("version", versionRow(record));
        return ApiUtil.result(0, null, result);
    }

    /* ------------------------------- 内部 ------------------------------- */

    /**
     * 客户端能否看到某个分享项目：为空表示放行，否则是拒绝原因。
     * 停用与授权都不命中时给出同一句提示，避免把「有没有这个项目」探测出来。
     */
    protected Map<String, Object> visible(ProjectShare share, HttpServletRequest request) {
        if (null == share) return ApiUtil.result(1404, "分享的项目不存在", null);
        if (null == share.getStatus() || 1 != share.getStatus()) {
            return ApiUtil.result(1404, "分享的项目不存在或已停用", share.getId());
        }
        if (!authorized(share, request)) return ApiUtil.result(9403, "无权限查看该分享项目", share.getId());
        return Collections.emptyMap();
    }

    /** 授权校验：未配置授权角色时所有登录用户可见；配置后要求调用人的角色与之有交集 */
    public boolean authorized(ProjectShare info, HttpServletRequest request) {
        if (null == info) return false;
        Set<Integer> allowed = new LinkedHashSet<>(DPUtil.parseIntList(info.getRoleIds()));
        if (allowed.isEmpty()) return true;
        return !Collections.disjoint(allowed, rbacService.roleIds(request));
    }

    /**
     * 项目行的客户端出参：不下发 roleIds（可见性由服务端判定），mine 标记供页面区分「我分享的」。
     */
    protected ObjectNode row(ProjectShare share, int uid) {
        ObjectNode node = DPUtil.objectNode();
        node.put("id", share.getId());
        node.put("name", DPUtil.parseString(share.getName()));
        node.put("icon", DPUtil.parseString(share.getIcon()));
        node.put("description", DPUtil.parseString(share.getDescription()));
        node.put("sort", null == share.getSort() ? 0 : share.getSort());
        node.put("status", null == share.getStatus() ? 0 : share.getStatus());
        node.put("statusText", status().getOrDefault(null == share.getStatus() ? 0 : share.getStatus(), ""));
        node.put("createdUid", null == share.getCreatedUid() ? 0 : share.getCreatedUid());
        node.put("mine", uid > 0 && Objects.equals(uid, share.getCreatedUid()));
        node.put("createdTime", null == share.getCreatedTime() ? 0L : share.getCreatedTime());
        node.put("updatedTime", null == share.getUpdatedTime() ? 0L : share.getUpdatedTime());
        return node;
    }

    /** 版本行的客户端出参：不含配置原文，需要原文时按 id 取版本详情 */
    protected ObjectNode versionRow(ProjectVersion version) {
        ObjectNode node = DPUtil.objectNode();
        node.put("id", version.getId());
        node.put("projectId", version.getProjectId());
        node.put("name", DPUtil.parseString(version.getName()));
        node.put("status", null == version.getStatus() ? 0 : version.getStatus());
        node.put("statusText", status().getOrDefault(null == version.getStatus() ? 0 : version.getStatus(), ""));
        node.put("description", DPUtil.parseString(version.getDescription()));
        node.put("contentSize", ProjectVersionService.contentSize(DPUtil.parseString(version.getContent())));
        node.put("createdUid", null == version.getCreatedUid() ? 0 : version.getCreatedUid());
        node.put("createdTime", null == version.getCreatedTime() ? 0L : version.getCreatedTime());
        node.put("updatedTime", null == version.getUpdatedTime() ? 0L : version.getUpdatedTime());
        return node;
    }

    /**
     * 给项目行补上版本数量与最新版本：逐个项目各查两条轻量语句（COUNT + 只取最新一条），
     * 而不是把涉及项目的**全部版本一次查回内存**再分组——版本是带配置原文（content 长文本）的记录，
     * 版本数量一多，后者会把几 MB 甚至几十 MB 原文拖进来只为数一个数字、挑一条最新的。
     *
     * 列表一页最多 60 个项目，这些索引命中（idx_project_id）的小查询量是确定的，
     * 换掉的是不确定的放大：版本数涨多少，这里都不会跟着变重。
     *
     * @param enabledOnly 是否只统计启用的版本（浏览他人项目时用）
     * @param withLatest  是否带上最新版本（只看数量时不必带）
     */
    protected void fillVersions(ArrayNode rows, boolean enabledOnly, boolean withLatest) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            int id = node.at("/id").asInt(0);
            if (id < 1) {
                node.put("versionCount", 0);
                if (withLatest) node.set("latestVersion", null);
                continue;
            }
            Specification<ProjectVersion> spec = versionSpec(id, enabledOnly);
            node.put("versionCount", versionDao.count(spec));
            if (!withLatest) continue;
            List<ProjectVersion> latest = versionDao
                    .findAll(spec, PageRequest.of(0, 1, Sort.by(Sort.Order.desc("id"))))
                    .getContent();
            node.set("latestVersion", latest.isEmpty() ? null : versionRow(latest.get(0)));
        }
    }

    /** 某个项目的版本查询条件：enabledOnly 时只算启用的版本（浏览他人项目时用） */
    protected Specification<ProjectVersion> versionSpec(Integer projectId, boolean enabledOnly) {
        return versionSpec(projectId, enabledOnly, null);
    }

    /** 某个项目的版本查询条件：另可按版本号模糊过滤（下拉面板的检索） */
    protected Specification<ProjectVersion> versionSpec(Integer projectId, boolean enabledOnly, String name) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("projectId"), projectId));
            if (enabledOnly) predicates.add(cb.equal(root.get("status"), 1));
            if (!DPUtil.empty(name)) predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 项目配置归一：客户端可以按 JSON 对象提交，也可以提交已经序列化好的文本；
     * 两种形态都要求能解析成一个 JSON 对象，避免把非配置内容存进版本里。
     */
    protected String content(Object value) {
        if (null == value) return null;
        if (value instanceof String) {
            String text = DPUtil.trim((String) value);
            if (DPUtil.empty(text)) return null;
            JsonNode node = DPUtil.parseJSON(text);
            return null != node && node.isObject() ? text : null;
        }
        JsonNode node = DPUtil.toJSON(value);
        if (null == node || !node.isObject()) return null;
        return node.toString();
    }

}
