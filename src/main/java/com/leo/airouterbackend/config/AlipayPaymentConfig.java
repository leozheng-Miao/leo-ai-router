package com.leo.airouterbackend.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "payment.alipay")
@Data
@Slf4j
public class AlipayPaymentConfig {

    private String appId;

    private String appPrivateKey;

    private String alipayPublicKey;

    private String gatewayUrl;

    private String format = "json";

    private String charset = "UTF-8";

    private String signType = "RSA2";

    private String notifyUrl;

    private String returnUrl;

    private String frontendSuccessUrl;

    private String frontendCancelUrl;

    @Bean
    public AlipayClient alipayClient() {
        log.info("支付宝沙箱网关：{}", gatewayUrl);
        log.info("支付宝异步通知地址：{}", notifyUrl);
        log.info("支付宝同步回跳地址：{}", returnUrl);
        return new DefaultAlipayClient(
                gatewayUrl,
                appId,
                appPrivateKey,
                format,
                charset,
                alipayPublicKey,
                signType
        );
    }
}