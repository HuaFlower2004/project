package com.mi.project.controller;

import com.mi.project.common.Result;
import com.mi.project.entity.User;
import com.mi.project.util.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 组件复用控制器
 * 提供Excel导入导出、日志管理、可视化展示等功能
 */
@Slf4j
@RestController
@RequestMapping("/api/component")
@RequiredArgsConstructor
@Tag(name = "组件复用", description = "Excel导入导出、日志管理、可视化展示")
@CrossOrigin(origins = {"http://192.168.93.182:5174", "http://192.168.93.182:5173"})
public class ComponentController {

    private final ExcelUtil excelUtil;
    private final SimpleExcelUtil simpleExcelUtil;
    private final LogUtil logUtil;
    private final VisualizationUtil visualizationUtil;
    // === Excel导入导出功能 ===
    @PostMapping("/excel/import")
    @Operation(summary = "Excel导入", description = "从Excel文件导入数据")
    public Result<List<Map<String, Object>>> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            // 记录操作日志
            LogUtil.logOperation("EXCEL_IMPORT", "system", "导入Excel文件: " + file.getOriginalFilename());
            // 使用简化的Excel工具导入用户数据
            List<Map<String, Object>> result = simpleExcelUtil.importUsersFromExcel(file);
            return Result.success("Excel导入成功", result);
        } catch (Exception e) {
            LogUtil.logError("EXCEL_IMPORT", "导入Excel文件", e);
            return Result.failure(500, "Excel导入失败: " + e.getMessage());
        }
    }

    @GetMapping("/excel/export")
    @Operation(summary = "Excel导出", description = "导出数据到Excel文件")
    public void exportExcel(@RequestParam String dataType, HttpServletResponse response) {
        try {
            // 记录操作日志
            LogUtil.logOperation("EXCEL_EXPORT", "system", "导出Excel文件: " + dataType);
            String fileName = dataType + "_" + System.currentTimeMillis() + ".xlsx";
            // 根据数据类型生成不同的导出数据
            List<Map<String, Object>> data = generateExportData(dataType);
            // 使用简化的Excel工具进行导出
            switch (dataType.toLowerCase()) {
                case "user":
                    simpleExcelUtil.exportUsersToExcel(data, fileName, response);
                    break;
                case "file":
                    simpleExcelUtil.exportFileRecordsToExcel(data, fileName, response);
                    break;
                case "powerline":
                    simpleExcelUtil.exportPowerlineAnalysisToExcel(data, fileName, response);
                    break;
                default:
                    // 使用通用Excel工具
                    List<User> users = data.stream()
                            .map(this::mapToUser)
                            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
                    excelUtil.exportToExcel(users, fileName, response);
            }
        } catch (Exception e) {
            LogUtil.logError("EXCEL_EXPORT", "导出Excel文件", e);
            log.error("Excel导出失败", e);
        }
    }

    // === 日志管理功能 ===

    @GetMapping("/logs/operation")
    @Operation(summary = "获取操作日志", description = "获取系统操作日志")
    public Result<Map<String, LogUtil.OperationLog>> getOperationLogs() {
        try {
            Map<String, LogUtil.OperationLog> logs = LogUtil.getAllOperationLogs();
            return Result.success("获取操作日志成功", logs);
        } catch (Exception e) {
            LogUtil.logError("LOG_MANAGEMENT", "获取操作日志", e);
            return Result.failure(500, "获取操作日志失败: " + e.getMessage());
        }
    }
    @PostMapping("/logs/clean")
    @Operation(summary = "清理过期日志", description = "清理过期的操作日志")
    public Result<Void> cleanExpiredLogs(@RequestParam(defaultValue = "24") int maxAgeHours) {
        try {
            LogUtil.cleanExpiredLogs(maxAgeHours);
            LogUtil.logOperation("LOG_CLEAN", "system", "清理过期日志，保留" + maxAgeHours + "小时");
            return Result.success("清理过期日志成功", null);
        } catch (Exception e) {
            LogUtil.logError("LOG_MANAGEMENT", "清理过期日志", e);
            return Result.failure(500, "清理过期日志失败: " + e.getMessage());
        }
    }
    @GetMapping("/logs/operation/{logId}")
    @Operation(summary = "获取单个操作日志", description = "根据ID获取操作日志详情")
    public Result<LogUtil.OperationLog> getOperationLog(@PathVariable String logId) {
        try {
            LogUtil.OperationLog log = LogUtil.getOperationLog(logId);
            if (log != null) {
                return Result.success("获取操作日志成功", log);
            } else {
                return Result.failure(404, "操作日志不存在");
            }
        } catch (Exception e) {
            LogUtil.logError("LOG_MANAGEMENT", "获取操作日志", e);
            return Result.failure(500, "获取操作日志失败: " + e.getMessage());
        }
    }

    @PostMapping("/logs/record")
    @Operation(summary = "记录操作日志", description = "手动记录操作日志")
    public Result<String> recordOperationLog(@RequestBody Map<String, String> logData) {
        try {
            String operation = logData.get("operation");
            String userId = logData.get("userId");
            String details = logData.get("details");
            
            LogUtil.logOperation(operation, userId, details);
            return Result.success("记录操作日志成功", "日志已记录");
        } catch (Exception e) {
            LogUtil.logError("LOG_MANAGEMENT", "记录操作日志", e);
            return Result.failure(500, "记录操作日志失败: " + e.getMessage());
        }
    }

    // === 可视化展示功能 ===

    @GetMapping("/visualization/overview")
    @Operation(summary = "系统概览", description = "获取系统概览数据")
    public Result<Map<String, Object>> getSystemOverview() {
        try {
            Map<String, Object> overview = visualizationUtil.generateSystemOverview();
            return Result.success("获取系统概览成功", overview);
        } catch (Exception e) {
            LogUtil.logError("VISUALIZATION", "获取系统概览", e);
            return Result.failure(500, "获取系统概览失败: " + e.getMessage());
        }
    }

    @GetMapping("/visualization/monitoring")
    @Operation(summary = "实时监控", description = "获取实时监控数据")
    public Result<Map<String, Object>> getRealTimeMonitoring() {
        try {
            Map<String, Object> monitoring = visualizationUtil.generateRealTimeMonitoring();
            return Result.success("获取实时监控数据成功", monitoring);
        } catch (Exception e) {
            LogUtil.logError("VISUALIZATION", "获取实时监控", e);
            return Result.failure(500, "获取实时监控数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/visualization/analysis-report")
    @Operation(summary = "分析报告", description = "获取电力线分析报告数据")
    public Result<Map<String, Object>> getAnalysisReport() {
        try {
            Map<String, Object> report = visualizationUtil.generatePowerlineAnalysisReport();
            return Result.success("获取分析报告成功", report);
        } catch (Exception e) {
            LogUtil.logError("VISUALIZATION", "获取分析报告", e);
            return Result.failure(500, "获取分析报告失败: " + e.getMessage());
        }
    }

    @GetMapping("/visualization/user-activity")
    @Operation(summary = "用户活动", description = "获取用户活动数据")
    public Result<Map<String, Object>> getUserActivity() {
        try {
            Map<String, Object> activity = visualizationUtil.generateUserActivityData();
            return Result.success("获取用户活动数据成功", activity);
        } catch (Exception e) {
            LogUtil.logError("VISUALIZATION", "获取用户活动", e);
            return Result.failure(500, "获取用户活动数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/visualization/dashboard")
    @Operation(summary = "仪表盘", description = "获取仪表盘数据")
    public Result<Map<String, Object>> getDashboard() {
        try {
            Map<String, Object> dashboard = visualizationUtil.generateDashboardData();
            return Result.success("获取仪表盘数据成功", dashboard);
        } catch (Exception e) {
            LogUtil.logError("VISUALIZATION", "获取仪表盘", e);
            return Result.failure(500, "获取仪表盘数据失败: " + e.getMessage());
        }
    }

    @PostMapping("/visualization/chart")
    @Operation(summary = "图表数据", description = "生成图表数据")
    public Result<Map<String, Object>> generateChartData(
            @RequestParam String chartType,
            @RequestBody(required = false) Map<String, Object> params) {
        try {
            Map<String, Object> chartData = visualizationUtil.generateChartData(chartType, params);
            return Result.success("生成图表数据成功", chartData);
        } catch (Exception e) {
            LogUtil.logError("VISUALIZATION", "生成图表数据", e);
            return Result.failure(500, "生成图表数据失败: " + e.getMessage());
        }
    }

    // === 性能监控功能 ===

    @PostMapping("/performance/start")
    @Operation(summary = "开始性能监控", description = "开始性能监控")
    public Result<String> startPerformanceMonitor(@RequestParam String operation) {
        try {
            LogUtil.PerformanceMonitor monitor = LogUtil.startPerformanceMonitor(operation);
            return Result.success("性能监控已开始", monitor.toString());
        } catch (Exception e) {
            LogUtil.logError("PERFORMANCE", "开始性能监控", e);
            return Result.failure(500, "开始性能监控失败: " + e.getMessage());
        }
    }

    // === 私有方法 ===

    private List<Map<String, Object>> generateExportData(String dataType) {
        List<Map<String, Object>> data = new ArrayList<>();
        
        switch (dataType.toLowerCase()) {
            case "user":
                // 生成用户数据
                for (int i = 1; i <= 10; i++) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("id", (long) i);
                    user.put("userName", "user" + i);
                    user.put("email", "user" + i + "@example.com");
                    user.put("phoneNumber", "1380000000" + i);
                    user.put("isActive", true);
                    user.put("createdTime", new Date());
                    data.add(user);
                }
                break;
            default:
                // 默认数据
                data.add(Map.of("message", "暂无数据"));
        }
        
        return data;
    }

    private User mapToUser(Map<String, Object> data) {
        User user = new User();
        user.setId((Long) data.get("id"));
        user.setUserName((String) data.get("userName"));
        user.setEmail((String) data.get("email"));
        user.setPhoneNumber((String) data.get("phoneNumber"));
        user.setActive((Boolean) data.get("isActive"));
        return user;
    }
}

