package com.mi.project.controller;

import com.mi.project.mq.ParseTask;
import com.mi.project.mq.TaskProducer;
import org.springframework.web.bind.annotation.*;


import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/task")
public class TaskController {

    private final TaskProducer producer;

    public TaskController(TaskProducer producer) {
        this.producer = producer;
    }

    @PostMapping("/send")
    public Map<String, Object> send(@RequestParam String fileId) {
        String taskId = UUID.randomUUID().toString();
        producer.send(new ParseTask(taskId, fileId));
        return Map.of("ok", true, "taskId", taskId);
    }
}
