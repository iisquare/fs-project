package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.lm.dao.ModelDao;
import com.iisquare.fs.web.lm.entity.Model;
import com.iisquare.fs.web.lm.entity.Provider;
import com.iisquare.fs.web.lm.mvc.Configuration;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.bouncycastle.asn1.x500.style.RFC4519Style.c;

@Service
public class ModelService extends JPAServiceBase {

    @Autowired
    private ModelDao modelDao;
    @Autowired
    private ProviderService providerService;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;
    @Autowired
    AIService aiService;

    private static final ObjectNode plans = DPUtil.objectNode();

    static {
        ObjectNode item = plans.putObject("chat");
        item.put("name", "对话");
        ObjectNode parameters = item.putObject("parameters");
        parameters.replace("max_position_embeddings", parameter("输入长度", "number", "Tokens"));
        parameters.replace("rpm", parameter("每分钟请求数", "number", "RPM"));
        parameters.replace("tpm", parameter("每分钟词元数", "number", "TPM"));
        parameters.replace("prompt_cache_hit_credits", parameter("命中缓存输入消耗积分数", "number", "积分/百万词元"));
        parameters.replace("prompt_cache_miss_credits", parameter("未命中缓存输入消耗积分数", "number", "积分/百万词元"));
        parameters.replace("completion_credits", parameter("输出消耗积分数", "number", "积分/百万词元"));
        item = plans.putObject("embedding");
        item.put("name", "词嵌入");
        parameters = item.putObject("parameters");
        parameters.replace("max_position_embeddings", parameter("输入长度", "number", "Tokens"));
        parameters.replace("dimension", parameter("输出维度", "number", "维"));
        parameters.replace("rpm", parameter("每分钟请求数", "number", "RPM"));
        parameters.replace("tpm", parameter("每分钟词元数", "number", "TPM"));
        parameters.replace("credits", parameter("消耗积分数", "number", "积分/百万词元"));
        item = plans.putObject("reranker");
        item.put("name", "重排序");
        parameters = item.putObject("parameters");
        parameters.replace("max_position_embeddings", parameter("输入长度", "number", "Tokens"));
        parameters.replace("rpm", parameter("每分钟请求数", "number", "RPM"));
        parameters.replace("tpm", parameter("每分钟词元数", "number", "TPM"));
        parameters.replace("credits", parameter("消耗积分数", "number", "积分/百万词元"));
        parameters.putObject("");
    }

    public static ObjectNode parameter(String name, String type, String unit) {
        ObjectNode parameter = DPUtil.objectNode();
        parameter.put("name", name);
        parameter.put("type", type);
        parameter.put("unit", unit);
        return parameter;
    }

    public ObjectNode plans() {
        return plans;
    }

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
    }

    public Map<String, String> types() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("chat", "对话");
        types.put("embedding", "词嵌入");
        types.put("reranker", "重排序");
        return types;
    }

    public Map<String, Object> embedding(Integer id, List<String> inputs) {
        if (inputs.isEmpty()) return ApiUtil.result(16101, "待生成内容不能为空", inputs);
        for (String input : inputs) {
            int length = input.length();
            if (length < 1 || length > 512) {
                return ApiUtil.result(16102, "待生成内容长度异常", input);
            }
        }
        Model model = info(id);
        if (null == model || 1 != model.getStatus()) {
            return ApiUtil.result(16001, "模型不存在或已禁用", id);
        }
        if (!"embedding".equals(model.getType())) {
            return ApiUtil.result(16002, "模型类型异常", model.getType());
        }
        Provider provider = providerService.info(model.getProviderId());
        if (null == provider || 1 != provider.getStatus()) {
            return ApiUtil.result(16003, "模型提供商不存在或已禁用", model.getProviderId());
        }
        String url = provider.getEndpoint() + "/embeddings";
        ObjectNode body = DPUtil.objectNode();
        body.put("model", model.getName());
        body.replace("input", DPUtil.toJSON(inputs));
        return aiService.post(url, body, aiService.authorization(provider.getToken()));
    }

    public Model info(Integer id) {
        return info(modelDao, id);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "模型名称不能为空", name);
        String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
        if(!types().containsKey(type)) return ApiUtil.result(1002, "类型异常", type);
        String alias = DPUtil.trim(DPUtil.parseString(param.get("alias")));
        if(!DPUtil.empty(alias) && !ValidateUtil.isLabel(alias)) return ApiUtil.result(1003, "别名格式不正确", alias);
        String plan = DPUtil.trim(DPUtil.parseString(param.get("plan")));
        if(!plans.has(plan)) return ApiUtil.result(1004, "计费方案异常", plan);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1005, "状态异常", status);
        Model info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Model();
        }
        Provider provider = providerService.info(DPUtil.parseInt(param.get("providerId")));
        if (null == provider) {
            return ApiUtil.result(2001, "所属供应商不存在", null);
        }
        info.setProviderId(provider.getId());
        info.setName(name);
        info.setType(type);
        info.setAlias(alias);
        info.setRoleIds(DPUtil.implode(",", DPUtil.parseIntList(param.get("roleIds"))));
        info.setExplorable(DPUtil.parseBoolean(param.get("explorable")) ? 1 : 0);
        info.setAllVisible(DPUtil.parseBoolean(param.get("allVisible")) ? 1 : 0);
        info.setSecurityDetectable(DPUtil.parseBoolean(param.get("securityDetectable")) ? 1 : 0);
        info.setPlan(plan);
        info.setContent(DPUtil.stringify(param.get("content")));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(modelDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(modelDao, param, (root, query, cb) -> {
            SpecificationHelper<Model> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntGTZero("providerId");
            helper.equalWithIntNotEmpty("status").like("name").equal("type");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort")), "id", "status", "sort");
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withProviderInfo"))) {
            providerService.fillInfo(rows, "providerId");
        }
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        if (!DPUtil.empty(args.get("withRoles"))) {
            rbacService.fillInfos(rows);
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        Map<String, String> types = types();
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            List<Integer> ids = DPUtil.parseIntList(node.at("/roleIds").asText(""));
            node.replace("roleIds", DPUtil.toJSON(ids));
            node.replace("content", DPUtil.parseJSON(node.at("/content").asText()));
            String type = node.at("/type").asText();
            node.put("typeText", types.getOrDefault(type, ""));
            String plan = node.at("/plan").asText();
            node.put("planText", plans.at("/" + plan + "/name").asText());
            int explorable = node.at("/explorable").asInt(0);
            node.put("explorable", 1 == explorable);
            int allVisible = node.at("/allVisible").asInt(0);
            node.put("allVisible", 1 == allVisible);
            int securityDetectable = node.at("/securityDetectable").asInt(0);
            node.put("securityDetectable", 1 == securityDetectable);
        }
        return rows;
    }

    public boolean remove(List<Integer> ids) {
        return remove(modelDao, ids);
    }

    public JsonNode fillInfo(JsonNode rows, String ...properties) {
        return fillInfo(modelDao, rows, properties);
    }

    public JsonNode fillInfos(JsonNode rows, String ...properties) {
        return fillInfos(modelDao, rows, properties);
    }

}
