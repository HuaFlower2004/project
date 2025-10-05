package com.mi.project.common;

import lombok.Data;
@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    public Result() {}
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    public Result(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
    public static <T> Result<T> success(T data){
        return new Result<>(200,"操作成功",data);
    }
    public static <T> Result<T> success(String message, T data){
        return new Result<>(200,message,data);
    }
    public static <T> Result<T> failure(Integer code,String message){
        return new Result<>(code,message,null);
    }
}
