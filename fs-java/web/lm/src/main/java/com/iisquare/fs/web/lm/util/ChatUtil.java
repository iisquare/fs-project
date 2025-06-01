package com.iisquare.fs.web.lm.util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.sse.SsePlainEmitter;

public class ChatUtil {

    public static ObjectNode parse(String line) {
        ObjectNode message = DPUtil.objectNode();
        if (DPUtil.empty(line)) return message;
        if (line.startsWith(SsePlainEmitter.EVENT_DATA_PREFIX)) {
            line = line.substring(SsePlainEmitter.EVENT_DATA_PREFIX.length());
        }
        if (isJSON(line)) {
            return (ObjectNode) DPUtil.parseJSON(line, j -> message);
        }
        return message;
    }

    public static boolean isJSON(String line) {
        int length = line.length();
        for (int i = 0; i < length; i++) {
            if (line.charAt(i) == ' ') continue;
            return line.charAt(i) == '{';
        }
        return false;
    }

    public static String message(String action, Object data) {
        ObjectNode message = DPUtil.objectNode();
        message.put("action", action);
        message.replace("data", DPUtil.toJSON(data));
        return message.toString();
    }

    public static SsePlainEmitter message(SsePlainEmitter emitter, String action, Object data) {
        return emitter.data(message(action, data));
    }

}
