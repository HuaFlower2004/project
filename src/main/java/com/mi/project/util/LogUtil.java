package com.mi.project.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日志工具类
 * 提供统一的日志记录和管理功能
 */
@Slf4j
@Component
public class LogUtil {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    // 日志记录器缓存
    private static final Map<String, Logger> loggerCache = new ConcurrentHashMap<>();
    
    // 操作日志记录
    private static final Map<String, OperationLog> operationLogs = new ConcurrentHashMap<>();

    /**
     * 获取日志记录器
     */
    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    /**
     * 获取日志记录器
     */
    public static Logger getLogger(String name) {
        return loggerCache.computeIfAbsent(name, LoggerFactory::getLogger);
    }

    /**
     * 记录操作日志
     */
    public static void logOperation(String operation, String userId, String details) {
        String logId = generateLogId();
        OperationLog operationLog = OperationLog.builder()
                .logId(logId)
                .operation(operation)
                .userId(userId)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
        
        operationLogs.put(logId, operationLog);
        
        Logger logger = getLogger("OPERATION");
        logger.info("操作日志 - ID: {}, 操作: {}, 用户: {}, 详情: {}", 
                logId, operation, userId, details);
    }

    /**
     * 记录系统日志
     */
    public static void logSystem(String level, String module, String message, Object... args) {
        Logger logger = getLogger("SYSTEM");
        String formattedMessage = String.format("[%s] %s: %s", 
                module, LocalDateTime.now().format(TIMESTAMP_FORMATTER), message);
        
        switch (level.toUpperCase()) {
            case "DEBUG":
                logger.debug(formattedMessage, args);
                break;
            case "INFO":
                logger.info(formattedMessage, args);
                break;
            case "WARN":
                logger.warn(formattedMessage, args);
                break;
            case "ERROR":
                logger.error(formattedMessage, args);
                break;
            default:
                logger.info(formattedMessage, args);
        }
    }

    /**
     * 记录性能日志
     */
    public static void logPerformance(String operation, long startTime, long endTime, Map<String, Object> params) {
        long duration = endTime - startTime;
        Logger logger = getLogger("PERFORMANCE");
        
        StringBuilder sb = new StringBuilder();
        sb.append("性能日志 - 操作: ").append(operation)
          .append(", 耗时: ").append(duration).append("ms");
        
        if (params != null && !params.isEmpty()) {
            sb.append(", 参数: ").append(params);
        }
        
        logger.info(sb.toString());
    }

    /**
     * 记录安全日志
     */
    public static void logSecurity(String event, String userId, String ip, String details) {
        Logger logger = getLogger("SECURITY");
        logger.warn("安全日志 - 事件: {}, 用户: {}, IP: {}, 详情: {}", 
                event, userId, ip, details);
    }

    /**
     * 记录业务日志
     */
    public static void logBusiness(String businessType, String businessId, String action, String details) {
        Logger logger = getLogger("BUSINESS");
        logger.info("业务日志 - 类型: {}, ID: {}, 操作: {}, 详情: {}", 
                businessType, businessId, action, details);
    }

    /**
     * 记录错误日志
     */
    public static void logError(String module, String operation, Throwable throwable) {
        Logger logger = getLogger("ERROR");
        logger.error("错误日志 - 模块: {}, 操作: {}, 错误: {}", 
                module, operation, throwable.getMessage(), throwable);
    }

    /**
     * 记录访问日志
     */
    public static void logAccess(String method, String uri, String ip, String userAgent, int status, long duration) {
        Logger logger = getLogger("ACCESS");
        logger.info("访问日志 - {} {} {} {} {} {}ms", 
                method, uri, ip, userAgent, status, duration);
    }

    /**
     * 获取操作日志
     */
    public static OperationLog getOperationLog(String logId) {
        return operationLogs.get(logId);
    }

    /**
     * 获取所有操作日志
     */
    public static Map<String, OperationLog> getAllOperationLogs() {
        return new HashMap<>(operationLogs);
    }

    /**
     * 清理过期日志
     */
    public static void cleanExpiredLogs(int maxAgeHours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(maxAgeHours);
        operationLogs.entrySet().removeIf(entry -> 
                entry.getValue().getTimestamp().isBefore(cutoff));
        
        log.info("清理过期日志完成，保留{}小时内的日志", maxAgeHours);
    }

    /**
     * 生成日志ID
     */
    private static String generateLogId() {
        return "LOG_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
    }

    /**
     * 操作日志实体
     */
    @lombok.Data
    @lombok.Builder
    public static class OperationLog {
        private String logId;
        private String operation;
        private String userId;
        private String details;
        private LocalDateTime timestamp;
    }

    /**
     * 性能监控装饰器
     */
    public static class PerformanceMonitor {
        private final String operation;
        private final long startTime;
        private final Map<String, Object> params;

        public PerformanceMonitor(String operation, Map<String, Object> params) {
            this.operation = operation;
            this.startTime = System.currentTimeMillis();
            this.params = params;
        }

        public void finish() {
            long endTime = System.currentTimeMillis();
            logPerformance(operation, startTime, endTime, params);
        }
    }

    /**
     * 创建性能监控器
     */
    public static PerformanceMonitor startPerformanceMonitor(String operation, Map<String, Object> params) {
        return new PerformanceMonitor(operation, params);
    }

    /**
     * 创建性能监控器
     */
    public static PerformanceMonitor startPerformanceMonitor(String operation) {
        return new PerformanceMonitor(operation, null);
    }
}


