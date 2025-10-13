
package com.mi.project.mq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 * 配置交换器、队列、绑定关系等
 */
@Configuration
public class RabbitConfig {

    // === 交换器常量 ===
    public static final String DIRECT_EXCHANGE = "powerline.direct";
    public static final String TOPIC_EXCHANGE = "powerline.topic";
    public static final String FANOUT_EXCHANGE = "powerline.fanout";
    public static final String DLX_EXCHANGE = "powerline.dlx";

    // === 队列常量 ===
    public static final String FILE_PROCESS_QUEUE = "file.process.queue";
    public static final String POWERLINE_ANALYSIS_QUEUE = "powerline.analysis.queue";
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String SYSTEM_LOG_QUEUE = "system.log.queue";
    public static final String DEAD_LETTER_QUEUE = "dead.letter.queue";

    // === 路由键常量 ===
    public static final String FILE_PROCESS_RK = "file.process";
    public static final String POWERLINE_ANALYSIS_RK = "powerline.analysis";
    public static final String NOTIFICATION_RK = "notification";
    public static final String SYSTEM_LOG_RK = "system.log";
    
    // === 兼容旧版本的常量 ===
    public static final String EXCHANGE = DIRECT_EXCHANGE;
    public static final String QUEUE = FILE_PROCESS_QUEUE;
    public static final String RK = FILE_PROCESS_RK;

    // === 直连交换器 ===
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(DIRECT_EXCHANGE, true, false);
    }

    // === 主题交换器 ===
    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(TOPIC_EXCHANGE, true, false);
    }

    // === 扇出交换器 ===
    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(FANOUT_EXCHANGE, true, false);
    }

    // === 死信交换器 ===
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    // === 文件处理队列 ===
    @Bean
    public Queue fileProcessQueue() {
        return QueueBuilder.durable(FILE_PROCESS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dead.letter")
                .withArgument("x-message-ttl", 300000) // 5分钟TTL
                .build();
    }

    // === 电力线分析队列 ===
    @Bean
    public Queue powerlineAnalysisQueue() {
        return QueueBuilder.durable(POWERLINE_ANALYSIS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dead.letter")
                .withArgument("x-message-ttl", 600000) // 10分钟TTL
                .build();
    }

    // === 通知队列 ===
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dead.letter")
                .build();
    }

    // === 系统日志队列 ===
    @Bean
    public Queue systemLogQueue() {
        return QueueBuilder.durable(SYSTEM_LOG_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dead.letter")
                .build();
    }

    // === 死信队列 ===
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    // === 绑定关系 ===
    @Bean
    public Binding fileProcessBinding() {
        return BindingBuilder.bind(fileProcessQueue()).to(directExchange()).with(FILE_PROCESS_RK);
    }

    @Bean
    public Binding powerlineAnalysisBinding() {
        return BindingBuilder.bind(powerlineAnalysisQueue()).to(directExchange()).with(POWERLINE_ANALYSIS_RK);
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue()).to(topicExchange()).with("notification.*");
    }

    @Bean
    public Binding systemLogBinding() {
        return BindingBuilder.bind(systemLogQueue()).to(topicExchange()).with("system.log.*");
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("dead.letter");
    }

    // === 序列化 ===
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // === Producer 模板（打开 Confirm & Return 日志）===
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter converter) {
        RabbitTemplate tpl = new RabbitTemplate(cf);
        tpl.setMessageConverter(converter);

        // 不可路由返回（需要 mandatory=true 才会触发）
        tpl.setMandatory(true);

        tpl.setReturnsCallback(ret -> {
            System.err.printf("UNROUTABLE -> exchange=%s rk=%s code=%d text=%s%n",
                    ret.getExchange(), ret.getRoutingKey(), ret.getReplyCode(), ret.getReplyText());
        });

        tpl.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                System.err.printf("PUBLISH NACK -> cause=%s%n", cause);
            }
        });

        return tpl;
    }

    // === Listener 工厂（让 @RabbitListener 用 JSON 反序列化）===
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory cf, MessageConverter converter) {
        SimpleRabbitListenerContainerFactory f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(cf);
        f.setMessageConverter(converter);
        return f;
    }
}
