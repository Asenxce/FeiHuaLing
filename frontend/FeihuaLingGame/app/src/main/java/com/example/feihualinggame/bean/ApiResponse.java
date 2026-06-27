package com.example.feihualinggame.bean;

/**
 * 统一API响应封装
 */
public class ApiResponse<T> {
    private int code;           // 状态码: 200-成功, 其他-失败
    private String message;     // 响应消息
    private T data;            // 响应数据

    public ApiResponse() {}

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // Getter & Setter
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
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

    /**
     * 判断请求是否成功
     */
    public boolean isSuccess() {
        return code == 200;
    }
}
