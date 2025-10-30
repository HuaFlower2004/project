package com.mi.project.mq;
import com.mi.project.util.WebSocketSenderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
/**
 * 消息消费者
 * 处理各种类型的消息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageConsumer {
    /**
     * 处理文件处理消息
     */
    @RabbitListener(queues = RabbitConfig.FILE_PROCESS_QUEUE)
    public void handleFileProcessMessage(FileProcessMessage message) {
        log.info("收到文件处理消息: fileId={}, fileName={}, step={}", 
                message.getFileId(), message.getFileName(), message.getCurrentStep());
        
        try {
            // 更新消息状态
            message.setStatus(BaseMessage.MessageStatus.PROCESSING);
            
            // 根据处理步骤执行相应逻辑
            switch (message.getCurrentStep()) {
                case UPLOAD:
                    handleFileUpload(message);
                    break;
                case VALIDATE:
                    handleFileValidation(message);
                    break;
                case PREPROCESS:
                    handleFilePreprocess(message);
                    break;
                case EXTRACT:
                    handlePowerlineExtraction(message);
                    break;
                case FIT:
                    handleRansacFitting(message);
                    break;
                case ANALYZE:
                    handleAnalysis(message);
                    break;
                case REPORT:
                    handleReportGeneration(message);
                    break;
                default:
                    log.warn("未知的处理步骤: {}", message.getCurrentStep());
            }
            
            // 发送WebSocket通知
            sendWebSocketNotification(message);
            
        } catch (Exception e) {
            log.error("处理文件消息失败: fileId={}", message.getFileId(), e);
            message.setStatus(BaseMessage.MessageStatus.FAILED);
            message.setErrorMessage(e.getMessage());
        }
    }

    /**
     * 处理电力线分析消息
     */
    @RabbitListener(queues = RabbitConfig.POWERLINE_ANALYSIS_QUEUE)
    public void handlePowerlineAnalysisMessage(BaseMessage message) {
        log.info("收到电力线分析消息: messageId={}, type={}", 
                message.getMessageId(), message.getMessageType());
        
        try {
            message.setStatus(BaseMessage.MessageStatus.PROCESSING);
            
            // 根据消息类型处理
            switch (message.getMessageType()) {
                case MessageTypes.POWERLINE_EXTRACT:
                    handlePowerlineExtraction(message);
                    break;
                case MessageTypes.POWERLINE_FIT:
                    handleRansacFitting(message);
                    break;
                case MessageTypes.POWERLINE_REPORT:
                    handleReportGeneration(message);
                    break;
                default:
                    log.warn("未知的电力线分析消息类型: {}", message.getMessageType());
            }
            
            message.setStatus(BaseMessage.MessageStatus.COMPLETED);
            
        } catch (Exception e) {
            log.error("处理电力线分析消息失败: messageId={}", message.getMessageId(), e);
            message.setStatus(BaseMessage.MessageStatus.FAILED);
            message.setErrorMessage(e.getMessage());
        }
    }

    /**
     * 处理通知消息
     */
    @RabbitListener(queues = RabbitConfig.NOTIFICATION_QUEUE)
    public void handleNotificationMessage(BaseMessage message) {
        log.info("收到通知消息: messageId={}, type={}", 
                message.getMessageId(), message.getMessageType());
        
        try {
            message.setStatus(BaseMessage.MessageStatus.PROCESSING);
            
            // 根据通知类型处理
            switch (message.getMessageType()) {
                case MessageTypes.NOTIFICATION_EMAIL:
                    handleEmailNotification(message);
                    break;
                case MessageTypes.NOTIFICATION_WEBSOCKET:
                    handleWebSocketNotification(message);
                    break;
                case MessageTypes.NOTIFICATION_SMS:
                    handleSmsNotification(message);
                    break;
                default:
                    log.warn("未知的通知消息类型: {}", message.getMessageType());
            }
            
            message.setStatus(BaseMessage.MessageStatus.COMPLETED);
            
        } catch (Exception e) {
            log.error("处理通知消息失败: messageId={}", message.getMessageId(), e);
            message.setStatus(BaseMessage.MessageStatus.FAILED);
            message.setErrorMessage(e.getMessage());
        }
    }

    /**
     * 处理系统日志消息
     */
    @RabbitListener(queues = RabbitConfig.SYSTEM_LOG_QUEUE)
    public void handleSystemLogMessage(BaseMessage message) {
        log.info("收到系统日志消息: messageId={}, type={}", 
                message.getMessageId(), message.getMessageType());
        
        try {
            message.setStatus(BaseMessage.MessageStatus.PROCESSING);
            
            // 记录系统日志
            logSystemEvent(message);
            
            message.setStatus(BaseMessage.MessageStatus.COMPLETED);
            
        } catch (Exception e) {
            log.error("处理系统日志消息失败: messageId={}", message.getMessageId(), e);
            message.setStatus(BaseMessage.MessageStatus.FAILED);
            message.setErrorMessage(e.getMessage());
        }
    }

    /**
     * 处理死信消息
     */
    @RabbitListener(queues = RabbitConfig.DEAD_LETTER_QUEUE)
    public void handleDeadLetterMessage(BaseMessage message) {
        log.error("收到死信消息: messageId={}, type={}, error={}", 
                message.getMessageId(), message.getMessageType(), message.getErrorMessage());
        
        // 记录死信消息，可以发送告警或进行其他处理
        message.setStatus(BaseMessage.MessageStatus.FAILED);
    }

    // === 私有处理方法 ===

    private void handleFileUpload(FileProcessMessage message) {
        log.info("处理文件上传: {}", message.getFileName());
        message.updateStep(FileProcessMessage.ProcessStep.VALIDATE, 20);
    }

    private void handleFileValidation(FileProcessMessage message) {
        log.info("验证文件: {}", message.getFileName());
        message.updateStep(FileProcessMessage.ProcessStep.PREPROCESS, 40);
    }

    private void handleFilePreprocess(FileProcessMessage message) {
        log.info("预处理文件: {}", message.getFileName());
        message.updateStep(FileProcessMessage.ProcessStep.EXTRACT, 60);
    }

    private void handlePowerlineExtraction(Object message) {
        log.info("提取电力线");
        if (message instanceof FileProcessMessage) {
            ((FileProcessMessage) message).updateStep(FileProcessMessage.ProcessStep.FIT, 80);
        }
    }

    private void handleRansacFitting(Object message) {
        log.info("执行RANSAC拟合");
        if (message instanceof FileProcessMessage) {
            ((FileProcessMessage) message).updateStep(FileProcessMessage.ProcessStep.ANALYZE, 90);
        }
    }

    private void handleAnalysis(Object message) {
        log.info("执行分析");
        if (message instanceof FileProcessMessage) {
            ((FileProcessMessage) message).updateStep(FileProcessMessage.ProcessStep.REPORT, 95);
        }
    }

    private void handleReportGeneration(Object message) {
        log.info("生成报告");
        if (message instanceof FileProcessMessage) {
            ((FileProcessMessage) message).markCompleted("分析完成");
        }
    }

    private void handleEmailNotification(BaseMessage message) {
        log.info("发送邮件通知");
        // 实现邮件发送逻辑
    }

    private void handleWebSocketNotification(Object message) {
        log.info("发送WebSocket通知");
        if (message instanceof FileProcessMessage) {
            FileProcessMessage fileMessage = (FileProcessMessage) message;
            String notification = String.format(
                "{\"type\":\"file_process\",\"fileId\":%d,\"fileName\":\"%s\",\"progress\":%d,\"status\":\"%s\"}",
                fileMessage.getFileId(),
                fileMessage.getFileName(),
                fileMessage.getProgress(),
                fileMessage.getStatus()
            );
            WebSocketSenderUtil.sendJsonToAll(notification);
        }
    }

    private void handleSmsNotification(BaseMessage message) {
        log.info("发送短信通知");
        // 实现短信发送逻辑
    }

    private void logSystemEvent(BaseMessage message) {
        log.info("记录系统事件: {}", message.getPayload());
        // 实现系统日志记录逻辑
    }

    private void sendWebSocketNotification(FileProcessMessage message) {
        String notification = String.format(
            "{\"type\":\"file_process_update\",\"fileId\":%d,\"fileName\":\"%s\",\"progress\":%d,\"step\":\"%s\"}",
            message.getFileId(),
            message.getFileName(),
            message.getProgress(),
            message.getCurrentStep()
        );
        WebSocketSenderUtil.sendJsonToAll(notification);
    }
}

