package com.ai.exam.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一API响应结果")
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public static <T> Result<T> success(T data){
        Result<T> result=new Result<T>();
        result.setData(data);
        result.setCode(200);
        result.setMessage("操作成功");
        return result;
    }

    public static <T> Result<T> success(T data,String message){
        Result<T> result=new Result<T>();
        result.setData(data);
        result.setCode(200);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> success(String message){
        Result<T> result=new Result<T>();
        result.setCode(200);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> error(String message){
        Result<T> result=new Result<T>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> error(Integer code,String message){
        Result<T> result=new Result<T>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
