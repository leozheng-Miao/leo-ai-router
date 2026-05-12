package com.leo.airouterbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tencent.sms")
public class TencentSmsProperties {

    private boolean enabled = false;
    private String secretId;
    private String secretKey;
    private String region = "ap-guangzhou";
    private String sdkAppId;
    private String signName;
    private String loginTemplateId;
    private boolean localCodeVisible = true;
    private int codeExpireMinutes = 5;
    private int codeCoolDownSeconds = 60;
}
