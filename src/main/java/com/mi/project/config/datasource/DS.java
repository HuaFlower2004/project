
// 文件路径: src/main/java/com/mi/project/config/datasource/DS.java
package com.mi.project.config.datasource;

import java.lang.annotation.*;

/**
 * 数据源切换注解
 * 可以用在类或方法上,指定使用哪个数据源
 * @author 31591
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DS {
    /**
     * 数据源名称
     * master: 主库(写)
     * slave: 从库(读)
     */
    String value() default "master";
}
