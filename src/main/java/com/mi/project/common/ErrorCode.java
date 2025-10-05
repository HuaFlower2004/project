package com.mi.project.common;

public class ErrorCode {
    // 系统级错误码
    public static final Integer SUCCESS = 200;
    public static final Integer SYSTEM_ERROR = 500;
    public static final Integer PARAM_ERROR = 400;

    // 用户相关错误码 1000-1999
    public static final Integer USER_ERROR = 1000;
    public static final Integer USER_NAME_EXISTS = 1001;
    public static final Integer EMAIL_EXISTS = 1002;
    public static final Integer PHONE_EXISTS = 1003;
    public static final Integer USER_NOT_FOUND = 1004;
    public static final Integer PASSWORD_NOT_MATCH = 1005;
}
