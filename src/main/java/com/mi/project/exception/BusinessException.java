package com.mi.project.exception;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BusinessException extends RuntimeException{

    private final Integer code;

    private final String message;

    public BusinessException(Integer code, String message){
        this.code = code;
        this.message = message;
    }

    public BusinessException(String message){
        this.code = 400;
        this.message = message;
    }
}
