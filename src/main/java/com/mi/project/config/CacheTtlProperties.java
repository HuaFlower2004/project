package com.mi.project.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 绑定配置项 cache.ttl.*，用于集中管理不同数据类型的TTL。
 * 仅绑定即可消除IDE对属性“无法解析”的告警；
 * 如需生效，可在业务处按需注入使用。
 */
@Data
@Component
@ConfigurationProperties(prefix = "cache.ttl")
public class CacheTtlProperties {

    /** 默认TTL（秒） */
    private Long defaults; // 可映射 yml 中 default，需要自定义setter

    /** 用户相关TTL（秒） */
    private Long user;

    /** 文件相关TTL（秒） */
    private Long file;

    /** 列表相关TTL（秒） */
    private Long list;

    // Spring Boot 对关键字 default 的映射处理：提供 setDefault 显式接收
    public void setDefault(Long value) {
        this.defaults = value;
    }

    public Long getDefault() {
        return this.defaults;
    }
}


