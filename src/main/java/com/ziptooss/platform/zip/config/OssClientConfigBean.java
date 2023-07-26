package com.ziptooss.platform.zip.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @author yukun.yan
 * @description OssClientConfigBean
 * @date 2023/7/20 12:43
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "ali")
@PropertySource("classpath:application.properties")
public class OssClientConfigBean {

    private List<OssClientConfig> oss;

    @PostConstruct
    public void testWechatConfig() {
        if (CollectionUtils.isEmpty(oss)) {
            log.error("get oss config is empty, application stop");
            System.exit(1);
        }
    }

}
