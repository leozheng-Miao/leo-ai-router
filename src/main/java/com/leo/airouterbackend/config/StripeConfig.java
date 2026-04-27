package com.leo.airouterbackend.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConfigurationProperties(prefix = "stripe")
@Data
@Slf4j
public class StripeConfig {

    /**
     * Stripe API 密钥
     */
    private String apiKey;

    /**
     * Webhook 签名密钥
     */
    private String webhookSecret;

    /**
     * 支付成功回调URL
     */
    private String successUrl;

    /**
     * 支付取消回调URL
     */
    private String cancelUrl;

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(apiKey)) {
            log.warn("Stripe API 密钥未配置,请在 application.yml 中配置测试密钥");
            return;
        }
        Stripe.apiKey = apiKey;
        if (apiKey.startsWith("sk_test_")) {
            log.info("Stripe API 初始化完成 - 沙箱环境(测试模式)");
            log.info("支付成功回调URL: {}", successUrl);
            log.info("支付取消回调URL: {}", cancelUrl);
        } else {
            log.warn("检测到非测试密钥,请确保使用 sk_test_ 开头的测试密钥");
        }
    }
}
