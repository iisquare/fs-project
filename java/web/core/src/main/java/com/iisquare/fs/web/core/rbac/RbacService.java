package com.iisquare.fs.web.core.rbac;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.core.rpc.MemberRpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Service
public class RbacService extends RbacServiceBase {

    @Autowired
    private MemberRpc memberRpc;

    private JsonNode post(String uri, Map param) {
        return RpcUtil.data(memberRpc.post("/rbac/" + uri, null));
    }

    @Override
    public JsonNode currentInfo(HttpServletRequest request) {
        return post("currentInfo", null);
    }

    @Override
    public boolean hasPermit(HttpServletRequest request, String module, String controller, String action) {
        return hasPermit(request, DPUtil.buildMap(keyPermit(module, controller, action), false));
    }

    @Override
    public boolean hasPermit(HttpServletRequest request, Map name2boolean) {
        return post("hasPermit", name2boolean).asBoolean();
    }

    @Override
    public JsonNode resource(HttpServletRequest request, Map<String, Boolean> name2boolean) {
        return post("resource", name2boolean);
    }

    @Override
    public JsonNode menu(HttpServletRequest request, Integer parentId) {
        return post("hasPermit", DPUtil.buildMap("parentId", parentId));
    }

}
