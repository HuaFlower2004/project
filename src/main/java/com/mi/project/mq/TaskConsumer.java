package com.mi.project.mq;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * @author 31591
 */
@Service
public class TaskConsumer {
    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void onMessage(ParseTask task) {
        System.out.println("收到任务: taskId=" + task.getTaskId() + ", fileId=" + task.getFileId());
        // TODO: 在这里调用你的解析逻辑
    }
}
