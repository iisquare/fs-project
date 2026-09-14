package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.base.web.util.ServiceUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.lm.dao.CreditDao;
import com.iisquare.fs.web.lm.dao.UsageDao;
import com.iisquare.fs.web.lm.mapper.UsageMapper;
import com.iisquare.fs.web.lm.entity.Usage;
import com.iisquare.fs.web.lm.mvc.Configuration;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    AuthService authService;
    @Autowired
    ProviderService  providerService;
    @Autowired
    ModelService modelService;
    @Autowired
    UsageMapper usageMapper;
    @Autowired
    RemindService  remindService;


    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("beginTime", "asc");
        sorts.put("endTime", "asc");
        sorts.put("auditTime", "asc");
        sorts.put("coastTotal", "asc");
        sorts.put("creditAmount", "asc");
        return sorts;
    }

    public Usage info(Long id) {
        return info(usageDao, id);
    }

    public Map<String, Object> info(Map<?, ?> param) {
        Usage info = info(DPUtil.parseLong(param.get("id")));
        if (null == info) return ApiUtil.result(1404, "信息不存在", null);
        JsonNode rows = withInfo(DPUtil.toArrayNode(info));
        JsonNode node = DPUtil.firstNode(format(rows));
        return ApiUtil.result(0, null, node);
    }

    public JsonNode withInfo(JsonNode rows) {
        rbacService.fillUserInfo(rows, "uid", "auditUid");
        authService.fillInfo(rows, "authId");
        providerService.fillInfo(rows, "providerId");
        modelService.fillInfo(rows, "modelId");
        return rows;
    }

    @Transactional
    public boolean record(Usage usage, ObjectNode auth) {
        if (null == usage.getCreditAmount()) {
            usage.setCreditAmount(BigDecimal.ZERO);
        }
        BigDecimal amount = usage.getCreditAmount().abs();
        try {
            creditDao.consume(usage.getUid(), amount);
            usageDao.save(usage);
        } catch (Exception e) {
            log.error("记录积分使用信息失败：{}", DPUtil.toJSON(usage), e);
        }
        remindService.amount(auth, amount.doubleValue()); // 发送预警通知
        return true;
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
            helper.equal("requestIp").like("requestHeader").like("requestBody");
            helper.like("requestPrompt").like("requestSystem").like("requestUser");
            helper.like("responseHeader").like("responseBody");
            helper.like("responseReason").like("responseCompletion").like("responseTool");
            helper.equal("finishReason").like("finishDetail");
            helper.like("auditReason").like("auditDetail").equalWithIntNotEmpty("auditUid");
            helper.betweenWithDate("beginTime").betweenWithDate("endTime").betweenWithDate("auditTime");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("id")), sorts().keySet());
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withInfo"))) withInfo(rows);
        ServiceUtil.retain(rows, param.get("columns"));
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
        where.append(" AND begin_time <= #{endTime}");
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

        // 一次读取明细数据，后续统计均在内存中完成
        List<Map<String, Object>> rows = usageMapper.statisticRows(buildQuery(filters, wh));

        ObjectNode result = DPUtil.objectNode();

        // 总计
        ObjectNode summary = DPUtil.objectNode();
        long calls = 0, cachedTokens = 0, inputTokens = 0, outputTokens = 0, totalTokens = 0;
        BigDecimal rechargeCredits = BigDecimal.ZERO, consumeCredits = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            calls++;
            BigDecimal creditAmount = DPUtil.parseDecimal(row.get("creditAmount"));
            if (null == creditAmount) creditAmount = BigDecimal.ZERO;
            if (creditAmount.signum() > 0) {
                rechargeCredits = rechargeCredits.add(creditAmount);
            } else if (creditAmount.signum() < 0) {
                consumeCredits = consumeCredits.add(creditAmount.negate());
            }
            cachedTokens += DPUtil.parseLong(row.get("cachedTokens"));
            inputTokens += DPUtil.parseLong(row.get("inputTokens"));
            outputTokens += DPUtil.parseLong(row.get("outputTokens"));
            totalTokens += DPUtil.parseLong(row.get("totalTokens"));
        }
        summary.put("calls", calls);
        summary.put("rechargeCredits", rechargeCredits);
        summary.put("consumeCredits", consumeCredits);
        summary.put("cachedTokens", cachedTokens);
        summary.put("inputTokens", inputTokens);
        summary.put("outputTokens", outputTokens);
        summary.put("totalTokens", totalTokens);
        result.replace("summary", summary);

        // 排名
        int rankLimit = DPUtil.parseInt(param.get("rankLimit"));
        if (rankLimit < 1) rankLimit = 15;
        ObjectNode ranking = DPUtil.objectNode();
        Map<Integer, RankItem> userRank = new HashMap<>();
        Map<Integer, RankItem> authRank = new HashMap<>();
        Map<Integer, RankItem> providerRank = new HashMap<>();
        Map<Integer, RankItem> modelRank = new HashMap<>();
        for (Map<String, Object> row : rows) {
            aggregateRank(userRank, DPUtil.parseInt(row.get("uid")), row);
            aggregateRank(authRank, DPUtil.parseInt(row.get("authId")), row);
            aggregateRank(providerRank, DPUtil.parseInt(row.get("providerId")), row);
            aggregateRank(modelRank, DPUtil.parseInt(row.get("modelId")), row);
        }
        ranking.replace("byUser", rankArray(userRank, "uid", rankLimit));
        ranking.replace("byAuth", rankArray(authRank, "authId", rankLimit));
        ranking.replace("byProvider", rankArray(providerRank, "providerId", rankLimit));
        ranking.replace("byModel", rankArray(modelRank, "modelId", rankLimit));
        result.replace("ranking", ranking);

        // 分布
        ObjectNode distribution = DPUtil.objectNode();
        Map<String, Long> statusCount = new LinkedHashMap<>();
        Map<String, Long> finishReasonCount = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            count(statusCount, DPUtil.parseString(row.get("status")));
            count(finishReasonCount, DPUtil.parseString(row.get("finishReason")));
        }
        distribution.replace("byStatus", countArray(statusCount, "status"));
        distribution.replace("byFinishReason", countArray(finishReasonCount, "finishReason"));
        result.replace("distribution", distribution);

        // 时间轴
        ObjectNode timeline = DPUtil.objectNode();
        Map<String, Map<String, double[]>> creditsByTime = new TreeMap<>(); // time -> place -> [consume, calls]
        Map<String, long[]> tokensByTime = new TreeMap<>(); // time -> [cached, input, output, total]
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timePattern(aggregation));
        for (Map<String, Object> row : rows) {
            String time = timeKey(DPUtil.parseLong(row.get("beginTime")), formatter);
            String placeName = DPUtil.parseString(row.get("place"));
            if (DPUtil.empty(placeName)) placeName = "";
            BigDecimal creditAmount = DPUtil.parseDecimal(row.get("creditAmount"));
            double consume = (null != creditAmount && creditAmount.signum() < 0) ? creditAmount.negate().doubleValue() : 0;
            double[] placeValue = creditsByTime.computeIfAbsent(time, k -> new LinkedHashMap<>())
                    .computeIfAbsent(placeName, k -> new double[2]);
            placeValue[0] += consume;
            placeValue[1] += 1;
            long[] tokenValue = tokensByTime.computeIfAbsent(time, k -> new long[4]);
            tokenValue[0] += DPUtil.parseLong(row.get("cachedTokens"));
            tokenValue[1] += DPUtil.parseLong(row.get("inputTokens"));
            tokenValue[2] += DPUtil.parseLong(row.get("outputTokens"));
            tokenValue[3] += DPUtil.parseLong(row.get("totalTokens"));
        }
        timeline.replace("credits", timelineArray(creditsByTime, true));
        timeline.replace("calls", timelineArray(creditsByTime, false));
        timeline.replace("tokens", timelineTokensArray(tokensByTime));
        result.replace("timeline", timeline);

        // 用户角色排名（角色Top5分组，组内用户Top10）
        ranking.replace("byRole", rankByRole(userRank, 5, 10));

        // Fill dimension info for ranking and role groups
        JsonNode byUser = ranking.at("/byUser");
        if (!byUser.isEmpty()) rbacService.fillUserInfo(byUser, "uid");
        JsonNode byAuth = ranking.at("/byAuth");
        if (!byAuth.isEmpty()) authService.fillInfo(byAuth, "authId");
        JsonNode byProvider = ranking.at("/byProvider");
        if (!byProvider.isEmpty()) providerService.fillInfo(byProvider, "providerId");
        JsonNode byModel = ranking.at("/byModel");
        if (!byModel.isEmpty()) modelService.fillInfo(byModel, "modelId");
        JsonNode byRole = ranking.at("/byRole");
        if (!byRole.isEmpty()) {
            for (JsonNode group : byRole) {
                JsonNode users = group.at("/users");
                if (!users.isEmpty()) rbacService.fillUserInfo(users, "uid");
            }
        }

        return ApiUtil.result(0, null, result);
    }

    private static class RankItem {
        long calls;
        long tokens;
        BigDecimal consumeCredits = BigDecimal.ZERO;
    }

    private void aggregateRank(Map<Integer, RankItem> rank, int key, Map<String, Object> row) {
        if (key < 1) return;
        RankItem item = rank.computeIfAbsent(key, k -> new RankItem());
        item.calls++;
        item.tokens += DPUtil.parseLong(row.get("totalTokens"));
        BigDecimal creditAmount = DPUtil.parseDecimal(row.get("creditAmount"));
        if (null != creditAmount && creditAmount.signum() < 0) {
            item.consumeCredits = item.consumeCredits.add(creditAmount.negate());
        }
    }

    private ArrayNode rankArray(Map<Integer, RankItem> rank, String field, int limit) {
        List<Map.Entry<Integer, RankItem>> list = sortedRank(rank);
        ArrayNode result = DPUtil.arrayNode();
        for (int i = 0; i < list.size() && i < limit; i++) {
            Map.Entry<Integer, RankItem> entry = list.get(i);
            RankItem item = entry.getValue();
            ObjectNode node = DPUtil.objectNode();
            node.put(field, entry.getKey());
            node.put("calls", item.calls);
            node.put("tokens", item.tokens);
            node.put("consumeCredits", item.consumeCredits);
            result.add(node);
        }
        return result;
    }

    private List<Map.Entry<Integer, RankItem>> sortedRank(Map<Integer, RankItem> rank) {
        List<Map.Entry<Integer, RankItem>> list = new ArrayList<>(rank.entrySet());
        list.sort((a, b) -> {
            int compare = b.getValue().consumeCredits.compareTo(a.getValue().consumeCredits);
            if (0 != compare) return compare;
            compare = Long.compare(b.getValue().calls, a.getValue().calls);
            if (0 != compare) return compare;
            compare = Long.compare(b.getValue().tokens, a.getValue().tokens);
            if (0 != compare) return compare;
            return a.getKey().compareTo(b.getKey());
        });
        return list;
    }

    private ArrayNode rankByRole(Map<Integer, RankItem> userRank, int roleLimit, int groupLimit) {
        if (userRank.isEmpty()) return DPUtil.arrayNode();
        JsonNode userInfos = rbacService.userInfos(userRank.keySet());
        if (null == userInfos || userInfos.isEmpty()) return DPUtil.arrayNode();
        Map<Integer, RoleBucket> buckets = new HashMap<>();
        for (Map.Entry<Integer, RankItem> entry : sortedRank(userRank)) {
            JsonNode user = userInfos.at("/" + entry.getKey());
            JsonNode roles = user.at("/roles");
            if (null == roles || roles.isEmpty()) continue;
            for (JsonNode role : roles) {
                int roleId = role.at("/id").asInt();
                if (roleId < 1) continue;
                RoleBucket bucket = buckets.computeIfAbsent(roleId, k -> new RoleBucket());
                bucket.roleId = roleId;
                bucket.roleName = role.at("/name").asText("");
                bucket.items.add(entry);
                RankItem total = bucket.total;
                total.calls += entry.getValue().calls;
                total.tokens += entry.getValue().tokens;
                total.consumeCredits = total.consumeCredits.add(entry.getValue().consumeCredits);
            }
        }
        List<RoleBucket> sorted = new ArrayList<>(buckets.values());
        sorted.sort((a, b) -> {
            int compare = b.total.consumeCredits.compareTo(a.total.consumeCredits);
            if (0 != compare) return compare;
            compare = Long.compare(b.total.calls, a.total.calls);
            if (0 != compare) return compare;
            return Integer.compare(a.roleId, b.roleId);
        });
        ArrayNode result = DPUtil.arrayNode();
        for (int i = 0; i < sorted.size() && i < roleLimit; i++) {
            RoleBucket bucket = sorted.get(i);
            ObjectNode group = result.addObject();
            group.put("roleId", bucket.roleId);
            ObjectNode roleInfo = group.putObject("roleInfo");
            roleInfo.put("id", bucket.roleId);
            roleInfo.put("name", DPUtil.empty(bucket.roleName) ? ("角色#" + bucket.roleId) : bucket.roleName);
            group.put("calls", bucket.total.calls);
            group.put("tokens", bucket.total.tokens);
            group.put("consumeCredits", bucket.total.consumeCredits);
            ArrayNode users = group.putArray("users");
            for (int j = 0; j < bucket.items.size() && j < groupLimit; j++) {
                Map.Entry<Integer, RankItem> entry = bucket.items.get(j);
                ObjectNode user = users.addObject();
                user.put("uid", entry.getKey());
                user.put("calls", entry.getValue().calls);
                user.put("tokens", entry.getValue().tokens);
                user.put("consumeCredits", entry.getValue().consumeCredits);
            }
        }
        return result;
    }

    private static class RoleBucket {
        int roleId;
        String roleName;
        RankItem total = new RankItem();
        List<Map.Entry<Integer, RankItem>> items = new ArrayList<>();
    }

    private void count(Map<String, Long> counter, String key) {
        if (DPUtil.empty(key)) key = "";
        counter.put(key, counter.getOrDefault(key, 0L) + 1);
    }

    private ArrayNode countArray(Map<String, Long> counter, String field) {
        List<Map.Entry<String, Long>> list = new ArrayList<>(counter.entrySet());
        list.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        ArrayNode result = DPUtil.arrayNode();
        for (Map.Entry<String, Long> entry : list) {
            ObjectNode node = result.addObject();
            if (!DPUtil.empty(entry.getKey())) node.put(field, entry.getKey());
            node.put("count", entry.getValue());
        }
        return result;
    }

    private String timePattern(String aggregation) {
        return switch (aggregation) {
            case "hour" -> "yyyy-MM-dd HH:00";
            case "month" -> "yyyy-MM";
            default -> "yyyy-MM-dd";
        };
    }

    private String timeKey(long time, DateTimeFormatter formatter) {
        if (time < 1) return "";
        return Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(formatter);
    }

    private ArrayNode timelineArray(Map<String, Map<String, double[]>> data, boolean credits) {
        ArrayNode result = DPUtil.arrayNode();
        for (Map.Entry<String, Map<String, double[]>> entry : data.entrySet()) {
            ObjectNode node = DPUtil.objectNode();
            node.put("time", entry.getKey());
            double total = 0;
            for (Map.Entry<String, double[]> place : entry.getValue().entrySet()) {
                double value = credits ? place.getValue()[0] : place.getValue()[1];
                total += value;
                node.put(place.getKey(), value);
            }
            node.put("total", total);
            result.add(node);
        }
        return result;
    }

    private ArrayNode timelineTokensArray(Map<String, long[]> data) {
        ArrayNode result = DPUtil.arrayNode();
        for (Map.Entry<String, long[]> entry : data.entrySet()) {
            long[] value = entry.getValue();
            ObjectNode node = result.addObject();
            node.put("time", entry.getKey());
            node.put("cachedTokens", value[0]);
            node.put("inputTokens", value[1]);
            node.put("outputTokens", value[2]);
            node.put("totalTokens", value[3]);
        }
        return result;
    }

    private Map<String, Object> buildQuery(Map<String, Object> filters, String where, Object... extras) {
        Map<String, Object> qp = new LinkedHashMap<>(filters);
        qp.put("where", where);
        for (int i = 0; i < extras.length; i += 2) {
            qp.put((String) extras[i], extras[i + 1]);
        }
        return qp;
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
