package com.leo.airouter.sdk;

import com.leo.airouter.sdk.callback.StreamCallback;
import com.leo.airouter.sdk.config.ClientConfig;
import com.leo.airouter.sdk.http.HttpClient;
import com.leo.airouter.sdk.model.ChatRequest;
import com.leo.airouter.sdk.model.ChatResponse;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class LeoAIClient {

    private final ClientConfig config;
    private final HttpClient httpClient;

    private LeoAIClient(ClientConfig config) {
        this.config = config;
        this.config.validate();
        this.httpClient = new HttpClient(config);
        log.info("YuAIClient initialized with baseUrl: {}", config.getBaseUrl());
    }

    /**
     * 创建客户端构建器
     */
    public static ClientConfigBuilder builder() {
        return new ClientConfigBuilder();
    }

    /**
     * 同步聊天 - 简单文本
     *
     * @param message 用户消息
     * @return 聊天响应
     */
    public ChatResponse chat(String message) {
        return chat(ChatRequest.simple(message));
    }

    /**
     * 同步聊天 - 指定模型
     *
     * @param model   模型名称
     * @param message 用户消息
     * @return 聊天响应
     */
    public ChatResponse chat(String model, String message) {
        return chat(ChatRequest.withModel(model, message));
    }

    /**
     * 同步聊天 - 完整请求
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    public ChatResponse chat(ChatRequest request) {
        log.debug("Sending chat request: {}", request);
        ChatResponse response = httpClient.chat(request);
        log.debug("Received chat response: {}", response);
        return response;
    }

    /**
     * 流式聊天 - 简单文本
     *
     * @param message  用户消息
     * @param callback 流式回调
     */
    public void chatStream(String message, StreamCallback callback) {
        chatStream(ChatRequest.simple(message), callback);
    }

    /**
     * 流式聊天 - 指定模型
     *
     * @param model    模型名称
     * @param message  用户消息
     * @param callback 流式回调
     */
    public void chatStream(String model, String message, StreamCallback callback) {
        chatStream(ChatRequest.withModel(model, message), callback);
    }

    /**
     * 流式聊天 - 完整请求
     *
     * @param request  聊天请求
     * @param callback 流式回调
     */
    public void chatStream(ChatRequest request, StreamCallback callback) {
        log.debug("Sending stream chat request: {}", request);
        httpClient.chatStream(request, callback);
    }

    /**
     * 关闭客户端，释放资源
     */
    public void close() {
        httpClient.close();
        log.info("YuAIClient closed");
    }

    /**
     * 客户端配置构建器
     */
    public static class ClientConfigBuilder {
        private String apiKey;
        private String baseUrl = "http://localhost:8123/api";
        private Integer connectTimeout = 10000;
        private Integer readTimeout = 30000;
        private Integer writeTimeout = 30000;
        private Integer maxRetries = 3;
        private Integer retryDelay = 1000;

        public ClientConfigBuilder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public ClientConfigBuilder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public ClientConfigBuilder connectTimeout(Integer connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public ClientConfigBuilder readTimeout(Integer readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public ClientConfigBuilder writeTimeout(Integer writeTimeout) {
            this.writeTimeout = writeTimeout;
            return this;
        }

        public ClientConfigBuilder maxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public ClientConfigBuilder retryDelay(Integer retryDelay) {
            this.retryDelay = retryDelay;
            return this;
        }

        public LeoAIClient build() {
            ClientConfig config = ClientConfig.builder()
                    .apiKey(apiKey)
                    .baseUrl(baseUrl)
                    .connectTimeout(connectTimeout)
                    .readTimeout(readTimeout)
                    .writeTimeout(writeTimeout)
                    .maxRetries(maxRetries)
                    .retryDelay(retryDelay)
                    .build();
            return new LeoAIClient(config);
        }
    }
}