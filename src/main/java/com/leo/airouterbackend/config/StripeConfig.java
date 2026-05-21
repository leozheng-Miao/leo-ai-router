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
            log.warn("Stripe API 密钥未配置，Stripe 支付将不可用");
            return;
        }
        Stripe.apiKey = apiKey;
        if (apiKey.startsWith("sk_test_")) {
            log.info("Stripe API 初始化完成 - 测试模式");
        } else if (apiKey.startsWith("sk_live_")) {
            log.info("Stripe API 初始化完成 - 生产模式");
        } else {
            log.warn("Stripe API 密钥格式无法识别，请确认配置正确");
        }
    }
}
