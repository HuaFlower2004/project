package com.mi.project.common;

public class FileStatus {
    public static final Integer UPLOADED = 1;    // 已上传，待处理
    public static final Integer PROCESSING = 2;  // 处理中
    public static final Integer COMPLETED = 3;   // 处理完成
    public static final Integer FAILED = 4;      // 处理失败
}