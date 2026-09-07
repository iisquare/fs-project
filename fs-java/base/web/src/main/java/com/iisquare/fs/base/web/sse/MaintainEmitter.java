package com.iisquare.fs.base.web.sse;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 维护任务 SSE 输出器
 *
 * 统一维护任务（如目录重载）的事件协议，将任务步骤、待处理数量、执行进度、过程日志以 SSE 方式输出。
 * 每个事件 data 行的内容为 JSON 字符串：
 * {
 *   "action": "plan | start | step | progress | log | result | error",
 *   "code": 0,
 *   "message": "过程描述",
 *   "step": "步骤标识",
 *   "progress": 0-100,
 *   "total": 0,
 *   "level": "info | success | warning | error",  // 仅 log 事件
 *   "data": {}                                     // plan/result/error 事件
 * }
 */
public class MaintainEmitter extends SsePlainEmitter {

    public MaintainEmitter(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    public MaintainEmitter(HttpServletRequest request, HttpServletResponse response, Long timeout) {
        super(request, response, timeout);
    }

    public MaintainEmitter plan(Object data) {
        return send("plan", 0, null, null, 0, 0, null, data);
    }

    public MaintainEmitter start(String message, String step) {
        return send("start", 0, message, step, 0, 0, null, null);
    }

    public MaintainEmitter step(String message, String step, int progress) {
        return step(message, step, progress, 0);
    }

    public MaintainEmitter step(String message, String step, int progress, int total) {
        return send("step", 0, message, step, progress, total, null, null);
    }

    public MaintainEmitter progress(int progress, String message, String step) {
        return progress(progress, message, step, 0);
    }

    public MaintainEmitter progress(int progress, String message, String step, int total) {
        return send("progress", 0, message, step, progress, total, null, null);
    }

    public MaintainEmitter log(String message, String step, int progress) {
        return log(message, step, progress, null);
    }

    public MaintainEmitter log(String message, String step, int progress, String level) {
        return send("log", 0, message, step, progress, 0, level, null);
    }

    public MaintainEmitter log(String message, String step, int progress, String level, int current, int total) {
        return send("log", 0, message, step, progress, total, level, DPUtil.objectNode().put("current", current));
    }

    public MaintainEmitter result(int code, String message, Object data) {
        return send("result", code, message, "reload", 100, 0, null, data);
    }

    public MaintainEmitter error(int code, String message, Object data) {
        return send("error", code, message, "reload", 100, 0, null, data);
    }

    protected MaintainEmitter send(String action, int code, String message, String step, int progress, int total, String level, Object data) {
        if (!isRunning()) return this;
        ObjectNode event = DPUtil.objectNode();
        event.put("action", action);
        event.put("code", code);
        event.put("message", DPUtil.parseString(message));
        event.put("step", DPUtil.parseString(step));
        event.put("progress", progress);
        event.put("total", total);
        if (null != level) {
            event.put("level", level);
        }
        if (null != data) {
            event.replace("data", DPUtil.toJSON(data));
        }
        data(event.toString());
        return this;
    }
}
