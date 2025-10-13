package com.mi.project.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 文件处理消息
 * 用于文件上传和处理流程
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FileProcessMessage extends BaseMessage {
    
    /**
     * 文件ID
     */
    private Long fileId;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件路径
     */
    private String filePath;
    
    /**
     * 文件类型
     */
    private String fileType;
    
    /**
     * 文件大小
     */
    private Long fileSize;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String userName;
    
    /**
     * 处理参数
     */
    private Map<String, Object> processParams;
    
    /**
     * 处理步骤
     */
    private ProcessStep currentStep;
    
    /**
     * 处理进度 (0-100)
     */
    private Integer progress;
    
    /**
     * 处理结果
     */
    private String result;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 处理步骤枚举
     */
    public enum ProcessStep {
        UPLOAD,         // 文件上传
        VALIDATE,       // 文件验证
        PREPROCESS,     // 预处理
        EXTRACT,        // 电力线提取
        FIT,           // RANSAC拟合
        ANALYZE,       // 分析
        REPORT,        // 生成报告
        COMPLETE       // 完成
    }
    
    /**
     * 创建文件处理消息
     */
    public static FileProcessMessage create(Long fileId, String fileName, String filePath, 
                                          String fileType, Long fileSize, Long userId, String userName) {
        FileProcessMessage message = new FileProcessMessage();
        message.setFileId(fileId);
        message.setFileName(fileName);
        message.setFilePath(filePath);
        message.setFileType(fileType);
        message.setFileSize(fileSize);
        message.setUserId(userId);
        message.setUserName(userName);
        message.setCurrentStep(ProcessStep.UPLOAD);
        message.setProgress(0);
        message.setStatus(MessageStatus.PENDING);
        message.setMessageType(MessageTypes.FILE_PROCESS);
        message.setPriority(5);
        message.setPersistent(true);
        return message;
    }
    
    /**
     * 更新处理步骤
     */
    public void updateStep(ProcessStep step, Integer progress) {
        this.currentStep = step;
        this.progress = progress;
    }
    
    /**
     * 标记为完成
     */
    public void markCompleted(String result) {
        this.setStatus(MessageStatus.COMPLETED);
        this.currentStep = ProcessStep.COMPLETE;
        this.progress = 100;
        this.result = result;
    }
    
    /**
     * 标记为失败
     */
    public void markFailed(String errorMessage) {
        this.setStatus(MessageStatus.FAILED);
        this.errorMessage = errorMessage;
    }
}

