package com.leo.airouterbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.async")
public class AppAsyncProperties {

    private int corePoolSize = 8;

    private int maxPoolSize = 32;

    private int queueCapacity = 200;

    private long requestTimeoutMillis = 60000L;
}
