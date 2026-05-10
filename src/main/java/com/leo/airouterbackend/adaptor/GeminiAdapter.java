package com.leo.airouterbackend.adaptor;

import com.google.genai.Client;
import com.google.genai.types.ClientOptions;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.ProxyOptions;
import com.google.genai.types.ProxyType;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.model.dto.chat.ChatMessage;
import com.leo.airouterbackend.model.dto.chat.ChatRequest;
import com.leo.airouterbackend.model.dto.chat.StreamChunk;
import com.leo.airouterbackend.model.entity.Model;
import com.leo.airouterbackend.model.entity.ModelProvider;
import com.leo.airouterbackend.util.GeminiProviderUtils;
import com.leo.airouterbackend.util.OpenAiProviderUtils;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.common.GoogleGenAiThinkingLevel;
import org.springframework.ai.google.genai.schema.GoogleGenAiToolCallingManager;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityPredicate;
import org.springframework.ai.tool.resolution.DelegatingToolCallbackResolver;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.netty.transport.ProxyProvider;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gemini 文本模型适配器
 */
@Slf4j
@Component
public class GeminiAdapter implements ModelAdapter {

    private static final Set<String> SUPPORTED_PROVIDERS = Set.of("gemini", "google");

    @Override
    public ChatResponse invoke(Model model, ModelProvider provider, ChatRequest chatRequest) {
        try {
            GoogleGenAiChatModel chatModel = createChatModel(provider, model);
            Prompt prompt = buildPrompt(chatRequest, model.getModelKey());
            return chatModel.call(prompt);
        } catch (Exception e) {
            log.error("Gemini 适配器调用模型 {} 失败", model.getModelKey(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用 Gemini 模型失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<ChatResponse> invokeStream(Model model, ModelProvider provider, ChatRequest chatRequest) {
        try {
            GoogleGenAiChatModel chatModel = createChatModel(provider, model);
            Prompt prompt = buildPrompt(chatRequest, model.getModelKey());
            return chatModel.stream(prompt);
        } catch (Exception e) {
            log.error("Gemini 适配器流式调用模型 {} 失败", model.getModelKey(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "流式调用 Gemini 模型失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<StreamChunk> invokeStreamChunk(Model model, ModelProvider provider, ChatRequest chatRequest) {
        return invokeStream(model, provider, chatRequest)
                .map(this::convertToStreamChunk)
                .filter(chunk -> !chunk.isEmpty());
    }

    @Override
    public boolean supports(String providerName) {
        return providerName != null && SUPPORTED_PROVIDERS.contains(providerName.toLowerCase());
    }

    protected Prompt buildPrompt(ChatRequest chatRequest, String modelKey) {
        List<Message> messages = chatRequest.getMessages().stream()
                .map(this::convertMessage)
                .collect(Collectors.toList());

        GoogleGenAiChatOptions.Builder optionsBuilder = GoogleGenAiChatOptions.builder()
                .model(modelKey);

        if (chatRequest.getTemperature() != null) {
            optionsBuilder.temperature(chatRequest.getTemperature());
        }
        if (chatRequest.getMaxTokens() != null) {
            optionsBuilder.maxOutputTokens(chatRequest.getMaxTokens());
        }

        applyReasoningOptions(optionsBuilder, modelKey, chatRequest.getEnableReasoning());
        return new Prompt(messages, optionsBuilder.build());
    }

    void applyReasoningOptions(GoogleGenAiChatOptions.Builder optionsBuilder, String modelKey, Boolean enableReasoning) {
        boolean enabled = Boolean.TRUE.equals(enableReasoning);
        if ("gemini-3.1-pro-preview".equals(modelKey) || "gemini-3-pro-preview".equals(modelKey)) {
            optionsBuilder.thinkingLevel(enabled ? GoogleGenAiThinkingLevel.HIGH : GoogleGenAiThinkingLevel.LOW);
            return;
        }
        if ("gemini-3-flash-preview".equals(modelKey)) {
            if (enabled) {
                optionsBuilder.thinkingLevel(GoogleGenAiThinkingLevel.HIGH);
            } else {
                optionsBuilder.thinkingBudget(0);
            }
            return;
        }
        if ("gemini-3.1-flash-lite".equals(modelKey) || "gemini-2.5-flash-lite".equals(modelKey)) {
            optionsBuilder.thinkingBudget(enabled ? -1 : 0);
        }
    }

    private StreamChunk convertToStreamChunk(ChatResponse response) {
        StreamChunk.StreamChunkBuilder builder = StreamChunk.builder();

        if (ObjectUtils.isNotEmpty(response.getMetadata()) && ObjectUtils.isNotEmpty(response.getMetadata().getUsage())) {
            Integer promptTokens = response.getMetadata().getUsage().getPromptTokens();
            Integer completionTokens = response.getMetadata().getUsage().getCompletionTokens();
            if (promptTokens != null && promptTokens > 0) {
                builder.promptTokens(promptTokens);
            }
            if (completionTokens != null && completionTokens > 0) {
                builder.completionTokens(completionTokens);
            }
        }

        if (ObjectUtils.isNotEmpty(response.getResult()) && ObjectUtils.isNotEmpty(response.getResult().getOutput())) {
            String text = response.getResult().getOutput().getText();
            if (text != null && !text.isEmpty()) {
                builder.text(text);
            }
        }

        StreamChunk chunk = builder.build();
        if (!chunk.hasText() && !chunk.hasReasoningContent()
                && chunk.getPromptTokens() == null && chunk.getCompletionTokens() == null) {
            return StreamChunk.empty();
        }
        return chunk;
    }

    private Message convertMessage(ChatMessage message) {
        return switch (message.getRole()) {
            case "system" -> new SystemMessage(message.getContent());
            case "assistant" -> new AssistantMessage(message.getContent());
            default -> new UserMessage(message.getContent());
        };
    }

    private GoogleGenAiChatModel createChatModel(ModelProvider provider, Model model) {
        GoogleGenAiChatOptions defaultOptions = GoogleGenAiChatOptions.builder()
                .model(model.getModelKey())
                .build();

        return GoogleGenAiChatModel.builder()
                .genAiClient(buildGenAiClient(provider))
                .defaultOptions(defaultOptions)
                .toolCallingManager(buildToolCallingManager())
                .retryTemplate(RetryTemplate.defaultInstance())
                .observationRegistry(ObservationRegistry.NOOP)
                .toolExecutionEligibilityPredicate(buildToolExecutionEligibilityPredicate())
                .build();
    }

    private Client buildGenAiClient(ModelProvider provider) {
        OpenAiProviderUtils.OpenAiTransportConfig transportConfig = OpenAiProviderUtils.resolveTransportConfig(provider);
        HttpOptions.Builder httpOptionsBuilder = HttpOptions.builder()
                .apiVersion("v1beta")
                .baseUrl(GeminiProviderUtils.normalizeBaseUrl(provider.getBaseUrl()))
                .timeout(transportConfig.responseTimeoutSeconds() * 1000);

        Client.Builder builder = Client.builder()
                .apiKey(provider.getApiKey())
                .httpOptions(httpOptionsBuilder.build());

        ClientOptions clientOptions = buildClientOptions(transportConfig.proxySettings());
        if (clientOptions != null) {
            builder.clientOptions(clientOptions);
        }
        return builder.build();
    }

    private ToolCallingManager buildToolCallingManager() {
        DefaultToolCallingManager defaultManager = DefaultToolCallingManager.builder()
                .observationRegistry(ObservationRegistry.NOOP)
                .toolCallbackResolver(new DelegatingToolCallbackResolver(Collections.emptyList()))
                .build();
        return new GoogleGenAiToolCallingManager(defaultManager);
    }

    private ToolExecutionEligibilityPredicate buildToolExecutionEligibilityPredicate() {
        return new DefaultToolExecutionEligibilityPredicate();
    }

    private ClientOptions buildClientOptions(OpenAiProviderUtils.ProxySettings proxySettings) {
        if (proxySettings == null || !proxySettings.enabled()) {
            return null;
        }
        ProxyOptions.Builder proxyBuilder = ProxyOptions.builder()
                .type(toProxyType(proxySettings.type()))
                .host(proxySettings.host())
                .port(proxySettings.port());
        if (proxySettings.username() != null) {
            proxyBuilder.username(proxySettings.username());
        }
        if (proxySettings.password() != null) {
            proxyBuilder.password(proxySettings.password());
        }
        return ClientOptions.builder()
                .proxyOptions(proxyBuilder.build())
                .build();
    }

    private ProxyType toProxyType(ProxyProvider.Proxy proxyType) {
        if (proxyType == ProxyProvider.Proxy.SOCKS4 || proxyType == ProxyProvider.Proxy.SOCKS5) {
            return new ProxyType(ProxyType.Known.SOCKS);
        }
        return new ProxyType(ProxyType.Known.HTTP);
    }
}
