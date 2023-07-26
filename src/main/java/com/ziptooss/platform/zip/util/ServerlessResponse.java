package com.ziptooss.platform.zip.util;

import lombok.Data;

import java.io.Serializable;

/**
 * @author yukun.yan
 * @description Response
 * @date 2023/5/17 14:48
 */
@Data
public class ServerlessResponse<T> implements Serializable {

    private static final long serialVersionUID = -711336860613266038L;

    /**
     * 成功
     */
    private static final int SUCCESS = 200;

    /**
     * 失败
     */
    private static final int FAIL = 500;

    private int code;

    private String msg;

    private T data;

    public static <T> ServerlessResponse<T> ok() {
        return restResult(null, SUCCESS, null);
    }

    public static <T> ServerlessResponse<T> ok(T data) {
        return restResult(data, SUCCESS, null);
    }

    public static <T> ServerlessResponse<T> ok(T data, String msg) {
        return restResult(data, SUCCESS, msg);
    }

    public static <T> ServerlessResponse<T> fail() {
        return restResult(null, FAIL, null);
    }

    public static <T> ServerlessResponse<T> fail(String msg) {
        return restResult(null, FAIL, msg);
    }

    public static <T> ServerlessResponse<T> fail(T data) {
        return restResult(data, FAIL, null);
    }

    public static <T> ServerlessResponse<T> fail(T data, String msg) {
        return restResult(data, FAIL, msg);
    }

    public static <T> ServerlessResponse<T> fail(int code, String msg) {
        return restResult(null, code, msg);
    }

    private static <T> ServerlessResponse<T> restResult(T data, int code, String msg) {
        ServerlessResponse<T> apiResult = new ServerlessResponse<>();
        apiResult.setCode(code);
        apiResult.setData(data);
        apiResult.setMsg(msg);
        return apiResult;
    }

}
