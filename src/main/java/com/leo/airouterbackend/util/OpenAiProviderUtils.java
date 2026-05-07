package com.leo.airouterbackend.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.leo.airouterbackend.model.entity.ModelProvider;
import io.netty.channel.ChannelOption;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.time.Duration;

/**
 * OpenAI 提供者相关工具类
 */
public final class OpenAiProviderUtils {

    private static final Set<String> OPENAI_PROVIDER_NAMES = Set.of("openai", "gpt");
    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 30_000;
    private static final int DEFAULT_RESPONSE_TIMEOUT_SECONDS = 90;

    private OpenAiProviderUtils() {
    }

    public static boolean isOpenAiProvider(String providerName) {
        return StrUtil.isNotBlank(providerName)
                && OPENAI_PROVIDER_NAMES.contains(providerName.trim().toLowerCase(Locale.ROOT));
    }

    public static String normalizeBaseUrl(String baseUrl) {
        String trimmed = trimTrailingSlash(baseUrl);
        if (StrUtil.endWithIgnoreCase(trimmed, "/v1")) {
            return StrUtil.removeSuffixIgnoreCase(trimmed, "/v1");
        }
        return trimmed;
    }

    public static String buildModelsUrl(String baseUrl) {
        return normalizeBaseUrl(baseUrl) + "/v1/models";
    }

    public static String normalizeImageSize(String size) {
        if (StrUtil.isBlank(size)) {
            return null;
        }
        return size.trim().replace('*', 'x').toLowerCase(Locale.ROOT);
    }

    public static String normalizeImageQuality(String quality) {
        if (StrUtil.isBlank(quality)) {
            return null;
        }
        return switch (quality.trim().toLowerCase(Locale.ROOT)) {
            case "standard" -> "medium";
            case "hd" -> "high";
            case "low", "medium", "high" -> quality.trim().toLowerCase(Locale.ROOT);
            default -> quality.trim().toLowerCase(Locale.ROOT);
        };
    }

    public static String trimTrailingSlash(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        return StrUtil.removeSuffix(value.trim(), "/");
    }

    public static HttpClient buildReactorHttpClient(ModelProvider provider) {
        OpenAiTransportConfig transportConfig = resolveTransportConfig(provider);
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, transportConfig.connectTimeoutMillis())
                .responseTimeout(Duration.ofSeconds(transportConfig.responseTimeoutSeconds()));

