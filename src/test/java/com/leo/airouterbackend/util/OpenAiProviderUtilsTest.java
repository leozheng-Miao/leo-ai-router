package com.leo.airouterbackend.util;

import com.leo.airouterbackend.model.entity.ModelProvider;
import org.junit.jupiter.api.Test;
import reactor.netty.transport.ProxyProvider;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiProviderUtilsTest {

    @Test
    void shouldNormalizeOpenAiBaseUrl() {
        assertEquals("https://api.openai.com", OpenAiProviderUtils.normalizeBaseUrl("https://api.openai.com/v1"));
        assertEquals("https://api.openai.com", OpenAiProviderUtils.normalizeBaseUrl("https://api.openai.com/"));
    }

    @Test
    void shouldNormalizeImageFields() {
        assertEquals("1024x1024", OpenAiProviderUtils.normalizeImageSize("1024*1024"));
        assertEquals("medium", OpenAiProviderUtils.normalizeImageQuality("standard"));
        assertEquals("high", OpenAiProviderUtils.normalizeImageQuality("hd"));
    }

    @Test
    void shouldIdentifyOpenAiProviders() {
        assertTrue(OpenAiProviderUtils.isOpenAiProvider("openai"));
        assertTrue(OpenAiProviderUtils.isOpenAiProvider("gpt"));
    }

    @Test
    void shouldResolveTransportConfigFromProviderConfig() {
        ModelProvider provider = ModelProvider.builder()
                .config("""
                        {
                          "connectTimeoutMillis": 45000,
                          "responseTimeoutSeconds": 180,
                          "proxyType": "socks5",
                          "proxyHost": "127.0.0.1",
                          "proxyPort": 7890,
                          "proxyUsername": "user",
                          "proxyPassword": "pass"
                        }
                        """)
                .build();

        OpenAiProviderUtils.OpenAiTransportConfig config =
                OpenAiProviderUtils.resolveTransportConfig(provider, Map.of());

        assertEquals(45000, config.connectTimeoutMillis());
        assertEquals(180, config.responseTimeoutSeconds());
        assertNotNull(config.proxySettings());
        assertEquals(ProxyProvider.Proxy.SOCKS5, config.proxySettings().type());
        assertEquals("127.0.0.1", config.proxySettings().host());
        assertEquals(7890, config.proxySettings().port());
    }

    @Test
    void shouldResolveTransportConfigFromEnvironmentProxy() {
        OpenAiProviderUtils.OpenAiTransportConfig config =
                OpenAiProviderUtils.resolveTransportConfig(ModelProvider.builder().build(),
                        Map.of("HTTPS_PROXY", "http://proxy.local:8081"));

        assertNotNull(config.proxySettings());
        assertEquals(ProxyProvider.Proxy.HTTP, config.proxySettings().type());
        assertEquals("proxy.local", config.proxySettings().host());
        assertEquals(8081, config.proxySettings().port());
    }
}
