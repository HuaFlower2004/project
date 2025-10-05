package com.mi.project.mq;

public class ParseTask {
    private String taskId;
    private String fileId;

    public ParseTask() {}                     // 无参构造
    public ParseTask(String taskId, String fileId) { this.taskId = taskId; this.fileId = fileId; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
}
