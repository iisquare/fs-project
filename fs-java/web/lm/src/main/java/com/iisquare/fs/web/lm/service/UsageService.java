package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rpc.MemberRpc;
import com.iisquare.fs.web.lm.core.RedisKey;
import com.iisquare.fs.web.lm.dao.CreditDao;
import com.iisquare.fs.web.lm.dao.UsageDao;
import com.iisquare.fs.web.lm.mapper.UsageMapper;
import com.iisquare.fs.web.lm.entity.Usage;
import com.iisquare.fs.web.lm.mvc.Configuration;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UsageService extends JPAServiceBase {

    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;
    @Autowired
    UsageDao usageDao;
    @Autowired
    CreditDao creditDao;
    @Autowired
    MemberRpc memberRpc;
    @Autowired
    AuthService authService;
    @Autowired
    ProviderService  providerService;
    @Autowired
    ModelService modelService;
    @Autowired
    private UsageMapper usageMapper;
    @Autowired
    private StringRedisTemplate redis;

    public static final String HTML_REMINDER;
    public static final String HTML_LIMITED;

    static {
        try (InputStream input = new ClassPathResource("/email/reminder.html").getInputStream()) {
            HTML_REMINDER = FileUtil.getContent(input, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (InputStream input = new ClassPathResource("/email/limited.html").getInputStream()) {
            HTML_LIMITED = FileUtil.getContent(input, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Usage info(Long id) {
        return info(usageDao, id);
    }

    @Transactional
    public boolean record(Usage usage, ObjectNode auth) {
        BigDecimal amount = usage.getCreditAmount().abs();
        try {
            creditDao.consume(usage.getUid(), amount);
            usageDao.save(usage);
        } catch (Exception e) {
            log.error("记录积分使用信息失败：{}", DPUtil.toJSON(usage), e);
        }
        double remained = auth.at("/credit/remained").asDouble();
        boolean reminderEnabled = auth.at("/credit/reminderEnabled").asBoolean();
        double reminderThreshold = auth.at("/credit/reminderThreshold").asDouble();
        if (!reminderEnabled) return false; // 未开启预警
        if (remained <= reminderThreshold) return false; // 余额已小于预警阈值
        if (remained - amount.doubleValue() > reminderThreshold) return false; // 余额未达到预警阈值
        reminder(auth, reminderThreshold); // 发送预警通知
        return true;
    }

    public Map<String, Object> reminder(ObjectNode auth, double threshold) {
        String email = auth.at("/identity/email").asText();
        if (DPUtil.empty(email)) {
            return ApiUtil.result(13001, "未配置邮箱", email);
        }
        String key = RedisKey.reminder(auth.at("/uid").asInt());
        Boolean locked = redis.opsForValue().setIfAbsent(key, email, 30, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(locked)) {
            return ApiUtil.result(13002, "已执行过通知，半小时内不再重复处理", email);
        }
        String name = auth.at("/identity/name").asText();
        String subject = "平方域积分不足提醒";
        String html = HTML_REMINDER.replace("{name}", name).replace("{threshold}", String.valueOf(threshold));
        Map<String, Object> result = RpcUtil.result(memberRpc.email(email, subject, html));
        if (ApiUtil.failed(result)) {
            log.warn("usage reminder send email failed: {}", DPUtil.stringify(result));
            redis.delete(key); // 发送失败，清理分布式锁
        }
        return result;
    }

    public Map<String, Object> limited(ObjectNode auth, Map.Entry<String, String> entry) {
        String email = auth.at("/identity/email").asText();
        if (DPUtil.empty(email)) {
            return ApiUtil.result(13001, "未配置邮箱", email);
        }
        String key = RedisKey.limited(entry.getKey(), auth.at("/uid").asInt());
        Boolean locked = redis.opsForValue().setIfAbsent(key, email, 30, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(locked)) {
            return ApiUtil.result(13002, "已执行过通知，半小时内不再重复处理", email);
        }
        String name = auth.at("/identity/name").asText();
        String subject = "平方域" + entry.getValue() + "间隔内用量超限提醒";
        String html = HTML_LIMITED.replace("{name}", name).replace("{type}", entry.getValue());
        Map<String, Object> result = RpcUtil.result(memberRpc.email(email, subject, html));
        if (ApiUtil.failed(result)) {
            log.warn("usage limited send email failed: {}", DPUtil.stringify(result));
            redis.delete(key); // 发送失败，清理分布式锁
        }
        return result;
    }

    public Map<String, Object> audit(Map<?, ?> param, HttpServletRequest request) {
        Long id = ValidateUtil.filterLong(param.get("id"), true, 1L, null, 0L);
        Usage info = info(id);
        if (null == info) {
            return ApiUtil.result(404, null, id);
        }
        info.setAuditReason(DPUtil.implode(DPUtil.parseStringList(param.get("auditReason"))));
        info.setAuditDetail(DPUtil.parseString(param.get("auditDetail")));
        info.setAuditTime(System.currentTimeMillis());
        info.setAuditUid(rbacService.uid(request));
        info = usageDao.save(info);
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(usageDao, param, (root, query, cb) -> {
            SpecificationHelper<Usage> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).withoutDeleted().equalWithLongGTZero("id");
            helper.equalWithIntGTZero("uid").equal("type").equal("place").equal("status");
            helper.equalWithIntGTZero("authId").equalWithIntGTZero("modelId").equalWithIntGTZero("providerId");
            helper.equal("requestIp").like("requestBody").like("responseBody");
            helper.equal("finishReason").like("finishDetail");
            helper.like("auditReason").like("auditDetail").equalWithIntNotEmpty("auditUid");
            helper.betweenWithDate("beginTime").betweenWithDate("endTime").betweenWithDate("auditTime");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("id")), "id", "beginTime", "endTime", "auditTime", "coastTotal", "creditAmount");
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withInfo"))) {
            rbacService.fillUserInfo(rows, "uid", "auditUid");
            authService.fillInfo(rows, "authId");
            providerService.fillInfo(rows, "providerId");
            modelService.fillInfo(rows, "modelId");
        }
        return result;
    }

    public Map<String, Object> statistic(Map<String, Object> param) {
        StringBuilder where = new StringBuilder(" WHERE deleted_time = 0");
        Map<String, Object> filters = new LinkedHashMap<>();

        String beginTime = DPUtil.parseString(param.get("beginTime"));
        String endTime = DPUtil.parseString(param.get("endTime"));
        if (DPUtil.empty(beginTime) || DPUtil.empty(endTime)) {
            return ApiUtil.result(1001, "起止时间为必填字段", null);
        }
        long begin = DPUtil.dateTime2millis(beginTime, configuration.getFormatDate());
        long end = DPUtil.dateTime2millis(endTime, configuration.getFormatDate());
        if (end - begin > 15552000000L) {
            return ApiUtil.result(1002, "起止时间范围不能超过6个月", null);
        }
        where.append(" AND begin_time >= #{beginTime}");
        filters.put("beginTime", begin);
        where.append(" AND end_time <= #{endTime}");
        filters.put("endTime", end + 999);

        int uid = DPUtil.parseInt(param.get("uid"));
        if (uid > 0) { where.append(" AND uid = #{uid}"); filters.put("uid", uid); }

        String place = DPUtil.parseString(param.get("place"));
        if (!DPUtil.empty(place)) { where.append(" AND place = #{place}"); filters.put("place", place); }

        int authId = DPUtil.parseInt(param.get("authId"));
        if (authId > 0) { where.append(" AND auth_id = #{authId}"); filters.put("authId", authId); }

        int modelId = DPUtil.parseInt(param.get("modelId"));
        if (modelId > 0) { where.append(" AND model_id = #{modelId}"); filters.put("modelId", modelId); }

        int providerId = DPUtil.parseInt(param.get("providerId"));
        if (providerId > 0) { where.append(" AND provider_id = #{providerId}"); filters.put("providerId", providerId); }

        String wh = where.toString();

        String aggregation = DPUtil.parseString(param.get("aggregation"));
        if (DPUtil.empty(aggregation)) aggregation = "day";

        ObjectNode result = DPUtil.objectNode();

        // 总计
        result.replace("summary", toObjectNode(usageMapper.summary(buildQuery(filters, wh))));

        // 排名
        int rankLimit = DPUtil.parseInt(param.get("rankLimit"));
        if (rankLimit < 1) rankLimit = 20;
        ObjectNode ranking = DPUtil.objectNode();
        ranking.replace("byUser", DPUtil.toJSON(
                usageMapper.rank(buildQuery(filters, wh,
                        "field", "uid", "alias", "uid", "orderBy", "consumeCredits DESC", "limit", rankLimit)),
                ArrayNode.class));
        ranking.replace("byAuth", DPUtil.toJSON(
                usageMapper.rank(buildQuery(filters, wh,
                        "field", "auth_id", "alias", "authId", "orderBy", "consumeCredits DESC", "limit", rankLimit)),
                ArrayNode.class));
        ranking.replace("byProvider", DPUtil.toJSON(
                usageMapper.rank(buildQuery(filters, wh,
                        "field", "provider_id", "alias", "providerId", "orderBy", "consumeCredits DESC", "limit", rankLimit)),
                ArrayNode.class));
        ranking.replace("byModel", DPUtil.toJSON(
                usageMapper.rank(buildQuery(filters, wh,
                        "field", "model_id", "alias", "modelId", "orderBy", "consumeCredits DESC", "limit", rankLimit)),
                ArrayNode.class));
        result.replace("ranking", ranking);

        // 分布
        ObjectNode distribution = DPUtil.objectNode();
        distribution.replace("byStatus", DPUtil.toJSON(
                usageMapper.groupCount(buildQuery(filters, wh,
                        "field", "status", "alias", "status", "orderBy", "count DESC")),
                ArrayNode.class));
        distribution.replace("byFinishReason", DPUtil.toJSON(
                usageMapper.groupCount(buildQuery(filters, wh,
                        "field", "finish_reason", "alias", "finishReason", "orderBy", "count DESC")),
                ArrayNode.class));
        result.replace("distribution", distribution);

        // 时间轴
        ObjectNode timeline = DPUtil.objectNode();
        timeline.replace("credits", pivotByPlace(usageMapper.timelineByPlace(buildQuery(filters, wh,
                "aggregation", aggregation, "aggregateCredits", true))));
        timeline.replace("calls", pivotByPlace(usageMapper.timelineByPlace(buildQuery(filters, wh,
                "aggregation", aggregation, "aggregateCredits", false))));
        timeline.replace("tokens", DPUtil.toJSON(
                usageMapper.timelineTokens(buildQuery(filters, wh, "aggregation", aggregation)),
                ArrayNode.class));
        result.replace("timeline", timeline);

        // Fill dimension info for ranking
        JsonNode byUser = ranking.at("/byUser");
        if (!byUser.isEmpty()) rbacService.fillUserInfo(byUser, "uid");
        JsonNode byAuth = ranking.at("/byAuth");
        if (!byAuth.isEmpty()) authService.fillInfo(byAuth, "authId");
        JsonNode byProvider = ranking.at("/byProvider");
        if (!byProvider.isEmpty()) providerService.fillInfo(byProvider, "providerId");
        JsonNode byModel = ranking.at("/byModel");
        if (!byModel.isEmpty()) modelService.fillInfo(byModel, "modelId");

        return ApiUtil.result(0, null, result);
    }

    private Map<String, Object> buildQuery(Map<String, Object> filters, String where, Object... extras) {
        Map<String, Object> qp = new LinkedHashMap<>(filters);
        qp.put("where", where);
        for (int i = 0; i < extras.length; i += 2) {
            qp.put((String) extras[i], extras[i + 1]);
        }
        return qp;
    }

    private ObjectNode toObjectNode(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) return DPUtil.objectNode();
        return DPUtil.toJSON(rows.get(0), ObjectNode.class);
    }

    private ArrayNode pivotByPlace(List<Map<String, Object>> rows) {
        Map<String, ObjectNode> timeMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String time = DPUtil.parseString(row.get("time"));
            String place = DPUtil.parseString(row.get("place"));
            double value = row.get("value") == null ? 0 : ((Number) row.get("value")).doubleValue();
            ObjectNode node = timeMap.computeIfAbsent(time, k -> {
                ObjectNode n = DPUtil.objectNode();
                n.put("time", time);
                n.put("total", 0.0);
                return n;
            });
            node.put(place, value);
            node.put("total", node.get("total").asDouble() + value);
        }
        ArrayNode result = DPUtil.arrayNode();
        for (ObjectNode node : timeMap.values()) {
            result.add(node);
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            List<String> auditReason = DPUtil.parseStringList(node.at("/auditReason").asText(""));
            node.replace("auditReason", DPUtil.toJSON(auditReason));
            int requestStream = node.at("/requestStream").asInt(0);
            node.put("requestStream", 1 == requestStream);
        }
        return rows;
    }

    public boolean delete(List<Long> ids, HttpServletRequest request) {
        return delete(usageDao, ids, rbacService.uid(request));
    }

}
