// 文件路径: src/main/java/com/mi/project/config/datasource/DataSourceContextHolder.java
package com.mi.project.config.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据源上下文持有者
 * 使用ThreadLocal保证线程安全
 */
public class DataSourceContextHolder {

    private static final Logger log = LoggerFactory.getLogger(DataSourceContextHolder.class);

    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置数据源
     */
    public static void setDataSource(String dataSource) {
        log.debug("切换到数据源: {}", dataSource);
        CONTEXT_HOLDER.set(dataSource);
    }

    /**
     * 获取数据源
     */
    public static String getDataSource() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 清除数据源
     */
    public static void clearDataSource() {
        CONTEXT_HOLDER.remove();
    }
}