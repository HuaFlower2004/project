package com.mi.project.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 缓存服务接口
 * 提供缓存操作、热点数据识别等功能
 */
public interface ICacheService {

    /**
     * 设置缓存
     */
    void setCache(String key, Object value, long ttlSeconds);

    /**
     * 获取缓存
     */
    <T> T getCache(String key, Class<T> clazz);

    /**
     * 删除缓存
     */
    void deleteCache(String key);

    /**
     * 批量删除缓存
     */
    void deleteCachePattern(String pattern);

    /**
     * 检查缓存是否存在
     */
    boolean hasCache(String key);

    /**
     * 获取缓存剩余过期时间
     */
    long getCacheTtl(String key);

    /**
     * 记录数据访问，用于热点数据识别
     */
    void recordDataAccess(String dataKey, String dataType);

    /**
     * 获取热点数据列表
     */
    List<String> getHotDataKeys(String dataType);

    /**
     * 获取数据访问统计
     */
    Map<String, Long> getDataAccessStats(String dataType);

    /**
     * 预热缓存
     */
    void warmUpCache(String cacheType);

    /**
     * 清理过期缓存
     */
    void cleanExpiredCache();

    /**
     * 获取缓存统计信息
     */
    Map<String, Object> getCacheStats();
}

