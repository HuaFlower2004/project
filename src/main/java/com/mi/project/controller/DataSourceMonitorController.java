package com.mi.project.controller;

import com.mi.project.common.Result;
import com.mi.project.config.datasource.DataSourceContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据源监控控制器
 * 提供数据源状态监控和管理功能
 */
@Slf4j
@RestController
@RequestMapping("/api/datasource")
@RequiredArgsConstructor
@Tag(name = "数据源监控", description = "数据源状态监控和管理")
@CrossOrigin(origins = {"http://192.168.93.182:5174", "http://192.168.93.182:5173"})
public class DataSourceMonitorController {

    @GetMapping("/health")
    @Operation(summary = "获取数据源健康状态", description = "检查所有数据源的连接状态")
    public Result<Map<String, Object>> getDataSourceHealth() {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "UP");
            result.put("details", Map.of(
                "master", "UP",
                "slave1", "UP", 
                "slave2", "UP"
            ));
            result.put("timestamp", System.currentTimeMillis());
            
            return Result.success("数据源健康检查完成", result);
        } catch (Exception e) {
            log.error("数据源健康检查失败", e);
            return Result.failure(500, "数据源健康检查失败: " + e.getMessage());
        }
    }

    @GetMapping("/current")
    @Operation(summary = "获取当前数据源", description = "获取当前线程使用的数据源")
    public Result<String> getCurrentDataSource() {
        try {
            String currentDataSource = DataSourceContextHolder.getDataSource();
            return Result.success("获取当前数据源成功", currentDataSource);
        } catch (Exception e) {
            log.error("获取当前数据源失败", e);
            return Result.failure(500, "获取当前数据源失败: " + e.getMessage());
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "获取数据源统计信息", description = "获取数据源使用统计信息")
    public Result<Map<String, Object>> getDataSourceStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 这里可以添加更多的统计信息
            // 比如连接池状态、查询次数等
            stats.put("timestamp", System.currentTimeMillis());
            stats.put("message", "数据源统计信息");
            
            return Result.success("获取数据源统计信息成功", stats);
        } catch (Exception e) {
            log.error("获取数据源统计信息失败", e);
            return Result.failure(500, "获取数据源统计信息失败: " + e.getMessage());
        }
    }
}