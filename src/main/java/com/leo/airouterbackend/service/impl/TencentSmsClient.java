package com.leo.airouterbackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leo.airouterbackend.config.TencentSmsProperties;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;

@Slf4j
@Component
public class TencentSmsClient {

    private static final String HOST = "sms.tencentcloudapi.com";
    private static final String SERVICE = "sms";
    private static final String VERSION = "2021-01-11";

    @Resource
    private TencentSmsProperties properties;

    @Resource
    private ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void sendLoginCode(String phone, String code) {
        if (!properties.isEnabled()) {
            log.info("腾讯云短信未启用，手机号 {} 的登录验证码为 {}", phone, code);
            return;
        }
        if (StrUtil.hasBlank(properties.getSecretId(), properties.getSecretKey(), properties.getSdkAppId(),
                properties.getSignName(), properties.getLoginTemplateId())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "腾讯云短信配置不完整");
        }
        try {
            Map<String, Object> payload = Map.of(
                    "PhoneNumberSet", new String[]{"+86" + phone},
                    "SmsSdkAppId", properties.getSdkAppId(),
                    "SignName", properties.getSignName(),
                    "TemplateId", properties.getLoginTemplateId(),
                    "TemplateParamSet", new String[]{code, String.valueOf(properties.getCodeExpireMinutes())}
            );
            String body = objectMapper.writeValueAsString(payload);
            long timestamp = Instant.now().getEpochSecond();
            String date = LocalDate.ofInstant(Instant.ofEpochSecond(timestamp), ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_DATE);
            String authorization = buildAuthorization(body, timestamp, date);
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://" + HOST))
                    .header("Authorization", authorization)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Host", HOST)
                    .header("X-TC-Action", "SendSms")
                    .header("X-TC-Version", VERSION)
                    .header("X-TC-Timestamp", String.valueOf(timestamp))
                    .header("X-TC-Region", properties.getRegion())
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300 || response.body().contains("\"Error\"")) {
                log.error("腾讯云短信发送失败：status={}, body={}", response.statusCode(), response.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "短信发送失败，请稍后重试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("腾讯云短信发送异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "短信发送失败，请稍后重试");
        }
    }

    private String buildAuthorization(String payload, long timestamp, String date) throws Exception {
        String canonicalRequest = "POST\n/\n\ncontent-type:application/json; charset=utf-8\nhost:" + HOST + "\n\ncontent-type;host\n" + sha256(payload);
        String credentialScope = date + "/" + SERVICE + "/tc3_request";
        String stringToSign = "TC3-HMAC-SHA256\n" + timestamp + "\n" + credentialScope + "\n" + sha256(canonicalRequest);
        byte[] secretDate = hmac(("TC3" + properties.getSecretKey()).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmac(secretDate, SERVICE);
        byte[] secretSigning = hmac(secretService, "tc3_request");
        String signature = HexFormat.of().formatHex(hmac(secretSigning, stringToSign));
        return "TC3-HMAC-SHA256 Credential=" + properties.getSecretId() + "/" + credentialScope
                + ", SignedHeaders=content-type;host, Signature=" + signature;
    }

    private String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private byte[] hmac(byte[] key, String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }
}
