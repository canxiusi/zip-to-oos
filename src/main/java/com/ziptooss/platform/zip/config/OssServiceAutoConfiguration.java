package com.ziptooss.platform.zip.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.CannedAccessControlList;
import com.ziptooss.platform.zip.util.OssUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

/**
 * @author yukun.yan
 * @description OssServiceAutoConfiguration
 * @date 2023/5/30 10:00
 */
@Slf4j
@Configuration
@ConditionalOnBean(OssClientConfigBean.class)
public class OssServiceAutoConfiguration {

    /**
     * OssService注入到容器中
     *
     * @return
     */
    @Bean(destroyMethod = "destroy")
    public OssUtils ossUtils(final OssClientConfigBean clientConfigBean) {
        OssClientConfig ossClientConfig = clientConfigBean.getOss().get(0);
        configVerify(ossClientConfig);
        String bucketName = ossClientConfig.getBucket();
        String region = ossClientConfig.getRegion();
        String endpoint = "https://" + ossClientConfig.getRegion() + ".aliyuncs.com";
        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, ossClientConfig.getAccessKeyId(), ossClientConfig.getAccessKeySecret());
            ossClient.setBucketAcl(bucketName, CannedAccessControlList.PublicRead);
        } catch (Exception e) {
            log.error("[OssClientAutoConfiguration] OssClient init error, ", e);
            System.exit(1);
        }
        OssUtils ossUtils = new OssUtils(ossClient, bucketName, region, endpoint);
        log.info("[OssClientAutoConfiguration] properties={}", clientConfigBean.getOss());
        return ossUtils;
    }

    private void configVerify(OssClientConfig ossClientConfig) {
        Assert.notNull(ossClientConfig.getRegion(), "Region not be null");
        Assert.notNull(ossClientConfig.getAccessKeyId(), "AccessKeyId not be null");
        Assert.notNull(ossClientConfig.getAccessKeySecret(), "AccessKeySecret not be null");
    }

}
