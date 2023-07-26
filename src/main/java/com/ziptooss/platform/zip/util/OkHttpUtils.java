package com.ziptooss.platform.zip.util;

import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import okhttp3.Response;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author yukun.yan
 * @description OkHttpUtils
 * @date 2023/5/29 11:38
 */
public class OkHttpUtils {

    /**
     * 单例
     */
    private final OkHttpClient client;

    public OkHttpUtils() {
        client = new OkHttpClient.Builder()
                .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(1000, 2000, TimeUnit.MINUTES))
                .build();
    }

    /**
     * 同步执行一个http请求, 返回 ResponseBody
     *
     * @param request
     * @return
     */
    public Response newCall(Request request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        Response response;
        try {
            response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new RuntimeException("Unexpected response code");
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new RuntimeException("get response body is null");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return response;
    }

    /**
     * 解析结果为json
     *
     * @param newCall
     * @return
     */
    public JSONObject parseResult(okhttp3.Response newCall) {
        try {
            String result = newCall.body().string();
            return (JSONObject) JSONObject.parse(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 静态内部类机制来保证线程安全性
     */
    private static class OkHttpUtilsInstanceOwner {

        private static final OkHttpUtils INSTANCE = new OkHttpUtils();

    }

    /**
     * 返回该类的单例对象, 并自动初始化构造器
     *
     * @return
     */
    public static OkHttpUtils getInstance() {

        return OkHttpUtilsInstanceOwner.INSTANCE;
    }

    public OkHttpClient getClient() {
        return client;
    }

}
