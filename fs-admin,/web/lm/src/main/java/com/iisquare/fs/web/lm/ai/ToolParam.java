package com.iisquare.fs.web.lm.ai;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target({ ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ToolParam {

    @AliasFor("name")
    String value() default "";

    /**
     * 字段名称，默认为Parameter.getName()
     */
    @AliasFor("value")
    String name() default "";

    /**
     * Whether the tool argument is required.
     */
    boolean required() default true;

    /**
     * The description of the tool argument.
     */
    String description() default "";

}
