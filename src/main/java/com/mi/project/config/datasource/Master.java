// 文件路径: src/main/java/com/mi/project/config/datasource/Master.java
package com.mi.project.config.datasource;

import java.lang.annotation.*;

/**
 * 主库操作注解
 * 标注此注解的方法会强制使用主库(master)
 * 用于Service层的写操作方法
 * @author 31591
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Master {
}