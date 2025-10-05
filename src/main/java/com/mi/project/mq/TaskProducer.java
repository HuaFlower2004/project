
package com.mi.project.mq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class TaskProducer {
    private final RabbitTemplate template;
    public TaskProducer(RabbitTemplate template) { this.template = template; }
    public void send(ParseTask task) {
        template.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK, task);
    }
}

