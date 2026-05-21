package com.leo.airouterbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String frontendBaseUrl = "http://localhost:5173";

    public String buildFrontendUrl(String pathAndQuery) {
        String baseUrl = frontendBaseUrl == null ? "" : frontendBaseUrl.strip();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String path = pathAndQuery == null ? "" : pathAndQuery.strip();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return baseUrl + path;
    }
}
