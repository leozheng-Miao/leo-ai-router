package com.leo.airouterbackend.adaptor;


import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.model.dto.chat.ChatRequest;
import com.leo.airouterbackend.model.dto.chat.StreamChunk;
import com.leo.airouterbackend.model.entity.Model;
import com.leo.airouterbackend.model.entity.ModelProvider;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DeepSeek 模型适配器
 * 使用 spring-ai-starter-model-deepseek 依赖
 */
@Slf4j
@Component
public class DeepSeekAdapter implements ModelAdapter {

    /**
     * 支持的提供者名称
     */
    private static final Set<String> SUPPORTED_PROVIDERS = Set.of("deepseek");

    @Override
    public ChatResponse invoke(Model model, ModelProvider provider, ChatRequest chatRequest) {
        DeepSeekChatModel chatModel = createChatModel(provider, model);
        Prompt prompt = buildPrompt(chatRequest, model);
        try {
            return chatModel.call(prompt);
        } catch (Exception e) {
            log.error("DeepSeek 适配器调用失败，模型: {}, baseUrl: {}, apiKey前缀: {}",
                    model.getModelKey(),
                    provider.getBaseUrl(),
                    StringUtils.left(provider.getApiKey(), 10) + "...",
                    e);   // 把异常对象作为最后一个参数传入，会打印完整堆栈
            Throwable cause = e.getCause();
            String errorDetail = cause != null ? cause.toString() : e.toString();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用模型失败: " + errorDetail);
        }
    }

    @Override
    public Flux<ChatResponse> invokeStream(Model model, ModelProvider provider, ChatRequest chatRequest) {
        try {
            DeepSeekChatModel chatModel = createChatModel(provider, model);
            Prompt prompt = buildPrompt(chatRequest, model);
            return chatModel.stream(prompt);
        } catch (Exception e) {
            log.error("DeepSeek 适配器流式调用模型 {} 失败", model.getModelKey(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "流式调用模型失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<StreamChunk> invokeStreamChunk(Model model, ModelProvider provider, ChatRequest chatRequest) {
        return invokeStream(model, provider, chatRequest)
                .map(this::convertToStreamChunk)
                .filter(chunk -> !chunk.isEmpty());
    }

    /**
     * 将 DeepSeek 响应转换为统一的 StreamChunk 格式
     * DeepSeek 深度思考内容存储在 metadata 的 "reasoningContent" 字段
     */
    private StreamChunk convertToStreamChunk(ChatResponse response) {
        StreamChunk.StreamChunkBuilder builder = StreamChunk.builder();

        // 提取 Token 信息
        if (ObjectUtils.isNotEmpty(response.getMetadata()) && ObjectUtils.isNotEmpty(response.getMetadata().getUsage())) {
            Usage usage = response.getMetadata().getUsage();
            Integer promptTokens = usage.getPromptTokens();
            Integer completionTokens = usage.getCompletionTokens();
            if (promptTokens != null && promptTokens > 0) {
                builder.promptTokens(promptTokens);
            }
            if (completionTokens != null && completionTokens > 0) {
                builder.completionTokens(completionTokens);
            }
        }

        // 提取内容
        if (ObjectUtils.isNotEmpty(response.getResult()) && ObjectUtils.isNotEmpty(response.getResult().getOutput())) {
            // 提取深度思考内容 - DeepSeek 使用 reasoningContent 字段
            DeepSeekAssistantMessage assistantMessage = (DeepSeekAssistantMessage) response.getResult().getOutput();
            String reasoningContent = assistantMessage.getReasoningContent();
            if (StringUtils.isNotBlank(reasoningContent)) {
                builder.reasoningContent(reasoningContent);
            }

            // 提取普通文本内容
            String text = assistantMessage.getText();
            if (text != null && !text.isEmpty()) {
                builder.text(text);
            }
        }

        StreamChunk chunk = builder.build();
        // 如果没有任何内容，标记为空
        if (!chunk.hasText() && !chunk.hasReasoningContent()
                && chunk.getPromptTokens() == null && chunk.getCompletionTokens() == null) {
            return StreamChunk.empty();
        }
        return chunk;
    }

    @Override
    public boolean supports(String providerName) {
        return providerName != null && SUPPORTED_PROVIDERS.contains(providerName.toLowerCase());
    }

    /**
     * 创建 DeepSeek ChatModel
     */
    private DeepSeekChatModel createChatModel(ModelProvider provider, Model model) {
        // 创建具有明确超时配置的 HttpClient
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30_000)   // 连接超时 30 秒
                .responseTimeout(Duration.ofSeconds(90));               // 响应超时 90 秒

        // 构建自定义 RestClient
        RestClient.Builder restClientBuilder = RestClient.builder()
                .requestFactory(new ReactorClientHttpRequestFactory(httpClient));

        // 构建自定义 WebClient
        WebClient.Builder webClientBuilder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));

        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .baseUrl(provider.getBaseUrl())
                .apiKey(provider.getApiKey())
                .restClientBuilder(restClientBuilder)
                .webClientBuilder(webClientBuilder)
                .build();

        DeepSeekChatOptions options = DeepSeekChatOptions.builder()
                .model(model.getModelKey())
                .build();

        return DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .defaultOptions(options)
                .build();
    }

    /**
     * 构建 Prompt
     */
    private Prompt buildPrompt(ChatRequest chatRequest, Model model) {
        List<Message> messages = chatRequest.getMessages().stream()
                .map(this::convertMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        DeepSeekChatOptions.Builder optionsBuilder = DeepSeekChatOptions.builder()
                .model(model.getModelKey());

        if (chatRequest.getTemperature() != null) {
            optionsBuilder.temperature(chatRequest.getTemperature());
        }
        if (chatRequest.getMaxTokens() != null) {
            optionsBuilder.maxTokens(chatRequest.getMaxTokens());
        }

        return new Prompt(messages, optionsBuilder.build());
    }

    /**
     * 转换消息类型
     */
    private Message convertMessage(com.leo.airouterbackend.model.dto.chat.ChatMessage msg) {
        String role = msg.getRole();
        if ("system".equals(role)) {
            return new SystemMessage(msg.getContent());
        }
//        else if ("assistant".equals(role)) {
//            return new AssistantMessage(msg.getContent());
//        }
        else if ("user".equals(role)) {
            return new UserMessage(msg.getContent());
        } else {
            return null;
        }
    }
}