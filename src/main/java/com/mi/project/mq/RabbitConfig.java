
package com.mi.project.mq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // === 统一常量 ===
    public static final String EXCHANGE = "pc.direct";
    public static final String QUEUE    = "pc.process";
    public static final String RK       = "pc.process.rk";

    // === 基础声明 ===
    @Bean
    public DirectExchange exchange() {
        // durable=true, autoDelete=false
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue queue() {
        // durable=true
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding binding() {
        return BindingBuilder.bind(queue()).to(exchange()).with(RK);
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
