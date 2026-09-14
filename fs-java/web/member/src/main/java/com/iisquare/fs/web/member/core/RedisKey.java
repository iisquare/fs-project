package com.iisquare.fs.web.member.core;

public class RedisKey {

    public static String captcha(String uuid) {
        return "fs:member:captcha:" + uuid;
    }

    /**
     * 登录失败计数，按账号标识计数，账号不存在时同样累计，避免账号枚举
     */
    public static String login(String serial) {
        return "fs:member:login:" + serial;
    }

    /**
     * 修改密码时原密码失败计数，按用户标识计数，防止暴力猜解
     */
    public static String password(Integer uid) {
        return "fs:member:password:" + uid;
    }

    public static String signup(String email) {
        return "fs:member:signup:" + email;
    }

    public static String forgot(String email) {
        return "fs:member:forgot:" + email;
    }

    /**
     * 角色资源缓存，值为角色状态等基础信息，以及该角色在自身已授权应用范围内解析后的鉴权标识
     * 角色不存在或未启用时仅包含基础信息，不包含授权数据
     */
    public static String permitRole(Integer roleId) {
        return "fs:member:permit:role:" + roleId;
    }

    /**
     * 用户资源缓存，值为该用户基础信息（含邮箱、手机号等身份信息）与配置的全部角色标识，不按角色状态过滤
     */
    public static String permitUser(Integer uid) {
        return "fs:member:permit:user:" + uid;
    }

}
