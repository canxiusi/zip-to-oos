package com.ziptooss.platform.zip.service;

import com.alibaba.fastjson.JSONObject;
import com.ziptooss.platform.zip.api.ServerlessCallbackRsp;
import com.ziptooss.platform.zip.api.ZipRequest;
import com.ziptooss.platform.zip.util.ComEnum;
import com.ziptooss.platform.zip.util.OkHttpUtils;
import com.ziptooss.platform.zip.util.OssUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * @author yukun.yan
 * @description ZipServiceImpl
 * @date 2023/7/19 19:14
 */
@Slf4j
@Service
public class ZipServiceImpl implements ZipService {

    /**
     * 通过文件路径从oss下载, 并压缩, 再把压缩包上传到oss, 返回zip的oss的url
     *
     * @param zipRequest
     * @return
     */
    @Override
    public void zipToOss(ZipRequest zipRequest) {
        List<String> ossFilePathList = zipRequest.getOssFilePathList();
        if (CollectionUtils.isEmpty(ossFilePathList)) {
            log.info("ZipServiceImpl get ossUrl is null");
            return;
        }
        try {
            String zipOssUrl = OssUtils.putZipToOss(ossFilePathList, zipRequest.getBizSource());
            log.info("[ZipServiceImpl] zipToOss fetch OSS zip Url={}", zipOssUrl);
            ServerlessCallbackRsp callbackRsp = ServerlessCallbackRsp.builder()
                    .code(ComEnum.RspEnum.success.getCode()).msg(ComEnum.RspEnum.success.getMsg())
                    .data(zipOssUrl).taskId(zipRequest.getTaskId())
                    .build();
            String body = JSONObject.toJSONString(callbackRsp);
            // 回调网盘地址
            Request request = new Request.Builder()
                    .url(zipRequest.getCallbackUrl())
                    .post(okhttp3.RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body))
                    .build();
            OkHttpUtils.getInstance().newCall(request).close();
            log.info("[ZipServiceImpl] zipToOss callback success, request={}", request);
        } catch (Throwable t) {
            // 压缩打包错误或回调出错
            errorCallback(zipRequest, t);
            // 全局异常处理器就不写了, 直接抛出来吧
            throw t;
        }
    }

    /**
     * 通过文件路径从oss下载, 并压缩, 再把压缩包写入到临时文件中, 返回文件路径
     *
     * @param zipRequest
     * @return
     */
    @Override
    public void zipToTempFile(ZipRequest zipRequest) {
        List<String> ossFilePathList = zipRequest.getOssFilePathList();
        if (CollectionUtils.isEmpty(ossFilePathList)) {
            log.info(" ZipServiceImpl get ossUrl is null");
            return;
        }
        try {
            String zipTempFilePath = OssUtils.putZipToTempFile(ossFilePathList, zipRequest.getBizSource());
            log.info("[ZipServiceImpl] zipToTempFile fetch zip temp file path={}", zipTempFilePath);
            // 获取到zipTempFilePath说明文件处理完成, 返回网盘系统路径, 也不能立即重定向, 因为大文件慢...可能请求超时, 所以这个请求需要是异步的
            ServerlessCallbackRsp callbackRsp = ServerlessCallbackRsp.builder()
                    .code(ComEnum.RspEnum.success.getCode()).msg(ComEnum.RspEnum.success.getMsg())
                    .data(zipTempFilePath).taskId(zipRequest.getTaskId())
                    .build();
            String body = JSONObject.toJSONString(callbackRsp);
            Request request = new Request.Builder()
                    .url(zipRequest.getCallbackUrl())
                    .post(okhttp3.RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body))
                    .build();
            OkHttpUtils.getInstance().newCall(request).close();
            log.info("[ZipServiceImpl] zipToTempFile callback success, request={}", request);
        } catch (Throwable t) {
            errorCallback(zipRequest, t);
            throw t;
        }
    }

    /**
     * 根据文件路径从serverless磁盘下载文件
     *
     * @param filePath
     * @param response
     */
    @Override
    public void streamDownload(String filePath, HttpServletResponse response) {
        log.info("[ZipServiceImpl] streamDownload filePath={}", filePath);
        File file = new File(filePath);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + file.getName());
        response.setHeader("Content-Length", String.valueOf(file.length()));
        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        } catch (Exception e) {
            log.error("[ZipServiceImpl] streamDownload error, ", e);
            throw new RuntimeException(e);
        } finally {
            file.deleteOnExit();
        }
    }

    /**
     * 处理失败回调
     *
     * @param zipRequest
     * @param t
     */
    private void errorCallback(ZipRequest zipRequest, Throwable t) {
        log.error("[ZipServiceImpl] callback error, zipRequest={}, e={}", zipRequest, t.getStackTrace());
        // 发送错误回调, 通知网盘系统删除任务数据, 终止下载, 想着换成消息队列
        ServerlessCallbackRsp callbackRsp = ServerlessCallbackRsp.builder()
                .code(ComEnum.RspEnum.error.getCode()).msg(ComEnum.RspEnum.error.getMsg())
                .taskId(zipRequest.getTaskId())
                .build();
        String body = JSONObject.toJSONString(callbackRsp);
        Request request = new Request.Builder()
                .url(zipRequest.getCallbackUrl())
                .post(okhttp3.RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body))
                // 设置异步请求serverless, 通过回调获取oss的zipUrl
                .build();
        OkHttpUtils.getInstance().newCall(request).close();
    }

}
