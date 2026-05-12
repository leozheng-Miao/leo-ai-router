package com.leo.airouterbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private Jwt jwt = new Jwt();

    @Data
    public static class Jwt {
        private String secret;
        private String issuer;
        private long accessTokenTtlSeconds = 7200L;
        private long refreshTokenTtlSeconds = 2592000L;
    }
}