package com.ziptooss.platform.zip.api;

import lombok.Builder;
import lombok.Data;

/**
 * @author yukun.yan
 * @description ServerlessCallbackRsp
 * @date 2023/7/23 17:20
 */
@Data
@Builder
public class ServerlessCallbackRsp {

    private Integer code;

    private String msg;

    /**
     * zip url
     */
    private String data;

    /**
     * 任务id
     */
    private Long taskId;

}
