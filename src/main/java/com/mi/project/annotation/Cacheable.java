package com.mi.project.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 自定义缓存注解
 * 支持缓存操作和热点数据识别
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cacheable {

    /**
     * 缓存键名
     */
    String key() default "";

    /**
     * 缓存过期时间
     */
    long ttl() default 3600;

    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 是否启用热点数据识别
     */
    boolean enableHotDataTracking() default true;

    /**
     * 数据类型（用于热点数据分类）
     */
    String dataType() default "default";

    /**
     * 缓存条件（SpEL表达式）
     */
    String condition() default "";

    /**
     * 缓存更新条件（SpEL表达式）
     */
    String unless() default "";
}

