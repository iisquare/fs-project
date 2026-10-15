package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.base.web.util.ServiceUtil;
import com.iisquare.fs.base.web.util.ServletUtil;
import com.iisquare.fs.web.bi.dao.DataQueryLogDao;
import com.iisquare.fs.web.bi.entity.DataQueryLog;
import com.iisquare.fs.web.bi.mvc.Configuration;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据查询日志服务：记录即席查询（SQL查询）、数据集查询与数据主题查询的执行情况。
 * 日志只针对真正执行的 SQL 查询（用于审计业务数据），获取主题字典等元数据操作不入日志；
 * 日志包含查询时间、查询用户、来源IP、耗时与结果状态等关键信息。
 */
@Service
public class DataQueryLogService extends JPAServiceBase {

    private static final Logger logger = LoggerFactory.getLogger(DataQueryLogService.class);

    @Autowired
    DataQueryLogDao dataQueryLogDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("createdTime", "desc");
        sorts.put("duration", "asc");
        sorts.put("status", "asc");
        return sorts;
    }

    public Map<String, String> types() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put(DataQueryLog.TYPE_OLAP, "即席查询");
        types.put(DataQueryLog.TYPE_DATASET, "数据集查询");
        types.put(DataQueryLog.TYPE_THEME, "数据主题查询");
        return types;
    }

    public Map<Integer, String> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "成功");
        status.put(2, "失败");
        return status;
    }

    /**
     * 构建查询日志：填充查询时间、查询用户标识、来源IP等上下文信息，
     * 查询类型、查询对象与执行结果由调用方补充。
     */
    public DataQueryLog build(String type, HttpServletRequest request) {
        DataQueryLog log = new DataQueryLog();
        log.setType(type);
        log.setTargetId(0);
        log.setTargetName("");
        log.setSqlText("");
        log.setStatus(1);
        log.setResultCode(0);
        log.setMessage("");
        log.setDetail("");
        log.setCreatedUid(0);
        log.setRequestIp("");
        log.setUserAgent("");
        log.setRequestUrl("");
        log.setCreatedTime(System.currentTimeMillis());
        log.setMaxRows(0);
        log.setTimeout(0);
        log.setRowCount(0L);
        log.setColumnCount(0);
        log.setDuration(0L);
        if (null == request) return log;
        log.setCreatedUid(rbacService.uid(request)); // 会话内已缓存鉴权数据，无需额外调用鉴权服务
        log.setRequestIp(DPUtil.parseString(ServletUtil.getRemoteAddr(request)));
        log.setUserAgent(DPUtil.parseString(request.getHeader("user-agent")));
        log.setRequestUrl(DPUtil.parseString(ServletUtil.getFullUrl(request, true, true)));
        return log;
    }

    /**
     * 写入查询日志：日志失败不影响查询结果，仅输出告警。
     * 未记录 SQL 的日志直接跳过——查询日志只用于审计实际执行的 SQL。
     */
    public DataQueryLog record(DataQueryLog log) {
        if (null == log) return null;
        if (DPUtil.empty(log.getSqlText())) return null;
        try {
            if (null == log.getCreatedTime() || log.getCreatedTime() < 1) {
                log.setCreatedTime(System.currentTimeMillis());
            }
            return dataQueryLogDao.save(log);
        } catch (Exception e) {
            // 记录失败会导致查询日志缺失，此处输出错误日志便于排查（如表结构不一致、字段名冲突）
            logger.error("记录数据查询日志失败, type: {}, message: {}", log.getType(), e.getMessage(), e);
            return null;
        }
    }

    public Map<String, Object> info(Map<?, ?> param) {
        Long id = DPUtil.parseLong(param.get("id"));
        DataQueryLog info = info(dataQueryLogDao, id);
        if (null == info) return ApiUtil.result(404, null, id);
        JsonNode rows = format(DPUtil.toJSON(List.of(info)));
        rbacService.fillUserInfo(rows, "createdUid");
        return ApiUtil.result(0, null, DPUtil.firstNode(rows));
    }

    /**
     * 查询日志列表：支持按类型、状态、查询对象、查询用户与查询时间检索，用户名称按标识填充。
     */
    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(dataQueryLogDao, param, (root, query, cb) -> {
            SpecificationHelper<DataQueryLog> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate());
            helper.equalWithLongGTZero("id");
            helper.equal("type").equalWithIntNotEmpty("status");
            helper.like("targetName");
            helper.equalWithIntGTZero("targetId").equalWithIntGTZero("createdUid");
            helper.betweenWithDate("createdTime");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("createdTime"), Sort.Order.desc("id")), sorts().keySet());
        JsonNode rows = format(ApiUtil.rows(result));
        if (!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid");
        }
        ServiceUtil.retain(rows, param.get("columns"));
        return result;
    }

    public JsonNode format(JsonNode rows) {
        if (null == rows) return null;
        fillStatus(rows, status());
        DPUtil.fillValues(rows, "type", "typeText", types());
        return rows;
    }

    public Map<String, Object> remove(List<Long> ids) {
        if (null == ids || ids.isEmpty()) return ApiUtil.result(0, null, 0);
        try {
            dataQueryLogDao.deleteAllByIdInBatch(ids);
            return ApiUtil.result(0, null, ids.size());
        } catch (Exception e) {
            return ApiUtil.result(1500, "删除查询日志失败", e.getMessage());
        }
    }

}
