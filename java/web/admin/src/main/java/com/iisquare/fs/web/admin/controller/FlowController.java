package com.iisquare.fs.web.admin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.admin.mvc.AdminControllerBase;
import com.iisquare.fs.web.core.rpc.FlinkRpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/flow")
public class FlowController extends AdminControllerBase {

    @Autowired
    private FlinkRpc flinkRpc;

    @RequestMapping("/draw")
    public String drawAction(@RequestParam Map<String, Object> param, ModelMap model, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        JsonNode info = RpcUtil.data(flinkRpc.post("/flow/info", DPUtil.buildMap("id", id)), true);
        model.put("info", DPUtil.convertJSON(info, Map.class));
        return displayTemplate(model, request);
    }

    @RequestMapping("/property")
    public String propertyAction(@RequestParam Map<String, Object> param, ModelMap model, HttpServletRequest request) {
        return displayTemplate(model, request);
    }

    @RequestMapping("/field")
    public String fieldAction(@RequestParam Map<String, Object> param, ModelMap model, HttpServletRequest request) {
        return displayTemplate(model, request);
    }

    @GetMapping("/classname")
    @ResponseBody
    public String classnameAction() {
        return DPUtil.stringify(RpcUtil.data(flinkRpc.post("/tool/classname", new HashMap()), false));
    }

    private ArrayNode tree(ArrayNode data) {
        int size = data.size();
        for (int index = 0; index < size; index++) {
            JsonNode item = data.get(index);
            ObjectNode node = DPUtil.objectNode();
            node.replace("id", item.get("id"));
            node.replace("text", item.get("name"));
            node.replace("iconCls", item.get("icon"));
            node.replace("state", item.get("state"));
            node.replace("type", item.get("type"));
            node.replace("plugin", item.get("plugin"));
            node.replace("classname", item.get("classname"));
            node.put("draggable", DPUtil.parseBoolean(item.get("draggable").asInt()));
            node.replace("description", item.get("description"));
            node.replace("property", DPUtil.parseJSON(item.get("property").asText()));
            node.replace("returns", DPUtil.parseJSON(item.get("returns").asText()));
            JsonNode children = item.get("children");
            if (!children.isNull()) node.replace("children", tree((ArrayNode) children));
            data.set(index, node);
        }
        return data;
    }

    @RequestMapping("/node")
    @ResponseBody
    public String nodeAction() {
        JsonNode node = RpcUtil.data(flinkRpc.post("/flowNode/tree", DPUtil.buildMap("status", 1)), true);
        if (null == node) return "[]";
        return DPUtil.stringify(tree((ArrayNode) node));
    }

}
