package com.mi.project.rmi.client;
import com.mi.project.common.Result;
import com.mi.project.rmi.api.PowerLineAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * 电力线分析RMI客户端控制器
 * 调用远程电力线分析服务
 * @author 31591
 */
@Slf4j
@RestController
@RequestMapping("/api/rmi/powerline")
@Tag(name = "电力线分析RMI", description = "远程电力线分析服务调用")
@CrossOrigin(origins = {"http://192.168.93.182:5174", "http://192.168.93.182:5173"})
public class PowerLineAnalysisController {
    private static final String RMI_SERVICE_URL = "rmi://192.168.181.152:1099/PowerLineAnalysisService";
    /**
     * 获取RMI服务实例
     */
    private PowerLineAnalysisService getService() throws RemoteException, MalformedURLException, NotBoundException {
        return (PowerLineAnalysisService) Naming.lookup(RMI_SERVICE_URL);
    }

    @PostMapping("/process")
    @Operation(summary = "处理LAS文件", description = "调用远程服务处理LAS文件并提取电力线")
    public Result<Map<String, Object>> processLasFile(
            @RequestParam String filePath,
            @RequestParam String outputDir) {
        try {
            PowerLineAnalysisService service = getService();
            Map<String, Object> result = service.processLasFile(filePath, outputDir);
            return Result.success("文件处理成功", result);
        } catch (Exception e) {
            log.error("RMI调用失败", e);
            return Result.failure(500, "远程服务调用失败: " + e.getMessage());
        }
    }

    @PostMapping("/extract")
    @Operation(summary = "提取电力线", description = "调用远程服务提取电力线")
    public Result<List<Map<String, Object>>> extractPowerLines(
            @RequestParam String inputPath,
            @RequestBody(required = false) Map<String, Object> parameters) {
        try {
            PowerLineAnalysisService service = getService();
            List<Map<String, Object>> result = service.extractPowerLines(inputPath, parameters);
            return Result.success("电力线提取成功", result);
        } catch (Exception e) {
            log.error("RMI调用失败", e);
            return Result.failure(500, "远程服务调用失败: " + e.getMessage());
        }
    }

    @PostMapping("/ransac")
    @Operation(summary = "RANSAC拟合", description = "调用远程服务执行RANSAC拟合")
    public Result<Map<String, Object>> performRansacFitting(
            @RequestBody List<Map<String, Object>> powerLineData,
            @RequestBody(required = false) Map<String, Object> fitParameters) {
        try {
            PowerLineAnalysisService service = getService();
            Map<String, Object> result = service.performRansacFitting(powerLineData, fitParameters);
            return Result.success("RANSAC拟合成功", result);
        } catch (Exception e) {
            log.error("RMI调用失败", e);
            return Result.failure(500, "远程服务调用失败: " + e.getMessage());
        }
    }

    @PostMapping("/report")
    @Operation(summary = "生成分析报告", description = "调用远程服务生成分析报告")
    public Result<String> generateReport(
            @RequestBody Map<String, Object> analysisData,
            @RequestParam(defaultValue = "html") String reportType) {
        try {
            PowerLineAnalysisService service = getService();
            String report = service.generateAnalysisReport(analysisData, reportType);
            return Result.success("报告生成成功", report);
        } catch (Exception e) {
            log.error("RMI调用失败", e);
            return Result.failure(500, "远程服务调用失败: " + e.getMessage());
        }
    }

    @GetMapping("/status/{taskId}")
    @Operation(summary = "获取处理状态", description = "获取远程处理任务的状态")
    public Result<Map<String, Object>> getProcessingStatus(@PathVariable String taskId) {
        try {
            PowerLineAnalysisService service = getService();
            Map<String, Object> status = service.getProcessingStatus(taskId);
            return Result.success("获取状态成功", status);
        } catch (Exception e) {
            log.error("RMI调用失败", e);
            return Result.failure(500, "远程服务调用失败: " + e.getMessage());
        }
    }

    @PostMapping("/cancel/{taskId}")
    @Operation(summary = "取消处理任务", description = "取消远程处理任务")
    public Result<Boolean> cancelProcessing(@PathVariable String taskId) {
        try {
            PowerLineAnalysisService service = getService();
            boolean result = service.cancelProcessing(taskId);
            return Result.success("任务取消成功", result);
        } catch (Exception e) {
            log.error("RMI调用失败", e);
            return Result.failure(500, "远程服务调用失败: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    @Operation(summary = "获取服务健康状态", description = "获取远程服务的健康状态")
    public Result<Map<String, Object>> getHealthStatus() {
        try {
            PowerLineAnalysisService service = getService();
            Map<String, Object> health = service.getHealthStatus();
            return Result.success("获取健康状态成功", health);
        } catch (Exception e) {
            log.error("RMI调用失败", e);
            return Result.failure(500, "远程服务调用失败: " + e.getMessage());
        }
    }

    @GetMapping("/test")
    @Operation(summary = "测试RMI连接", description = "测试与远程RMI服务的连接")
    public Result<Map<String, Object>> testConnection() {
        try {
            PowerLineAnalysisService service = getService();
            Map<String, Object> health = service.getHealthStatus();
            
            Map<String, Object> result = new HashMap<>();
            result.put("connection", "SUCCESS");
            result.put("service_url", RMI_SERVICE_URL);
            result.put("health", health);
            result.put("timestamp", System.currentTimeMillis());
            
            return Result.success("RMI连接测试成功", result);
        } catch (Exception e) {
            log.error("RMI连接测试失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("connection", "FAILED");
            result.put("service_url", RMI_SERVICE_URL);
            result.put("error", e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            
            return Result.failure(500, "RMI连接测试失败: " + e.getMessage());
        }
    }
}

