package com.mi.project.exception;


import com.mi.project.common.ErrorCode;

public class UserException extends BusinessException {
    public UserException(String message){
        super(ErrorCode.USER_ERROR, message);
    }

    public UserException(Integer code,String message){
        super(ErrorCode.USER_ERROR , message);
    }

    public static class UserNAmeExistsException extends UserException{
        public UserNAmeExistsException(){
            super(ErrorCode.USER_NAME_EXISTS,"用户名已经存在");
        }
    }

    public static class EmailExistsException extends UserException{
        public EmailExistsException(){
            super(ErrorCode.EMAIL_EXISTS,"邮箱已被注册");
        }
    }

    public static class PhoneExistsException extends UserException{
        public PhoneExistsException(){
            super(ErrorCode.PHONE_EXISTS,"手机号已被注册");
        }
    }

    public static class UserNotFoundException extends UserException{
        public UserNotFoundException() {
            super(ErrorCode.USER_NOT_FOUND,"用户不存在");
        }
    }

    public static class PasswordNotMatchException extends UserException{
        public PasswordNotMatchException(){
            super(ErrorCode.PASSWORD_NOT_MATCH,"密码和确认密码不一致");
        }
    }
}
