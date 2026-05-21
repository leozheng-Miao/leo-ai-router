package com.leo.airouterbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wechat.oauth")
public class WechatOAuthProperties {

    private boolean enabled = false;
    private String appId;
    private String appSecret;
    private String redirectUri;
    private String frontendSuccessUrl;
    private long stateTtlSeconds = 300L;
}
