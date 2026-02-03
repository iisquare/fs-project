package com.iisquare.fs.web.member.core;

public class RedisKey {

    public static String captcha(String uuid) {
        return "fs:member:captcha:" + uuid;
    }

    public static String login(Integer uid) {
        return "fs:member:login:" + uid;
    }

    public static String signup(String email) {
        return "fs:member:signup:" + email;
    }

    public static String forgot(String email) {
        return "fs:member:forgot:" + email;
    }

}
