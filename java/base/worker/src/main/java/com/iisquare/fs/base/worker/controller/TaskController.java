package com.iisquare.fs.base.worker.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.HttpUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.base.worker.core.Task;
import com.iisquare.fs.base.worker.service.ContainerService;
import com.iisquare.fs.base.worker.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/task")
public class TaskController extends PermitControllerBase {

    @Autowired
    private TaskService taskService;
    @Autowired
    private ContainerService containerService;

    @GetMapping("/transient")
    public String transientAction(@RequestParam Map<?, ?> param) {
        int maxConsumer = DPUtil.parseInt(param.get("maxConsumer"));
        taskService.setMaxConsumer(maxConsumer);
        return ApiUtil.echoResult(0, null, taskService.node());
    }

    @GetMapping("/node")
    public String nodeAction(@RequestParam Map<String, String> param) {
        boolean withQueueKey = !DPUtil.empty(param.get("withQueueKey"));
        ObjectNode data = (ObjectNode) taskService.node();
        data.replace("containers", containerService.state(withQueueKey));
        return ApiUtil.echoResult(0, null, data);
    }

    @GetMapping("/nodes")
    public String nodesAction(@RequestParam Map<String, String> param) {
        ObjectNode data = DPUtil.objectNode();
        ObjectNode nodes = data.putObject("nodes");
        for (String node : taskService.participants()) {
            String url = "http://" + node;
            String content = HttpUtil.get(url + "/task/node", param);
            if (null == content) return ApiUtil.echoResult(5001, "载入节点信息失败", url);
            JsonNode json = DPUtil.parseJSON(content);
            if (null != json) json = json.get("data");
            if (null != json && !json.isNull()) nodes.replace(node, json);
        }
        if (!DPUtil.empty("withTask")) {
            data.putPOJO("tasks", taskService.tasks());
        }
        return ApiUtil.echoResult(0, null, data);
    }

    @RequestMapping("/submit")
    public String submitAction(@RequestBody Map<?, ?> param) {
        String queueName = DPUtil.trim(DPUtil.parseString(param.get("queueName")));
        if (DPUtil.empty(queueName)) return ApiUtil.echoResult(1001, "队列名称不能为空", queueName);
        String handlerName = DPUtil.trim(DPUtil.parseString(param.get("handlerName")));
        if (DPUtil.empty(handlerName)) return ApiUtil.echoResult(1004, "消费实例不能为空", handlerName);
        int prefetchCount = ValidateUtil.filterInteger(param.get("prefetchCount"), true, 0, 500, 1);
        if (prefetchCount < 0) return ApiUtil.echoResult(1002, "预先载入数量异常", prefetchCount);
        int consumerCount = ValidateUtil.filterInteger(param.get("consumerCount"), true, 0, null, 1);
        if (consumerCount < 0) return ApiUtil.echoResult(1003, "消费者数量异常", consumerCount);
        Task task = Task.create(queueName, handlerName, prefetchCount, consumerCount);
        task = taskService.save(task, true, true);
        return ApiUtil.echoResult(null == task ? 500: 0, null, task);
    }

    @RequestMapping("/list")
    public String listAction(@RequestBody Map<?, ?> param) {
        ArrayNode data = DPUtil.arrayNode();
        Map<String, Task> tasks = taskService.tasks();
        for (Map.Entry<String, Task> entry : tasks.entrySet()) {
            Task task = entry.getValue();
            data.addPOJO(task);
        }
        return ApiUtil.echoResult(0, null, data);
    }

    @RequestMapping("/remove")
    public String removeAction(@RequestBody Map<?, ?> param) {
        String queueName = DPUtil.trim(DPUtil.parseString(param.get("queueName")));
        if (DPUtil.empty(queueName)) return ApiUtil.echoResult(1001, "队列名称不能为空", queueName);
        boolean result = taskService.remove("task", queueName);
        return ApiUtil.echoResult(result ? 0: 500, null, queueName);
    }

    @RequestMapping("/start")
    public String startAction(@RequestBody Map<?, ?> param) {
        String queueName = DPUtil.trim(DPUtil.parseString(param.get("queueName")));
        if (DPUtil.empty(queueName)) return ApiUtil.echoResult(1001, "队列名称不能为空", queueName);
        Map<String, Object> result = taskService.start(queueName);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/stop")
    public String stopAction(@RequestBody Map<?, ?> param) {
        String queueName = DPUtil.trim(DPUtil.parseString(param.get("queueName")));
        if (DPUtil.empty(queueName)) return ApiUtil.echoResult(1001, "队列名称不能为空", queueName);
        Map<String, Object> result = taskService.stop(queueName);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/rebalance")
    public String rebalanceAction(@RequestBody Map<String, String> param) {
        Map<String, Object> result = taskService.rebalance(param.get("queueName"));
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/startAll")
    public String startAllAction(@RequestBody Map<?, ?> param) {
        Map<String, Object> result = taskService.startAll();
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/stopAll")
    public String stopAllAction(@RequestBody Map<?, ?> param) {
        Map<String, Object> result = taskService.stopAll();
        return ApiUtil.echoResult(result);
    }

}
