package com.iisquare.fs.web.admin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.admin.dao.UserDao;
import com.iisquare.fs.web.admin.entity.User;
import com.iisquare.fs.web.admin.mvc.Permission;
import com.iisquare.fs.web.admin.mvc.PermitController;
import com.iisquare.fs.web.admin.rpc.CronRpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("manage/cron")
public class CronController extends PermitController {

    @Autowired
    private CronRpc cronRpc;
    @Autowired
    private UserDao userDao;

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> params) {
        return cronRpc.list(params);
    }

    @RequestMapping("/map")
    @Permission("")
    public String mapAction(@RequestBody Map<String, Object> params) {
        return cronRpc.map(params);
    }

    @RequestMapping("/add")
    @Permission
    public String addAction(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        params.put("uid", uid(request));
        return cronRpc.add(params);
    }

    @RequestMapping("/modify")
    @Permission
    public String modifyAction(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        params.put("uid", uid(request));
        return cronRpc.modify(params);
    }

    @RequestMapping("/pause")
    @Permission
    public String pauseAction(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        params.put("uid", uid(request));
        return cronRpc.pause(params);
    }

    @RequestMapping("/resume")
    @Permission
    public String resumeAction(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        params.put("uid", uid(request));
        return cronRpc.resume(params);
    }

    @RequestMapping("/run")
    @Permission
    public String runAction(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        params.put("uid", uid(request));
        return cronRpc.run(params);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        params.put("uid", uid(request));
        return cronRpc.delete(params);
    }

    @RequestMapping("/logs")
    @Permission("")
    public String logsAction(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("page", 1);
        result.put("pageSize", 15);
        result.put("total", 0);
        List<ObjectNode> rows = new ArrayList<>();
        JsonNode ret = DPUtil.parseJSON(cronRpc.logs(params));
        if (ret != null && ret.has("data")) {
            JsonNode data = ret.get("data");
            String[] fields = {"page", "pageSize", "total"};
            for (String field : fields) {
                if (data.has(field)) {
                    result.put(field, data.get(field).asInt());
                }
            }
            Object logs = data.get("rows");
            Set<Integer> cuids = new HashSet<>();
            //填充操作人信息
            if (logs instanceof ArrayNode) {
                Iterator<JsonNode> iterator = ((ArrayNode) logs).elements();
                while (iterator.hasNext()) {
                    JsonNode item = iterator.next();
                    if (item.has("cuid")) {
                        cuids.add(item.get("cuid").asInt());
                    }
                    rows.add(item.deepCopy());
                }
                if (!cuids.isEmpty()) {
                    List<User> users = userDao.findAllById(cuids);
                    Map<Integer, User> userMap = new HashMap<>();
                    for (User user : users) {
                        userMap.put(user.getId(), user);
                    }
                    for (ObjectNode row : rows) {
                        User user = userMap.get(row.get("cuid").asInt());
                        if (user != null) {
                            row.put("cname", user.getName());
                        }
                    }
                }
            }
        }
        result.put("rows", rows);
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/log")
    @Permission("")
    public String logAction(@RequestBody Map<String, Object> params) {
        return cronRpc.log(params);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        return cronRpc.config(model);
    }

    @RequestMapping("/serviceinfo")
    @Permission("")
    public String serviceInfoAction() {
        return cronRpc.serviceInfo();
    }

    @RequestMapping("/start")
    @Permission
    public String startAction(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        params.put("uid", uid(request));
        return cronRpc.start(params);
    }

    @RequestMapping("/standby")
    @Permission
    public String standbyAction(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        params.put("uid", uid(request));
        return cronRpc.standby(params);
    }

    @RequestMapping("/isstandby")
    @Permission("")
    public String isStandbyAction(@RequestBody Map<String, Object> params) {
        return cronRpc.isStandby(params);
    }

    @RequestMapping("/isvalidcron")
    @Permission("")
    public String isValidCronAction(@RequestBody Map<String, Object> params) {
        return cronRpc.isValidCron(params);
    }
}
