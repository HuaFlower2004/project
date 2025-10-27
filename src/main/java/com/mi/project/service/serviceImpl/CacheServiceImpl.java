package com.mi.project.service.serviceImpl;

import com.mi.project.service.ICacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 缓存服务实现类
 * 实现Redis缓存操作和热点数据识别
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements ICacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${cache.hot-data-threshold:10}")
    private int hotDataThreshold;

    @Value("${cache.hot-data-window:60}")
    private int hotDataWindow;

    // 缓存键前缀
    private static final String CACHE_PREFIX = "cache:";
    private static final String HOT_DATA_PREFIX = "hot:";
    private static final String ACCESS_COUNT_PREFIX = "access:";
    private static final String STATS_PREFIX = "stats:";

    @Override
    public void setCache(String key, Object value, long ttlSeconds) {
        try {
            String cacheKey = CACHE_PREFIX + key;
            redisTemplate.opsForValue().set(cacheKey, value, ttlSeconds, TimeUnit.SECONDS);
            log.debug("设置缓存成功: key={}, ttl={}s", key, ttlSeconds);
        } catch (Exception e) {
            log.error("设置缓存失败: key={}", key, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCache(String key, Class<T> clazz) {
        try {
            String cacheKey = CACHE_PREFIX + key;
            Object value = redisTemplate.opsForValue().get(cacheKey);
            if (value != null) {
                log.debug("获取缓存成功: key={}", key);
                return (T) value;
            }
        } catch (Exception e) {
            log.error("获取缓存失败: key={}", key, e);
        }
        return null;
    }

    @Override
    public void deleteCache(String key) {
        try {
            String cacheKey = CACHE_PREFIX + key;
            redisTemplate.delete(cacheKey);
            log.debug("删除缓存成功: key={}", key);
        } catch (Exception e) {
            log.error("删除缓存失败: key={}", key, e);
        }
    }

    @Override
    public void deleteCachePattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(CACHE_PREFIX + pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("批量删除缓存成功: pattern={}, count={}", pattern, keys.size());
            }
        } catch (Exception e) {
            log.error("批量删除缓存失败: pattern={}", pattern, e);
        }
    }

    @Override
    public boolean hasCache(String key) {
        try {
            String cacheKey = CACHE_PREFIX + key;
            return Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey));
        } catch (Exception e) {
            log.error("检查缓存存在性失败: key={}", key, e);
            return false;
        }
    }

    @Override
    public long getCacheTtl(String key) {
        try {
            String cacheKey = CACHE_PREFIX + key;
            return redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("获取缓存TTL失败: key={}", key, e);
            return -1;
        }
    }

    @Override
    public void recordDataAccess(String dataKey, String dataType) {
        try {
            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
            String accessKey = ACCESS_COUNT_PREFIX + dataType + ":" + currentTime;
            String hotDataKey = HOT_DATA_PREFIX + dataType;

            // 记录访问次数
            redisTemplate.opsForValue().increment(accessKey + ":" + dataKey);
            redisTemplate.expire(accessKey + ":" + dataKey, hotDataWindow, TimeUnit.SECONDS);

            // 更新热点数据排行
            Double currentScore = redisTemplate.opsForZSet().score(hotDataKey, dataKey);
            double newScore = (currentScore != null ? currentScore : 0) + 1;
            redisTemplate.opsForZSet().add(hotDataKey, dataKey, newScore);
            redisTemplate.expire(hotDataKey, hotDataWindow * 2, TimeUnit.SECONDS);

            log.debug("记录数据访问: dataKey={}, dataType={}", dataKey, dataType);
        } catch (Exception e) {
            log.error("记录数据访问失败: dataKey={}, dataType={}", dataKey, dataType, e);
        }
    }

    @Override
    public List<String> getHotDataKeys(String dataType) {
        try {
            String hotDataKey = HOT_DATA_PREFIX + dataType;
            Set<ZSetOperations.TypedTuple<Object>> hotData = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(hotDataKey, 0, 9); // 获取前10个热点数据

            List<String> result = new ArrayList<>();
            if (hotData != null) {
                for (ZSetOperations.TypedTuple<Object> tuple : hotData) {
                    if (tuple.getScore() != null && tuple.getScore() >= hotDataThreshold) {
                        result.add(tuple.getValue().toString());
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.error("获取热点数据失败: dataType={}", dataType, e);
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Long> getDataAccessStats(String dataType) {
        try {
            String hotDataKey = HOT_DATA_PREFIX + dataType;
            Set<ZSetOperations.TypedTuple<Object>> allData = redisTemplate.opsForZSet()
                    .rangeWithScores(hotDataKey, 0, -1);

            Map<String, Long> stats = new HashMap<>();
            if (allData != null) {
                for (ZSetOperations.TypedTuple<Object> tuple : allData) {
                    if (tuple.getScore() != null) {
                        stats.put(tuple.getValue().toString(), tuple.getScore().longValue());
                    }
                }
            }
            return stats;
        } catch (Exception e) {
            log.error("获取数据访问统计失败: dataType={}", dataType, e);
            return new HashMap<>();
        }
    }

    @Override
    public void warmUpCache(String cacheType) {
        try {
            log.info("开始预热缓存: cacheType={}", cacheType);
            
            // 根据缓存类型进行不同的预热策略
            switch (cacheType.toLowerCase()) {
                case "user":
                    warmUpUserCache();
                    break;
                case "file":
                    warmUpFileCache();
                    break;
                case "statistics":
                    warmUpStatisticsCache();
                    break;
                default:
                    log.warn("未知的缓存类型: {}", cacheType);
            }
            
            log.info("缓存预热完成: cacheType={}", cacheType);
        } catch (Exception e) {
            log.error("缓存预热失败: cacheType={}", cacheType, e);
        }
    }

    private void warmUpUserCache() {
        // 预热用户相关缓存
        // 这里可以预加载一些常用的用户数据
        log.debug("预热用户缓存");
    }

    private void warmUpFileCache() {
        // 预热文件相关缓存
        log.debug("预热文件缓存");
    }

    private void warmUpStatisticsCache() {
        // 预热统计相关缓存
        log.debug("预热统计缓存");
    }

    @Override
    public void cleanExpiredCache() {
        try {
            // 清理过期的访问记录
            String pattern = ACCESS_COUNT_PREFIX + "*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null) {
                for (String key : keys) {
                    if (redisTemplate.getExpire(key) <= 0) {
                        redisTemplate.delete(key);
                    }
                }
            }
            log.debug("清理过期缓存完成");
        } catch (Exception e) {
            log.error("清理过期缓存失败", e);
        }
    }

    @Override
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            // 获取Redis信息
            Properties info = redisTemplate.getConnectionFactory().getConnection().info();
            stats.put("redis_version", info.getProperty("redis_version"));
            stats.put("used_memory", info.getProperty("used_memory_human"));
            stats.put("connected_clients", info.getProperty("connected_clients"));
            stats.put("total_commands_processed", info.getProperty("total_commands_processed"));
            
            // 获取缓存统计
            stats.put("cache_keys_count", redisTemplate.keys(CACHE_PREFIX + "*").size());
            stats.put("hot_data_count", redisTemplate.keys(HOT_DATA_PREFIX + "*").size());
            stats.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            log.error("获取缓存统计失败", e);
            stats.put("error", e.getMessage());
        }
        return stats;
    }
}


