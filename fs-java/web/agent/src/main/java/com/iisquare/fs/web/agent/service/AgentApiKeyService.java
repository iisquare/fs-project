package com.iisquare.fs.web.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.agent.dao.AgentApiKeyDao;
import com.iisquare.fs.web.agent.dao.AgenticDao;
import com.iisquare.fs.web.agent.entity.AgentApiKey;
import com.iisquare.fs.web.agent.entity.Agentic;
import com.iisquare.fs.web.agent.mvc.Configuration;
import com.iisquare.fs.web.core.mvc.FeignInterceptor;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;

@Service
public class AgentApiKeyService extends JPAServiceBase {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    @Autowired
    AgentApiKeyDao agentApiKeyDao;
    @Autowired
    AgenticDao agenticDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;

    public record AuthenticatedKey(AgentApiKey key, int uid, JsonNode identity,
                                   String session, boolean userProxy) {
    }

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("lastUsedTime", "desc");
        sorts.put("expiredTime", "asc");
        sorts.put("createdTime", "asc");
        return sorts;
    }

    public Map<Integer, String> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
    }

    public AgentApiKey info(Integer id) {
        return info(agentApiKeyDao, id);
    }

    protected String generateSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String random = URL_ENCODER.encodeToString(bytes);
        return "agt_" + random.substring(0, 8) + "." + random.substring(8);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
        int uid = DPUtil.parseInt(param.get("uid"));
        if (uid < 1) return ApiUtil.result(1002, "所属用户异常", uid);
        int status = DPUtil.parseInt(param.get("status"));
        if (!status().containsKey(status)) return ApiUtil.result(1003, "状态异常", status);
        List<Integer> agenticIds = DPUtil.parseIntList(param.get("agenticIds"));
        for (Integer agenticId : agenticIds) {
            Agentic agentic = info(agenticDao, agenticId);
            if (null == agentic) return ApiUtil.result(1005, "编排流程不存在", agenticId);
        }

        AgentApiKey info;
        if (id > 0) {
            if (!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if (null == info) return ApiUtil.result(404, null, id);
        } else {
            if (!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new AgentApiKey();
        }
        String secret = DPUtil.trim(DPUtil.parseString(param.get("secret")));
        // 兼容旧 prefix 回填值：完整 Agent Secret 固定带小数点，旧前缀形如 agt_xxxxxxxx。
        if (DPUtil.empty(secret) || (secret.startsWith("agt_") && !secret.contains("."))) {
            secret = generateSecret();
        }
        if (secret.length() > 64) return ApiUtil.result(1006, "密钥长度不能超过 64 个字符", null);
        AgentApiKey sameSecret = agentApiKeyDao.findFirstBySecretAndDeletedTime(secret, 0L).orElse(null);
        if (null != sameSecret && !Objects.equals(sameSecret.getId(), info.getId())) {
            return ApiUtil.result(1007, "密钥已存在", null);
        }
        info.setName(name);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info.setSecret(secret);
        info.setUid(uid);
        info.setAgenticIds(DPUtil.implode(",", agenticIds));
        info.setStatus(status);
        info.setExpiredTime(parseExpiredTime(param.get("expiredTime")));
        info.setLastUsedTime(null == info.getLastUsedTime() ? 0L : info.getLastUsedTime());
        info = save(agentApiKeyDao, info, rbacService.uid(request));

        ObjectNode data = DPUtil.toJSON(info, ObjectNode.class);
        if (null == data) data = DPUtil.objectNode();
        data.put("agenticIds", DPUtil.toJSON(agenticIds));
        return ApiUtil.result(0, null, data);
    }

    /** 流程授权范围：未配置时默认允许全部流程。 */
    public boolean allows(AgentApiKey key, int agenticId) {
        List<Integer> allowed = DPUtil.parseIntList(null == key ? "" : key.getAgenticIds());
        return allowed.isEmpty() || allowed.contains(agenticId);
    }

    protected long parseExpiredTime(Object value) {
        String expiredTime = DPUtil.trim(DPUtil.parseString(value));
        return DPUtil.empty(expiredTime) ? 0L
                : DPUtil.dateTime2millis(expiredTime, configuration.getFormatDate());
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(agentApiKeyDao, param, (root, query, cb) -> {
            SpecificationHelper<AgentApiKey> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).withoutDeleted()
                    .equalWithIntGTZero("id").equalWithIntGTZero("uid")
                    .equalWithIntNotEmpty("status").like("name").equal("secret");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("id")), sorts().keySet());
        JsonNode rows = format(ApiUtil.rows(result));
        if (!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "uid", "createdUid", "updatedUid");
        }
        if (!DPUtil.empty(args.get("withStatusText"))) fillStatus(rows, status());
        if (!DPUtil.empty(args.get("withAgenticInfo"))) fillAgenticInfo(rows);
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            List<Integer> ids = agenticIds(node.get("agenticIds"));
            node.replace("agenticIds", DPUtil.toJSON(ids));
        }
        return rows;
    }

    protected List<Integer> agenticIds(JsonNode value) {
        if (null == value) return new ArrayList<>();
        if (value.isTextual()) {
            String text = value.asText("").trim();
            if (text.startsWith("[")) {
                JsonNode json = DPUtil.parseJSON(text);
                if (null != json && json.isArray()) value = json;
            } else {
                return DPUtil.parseIntList(text);
            }
        }
        if (value.isArray()) {
            List<Integer> result = new ArrayList<>();
            for (JsonNode item : value) result.add(item.asInt(0));
            return result;
        }
        return DPUtil.parseIntList(value);
    }

    /** 运行日志展示信息：按 apiKeyId 批量填充密钥名称与所属用户，不在日志中保存快照。 */
    public JsonNode fillLogInfo(JsonNode rows) {
        Set<Integer> ids = new HashSet<>();
        for (JsonNode row : rows) {
            int id = row.at("/apiKeyId").asInt(0);
            if (id > 0) ids.add(id);
        }
        Map<Integer, AgentApiKey> infos = new HashMap<>();
        if (!ids.isEmpty()) {
            for (AgentApiKey item : agentApiKeyDao.findAllById(ids)) infos.put(item.getId(), item);
        }
        for (JsonNode row : rows) {
            AgentApiKey item = infos.get(row.at("/apiKeyId").asInt(0));
            ObjectNode node = (ObjectNode) row;
            node.put("apiKeyName", null == item ? "" : item.getName());
            node.put("apiKeyOwnerUid", null == item || null == item.getUid() ? 0 : item.getUid());
        }
        return rows;
    }

    protected void fillAgenticInfo(JsonNode rows) {
        Set<Integer> ids = new HashSet<>();
        for (JsonNode row : rows) ids.addAll(agenticIds(row.get("agenticIds")));
        Map<Integer, Agentic> infos = new HashMap<>();
        if (!ids.isEmpty()) for (Agentic item : agenticDao.findAllById(ids)) infos.put(item.getId(), item);
        for (JsonNode row : rows) {
            ArrayNode items = ((ObjectNode) row).putArray("agentics");
            for (Integer id : agenticIds(row.get("agenticIds"))) {
                Agentic item = infos.get(id);
                if (null == item) continue;
                ObjectNode node = items.addObject();
                node.put("id", item.getId());
                node.put("name", item.getName());
                node.put("status", item.getStatus());
            }
        }
    }

    /** 外部调用鉴权：按密钥查询，显式携带有效用户会话时切换到会话用户。 */
    public AuthenticatedKey authenticate(String secret, HttpServletRequest request) {
        if (DPUtil.empty(secret)) return null;
        AgentApiKey key = agentApiKeyDao.findFirstBySecretAndDeletedTime(secret, 0L).orElse(null);
        if (null == key || 1 != DPUtil.parseInt(key.getStatus())) return null;
        if (null != key.getExpiredTime() && key.getExpiredTime() > 0
                && key.getExpiredTime() <= System.currentTimeMillis()) return null;

        String suppliedSession = DPUtil.trim(request.getHeader(FeignInterceptor.HEADER_AUTH_TOKEN));
        String session = DPUtil.empty(suppliedSession) ? "" : rbacService.token(request);
        if (!DPUtil.empty(suppliedSession) && DPUtil.empty(session)) return null;
        // 密钥属主必须仍然有效；用户代理只切换生效用户，不绕过 Key 属主停用。
        JsonNode owner = rbacService.identityById(key.getUid());
        if (null == owner || owner.isNull() || owner.isEmpty()) return null;
        JsonNode identity;
        int uid = key.getUid();
        if (!DPUtil.empty(session)) {
            identity = rbacService.identityByToken(session);
            uid = null == identity ? 0 : identity.at("/id").asInt(0);
        } else {
            identity = owner;
        }
        if (uid < 1 || null == identity || identity.isNull() || identity.isEmpty()) return null;
        JsonNode roles = identity.at("/roles");
        if (null == roles || roles.isEmpty()) return null;

        key.setLastUsedTime(System.currentTimeMillis());
        agentApiKeyDao.save(key);
        return new AuthenticatedKey(key, uid, identity, session,
                !DPUtil.empty(session));
    }

    public Map<String, Object> remove(List<Integer> ids, HttpServletRequest request) {
        return delete(agentApiKeyDao, ids, rbacService.uid(request));
    }

}
