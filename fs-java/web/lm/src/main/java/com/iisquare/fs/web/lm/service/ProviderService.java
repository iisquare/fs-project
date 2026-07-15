package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.lm.dao.ModelDao;
import com.iisquare.fs.web.lm.dao.ProviderDao;
import com.iisquare.fs.web.lm.entity.Model;
import com.iisquare.fs.web.lm.entity.Provider;
import com.iisquare.fs.web.lm.mvc.Configuration;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProviderService extends JPAServiceBase {

    @Autowired
    ProviderDao providerDao;
    @Autowired
    ModelDao modelDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;

    public Map<String, String> types() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("vllm", "vLLM");
        types.put("sglang", "SGLang");
        types.put("mindie", "MindIE");
        types.put("mixed-compatible", "Mixed Compatible");
        types.put("openai-compatible", "OpenAI Compatible");
        types.put("anthropic-compatible", "Anthropic Compatible");
        types.put("deepseek", "深度求索");
        types.put("volcengine", "火山引擎");
        types.put("siliconflow", "硅基流动");
        types.put("aliyun", "阿里云百炼");
        return types;
    }

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
    }

    /**
     * 配置缓存
     * {
     *     aliases: {
     *         [alias or modelName]: [modelId],
     *     },
     *     models: {
     *         [modelId]: {
     *             id: Integer,
     *             providerId: Integer,
     *             name: String,
     *             alias: String,
     *             type: String,
     *             roleIds: Array<Integer>,
     *             explorable: Boolean,
     *             allVisible: Boolean,
     *             securityDetectable: Boolean,
     *             plan: String,
     *             content: {}
     *         }
     *     },
     *     providers: {
     *         [providerId]: {
     *             id: Integer,
     *             type: String,
     *             serial: String,
     *             name: String,
     *             endpoint: String,
     *             token: String
     *         }
     *     },
     * }
     */
    public ObjectNode cache() {
        ObjectNode cache = DPUtil.objectNode();
        ObjectNode models = cache.putObject("models");
        ObjectNode aliases = cache.putObject("aliases");
        ObjectNode providers = cache.putObject("providers");
        List<Provider> providerList = providerDao.findAll((Specification<Provider>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), 1));
            return cb.and(predicates.toArray(new Predicate[0]));
        });
        for (Provider provider : providerList) {
            ObjectNode item = providers.putObject(String.valueOf(provider.getId()));
            item.put("id", provider.getId());
            item.put("type", provider.getType());
            item.put("serial", provider.getSerial());
            item.put("name", provider.getName());
            item.put("endpoint", provider.getEndpoint());
            item.put("token", provider.getToken());
        }
        List<Model> modelList = modelDao.findAll((Specification<Model>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), 1));
            return cb.and(predicates.toArray(new Predicate[0]));
        });
        for (Model model : modelList) {
            JsonNode provider = providers.at("/" + model.getProviderId());
            if (provider.isEmpty()) continue;
            String place = (DPUtil.empty(model.getAlias()) ? model.getName() : model.getAlias());
            (aliases.has(place) ? (ArrayNode) aliases.at(place) : aliases.putArray(place)).add(model.getId());
            ObjectNode item = models.putObject(String.valueOf(model.getId()));
            item.put("id", model.getId());
            item.put("providerId", model.getProviderId());
            item.put("name", model.getName());
            item.put("alias", model.getAlias());
            item.put("type", model.getType());
            item.replace("roleIds", DPUtil.toJSON(DPUtil.parseIntList(model.getRoleIds())));
            item.put("explorable", model.getExplorable() == 1);
            item.put("allVisible", model.getAllVisible() == 1);
            item.put("securityDetectable", model.getSecurityDetectable() == 1);
            item.put("plan", model.getPlan());
            item.replace("content", DPUtil.parseJSON(model.getContent()));
        }
        return cache;
    }

    public Provider info(Integer id) {
        return info(providerDao, id);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
        if(DPUtil.empty(type)) return ApiUtil.result(1001, "供应商类型不能为空", type);
        String serial = DPUtil.trim(DPUtil.parseString(param.get("serial")));
        if(DPUtil.empty(serial)) return ApiUtil.result(1002, "唯一标识不能为空", serial);
        if(!ValidateUtil.isLabel(serial)) return ApiUtil.result(1003, "唯一标识格式不正确", serial);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1004, "供应商名称不能为空", name);
        String endpoint = DPUtil.trim(DPUtil.parseString(param.get("endpoint")));
        if(DPUtil.empty(endpoint)) return ApiUtil.result(1005, "调用地址不能为空", endpoint);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1006, "状态异常", status);
        if(!types().containsKey(type)) return ApiUtil.result(1007, "供应商类型异常", type);
        Provider info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Provider();
        }
        int count = providerDao.exist(serial, DPUtil.parseInt(info.getId()));
        if (count > 0) {
            return ApiUtil.result(1501, "标识已存在", serial);
        }
        info.setType(type);
        info.setSerial(serial);
        info.setName(name);
        info.setEndpoint(endpoint);
        info.setToken(DPUtil.parseString(param.get("token")));
        info.setWebsite(DPUtil.parseString(param.get("website")));
        info.setContent(DPUtil.stringify(param.get("content")));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(providerDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(providerDao, param, (root, query, cb) -> {
            SpecificationHelper<Provider> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equal("type").equal("serial").like("site");
            helper.equalWithIntNotEmpty("status").like("name").like("endpoint");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort")), "id", "status", "sort", "serial");
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withTypeText"))) {
            DPUtil.fillValues(rows, "type", "typeText", types());
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            node.replace("content", DPUtil.parseJSON(row.at("/content").asText()));
        }
        return rows;
    }

    public boolean remove(List<Integer> ids) {
        removeByParentId(modelDao, "providerId", ids);
        return remove(providerDao, ids);
    }

    public JsonNode fillInfo(JsonNode rows, String ...properties) {
        return fillInfo(providerDao, rows, properties);
    }
}
