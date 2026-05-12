package com.leo.airouterbackend.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leo.airouterbackend.config.AuthProperties;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.model.entity.User;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtils {

    private static final String ALG = "HS256";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    @Resource
    private AuthProperties authProperties;

    @Resource
    private ObjectMapper objectMapper;

    public String createAccessToken(User user, List<String> roles) {
        long now = Instant.now().getEpochSecond();
        long expiresAt = now + authProperties.getJwt().getAccessTokenTtlSeconds();
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("typ", "JWT");
        header.put("alg", ALG);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", authProperties.getJwt().getIssuer());
        payload.put("sub", String.valueOf(user.getId()));
        payload.put("jti", UUID.randomUUID().toString().replace("-", ""));
        payload.put("iat", now);
        payload.put("exp", expiresAt);
        payload.put("tokenVersion", user.getTokenVersion() == null ? 0 : user.getTokenVersion());
        payload.put("roles", roles == null ? List.of() : roles);

        try {
            String unsigned = encodeJson(header) + "." + encodeJson(payload);
            return unsigned + "." + sign(unsigned);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成登录凭证失败");
        }
    }

    public JwtClaims parseAccessToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "登录凭证格式错误");
            }
            String unsigned = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(unsigned).getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "登录凭证签名无效");
            }
            Map<String, Object> payload = objectMapper.readValue(URL_DECODER.decode(parts[1]), new TypeReference<>() {
            });
            long expiresAt = ((Number) payload.get("exp")).longValue();
            if (expiresAt <= Instant.now().getEpochSecond()) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "登录凭证已过期");
            }
            return JwtClaims.builder()
                    .userId(Long.valueOf(String.valueOf(payload.get("sub"))))
                    .jti(String.valueOf(payload.get("jti")))
                    .tokenVersion(((Number) payload.getOrDefault("tokenVersion", 0)).intValue())
                    .roles((List<String>) payload.getOrDefault("roles", List.of()))
                    .issuedAt(((Number) payload.get("iat")).longValue())
                    .expiresAt(expiresAt)
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "登录凭证无效");
        }
    }

    public String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return bytesToHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "摘要计算失败");
        }
    }

    private String encodeJson(Object value) throws Exception {
        return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(authProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
