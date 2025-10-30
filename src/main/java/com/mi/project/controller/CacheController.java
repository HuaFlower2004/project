package com.mi.project.controller;

import com.mi.project.common.Result;
import com.mi.project.service.ICacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 缓存管理控制器
 * 提供缓存操作和监控功能
 * @author 31591
 */
@Slf4j
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@Tag(name = "缓存管理", description = "缓存操作和监控")
@CrossOrigin(origins = {"http://192.168.93.182:5174", "http://192.168.93.182:5173"})
public class CacheController {

    private final ICacheService cacheService;

    @GetMapping("/stats")
    @Operation(summary = "获取缓存统计信息", description = "获取Redis缓存的使用统计")
    public Result<Map<String, Object>> getCacheStats() {
        try {
            Map<String, Object> stats = cacheService.getCacheStats();
            return Result.success("获取缓存统计成功", stats);
        } catch (Exception e) {
            log.error("获取缓存统计失败", e);
            return Result.failure(500, "获取缓存统计失败: " + e.getMessage());
        }
    }

    @GetMapping("/hot-data/{dataType}")
    @Operation(summary = "获取热点数据", description = "获取指定类型的热点数据列表")
    public Result<List<String>> getHotData(@PathVariable String dataType) {
        try {
            List<String> hotData = cacheService.getHotDataKeys(dataType);
            return Result.success("获取热点数据成功", hotData);
        } catch (Exception e) {
            log.error("获取热点数据失败: dataType={}", dataType, e);
            return Result.failure(500, "获取热点数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/access-stats/{dataType}")
    @Operation(summary = "获取数据访问统计", description = "获取指定类型的数据访问统计")
    public Result<Map<String, Long>> getAccessStats(@PathVariable String dataType) {
        try {
            Map<String, Long> stats = cacheService.getDataAccessStats(dataType);
            return Result.success("获取访问统计成功", stats);
        } catch (Exception e) {
            log.error("获取访问统计失败: dataType={}", dataType, e);
            return Result.failure(500, "获取访问统计失败: " + e.getMessage());
        }
    }

    @PostMapping("/warm-up/{cacheType}")
    @Operation(summary = "预热缓存", description = "预热指定类型的缓存")
    public Result<Void> warmUpCache(@PathVariable String cacheType) {
        try {
            cacheService.warmUpCache(cacheType);
            return Result.success("缓存预热成功", null);
        } catch (Exception e) {
            log.error("缓存预热失败: cacheType={}", cacheType, e);
            return Result.failure(500, "缓存预热失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/clean")
    @Operation(summary = "清理过期缓存", description = "清理过期的缓存数据")
    public Result<Void> cleanExpiredCache() {
        try {
            cacheService.cleanExpiredCache();
            return Result.success("清理过期缓存成功", null);
        } catch (Exception e) {
            log.error("清理过期缓存失败", e);
            return Result.failure(500, "清理过期缓存失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/pattern/{pattern}")
    @Operation(summary = "按模式删除缓存", description = "根据模式删除匹配的缓存")
    public Result<Void> deleteCacheByPattern(@PathVariable String pattern) {
        try {
            cacheService.deleteCachePattern(pattern);
            return Result.success("按模式删除缓存成功", null);
        } catch (Exception e) {
            log.error("按模式删除缓存失败: pattern={}", pattern, e);
            return Result.failure(500, "按模式删除缓存失败: " + e.getMessage());
        }
    }

    @GetMapping("/exists/{key}")
    @Operation(summary = "检查缓存是否存在", description = "检查指定键的缓存是否存在")
    public Result<Boolean> hasCache(@PathVariable String key) {
        try {
            boolean exists = cacheService.hasCache(key);
            return Result.success("检查缓存存在性成功", exists);
        } catch (Exception e) {
            log.error("检查缓存存在性失败: key={}", key, e);
            return Result.failure(500, "检查缓存存在性失败: " + e.getMessage());
        }
    }

    @GetMapping("/ttl/{key}")
    @Operation(summary = "获取缓存TTL", description = "获取指定键的缓存剩余过期时间")
    public Result<Long> getCacheTtl(@PathVariable String key) {
        try {
            long ttl = cacheService.getCacheTtl(key);
            return Result.success("获取缓存TTL成功", ttl);
        } catch (Exception e) {
            log.error("获取缓存TTL失败: key={}", key, e);
            return Result.failure(500, "获取缓存TTL失败: " + e.getMessage());
        }
    }
}

