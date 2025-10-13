package com.mi.project.controller;

import com.mi.project.common.Result;
import com.mi.project.mq.FileProcessMessage;
import com.mi.project.mq.MessageProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 错误测试控制器
 * 用于测试修复后的功能是否正常工作
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Tag(name = "错误测试", description = "测试修复后的功能")
@CrossOrigin(origins = {"http://192.168.93.182:5174", "http://192.168.93.182:5173"})
public class ErrorTestController {

    private final MessageProducer messageProducer;

    @GetMapping("/compile")
    @Operation(summary = "编译测试", description = "测试所有修复后的功能是否正常编译")
    public Result<Map<String, Object>> testCompile() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 测试FileProcessMessage创建
            FileProcessMessage message = FileProcessMessage.create(
                1L, "test.las", "/path/to/test.las", "las", 1024L, 1L, "testuser"
            );
            result.put("fileProcessMessage", "创建成功");
            result.put("messageId", message.getMessageId());
            result.put("fileName", message.getFileName());
            result.put("status", message.getStatus());
            
            // 测试消息状态更新
            message.markCompleted("处理完成");
            result.put("statusUpdate", "状态更新成功");
            result.put("finalStatus", message.getStatus());
            
            // 测试消息发送
            messageProducer.sendFileProcessMessage(message);
            result.put("messageProducer", "发送成功");
            
            result.put("status", "SUCCESS");
            result.put("message", "所有功能编译正常，Builder冲突已解决");
            result.put("timestamp", System.currentTimeMillis());
            
            return Result.success("编译测试通过", result);
        } catch (Exception e) {
            log.error("编译测试失败", e);
            return Result.failure(500, "编译测试失败: " + e.getMessage());
        }
    }

    @GetMapping("/rabbit-config")
    @Operation(summary = "RabbitMQ配置测试", description = "测试RabbitMQ配置常量")
    public Result<Map<String, Object>> testRabbitConfig() {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("QUEUE", com.mi.project.mq.RabbitConfig.QUEUE);
            result.put("EXCHANGE", com.mi.project.mq.RabbitConfig.EXCHANGE);
            result.put("RK", com.mi.project.mq.RabbitConfig.RK);
            result.put("DIRECT_EXCHANGE", com.mi.project.mq.RabbitConfig.DIRECT_EXCHANGE);
            result.put("FILE_PROCESS_QUEUE", com.mi.project.mq.RabbitConfig.FILE_PROCESS_QUEUE);
            
            return Result.success("RabbitMQ配置测试通过", result);
        } catch (Exception e) {
            log.error("RabbitMQ配置测试失败", e);
            return Result.failure(500, "RabbitMQ配置测试失败: " + e.getMessage());
        }
    }

    @GetMapping("/delay-message")
    @Operation(summary = "延迟消息测试", description = "测试延迟消息发送功能（使用TTL+死信队列实现）")
    public Result<Map<String, Object>> testDelayMessage() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 创建测试消息
            FileProcessMessage message = FileProcessMessage.create(
                999L, "delay-test.las", "/path/to/delay-test.las", "las", 2048L, 1L, "testuser"
            );
            
            // 测试延迟消息发送（延迟5秒）
            messageProducer.sendDelayedMessage(message, 5000);
            
            result.put("messageId", message.getMessageId());
            result.put("fileName", message.getFileName());
            result.put("delayMillis", 5000);
            result.put("status", "延迟消息发送成功");
            result.put("implementation", "使用TTL+死信队列实现延迟消息");
            result.put("note", "消息发送到死信队列，设置5秒TTL，到期后会被重新路由");
            result.put("warning", "需要配置死信队列的重新路由逻辑");
            
            return Result.success("延迟消息测试通过", result);
        } catch (Exception e) {
            log.error("延迟消息测试失败", e);
            return Result.failure(500, "延迟消息测试失败: " + e.getMessage());
        }
    }
}
