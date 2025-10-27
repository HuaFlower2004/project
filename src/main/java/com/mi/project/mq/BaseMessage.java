package com.mi.project.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 基础消息类
 * 所有消息的基类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseMessage {
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 消息类型
     */
    private String messageType;
    
    /**
     * 消息来源
     */
    private String source;
    
    /**
     * 消息目标
     */
    private String target;
    
    /**
     * 消息优先级 (1-10, 10最高)
     */
    private Integer priority;
    
    /**
     * 消息创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 消息过期时间
     */
    private LocalDateTime expireTime;
    
    /**
     * 重试次数
     */
    private Integer retryCount;
    
    /**
     * 最大重试次数
     */
    private Integer maxRetry;
    
    /**
     * 消息头信息
     */
    private Map<String, Object> headers;
    
    /**
     * 消息体
     */
    private Object payload;
    
    /**
     * 关联ID（用于消息追踪）
     */
    private String correlationId;
    
    /**
     * 回复队列
     */
    private String replyTo;
    
    /**
     * 是否持久化
     */
    private Boolean persistent;
    
    /**
     * 消息状态
     */
    private MessageStatus status;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 消息状态枚举
     */
    public enum MessageStatus {
        PENDING,    // 待处理
        PROCESSING, // 处理中
        COMPLETED,  // 已完成
        FAILED,     // 失败
        EXPIRED,    // 过期
        CANCELLED   // 已取消
    }
    
    /**
     * 检查消息是否过期
     */
    public boolean isExpired() {
        return expireTime != null && LocalDateTime.now().isAfter(expireTime);
    }
    
    /**
     * 检查是否可以重试
     */
    public boolean canRetry() {
        return retryCount == null || retryCount < maxRetry;
    }
    
    /**
     * 增加重试次数
     */
    public void incrementRetry() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
    }
}


