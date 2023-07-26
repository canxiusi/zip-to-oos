package com.ziptooss.platform.zip.api;

import lombok.Data;

import java.util.List;

/**
 * @author yukun.yan
 * @description ZipRequest
 * @date 2023/7/19 19:14
 */
@Data
public class ZipRequest {

    /**
     * 文件在oss的全路径
     */
    private List<String> ossFilePathList;

    /**
     * 调用方系统，需要授权
     */
    private String bizSource;

    /**
     * 任务id
     */
    private Long taskId;

    /**
     * 回调系统的地址
     */
    private String callbackUrl;

    /**
     * 是否需要回调
     */
    private Boolean isNeedCallback;

}
