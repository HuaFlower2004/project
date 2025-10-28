package com.mi.project.controller;

import com.mi.project.common.Result;
import com.mi.project.config.datasource.DataSourceContextHolder;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据源监控控制器
 * 提供数据源状态监控和管理功能
 * @author 31591
 */
@Slf4j
@RestController
@RequestMapping("/api/datasource")
@RequiredArgsConstructor
@Tag(name = "数据源监控", description = "数据源状态监控和管理")
@CrossOrigin(origins = {"http://192.168.93.182:5174", "http://192.168.93.182:5173"})
public class DataSourceMonitorController {

    @Resource
    private DynamicRoutingDataSource dynamicDataSource;

    @GetMapping("/health")
    @Operation(summary = "获取数据源健康状态", description = "检查所有数据源的连接状态")
    public Result<Map<String, Object>> getDataSourceHealth() {
        try {
            Map<String, Object> details = new HashMap<>();
            boolean allUp = true;

            for (String name : new String[]{"master", "slave1", "slave2"}) {
                Map<String, Object> ds = new HashMap<>();
                long startNs = System.nanoTime();
                try {
                    DataSource dataSource = dynamicDataSource.getDataSource(name);
                    if (dataSource == null) {
                        ds.put("status", "DOWN");
                        ds.put("error", "not found");
                        allUp = false;
                    } else {
                        try (Connection conn = dataSource.getConnection();
                             Statement stmt = conn.createStatement()) {
                            stmt.execute("SELECT 1");
                            ds.put("status", "UP");
                        }
                    }
                } catch (Exception ex) {
                    ds.put("status", "DOWN");
                    ds.put("error", ex.getMessage());
                    allUp = false;
                } finally {
                    long costMs = (System.nanoTime() - startNs) / 1_000_000;
                    ds.put("latencyMs", costMs);
                }
                details.put(name, ds);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("status", allUp ? "UP" : "DOWN");
            result.put("details", details);
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

    @GetMapping("/probe")
    @Operation(summary = "按名称探测数据源", description = "对指定数据源执行连接与'SELECT 1'探测")
    public Result<Map<String, Object>> probe(@RequestParam String name) {
        Map<String, Object> resp = new HashMap<>();
        long startNs = System.nanoTime();
        try {
            DataSource dataSource = dynamicDataSource.getDataSource(name);
            if (dataSource == null) {
                resp.put("status", "DOWN");
                resp.put("error", "not found");
                return Result.failure(404, "数据源不存在");
            }
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("SELECT 1");
                resp.put("status", "UP");
            }
            return Result.success("探测成功", resp);
        } catch (Exception e) {
            resp.put("status", "DOWN");
            resp.put("error", e.getMessage());
            return Result.failure(500, "探测失败");
        } finally {
            resp.put("latencyMs", (System.nanoTime() - startNs) / 1_000_000);
            resp.put("timestamp", System.currentTimeMillis());
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