        ProxySettings proxySettings = transportConfig.proxySettings();
        if (proxySettings != null && proxySettings.enabled()) {
            httpClient = httpClient.proxy(spec -> {
                ProxyProvider.Builder builder = spec.type(proxySettings.type())
                        .host(proxySettings.host())
                        .port(proxySettings.port());
                if (StrUtil.isNotBlank(proxySettings.username())) {
                    builder = builder.username(proxySettings.username());
                }
                if (StrUtil.isNotBlank(proxySettings.password())) {
                    builder = builder.password(ignored -> proxySettings.password());
                }
            });
        }
        return httpClient;
    }

    public static RestClient.Builder buildRestClientBuilder(ModelProvider provider) {
        return RestClient.builder()
                .requestFactory(new ReactorClientHttpRequestFactory(buildReactorHttpClient(provider)));
    }

    public static WebClient.Builder buildWebClientBuilder(ModelProvider provider) {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(buildReactorHttpClient(provider)));
    }

    public static OpenAiTransportConfig resolveTransportConfig(ModelProvider provider) {
        return resolveTransportConfig(provider, System.getenv());
    }

    static OpenAiTransportConfig resolveTransportConfig(ModelProvider provider, Map<String, String> env) {
        JSONObject configJson = parseProviderConfig(provider);

        int connectTimeoutMillis = getInt(configJson, "connectTimeoutMillis", DEFAULT_CONNECT_TIMEOUT_MILLIS);
        if (connectTimeoutMillis <= 0) {
            connectTimeoutMillis = DEFAULT_CONNECT_TIMEOUT_MILLIS;
        }

        int responseTimeoutSeconds = getInt(configJson, "responseTimeoutSeconds", DEFAULT_RESPONSE_TIMEOUT_SECONDS);
        if (responseTimeoutSeconds <= 0) {
            responseTimeoutSeconds = DEFAULT_RESPONSE_TIMEOUT_SECONDS;
        }

        ProxySettings proxySettings = resolveProxySettings(configJson);
        if (proxySettings == null || !proxySettings.enabled()) {
            proxySettings = resolveProxySettingsFromEnv(env);
        }

        return new OpenAiTransportConfig(connectTimeoutMillis, responseTimeoutSeconds, proxySettings);
    }

    private static JSONObject parseProviderConfig(ModelProvider provider) {
        if (provider == null || StrUtil.isBlank(provider.getConfig())) {
            return null;
        }
        try {
            return JSONUtil.parseObj(provider.getConfig());
        } catch (Exception e) {
            return null;
        }
    }

    private static int getInt(JSONObject jsonObject, String key, int defaultValue) {
        if (jsonObject == null) {
            return defaultValue;
        }
        Integer value = jsonObject.getInt(key);
        return value == null ? defaultValue : value;
    }

    private static ProxySettings resolveProxySettings(JSONObject configJson) {
        if (configJson == null) {
            return null;
        }
        String host = configJson.getStr("proxyHost");
        Integer port = configJson.getInt("proxyPort");
        String proxyType = configJson.getStr("proxyType", "http");
        String username = configJson.getStr("proxyUsername");
        String password = configJson.getStr("proxyPassword");
        return buildProxySettings(proxyType, host, port, username, password);
    }

    private static ProxySettings resolveProxySettingsFromEnv(Map<String, String> env) {
        if (env == null || env.isEmpty()) {
            return null;
        }
        String proxyUrl = firstNonBlank(env.get("HTTPS_PROXY"), env.get("https_proxy"), env.get("HTTP_PROXY"),
                env.get("http_proxy"), env.get("ALL_PROXY"), env.get("all_proxy"));
        if (StrUtil.isBlank(proxyUrl)) {
            return null;
        }
        try {
            URI uri = URI.create(proxyUrl.trim());
            String scheme = StrUtil.blankToDefault(uri.getScheme(), "http");
            int port = uri.getPort();
            if (port <= 0) {
                port = defaultPortForScheme(scheme);
            }
            String username = null;
            String password = null;
            if (StrUtil.isNotBlank(uri.getUserInfo())) {
                String[] parts = uri.getUserInfo().split(":", 2);
                username = parts[0];
                password = parts.length > 1 ? parts[1] : null;
            }
            return buildProxySettings(scheme, uri.getHost(), port, username, password);
        } catch (Exception e) {
            return null;
        }
    }

    private static ProxySettings buildProxySettings(String proxyType, String host, Integer port, String username, String password) {
        if (StrUtil.isBlank(host) || port == null || port <= 0) {
            return null;
        }
        return new ProxySettings(resolveProxyType(proxyType), host.trim(), port, StrUtil.emptyToNull(username),
                StrUtil.emptyToNull(password));
    }

    private static ProxyProvider.Proxy resolveProxyType(String proxyType) {
        if (StrUtil.isBlank(proxyType)) {
            return ProxyProvider.Proxy.HTTP;
        }
        return switch (proxyType.trim().toLowerCase(Locale.ROOT)) {
            case "socks4" -> ProxyProvider.Proxy.SOCKS4;
            case "socks5" -> ProxyProvider.Proxy.SOCKS5;
            default -> ProxyProvider.Proxy.HTTP;
        };
    }

    private static int defaultPortForScheme(String scheme) {
        return switch (scheme.trim().toLowerCase(Locale.ROOT)) {
            case "http" -> 80;
            case "https" -> 443;
            case "socks4", "socks5" -> 1080;
            default -> 8080;
        };
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    public record OpenAiTransportConfig(int connectTimeoutMillis, int responseTimeoutSeconds,
                                        ProxySettings proxySettings) {
    }

    public record ProxySettings(ProxyProvider.Proxy type, String host, int port, String username, String password) {
        public boolean enabled() {
            return StrUtil.isNotBlank(host) && port > 0;
        }
    }
}
