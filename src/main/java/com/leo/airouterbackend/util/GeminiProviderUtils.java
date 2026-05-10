package com.leo.airouterbackend.util;

import cn.hutool.core.util.StrUtil;

import java.util.Locale;
import java.util.Set;

/**
 * Gemini 提供者相关工具类
 */
public final class GeminiProviderUtils {

    private static final Set<String> GEMINI_PROVIDER_NAMES = Set.of("gemini", "google");

    private GeminiProviderUtils() {
    }

    public static boolean isGeminiProvider(String providerName) {
        return StrUtil.isNotBlank(providerName)
                && GEMINI_PROVIDER_NAMES.contains(providerName.trim().toLowerCase(Locale.ROOT));
    }

    public static String normalizeBaseUrl(String baseUrl) {
        String trimmed = OpenAiProviderUtils.trimTrailingSlash(baseUrl);
        if (StrUtil.endWithIgnoreCase(trimmed, "/v1beta")) {
            return StrUtil.removeSuffixIgnoreCase(trimmed, "/v1beta");
        }
        if (StrUtil.endWithIgnoreCase(trimmed, "/v1")) {
            return StrUtil.removeSuffixIgnoreCase(trimmed, "/v1");
        }
        return trimmed;
    }

    public static String buildModelsUrl(String baseUrl) {
        return normalizeBaseUrl(baseUrl) + "/v1beta/models";
    }
}
