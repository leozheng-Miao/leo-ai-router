package com.leo.airouterbackend.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiProviderUtilsTest {

    @Test
    void shouldNormalizeBaseUrl() {
        assertEquals("https://generativelanguage.googleapis.com",
                GeminiProviderUtils.normalizeBaseUrl("https://generativelanguage.googleapis.com/v1beta"));
        assertEquals("https://generativelanguage.googleapis.com",
                GeminiProviderUtils.normalizeBaseUrl("https://generativelanguage.googleapis.com/"));
    }

    @Test
    void shouldBuildModelsUrl() {
        assertEquals("https://generativelanguage.googleapis.com/v1beta/models",
                GeminiProviderUtils.buildModelsUrl("https://generativelanguage.googleapis.com"));
    }

    @Test
    void shouldSupportGeminiProviders() {
        assertTrue(GeminiProviderUtils.isGeminiProvider("gemini"));
        assertTrue(GeminiProviderUtils.isGeminiProvider("google"));
        assertFalse(GeminiProviderUtils.isGeminiProvider("openai"));
    }
}
