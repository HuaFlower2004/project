package com.mi.project.controller;

import com.mi.project.common.Result;
import com.mi.project.mq.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息队列管理控制器
 * 提供消息队列操作和监控功能
 */
@Slf4j
@RestController
@RequestMapping("/api/mq")
@RequiredArgsConstructor
@Tag(name = "消息队列管理", description = "消息队列操作和监控")
@CrossOrigin(origins = {"http://192.168.93.182:5174", "http://192.168.93.182:5173"})
public class MessageQueueController {

    private final MessageProducer messageProducer;

    @PostMapping("/file/process")
    @Operation(summary = "发送文件处理消息", description = "发送文件处理消息到队列")
    public Result<String> sendFileProcessMessage(@RequestBody FileProcessMessage message) {
        try {
            messageProducer.sendFileProcessMessage(message);
            return Result.success("文件处理消息发送成功", message.getMessageId());
        } catch (Exception e) {
            log.error("发送文件处理消息失败", e);
            return Result.failure(500, "发送消息失败: " + e.getMessage());
        }
    }

    @PostMapping("/powerline/analysis")
    @Operation(summary = "发送电力线分析消息", description = "发送电力线分析消息到队列")
    public Result<String> sendPowerlineAnalysisMessage(@RequestBody BaseMessage message) {
        try {
            messageProducer.sendPowerlineAnalysisMessage(message);
            return Result.success("电力线分析消息发送成功", message.getMessageId());
        } catch (Exception e) {
            log.error("发送电力线分析消息失败", e);
            return Result.failure(500, "发送消息失败: " + e.getMessage());
        }
    }

    @PostMapping("/notification")
    @Operation(summary = "发送通知消息", description = "发送通知消息到队列")
    public Result<String> sendNotificationMessage(@RequestBody BaseMessage message) {
        try {
            messageProducer.sendNotificationMessage(message);
            return Result.success("通知消息发送成功", message.getMessageId());
        } catch (Exception e) {
            log.error("发送通知消息失败", e);
            return Result.failure(500, "发送消息失败: " + e.getMessage());
        }
    }

    @PostMapping("/system/log")
    @Operation(summary = "发送系统日志消息", description = "发送系统日志消息到队列")
    public Result<String> sendSystemLogMessage(@RequestBody BaseMessage message) {
        try {
            messageProducer.sendSystemLogMessage(message);
            return Result.success("系统日志消息发送成功", message.getMessageId());
        } catch (Exception e) {
            log.error("发送系统日志消息失败", e);
            return Result.failure(500, "发送消息失败: " + e.getMessage());
        }
    }

    @PostMapping("/broadcast")
    @Operation(summary = "发送广播消息", description = "发送广播消息到所有队列")
    public Result<String> sendBroadcastMessage(@RequestBody BaseMessage message) {
        try {
            messageProducer.sendBroadcastMessage(message);
            return Result.success("广播消息发送成功", message.getMessageId());
        } catch (Exception e) {
            log.error("发送广播消息失败", e);
            return Result.failure(500, "发送消息失败: " + e.getMessage());
        }
    }

    @PostMapping("/delayed")
    @Operation(summary = "发送延迟消息", description = "发送延迟消息到队列")
    public Result<String> sendDelayedMessage(
            @RequestBody BaseMessage message,
            @RequestParam long delayMillis) {
        try {
            messageProducer.sendDelayedMessage(message, delayMillis);
            return Result.success("延迟消息发送成功", message.getMessageId());
        } catch (Exception e) {
            log.error("发送延迟消息失败", e);
            return Result.failure(500, "发送消息失败: " + e.getMessage());
        }
    }

    @PostMapping("/request-reply")
    @Operation(summary = "发送请求回复消息", description = "发送请求回复消息到队列")
    public Result<String> sendRequestReplyMessage(
            @RequestBody BaseMessage message,
            @RequestParam String replyQueue) {
        try {
            messageProducer.sendRequestReplyMessage(message, replyQueue);
            return Result.success("请求回复消息发送成功", message.getMessageId());
        } catch (Exception e) {
            log.error("发送请求回复消息失败", e);
            return Result.failure(500, "发送消息失败: " + e.getMessage());
        }
    }

    @GetMapping("/test")
    @Operation(summary = "测试消息队列", description = "发送测试消息验证队列功能")
    public Result<Map<String, Object>> testMessageQueue() {
        try {
            // 创建测试消息
            BaseMessage testMessage = BaseMessage.builder()
                    .messageType("test")
                    .source("controller")
                    .target("consumer")
                    .priority(5)
                    .payload("测试消息内容")
                    .persistent(true)
                    .build();

            // 发送不同类型的测试消息
            messageProducer.sendBroadcastMessage(testMessage);
            
            Map<String, Object> result = new HashMap<>();
            result.put("messageId", testMessage.getMessageId());
            result.put("timestamp", LocalDateTime.now());
            result.put("status", "SUCCESS");
            result.put("message", "测试消息发送成功");

            return Result.success("消息队列测试成功", result);
        } catch (Exception e) {
            log.error("消息队列测试失败", e);
            return Result.failure(500, "消息队列测试失败: " + e.getMessage());
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "获取消息队列统计", description = "获取消息队列的使用统计信息")
    public Result<Map<String, Object>> getMessageQueueStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("timestamp", LocalDateTime.now());
            stats.put("queues", Map.of(
                "file_process", RabbitConfig.FILE_PROCESS_QUEUE,
                "powerline_analysis", RabbitConfig.POWERLINE_ANALYSIS_QUEUE,
                "notification", RabbitConfig.NOTIFICATION_QUEUE,
                "system_log", RabbitConfig.SYSTEM_LOG_QUEUE,
                "dead_letter", RabbitConfig.DEAD_LETTER_QUEUE
            ));
            stats.put("exchanges", Map.of(
                "direct", RabbitConfig.DIRECT_EXCHANGE,
                "topic", RabbitConfig.TOPIC_EXCHANGE,
                "fanout", RabbitConfig.FANOUT_EXCHANGE,
                "dead_letter", RabbitConfig.DLX_EXCHANGE
            ));
            stats.put("status", "RUNNING");

            return Result.success("获取消息队列统计成功", stats);
        } catch (Exception e) {
            log.error("获取消息队列统计失败", e);
            return Result.failure(500, "获取统计信息失败: " + e.getMessage());
        }
    }
}


