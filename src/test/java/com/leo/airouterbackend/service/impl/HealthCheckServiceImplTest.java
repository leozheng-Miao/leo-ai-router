package com.leo.airouterbackend.service.impl;

import com.leo.airouterbackend.model.entity.ModelProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthCheckServiceImplTest {

    private final HealthCheckServiceImpl service = new HealthCheckServiceImpl();

    @Test
    void shouldBuildOpenAiHealthCheckUrlFromRootBaseUrl() {
        ModelProvider provider = ModelProvider.builder()
                .providerName("openai")
                .baseUrl("https://api.openai.com")
                .build();

        assertEquals("https://api.openai.com/v1/models", service.buildHealthCheckUrl(provider));
    }

    @Test
    void shouldBuildOpenAiHealthCheckUrlFromV1BaseUrl() {
        ModelProvider provider = ModelProvider.builder()
                .providerName("openai")
                .baseUrl("https://api.openai.com/v1")
                .build();

        assertEquals("https://api.openai.com/v1/models", service.buildHealthCheckUrl(provider));
    }

    @Test
    void shouldKeepLegacyProviderHealthCheckUrlShape() {
        ModelProvider provider = ModelProvider.builder()
                .providerName("qwen")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode")
                .build();

        assertEquals("https://dashscope.aliyuncs.com/compatible-mode/models", service.buildHealthCheckUrl(provider));
    }
}
