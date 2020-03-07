package com.iisquare.fs.web.core.rbac;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.core.rpc.MemberRpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
public class DefaultRbacService extends RbacServiceBase {

    @Autowired
    private MemberRpc memberRpc;

    private JsonNode post(String uri, Map param) {
        if (null == param) param = new HashMap();
        return RpcUtil.data(memberRpc.post("/rbac/" + uri, param));
    }

    /**
     * 批量获取鉴权信息，防止网络抖动导致单次访问的多次RPC调用结果不一致
     */
    public JsonNode pack(HttpServletRequest request, String key) {
        if (null != key) {
            JsonNode result = (JsonNode) request.getAttribute(key);
            if (null != result) return result;
        }
        JsonNode pack = post("pack", DPUtil.buildMap(
            PermitInterceptor.ATTRIBUTE_USER, null, PermitInterceptor.ATTRIBUTE_RESOURCE, null
        ));
        Iterator<Map.Entry<String, JsonNode>> iterator = pack.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            request.setAttribute(entry.getKey(), entry.getValue());
        }
        return null == key ? pack : pack.at(key);
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
        return post("menu", DPUtil.buildMap("parentId", parentId));
    }

}
