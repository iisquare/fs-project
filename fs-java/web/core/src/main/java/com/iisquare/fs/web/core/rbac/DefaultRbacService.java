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
     * 获取当前登录用户的个人信息及所属的角色信息
     * {
     *     id: Integer,
     *     name: String,
     *     status: Integer,
     *     roles: [{
     *         id: Integer,
     *         name: String
     *     }]
     * }
     */
    public JsonNode identity() {
        return RpcUtil.data(memberRpc.post("/rbac/identity", DPUtil.buildMap()), false);
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

    /**
     * 根据数据列表中的用户标识，填充用户信息
     */
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

    /**
     * 根据数据列表中的用户标识，填充用户信息
     */
    @Override
    public JsonNode fillUserInfo(JsonNode json, String... properties) {
        return fillUserInfo("Uid", "UserInfo", json, properties);
    }

    /**
     * 根据数据列表中的用户标识，填充用户信息
     */
    @Override
    public JsonNode fillUserInfo(String fromSuffix, String toSuffix, JsonNode json, String... properties) {
        if(null == json || json.size() < 1 || properties.length < 1) return json;
        Set<Integer> ids = DPUtil.values(json, Integer.class, properties);
        if(ids.size() < 1) return json;
        JsonNode userInfos = post("listByIds", DPUtil.buildMap("ids", ids), true);
        if (null == userInfos) return null;
        return DPUtil.fillValues(json, true, properties, DPUtil.suffix(properties, fromSuffix, toSuffix), userInfos);
    }

    /**
     * 获取当前登录用户的Session信息
     */
    @Override
    public JsonNode currentInfo(HttpServletRequest request) {
        return pack(request, PermitInterceptor.ATTRIBUTE_USER);
    }

    /**
     * 获取当前登录用户拥有的资源
     */
    @Override
    public JsonNode resource(HttpServletRequest request) {
        return pack(request, PermitInterceptor.ATTRIBUTE_RESOURCE);
    }

    /**
     * 获取当前登录用户拥有的菜单
     */
    @Override
    public JsonNode menu(HttpServletRequest request) {
        return post("menu", null, false);
    }

    /**
     * 获取配置信息
     */
    @Override
    public Map<String, String> setting(String type, List<String> include, List<String> exclude) {
        JsonNode result = post("setting", DPUtil.buildMap("type", type, "include", include, "exclude", exclude), true);
        if (null == result) return null;
        return DPUtil.toJSON(result, Map.class);
    }

    /**
     * 更新配置信息
     */
    @Override
    public int setting(String type, Map<String, String> data) {
        JsonNode result = post("setting", DPUtil.buildMap("type", type, "data", data, "alter", true), true);
        if (null == result) return -1;
        return result.asInt(-1);
    }

    /**
     * 根据根节点名称，获取字典项
     */
    public JsonNode dictionary(String ancestor) {
        Map<Object, Object> params = DPUtil.buildMap("dictionary", ancestor);
        return RpcUtil.data(memberRpc.post("/dictionary/options", params), true);
    }

    /**
     * 根据userIds、roleIds填充users、roles信息
     */
    public JsonNode fillInfos (JsonNode rows) {
        if (rows.size() == 0) return rows;
        Set<Integer> userIdSet = new HashSet<>();
        Set<Integer> roleIdSet = new HashSet<>();
        for (JsonNode row : rows) {
            List<Integer> userIds = DPUtil.parseIntList(DPUtil.toJSON(row.at("/userIds"), Object.class));
            List<Integer> roleIds = DPUtil.parseIntList(DPUtil.toJSON(row.at("/roleIds"), Object.class));
            userIdSet.addAll(userIds);
            roleIdSet.addAll(roleIds);
        }
        Map<Object, Object> params = DPUtil.buildMap("userIds", userIdSet, "roleIds", roleIdSet);
        JsonNode data = RpcUtil.data(memberRpc.post("/rbac/infos", params), false);
        JsonNode userInfos = data.at("/users");
        JsonNode roleInfos = data.at("/roles");
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            if (row.has("userIds")) {
                ArrayNode users = node.has("users") ? (ArrayNode) node.at("/users") : node.putArray("users");
                List<Integer> userIds = DPUtil.parseIntList(DPUtil.toJSON(row.at("/userIds"), Object.class));
                for (Integer userId : userIds) {
                    JsonNode user = roleInfos.at("/" + userId);
                    if (user.isNull() || user.isEmpty()) continue;
                    users.add(user);
                }
            }
            if (row.has("roleIds")) {
                ArrayNode roles = node.has("roles") ? (ArrayNode) node.at("/roles") : node.putArray("roles");
                List<Integer> roleIds = DPUtil.parseIntList(DPUtil.toJSON(row.at("/roleIds"), Object.class));
                for (Integer roleId : roleIds) {
                    JsonNode role = roleInfos.at("/" + roleId);
                    if (role.isNull() || role.isEmpty()) continue;
                    roles.add(role);
                }
            }
        }
        return rows;
    }

}
