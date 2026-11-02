package com.iisquare.fs.web.member.core;

public class RedisKey {

    /**
     * 鉴权缓存结构版本，结构变更后递增，旧版本缓存不再被读取，到期后自然失效
     * 版本仅体现在 key 名称中，缓存值不再写入版本号，多版本服务并行时各自使用独立命名空间，互不覆盖
     * 4：用户缓存存储全部角色标识与锁定时间，角色缓存存储角色状态，角色有效性与账号锁定由缓存读取时判定
     * 5：用户缓存增加邮箱、手机号等身份信息，identity 所需数据全部由用户与角色缓存组装
     */
    public static final int PERMIT_CACHE_VERSION = 5;

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
        return permitPrefix() + "role:" + roleId;
    }

    /**
     * 用户资源缓存，值为该用户基础信息（含邮箱、手机号等身份信息）与配置的全部角色标识，不按角色状态过滤
     */
    public static String permitUser(Integer uid) {
        return permitPrefix() + "user:" + uid;
    }

    /**
     * 当前版本角色缓存的键匹配规则，供管理端统一清理
     */
    public static String permitRolePattern() {
        return permitPrefix() + "role:*";
    }

    /**
     * 鉴权缓存命名空间，版本号位于角色、用户标识之前，便于按版本整体清理
     */
    private static String permitPrefix() {
        return "fs:member:permit:v" + PERMIT_CACHE_VERSION + ":";
    }

}
