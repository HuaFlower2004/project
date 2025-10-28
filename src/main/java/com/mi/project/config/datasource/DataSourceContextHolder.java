package com.mi.project.config.datasource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 数据源上下文持有者
 * 使用ThreadLocal保证线程安全
 * @author 31591
 */
public class DataSourceContextHolder {

    private static final Logger log = LoggerFactory.getLogger(DataSourceContextHolder.class);

    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();
    // 记录本次请求中最近一次实际使用的数据源，便于在控制层打印/返回
    private static final ThreadLocal<String> LAST_USED_HOLDER = new ThreadLocal<>();

    /**
     * 设置数据源
     */
    public static void setDataSource(String dataSource) {
        log.debug("切换到数据源: {}", dataSource);
        CONTEXT_HOLDER.set(dataSource);
        LAST_USED_HOLDER.set(dataSource);
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

    /**
     * 获取最近一次实际使用的数据源（在AOP清空当前数据源后仍可读取）
     */
    public static String getLastUsedDataSource() {
        return LAST_USED_HOLDER.get();
    }

    /**
     * 清除最近一次使用的数据源（建议在请求结束时调用）
     */
    public static void clearLastUsed() {
        LAST_USED_HOLDER.remove();
    }
}
