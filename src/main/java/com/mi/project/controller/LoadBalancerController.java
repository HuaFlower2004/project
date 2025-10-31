package com.mi.project.controller;

import com.mi.project.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 负载均衡控制器
 * 提供负载均衡相关的监控和管理功能
 */
@Slf4j
@RestController
@RequestMapping("/api/loadbalancer")
@Tag(name = "负载均衡管理", description = "负载均衡监控和管理")
@CrossOrigin(origins = {"http://192.168.93.182:5174", "http://192.168.93.182:5173"})
public class LoadBalancerController {

    @Value("${server.port:9090}")
    private String serverPort;

    @Value("${spring.application.name:project}")
    private String applicationName;
    
    // 请求计数器（每个端口实例独立计数）
    private static final java.util.concurrent.atomic.AtomicLong requestCount = new java.util.concurrent.atomic.AtomicLong(0);

    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "用于负载均衡器的健康检查")
    public Result<Map<String, Object>> healthCheck(HttpServletRequest request) {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("application", applicationName);
            health.put("port", serverPort);
            health.put("timestamp", LocalDateTime.now());
            health.put("server_ip", getServerIP());
            health.put("client_ip", getClientIP(request));
            health.put("request_id", request.getHeader("X-Request-ID"));
            
            return Result.success("健康检查通过", health);
        } catch (Exception e) {
            log.error("健康检查失败", e);
            return Result.failure(500, "健康检查失败: " + e.getMessage());
        }
    }

    @GetMapping("/info")
    @Operation(summary = "获取服务器信息", description = "获取当前服务器的详细信息")
    public Result<Map<String, Object>> getServerInfo(HttpServletRequest request) {
        try {
            Map<String, Object> info = new HashMap<>();
            info.put("application_name", applicationName);
            info.put("server_port", serverPort);
            info.put("server_ip", getServerIP());
            info.put("client_ip", getClientIP(request));
            info.put("user_agent", request.getHeader("User-Agent"));
            info.put("request_uri", request.getRequestURI());
            info.put("request_method", request.getMethod());
            info.put("timestamp", LocalDateTime.now());
            
            // 系统信息
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> systemInfo = new HashMap<>();
            systemInfo.put("total_memory", runtime.totalMemory());
            systemInfo.put("free_memory", runtime.freeMemory());
            systemInfo.put("max_memory", runtime.maxMemory());
            systemInfo.put("available_processors", runtime.availableProcessors());
            info.put("system", systemInfo);
            
            return Result.success("获取服务器信息成功", info);
        } catch (Exception e) {
            log.error("获取服务器信息失败", e);
            return Result.failure(500, "获取服务器信息失败: " + e.getMessage());
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "获取负载均衡统计", description = "获取负载均衡相关的统计信息")
    public Result<Map<String, Object>> getLoadBalancerStats(HttpServletRequest request) {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("timestamp", LocalDateTime.now());
            stats.put("server_info", Map.of(
                "application", applicationName,
                "port", serverPort,
                "ip", getServerIP()
            ));
            stats.put("request_info", Map.of(
                "client_ip", getClientIP(request),
                "user_agent", request.getHeader("User-Agent"),
                "request_uri", request.getRequestURI()
            ));
            stats.put("load_balancer", Map.of(
                "strategy", "round-robin",
                "health_check", "enabled",
                "session_affinity", "disabled"
            ));
            
            return Result.success("获取负载均衡统计成功", stats);
        } catch (Exception e) {
            log.error("获取负载均衡统计失败", e);
            return Result.failure(500, "获取负载均衡统计失败: " + e.getMessage());
        }
    }

    @PostMapping("/test")
    @Operation(summary = "负载均衡测试", description = "测试负载均衡功能")
    public Result<Map<String, Object>> testLoadBalancer(HttpServletRequest request) {
        try {
            Map<String, Object> test = new HashMap<>();
            test.put("test_id", System.currentTimeMillis());
            test.put("server_response", Map.of(
                "application", applicationName,
                "port", serverPort,
                "ip", getServerIP(),
                "timestamp", LocalDateTime.now()
            ));
            test.put("request_info", Map.of(
                "client_ip", getClientIP(request),
                "request_uri", request.getRequestURI(),
                "method", request.getMethod()
            ));
            test.put("load_balancer_test", "SUCCESS");
            
            log.info("负载均衡测试请求: client_ip={}, server_ip={}", 
                    getClientIP(request), getServerIP());
            
            return Result.success("负载均衡测试成功", test);
        } catch (Exception e) {
            log.error("负载均衡测试失败", e);
            return Result.failure(500, "负载均衡测试失败: " + e.getMessage());
        }
    }

    @GetMapping("/session")
    @Operation(summary = "会话信息", description = "获取当前会话信息")
    public Result<Map<String, Object>> getSessionInfo(HttpServletRequest request) {
        try {
            Map<String, Object> session = new HashMap<>();
            session.put("session_id", request.getSession().getId());
            session.put("session_creation_time", request.getSession().getCreationTime());
            session.put("session_last_accessed_time", request.getSession().getLastAccessedTime());
            session.put("session_max_inactive_interval", request.getSession().getMaxInactiveInterval());
            session.put("is_new_session", request.getSession().isNew());
            session.put("server_info", Map.of(
                "application", applicationName,
                "port", serverPort,
                "ip", getServerIP()
            ));
            
            return Result.success("获取会话信息成功", session);
        } catch (Exception e) {
            log.error("获取会话信息失败", e);
            return Result.failure(500, "获取会话信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 负载均衡测试接口 - 记录请求并返回当前端口统计
     * 用于测试负载均衡分发效果
     */
    @GetMapping("/test/count")
    @Operation(summary = "负载均衡测试计数", description = "测试负载均衡请求分发，记录每个端口接收的请求数")
    public Result<Map<String, Object>> testLoadBalancerCount(HttpServletRequest request) {
        // 增加计数器
        long currentCount = requestCount.incrementAndGet();
        
        Map<String, Object> result = new HashMap<>();
        result.put("server_port", serverPort);
        result.put("request_count", currentCount);
        result.put("server_ip", getServerIP());
        result.put("client_ip", getClientIP(request));
        result.put("timestamp", LocalDateTime.now());
        result.put("message", String.format("端口 %s 已接收 %d 次请求", serverPort, currentCount));
        
        log.info("负载均衡测试请求 - 端口: {}, 计数: {}, 客户端IP: {}", 
                serverPort, currentCount, getClientIP(request));
        
        return Result.success("请求已记录", result);
    }
    
    /**
     * 获取当前端口的请求统计
     */
    @GetMapping("/test/stats")
    @Operation(summary = "获取请求统计", description = "获取当前端口实例的请求统计信息")
    public Result<Map<String, Object>> getRequestStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("server_port", serverPort);
        stats.put("total_requests", requestCount.get());
        stats.put("server_ip", getServerIP());
        stats.put("application_name", applicationName);
        stats.put("timestamp", LocalDateTime.now());
        
        return Result.success("获取统计成功", stats);
    }
    
    /**
     * 重置计数器（用于测试）
     */
    @PostMapping("/test/reset")
    @Operation(summary = "重置计数器", description = "重置当前端口实例的请求计数器")
    public Result<Map<String, Object>> resetCounter() {
        long oldCount = requestCount.getAndSet(0);
        Map<String, Object> result = new HashMap<>();
        result.put("server_port", serverPort);
        result.put("previous_count", oldCount);
        result.put("current_count", 0);
        result.put("message", "计数器已重置");
        
        log.info("重置计数器 - 端口: {}, 重置前计数: {}", serverPort, oldCount);
        
        return Result.success("计数器已重置", result);
    }

    /**
     * 获取服务器IP地址
     */
    private String getServerIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}

