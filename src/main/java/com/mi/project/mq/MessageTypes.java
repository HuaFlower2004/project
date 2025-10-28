package com.mi.project.mq;

/**
 * 消息类型常量
 * 定义系统中使用的各种消息类型
 */
public class MessageTypes {

    // 文件处理相关消息
    public static final String FILE_UPLOAD = "file.upload";
    public static final String FILE_PROCESS = "file.process";
    public static final String FILE_ANALYSIS = "file.analysis";
    public static final String FILE_COMPLETE = "file.complete";

    // 电力线分析相关消息
    public static final String POWERLINE_EXTRACT = "powerline.extract";
    public static final String POWERLINE_FIT = "powerline.fit";
    public static final String POWERLINE_REPORT = "powerline.report";

    // 用户相关消息
    public static final String USER_LOGIN = "user.login";
    public static final String USER_LOGOUT = "user.logout";
    public static final String USER_REGISTER = "user.register";

    // 系统相关消息
    public static final String SYSTEM_HEALTH = "system.health";
    public static final String SYSTEM_ALERT = "system.alert";
    public static final String SYSTEM_LOG = "system.log";

    // 通知相关消息
    public static final String NOTIFICATION_EMAIL = "notification.email";
    public static final String NOTIFICATION_WEBSOCKET = "notification.websocket";
    public static final String NOTIFICATION_SMS = "notification.sms";

    // 缓存相关消息
    public static final String CACHE_UPDATE = "cache.update";
    public static final String CACHE_INVALIDATE = "cache.invalidate";
    public static final String CACHE_WARMUP = "cache.warmup";

    // 数据同步相关消息
    public static final String DATA_SYNC = "data.sync";
    public static final String DATA_BACKUP = "data.backup";
    public static final String DATA_RESTORE = "data.restore";
}

