package com.mi.project.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 消息生产者
 * 负责发送各种类型的消息到RabbitMQ
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送文件处理消息
     */
    public void sendFileProcessMessage(FileProcessMessage message) {
        try {
            message.setMessageId(UUID.randomUUID().toString());
            message.setCreateTime(LocalDateTime.now());
            message.setStatus(BaseMessage.MessageStatus.PENDING);
            
            rabbitTemplate.convertAndSend(
                RabbitConfig.DIRECT_EXCHANGE,
                RabbitConfig.FILE_PROCESS_RK,
                message
            );
            
            log.info("文件处理消息发送成功: fileId={}, messageId={}", 
                    message.getFileId(), message.getMessageId());
        } catch (Exception e) {
            log.error("发送文件处理消息失败: fileId={}", message.getFileId(), e);
            throw new RuntimeException("发送消息失败", e);
        }
    }

    /**
     * 发送电力线分析消息
     */
    public void sendPowerlineAnalysisMessage(BaseMessage message) {
        try {
            message.setMessageId(UUID.randomUUID().toString());
            message.setCreateTime(LocalDateTime.now());
            message.setStatus(BaseMessage.MessageStatus.PENDING);
            
            rabbitTemplate.convertAndSend(
                RabbitConfig.DIRECT_EXCHANGE,
                RabbitConfig.POWERLINE_ANALYSIS_RK,
                message
            );
            
            log.info("电力线分析消息发送成功: messageId={}", message.getMessageId());
        } catch (Exception e) {
            log.error("发送电力线分析消息失败", e);
            throw new RuntimeException("发送消息失败", e);
        }
    }

    /**
     * 发送通知消息
     */
    public void sendNotificationMessage(BaseMessage message) {
        try {
            message.setMessageId(UUID.randomUUID().toString());
            message.setCreateTime(LocalDateTime.now());
            message.setStatus(BaseMessage.MessageStatus.PENDING);
            
            rabbitTemplate.convertAndSend(
                RabbitConfig.TOPIC_EXCHANGE,
                "notification." + message.getMessageType(),
                message
            );
            
            log.info("通知消息发送成功: messageId={}, type={}", 
                    message.getMessageId(), message.getMessageType());
        } catch (Exception e) {
            log.error("发送通知消息失败", e);
            throw new RuntimeException("发送消息失败", e);
        }
    }

    /**
     * 发送系统日志消息
     */
    public void sendSystemLogMessage(BaseMessage message) {
        try {
            message.setMessageId(UUID.randomUUID().toString());
            message.setCreateTime(LocalDateTime.now());
            message.setStatus(BaseMessage.MessageStatus.PENDING);
            
            rabbitTemplate.convertAndSend(
                RabbitConfig.TOPIC_EXCHANGE,
                "system.log." + message.getMessageType(),
                message
            );
            
            log.info("系统日志消息发送成功: messageId={}", message.getMessageId());
        } catch (Exception e) {
            log.error("发送系统日志消息失败", e);
            throw new RuntimeException("发送消息失败", e);
        }
    }

    /**
     * 发送广播消息
     */
    public void sendBroadcastMessage(BaseMessage message) {
        try {
            message.setMessageId(UUID.randomUUID().toString());
            message.setCreateTime(LocalDateTime.now());
            message.setStatus(BaseMessage.MessageStatus.PENDING);
            
            rabbitTemplate.convertAndSend(RabbitConfig.FANOUT_EXCHANGE, "", message);
            
            log.info("广播消息发送成功: messageId={}", message.getMessageId());
        } catch (Exception e) {
            log.error("发送广播消息失败", e);
            throw new RuntimeException("发送消息失败", e);
        }
    }

    /**
     * 发送延迟消息
     * 注意：Spring AMQP原生不支持延迟消息，这里使用TTL+死信队列实现
     */
    public void sendDelayedMessage(BaseMessage message, long delayMillis) {
        try {
            message.setMessageId(UUID.randomUUID().toString());
            message.setCreateTime(LocalDateTime.now());
            message.setStatus(BaseMessage.MessageStatus.PENDING);
            
            // 由于Spring AMQP原生不支持延迟消息，我们使用TTL+死信队列的方式
            if (delayMillis > 0 && delayMillis <= Integer.MAX_VALUE) {
                // 发送到死信队列，设置TTL实现延迟效果
                rabbitTemplate.convertAndSend(
                    RabbitConfig.DIRECT_EXCHANGE,
                    RabbitConfig.DEAD_LETTER_QUEUE, // 使用死信队列
                    message,
                    msg -> {
                        // 设置消息TTL（毫秒）
                        msg.getMessageProperties().setExpiration(String.valueOf(delayMillis));
                        return msg;
                    }
                );
                log.info("延迟消息发送到死信队列: messageId={}, delay={}ms", 
                        message.getMessageId(), delayMillis);
            } else {
                // 如果延迟时间无效，直接发送
                rabbitTemplate.convertAndSend(
                    RabbitConfig.DIRECT_EXCHANGE,
                    RabbitConfig.FILE_PROCESS_RK,
                    message
                );
                log.info("直接发送消息: messageId={}", message.getMessageId());
            }
            
        } catch (Exception e) {
            log.error("发送延迟消息失败", e);
            // 如果延迟发送失败，尝试直接发送
            try {
                rabbitTemplate.convertAndSend(
                    RabbitConfig.DIRECT_EXCHANGE,
                    RabbitConfig.FILE_PROCESS_RK,
                    message
                );
                log.info("延迟消息发送失败，已改为直接发送: messageId={}", message.getMessageId());
            } catch (Exception fallbackException) {
                log.error("直接发送也失败", fallbackException);
                throw new RuntimeException("发送消息失败", fallbackException);
            }
        }
    }

    /**
     * 发送带回复的消息
     */
    public void sendRequestReplyMessage(BaseMessage message, String replyQueue) {
        try {
            message.setMessageId(UUID.randomUUID().toString());
            message.setCreateTime(LocalDateTime.now());
            message.setStatus(BaseMessage.MessageStatus.PENDING);
            message.setReplyTo(replyQueue);
            
            rabbitTemplate.convertAndSend(
                RabbitConfig.DIRECT_EXCHANGE,
                RabbitConfig.POWERLINE_ANALYSIS_RK,
                message
            );
            
            log.info("请求回复消息发送成功: messageId={}, replyTo={}", 
                    message.getMessageId(), replyQueue);
        } catch (Exception e) {
            log.error("发送请求回复消息失败", e);
            throw new RuntimeException("发送消息失败", e);
        }
    }
}

