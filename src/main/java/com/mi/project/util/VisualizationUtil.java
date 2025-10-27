package com.mi.project.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 可视化工具类
 * 提供数据可视化相关的功能
 */
@Slf4j
@Component
public class VisualizationUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 生成系统概览数据
     */
    public Map<String, Object> generateSystemOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        // 系统状态
        overview.put("system_status", "RUNNING");
        overview.put("uptime", getSystemUptime());
        overview.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));
        
        // 用户统计
        Map<String, Object> userStats = new HashMap<>();
        userStats.put("total_users", 1250);
        userStats.put("active_users", 89);
        userStats.put("new_users_today", 12);
        overview.put("user_statistics", userStats);
        
        // 文件统计
        Map<String, Object> fileStats = new HashMap<>();
        fileStats.put("total_files", 3456);
        fileStats.put("processed_files", 3200);
        fileStats.put("processing_files", 15);
        fileStats.put("failed_files", 41);
        overview.put("file_statistics", fileStats);
        
        // 电力线分析统计
        Map<String, Object> analysisStats = new HashMap<>();
        analysisStats.put("total_analyses", 1200);
        analysisStats.put("completed_analyses", 1150);
        analysisStats.put("powerlines_detected", 5670);
        analysisStats.put("average_accuracy", 94.5);
        overview.put("analysis_statistics", analysisStats);
        
        // 系统资源
        Map<String, Object> resourceStats = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        resourceStats.put("memory_usage", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
        resourceStats.put("memory_total", runtime.totalMemory() / 1024 / 1024);
        resourceStats.put("cpu_cores", runtime.availableProcessors());
        overview.put("resource_statistics", resourceStats);
        
        return overview;
    }

    /**
     * 生成实时监控数据
     */
    public Map<String, Object> generateRealTimeMonitoring() {
        Map<String, Object> monitoring = new HashMap<>();
        
        // 实时请求统计
        Map<String, Object> requestStats = new HashMap<>();
        requestStats.put("requests_per_minute", generateRandomData(60, 10, 100));
        requestStats.put("response_time_avg", 245.5);
        requestStats.put("error_rate", 0.02);
        monitoring.put("request_statistics", requestStats);
        
        // 数据库连接池状态
        Map<String, Object> dbStats = new HashMap<>();
        dbStats.put("active_connections", 15);
        dbStats.put("idle_connections", 5);
        dbStats.put("max_connections", 20);
        dbStats.put("connection_usage", 75.0);
        monitoring.put("database_statistics", dbStats);
        
        // 缓存状态
        Map<String, Object> cacheStats = new HashMap<>();
        cacheStats.put("cache_hit_rate", 89.5);
        cacheStats.put("cache_size", 1024);
        cacheStats.put("cache_evictions", 12);
        monitoring.put("cache_statistics", cacheStats);
        
        // 消息队列状态
        Map<String, Object> mqStats = new HashMap<>();
        mqStats.put("queue_depth", 25);
        mqStats.put("messages_processed", 1250);
        mqStats.put("processing_rate", 45.2);
        monitoring.put("message_queue_statistics", mqStats);
        
        return monitoring;
    }

    /**
     * 生成电力线分析报告数据
     */
    public Map<String, Object> generatePowerlineAnalysisReport() {
        Map<String, Object> report = new HashMap<>();
        
        // 分析概览
        Map<String, Object> overview = new HashMap<>();
        overview.put("total_files_analyzed", 1200);
        overview.put("total_powerlines_detected", 5670);
        overview.put("average_accuracy", 94.5);
        overview.put("analysis_duration_avg", 45.2);
        report.put("analysis_overview", overview);
        
        // 电压等级分布
        Map<String, Integer> voltageDistribution = new HashMap<>();
        voltageDistribution.put("110kV", 2340);
        voltageDistribution.put("220kV", 1890);
        voltageDistribution.put("500kV", 980);
        voltageDistribution.put("1000kV", 460);
        report.put("voltage_distribution", voltageDistribution);
        
        // 检测准确率趋势
        List<Map<String, Object>> accuracyTrend = generateAccuracyTrend();
        report.put("accuracy_trend", accuracyTrend);
        
        // 处理时间分布
        Map<String, Object> processingTime = new HashMap<>();
        processingTime.put("fast", 450);  // < 30s
        processingTime.put("normal", 650); // 30-60s
        processingTime.put("slow", 100);   // > 60s
        report.put("processing_time_distribution", processingTime);
        
        // 错误类型统计
        Map<String, Integer> errorTypes = new HashMap<>();
        errorTypes.put("文件格式错误", 15);
        errorTypes.put("点云数据不足", 8);
        errorTypes.put("算法处理失败", 12);
        errorTypes.put("内存不足", 3);
        errorTypes.put("其他", 6);
        report.put("error_types", errorTypes);
        
        return report;
    }

    /**
     * 生成用户活动数据
     */
    public Map<String, Object> generateUserActivityData() {
        Map<String, Object> activity = new HashMap<>();
        
        // 用户登录统计
        List<Map<String, Object>> loginStats = generateLoginStats();
        activity.put("login_statistics", loginStats);
        
        // 功能使用统计
        Map<String, Integer> featureUsage = new HashMap<>();
        featureUsage.put("文件上传", 1250);
        featureUsage.put("电力线分析", 980);
        featureUsage.put("报告生成", 750);
        featureUsage.put("数据导出", 320);
        activity.put("feature_usage", featureUsage);
        
        // 用户地域分布
        Map<String, Integer> geographicDistribution = new HashMap<>();
        geographicDistribution.put("北京", 320);
        geographicDistribution.put("上海", 280);
        geographicDistribution.put("广州", 250);
        geographicDistribution.put("深圳", 200);
        geographicDistribution.put("其他", 200);
        activity.put("geographic_distribution", geographicDistribution);
        
        return activity;
    }

    /**
     * 生成图表数据
     */
    public Map<String, Object> generateChartData(String chartType, Map<String, Object> params) {
        Map<String, Object> chartData = new HashMap<>();
        
        switch (chartType.toLowerCase()) {
            case "line":
                chartData = generateLineChartData(params);
                break;
            case "bar":
                chartData = generateBarChartData(params);
                break;
            case "pie":
                chartData = generatePieChartData(params);
                break;
            case "scatter":
                chartData = generateScatterChartData(params);
                break;
            default:
                chartData.put("error", "不支持的图表类型: " + chartType);
        }
        
        return chartData;
    }

    /**
     * 生成仪表盘数据
     */
    public Map<String, Object> generateDashboardData() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // 关键指标
        List<Map<String, Object>> kpis = Arrays.asList(
            createKPI("总用户数", "1250", "人", "up", 5.2),
            createKPI("处理文件数", "3200", "个", "up", 12.5),
            createKPI("检测准确率", "94.5", "%", "up", 2.1),
            createKPI("系统可用性", "99.8", "%", "up", 0.1)
        );
        dashboard.put("kpis", kpis);
        
        // 实时图表数据
        dashboard.put("realtime_charts", generateRealTimeCharts());
        
        // 告警信息
        List<Map<String, Object>> alerts = generateAlerts();
        dashboard.put("alerts", alerts);
        
        return dashboard;
    }

    // === 私有方法 ===

    private String getSystemUptime() {
        long uptime = System.currentTimeMillis() - 1000 * 60 * 60 * 24; // 模拟1天
        long days = uptime / (1000 * 60 * 60 * 24);
        long hours = (uptime % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        return days + "天" + hours + "小时";
    }

    private List<Map<String, Object>> generateRandomData(int count, int min, int max) {
        List<Map<String, Object>> data = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < count; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("x", i);
            point.put("y", min + random.nextInt(max - min));
            data.add(point);
        }
        
        return data;
    }

    private List<Map<String, Object>> generateAccuracyTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        String[] dates = {"2024-01", "2024-02", "2024-03", "2024-04", "2024-05", "2024-06"};
        double[] accuracies = {92.1, 93.5, 94.2, 94.8, 94.5, 94.5};
        
        for (int i = 0; i < dates.length; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("date", dates[i]);
            point.put("accuracy", accuracies[i]);
            trend.add(point);
        }
        
        return trend;
    }

    private List<Map<String, Object>> generateLoginStats() {
        List<Map<String, Object>> stats = new ArrayList<>();
        String[] hours = {"00", "02", "04", "06", "08", "10", "12", "14", "16", "18", "20", "22"};
        int[] counts = {5, 3, 2, 8, 25, 45, 60, 55, 50, 40, 30, 15};
        
        for (int i = 0; i < hours.length; i++) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("hour", hours[i]);
            stat.put("count", counts[i]);
            stats.add(stat);
        }
        
        return stats;
    }

    private Map<String, Object> generateLineChartData(Map<String, Object> params) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "line");
        data.put("data", generateRandomData(30, 10, 100));
        data.put("xAxis", "时间");
        data.put("yAxis", "数值");
        return data;
    }

    private Map<String, Object> generateBarChartData(Map<String, Object> params) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "bar");
        data.put("data", Arrays.asList(
            Map.of("name", "一月", "value", 120),
            Map.of("name", "二月", "value", 150),
            Map.of("name", "三月", "value", 180),
            Map.of("name", "四月", "value", 160)
        ));
        return data;
    }

    private Map<String, Object> generatePieChartData(Map<String, Object> params) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "pie");
        data.put("data", Arrays.asList(
            Map.of("name", "110kV", "value", 2340),
            Map.of("name", "220kV", "value", 1890),
            Map.of("name", "500kV", "value", 980),
            Map.of("name", "1000kV", "value", 460)
        ));
        return data;
    }

    private Map<String, Object> generateScatterChartData(Map<String, Object> params) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "scatter");
        data.put("data", generateRandomData(50, 0, 100));
        return data;
    }

    private Map<String, Object> createKPI(String name, String value, String unit, String trend, double change) {
        Map<String, Object> kpi = new HashMap<>();
        kpi.put("name", name);
        kpi.put("value", value);
        kpi.put("unit", unit);
        kpi.put("trend", trend);
        kpi.put("change", change);
        return kpi;
    }

    private Map<String, Object> generateRealTimeCharts() {
        Map<String, Object> charts = new HashMap<>();
        charts.put("requests", generateRandomData(60, 10, 100));
        charts.put("response_time", generateRandomData(60, 100, 500));
        charts.put("error_rate", generateRandomData(60, 0, 5));
        return charts;
    }

    private List<Map<String, Object>> generateAlerts() {
        return Arrays.asList(
            Map.of("level", "warning", "message", "数据库连接池使用率超过80%", "time", "2024-01-15 10:30:00"),
            Map.of("level", "info", "message", "系统维护将在今晚22:00开始", "time", "2024-01-15 09:00:00"),
            Map.of("level", "error", "message", "文件处理服务异常", "time", "2024-01-15 08:45:00")
        );
    }
}


