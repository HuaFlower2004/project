// 文件路径: src/main/java/com/mi/project/config/datasource/ReadOnly.java
package com.mi.project.config.datasource;

import java.lang.annotation.*;

/**
 * 只读操作注解
 * 标注此注解的方法会自动使用从库(slave)
 * 用于Service层的查询方法
 * @author 31591
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ReadOnly {
}
