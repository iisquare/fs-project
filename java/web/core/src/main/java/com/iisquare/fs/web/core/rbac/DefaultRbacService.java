package com.iisquare.fs.web.core.rbac;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ReflectUtil;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.core.rpc.MemberRpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class DefaultRbacService extends RbacServiceBase {

    @Autowired
    private MemberRpc memberRpc;

    private JsonNode post(String uri, Map param, boolean nullable) {
        if (null == param) param = new HashMap();
        return RpcUtil.data(memberRpc.post("/rbac/" + uri, param), nullable);
    }

    /**
     * 批量获取鉴权信息，防止网络抖动导致单次请求的多次RPC调用结果不一致
     */
    public JsonNode pack(HttpServletRequest request, String key) {
        if (null != key) {
            JsonNode result = (JsonNode) request.getAttribute(key);
            if (null != result) return result;
        }
        JsonNode pack = post("pack", DPUtil.buildMap(
            PermitInterceptor.ATTRIBUTE_USER, null, PermitInterceptor.ATTRIBUTE_RESOURCE, null
        ), false);
        Iterator<Map.Entry<String, JsonNode>> iterator = pack.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            request.setAttribute(entry.getKey(), entry.getValue());
        }
        return null == key ? pack : pack.at("/" + key);
    }

    @Override
    public <T> List<T> fillUserInfo(List<T> list, String... properties) {
        if(null == list || list.size() < 1 || properties.length < 1) return list;
        Set<Integer> ids = new HashSet<>();
        for (Object item : list) {
            for (String property : properties) {
                int id = DPUtil.parseInt(ReflectUtil.getPropertyValue(item, property));
                if(id < 1) continue;
                ids.add(DPUtil.parseInt(id));
            }
        }
        if(ids.size() < 1) return list;
        JsonNode userInfos = post("listByIds", DPUtil.buildMap("ids", ids), true);
        if (null == userInfos) return null;
        if(userInfos.size() < 1) return list;
        for (Object item : list) {
            for (String property : properties) {
                JsonNode user = userInfos.get(DPUtil.parseString(ReflectUtil.getPropertyValue(item, property)));
                if(null == user) continue;
                ReflectUtil.setPropertyValue(item, property + "Name", new Class[]{String.class}, new Object[]{user.get("name").asText()});
            }
        }
        return list;
    }

    @Override
    public ArrayNode fillUserInfo(ArrayNode array, String... properties) {
        if(null == array || array.size() < 1 || properties.length < 1) return array;
        Set<Integer> ids = DPUtil.values(array, Integer.class, properties);
        if(ids.size() < 1) return array;
        JsonNode userInfos = post("listByIds", DPUtil.buildMap("ids", ids), true);
        if (null == userInfos) return null;
        if(userInfos.size() < 1) return array;
        Iterator<JsonNode> iterator = array.iterator();
        while (iterator.hasNext()) {
            ObjectNode node = (ObjectNode) iterator.next();
            for (String property : properties) {
                JsonNode user = userInfos.get(node.at("/" + property).asText(""));
                if(null == user) continue;
                node.put(property + "Name", user.get("name").asText());
            }
        }
        return array;
    }

    @Override
    public JsonNode currentInfo(HttpServletRequest request) {
        return pack(request, PermitInterceptor.ATTRIBUTE_USER);
    }

    @Override
    public JsonNode resource(HttpServletRequest request) {
        return pack(request, PermitInterceptor.ATTRIBUTE_RESOURCE);
    }

    @Override
    public JsonNode menu(HttpServletRequest request, Integer parentId) {
        return post("menu", DPUtil.buildMap("parentId", parentId), false);
    }

    @Override
    public Map<String, String> setting(String type, List<String> include, List<String> exclude) {
        JsonNode result = post("setting", DPUtil.buildMap("type", type, "include", include, "exclude", exclude), true);
        if (null == result) return null;
        return DPUtil.toJSON(result, Map.class);
    }

    @Override
    public int setting(String type, Map<String, String> data) {
        JsonNode result = post("setting", DPUtil.buildMap("type", type, "data", data, "alter", true), true);
        if (null == result) return -1;
        return result.asInt(-1);
    }

}
