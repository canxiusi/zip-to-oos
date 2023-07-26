package com.ziptooss.platform.zip.config;

import lombok.Data;

/**
 * @author yukun.yan
 * @description ServerlessOssClientConfigProperties
 * @date 2023/7/20 12:09
 */
@Data
public class OssClientConfig {

    private String type;

    private String accessKeyId;

    private String accessKeySecret;

    private String roleArn;

    private Long durationSeconds;

    private String filePath;

    private String bucket;

    private String region;

    private String regionId;

}
