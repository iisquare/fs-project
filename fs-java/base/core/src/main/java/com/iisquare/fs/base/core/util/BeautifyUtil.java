package com.iisquare.fs.base.core.util;

import java.util.ArrayList;
import java.util.List;

public class BeautifyUtil {

    public static String duration(long timestamp) {
        List<String> result = new ArrayList<>();
        result.add(0, timestamp % 1000 + "ms");
        timestamp /= 1000;
        if (timestamp > 0) {
            result.add(0, timestamp % 60 + "s");
            timestamp /= 60;
        }
        if (timestamp > 0) {
            result.add(0, timestamp % 60 + "m");
            timestamp /= 60;
        }
        if (timestamp > 0) {
            result.add(0, timestamp % 24 + "h");
            timestamp /= 24;
        }
        if (timestamp > 0) {
            result.add(0, timestamp + "d");
        }
        return DPUtil.implode("", result);
    }

    public static String desensitizeEmail(String email) {
        email = email.trim();
        if (DPUtil.empty(email)) return email;
        int atIndex = email.indexOf("@");
        if (atIndex <= 0) {
            return "***"; // 格式不正确，返回脱敏值
        }
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        // 根据不同长度采用不同的脱敏策略
        if (username.length() <= 1) {
            return "*" + domain;
        } else if (username.length() == 2) {
            return username.charAt(0) + "*" + domain;
        } else {
            // 保留第一个字符和最后一个字符，中间用***
            return username.charAt(0) + "***" + username.charAt(username.length() - 1) + domain;
        }
    }

    public static String desensitizePhone(String phone) {
        phone = phone.trim();
        if (DPUtil.empty(phone)) return phone;
        if (phone.length() < 7) {
            // 如果数字少于7位，全部隐藏
            return "****";
        } else if (phone.length() < 8) {
            // 保留第一个字符和最后一个字符，中间用***
            return phone.charAt(0) + "***" + phone.charAt(phone.length() - 1);
        } else {
            // 保留后4位，隐藏中间4位
            return phone.substring(0, phone.length() - 8) + "****" + phone.substring(phone.length() - 4);
        }
    }

}
