package com.iisquare.fs.web.core.rbac;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 允许通过 API Key 请求头访问的接口。
 *
 * 拦截器只负责放行存在 API Key 的请求，密钥是否有效、映射到哪个生效用户以及业务权限，
 * 由接口实现自行校验。
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ApiKeyAuth {

    String header() default "x-api-key";

}
