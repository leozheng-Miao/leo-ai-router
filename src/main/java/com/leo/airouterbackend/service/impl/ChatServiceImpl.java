package com.leo.airouterbackend.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.constant.PermissionConstant;
import com.leo.airouterbackend.matrics.AIMetricsCollector;
import com.leo.airouterbackend.model.dto.chat.ChatMessage;
import com.leo.airouterbackend.model.dto.chat.ChatRequest;
import com.leo.airouterbackend.model.dto.chat.ChatResponse;
import com.leo.airouterbackend.model.dto.chat.StreamChunk;
import com.leo.airouterbackend.model.dto.chat.StreamResponse;
import com.leo.airouterbackend.model.dto.log.RequestLogDTO;
import com.leo.airouterbackend.model.dto.plugin.PluginExecuteRequest;
import com.leo.airouterbackend.model.entity.Model;
import com.leo.airouterbackend.model.entity.ModelProvider;
import com.leo.airouterbackend.model.vo.PluginExecuteVO;
import com.leo.airouterbackend.service.BalanceService;
import com.leo.airouterbackend.service.BillingService;
import com.leo.airouterbackend.service.ChatService;
import com.leo.airouterbackend.service.EntitlementService;
import com.leo.airouterbackend.service.ModelInvokeService;
import com.leo.airouterbackend.service.ModelProviderService;
import com.leo.airouterbackend.service.PluginService;
import com.leo.airouterbackend.service.QuotaService;
import com.leo.airouterbackend.service.RbacService;
import com.leo.airouterbackend.service.RequestLogService;
import com.leo.airouterbackend.service.RoutingService;
import com.leo.airouterbackend.service.UsageLimitService;
import com.leo.airouterbackend.service.UserProviderKeyService;
import com.leo.airouterbackend.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    @Resource
    private RequestLogService requestLogService;
    @Resource
    private RoutingService routingService;
    @Resource
    private ModelInvokeService modelInvokeService;
    @Resource
    private ModelProviderService modelProviderService;
    @Resource
    private QuotaService quotaService;
    @Resource
    private BalanceService balanceService;
    @Resource
    private BillingService billingService;
    @Resource
    private UserService userService;
    @Resource
    private UserProviderKeyService userProviderKeyService;
    @Resource
    private PluginService pluginService;
    @Resource
    private AIMetricsCollector aiMetricsCollector;
    @Resource
    private RbacService rbacService;
    @Resource
    private UsageLimitService usageLimitService;
    @Resource
    private EntitlementService entitlementService;
    /**
     * 最大 Fallback 重试次数
     */
    private static final int MAX_FALLBACK_RETRIES = 3;
    private static final String FIXED_ROUTING_STRATEGY = "fixed";

    private record SelectedRoute(Model primaryModel, List<Model> fallbackModels) {
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest, Long userId, Long apiKeyId, String clientIp, String userAgent) {
        long startTime = System.currentTimeMillis();
        String requestModel = chatRequest.getModel();
        String traceId = IdUtil.simpleUUID();

        if (userId != null && userService.isUserDisabled(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账号已被禁用，无法使用服务");
        }

        // 如果指定了插件，先执行插件获取上下文，然后注入到消息中
        if (StrUtil.isNotBlank(chatRequest.getPluginKey())) {
            chatRequest = injectPluginContext(chatRequest, userId);
        }

        String strategyType = determineStrategyType(chatRequest.getRoutingStrategy(), requestModel);

        SelectedRoute selectedRoute = selectEntitledRoute(userId, strategyType, requestModel);
        Model selectModel = selectedRoute.primaryModel();

        // 检查用户是否配置了 BYOK（提前检查，避免不必要的余额检查）
        boolean willUseByok = false;
        if (userId != null) {
            willUseByok = userProviderKeyService.hasUserProviderKey(userId, selectModel.getProviderId());
        }

        // BYOK 模式下不扣平台余额，但仍需套餐权益校验
        if (!willUseByok) {
            log.debug("平台密钥模式：用户 {} 使用套餐权益调用模型 {}", userId, selectModel.getModelKey());
        } else {
            log.info("BYOK 模式：用户 {} 跳过平台余额扣费", userId);
        }

        //TODO: 从缓存获取相应

        List<Model> fallbackModels = selectedRoute.fallbackModels();
        boolean isFallback = false;

        try {
            return invokeModelWithFallback(selectModel, fallbackModels, chatRequest,
                    userId, apiKeyId, traceId, startTime, strategyType, isFallback, clientIp, userAgent);
        } catch (Exception e) {
            log.error("所有模型调用失败", e);
            long duration = System.currentTimeMillis() - startTime;
            // 记录失败日志
            requestLogService.logRequest(RequestLogDTO.builder()
                    .traceId(traceId)
                    .userId(userId)
                    .apiKeyId(apiKeyId)
                    .requestModel(requestModel)
                    .requestType("chat")
                    .source(apiKeyId != null ? "api" : "web")
                    .duration((int) duration)
                    .status("failed")
                    .errorMessage(e.getMessage())
                    .errorCode("SYSTEM_ERROR")
                    .routingStrategy(strategyType)
                    .isFallback(false)
                    .clientIp(clientIp)
                    .userAgent(userAgent)
                    .build());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用模型失败: " + e.getMessage());
        }
    }

    /**
     * 带 Fallback 的模型调用
     */
    private ChatResponse invokeModelWithFallback(Model primaryModel, List<Model> fallbackModels,
                                                 ChatRequest chatRequest, Long userId, Long apiKeyId,
                                                 String traceId, long startTime, String strategyType, boolean isFallback,
                                                 String clientIp, String userAgent) {
        // 尝试调用主模型
        try {
            log.info("调用模型，{}", primaryModel.getModelKey());
            return callModel(primaryModel, chatRequest, userId, apiKeyId, traceId, startTime, strategyType, isFallback, clientIp, userAgent);
        } catch (Exception e) {
            log.warn("模型 {} 调用失败，尝试 Fallback", primaryModel.getModelKey(), e);

            // 如果主模型失败且有 Fallback 模型，尝试 Fallback
            if (fallbackModels != null && !fallbackModels.isEmpty()) {
                int retries = Math.min(fallbackModels.size(), MAX_FALLBACK_RETRIES);
                for (int i = 0; i < retries; i++) {
                    Model fallbackModel = fallbackModels.get(i);
                    try {
                        log.info("尝试 Fallback 模型: {}", fallbackModel.getModelKey());
                        return callModel(fallbackModel, chatRequest, userId, apiKeyId, traceId, startTime, strategyType, true, clientIp, userAgent);
                    } catch (Exception fallbackException) {
                        log.warn("Fallback 模型 {} 调用失败", fallbackModel.getModelKey(), fallbackException);
                        if (i == retries - 1) {
                            throw fallbackException;
                        }
                    }
                }
            }
            throw e;
        }
    }

    /**
     * 调用单个模型
     */
    private ChatResponse callModel(Model model, ChatRequest chatRequest, Long userId, Long apiKeyId,
                                   String traceId, long startTime, String strategyType, boolean isFallback,
                                   String clientIp, String userAgent) {
        checkChatPermission(userId, model);
        // 获取提供者信息
        ModelProvider provider = modelProviderService.getById(model.getProviderId());
        if (provider == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "模型提供者不存在");
        }

        // 检查用户是否配置了 BYOK（用户自带密钥）
        boolean isByok = false;
        if (userId != null) {
            String userApiKey = userProviderKeyService.getUserProviderApiKey(userId, model.getProviderId());
            if (userApiKey != null) {
                // 使用用户自己的密钥（BYOK 模式）
                provider = ModelProvider.builder()
                        .id(provider.getId())
                        .providerName(provider.getProviderName())
                        .displayName(provider.getDisplayName())
                        .baseUrl(provider.getBaseUrl())
                        // 使用用户的密钥
                        .apiKey(userApiKey)
                        .status(provider.getStatus())
                        .priority(provider.getPriority())
                        .build();
                isByok = true;
                log.info("用户 {} 使用 BYOK 模式调用模型 {}", userId, model.getModelKey());
            }
        }

        try {
            // 调用模型
            org.springframework.ai.chat.model.ChatResponse aiResponse =
                    modelInvokeService.invoke(model, provider, chatRequest);

            // 转换响应格式
            ChatResponse response = convertResponse(aiResponse, model.getModelKey());

            // 记录请求日志
            long duration = System.currentTimeMillis() - startTime;
            int promptTokens = response.getUsage().getPromptTokens();
            int completionTokens = response.getUsage().getCompletionTokens();
            int totalTokens = response.getUsage().getTotalTokens();
            java.math.BigDecimal cost = billingService.calculateCost(model, promptTokens, completionTokens);
            requestLogService.logRequest(RequestLogDTO.builder()
                    .traceId(traceId)
                    .userId(userId)
                    .apiKeyId(apiKeyId)
                    .modelId(model.getId())
                    .requestModel(model.getModelKey())
                    .requestType("chat")
                    .source(apiKeyId != null ? "api" : "web")
                    .promptTokens(response.getUsage().getPromptTokens())
                    .completionTokens(response.getUsage().getCompletionTokens())
                    .totalTokens(response.getUsage().getTotalTokens())
                    .cost(cost)
                    .duration((int) duration)
                    .status("success")
                    .routingStrategy(strategyType)
                    .isFallback(isFallback)
                    .clientIp(clientIp)
                    .userAgent(userAgent)
                    .build());
            // 收集监控指标
            aiMetricsCollector.recordRequest(model.getModelKey(), userId, apiKeyId != null ? apiKeyId.toString() : null);
            aiMetricsCollector.recordTokens(model.getModelKey(), totalTokens);
            aiMetricsCollector.recordResponseTime(model.getModelKey(), duration);
            // 聊天/API 中转消耗套餐次数，不消耗积分或余额
            if (userId != null && !isByok) {
                entitlementService.recordChatUsage(userId, model);
            }
            if (userId != null && isByok) {
                log.info("BYOK 模式：用户 {} 使用自己的密钥，不扣减平台次数", userId);
            }


            //TODO: 缓存响应

            return response;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            // 记录失败日志
            requestLogService.logRequest(RequestLogDTO.builder()
                    .traceId(traceId)
                    .userId(userId)
                    .apiKeyId(apiKeyId)
                    .modelId(model.getId())
                    .requestModel(model.getModelKey())
                    .requestType("chat")
                    .source(apiKeyId != null ? "api" : "web")
                    .duration((int) duration)
                    .status("failed")
                    .errorMessage(e.getMessage())
                    .errorCode("MODEL_ERROR")
                    .routingStrategy(strategyType)
                    .isFallback(isFallback)
                    .clientIp(clientIp)
                    .userAgent(userAgent)
                    .build());
            aiMetricsCollector.recordError(model.getModelKey(), "MODEL_ERROR");
            throw e;
        }
    }

    @Override
    public Flux<StreamResponse> chatStream(ChatRequest chatRequest, Long userId, Long apiKeyId, String clientIp, String userAgent) {
        String requestedModel = chatRequest.getModel();
        long startTime = System.currentTimeMillis();
        String traceId = IdUtil.simpleUUID();
        long created = System.currentTimeMillis() / 1000;

        // 检查用户状态
        if (userId != null && userService.isUserDisabled(userId)) {
            return Flux.error(new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账号已被禁用，无法使用服务"));
        }

        // Token 计数器
        final int[] promptTokens = {0};
        final int[] completionTokens = {0};

        // 深度思考状态追踪
        final boolean[] isFirstChunk = {true};

        try {

            // 确定路由策略：优先使用请求中指定的策略，否则根据是否指定模型决定
            String strategyType = determineStrategyType(chatRequest.getRoutingStrategy(), requestedModel);

            SelectedRoute selectedRoute = selectEntitledRoute(userId, strategyType, requestedModel);
            Model selectedModel = selectedRoute.primaryModel();

            // 获取提供者信息
            ModelProvider provider = modelProviderService.getById(selectedModel.getProviderId());
            if (provider == null) {
                return Flux.error(new BusinessException(ErrorCode.PARAMS_ERROR, "模型提供者不存在"));
            }

            // 检查用户是否配置了 BYOK（用户自带密钥）
            final boolean[] isByok = {false};
            if (userId != null) {
                String userApiKey = userProviderKeyService.getUserProviderApiKey(userId, selectedModel.getProviderId());
                if (userApiKey != null) {
                    // 使用用户自己的密钥（BYOK 模式）
                    provider = ModelProvider.builder()
                            .id(provider.getId())
                            .providerName(provider.getProviderName())
                            .displayName(provider.getDisplayName())
                            .baseUrl(provider.getBaseUrl())
                            .apiKey(userApiKey)  // 使用用户的密钥
                            .status(provider.getStatus())
                            .priority(provider.getPriority())
                            .build();
                    isByok[0] = true;
                    log.info("用户 {} 使用 BYOK 模式调用流式模型 {}", userId, selectedModel.getModelKey());
                }
            }

            // BYOK 模式下不扣平台余额，但仍需套餐权益校验
            if (!isByok[0]) {
                log.debug("平台密钥模式：用户 {} 使用套餐权益调用流式模型 {}", userId, selectedModel.getModelKey());
            } else {
                log.info("BYOK 模式：跳过平台余额扣费");
            }

            // 调用流式模型，获取统一格式的响应块
            Flux<StreamChunk> chunkFlux = modelInvokeService.invokeStreamChunk(selectedModel, provider, chatRequest);

            // 将统一格式的响应块转换为 OpenAI SSE 格式的 StreamResponse
            return chunkFlux.flatMap(chunk -> {
                        // 更新 Token 统计
                        if (chunk.getPromptTokens() != null && chunk.getPromptTokens() > 0) {
                            promptTokens[0] = chunk.getPromptTokens();
                        }
                        if (chunk.getCompletionTokens() != null && chunk.getCompletionTokens() > 0) {
                            completionTokens[0] = chunk.getCompletionTokens();
                        }

                        // 构建 Delta
                        StreamResponse.Delta.DeltaBuilder deltaBuilder = StreamResponse.Delta.builder();

                        // 第一个块包含 role
                        if (isFirstChunk[0]) {
                            deltaBuilder.role("assistant");
                            isFirstChunk[0] = false;
                        }

                        // 处理普通文本内容
                        if (chunk.hasText()) {
                            deltaBuilder.content(chunk.getText());
                        }

                        // 处理深入思考内容
                        if (chunk.hasReasoningContent()) {
                            deltaBuilder.reasoningContent(chunk.getReasoningContent());
                        }

                        // 如果既没有文本也没有思考内容， 跳过
                        if (!chunk.hasText() && !chunk.hasReasoningContent()) {
                            return Flux.empty();
                        }

                        StreamResponse.Delta delta = deltaBuilder.build();
                        StreamResponse.StreamChoice choice = StreamResponse.StreamChoice.builder()
                                .index(0)
                                .delta(delta)
                                .finishReason(null)
                                .build();

                        return Flux.just(StreamResponse.builder()
                                .id(traceId)
                                .object("chat.completion.chunk")
                                .created(created)
                                .model(selectedModel.getModelKey())
                                .choices(List.of(choice))
                                .build());

                    })
                    // 在流结束时追加一个带 finishReason: "stop" 的结束标识
                    .concatWith(Flux.defer(() -> {
                        StreamResponse.StreamChoice finishChoice = StreamResponse.StreamChoice.builder()
                                .index(0)
                                .delta(StreamResponse.Delta.builder().build())
                                .finishReason("stop")
                                .build();
                        return Flux.just(StreamResponse.builder()
                                .id(traceId)
                                .object("chat.completion.chunk")
                                .created(created)
                                .model(selectedModel.getModelKey())
                                .choices(List.of(finishChoice))
                                .build());
                    }))
                    .doOnComplete(() -> {
                        // 流结束时记录日志
                        long duration = System.currentTimeMillis() - startTime;
                        int totalTokens = promptTokens[0] + completionTokens[0];
                        java.math.BigDecimal cost = billingService.calculateCost(selectedModel, promptTokens[0], completionTokens[0]);
                        requestLogService.logRequest(RequestLogDTO.builder()
                                .traceId(traceId)
                                .userId(userId)
                                .apiKeyId(apiKeyId)
                                .modelId(selectedModel.getId())
                                .requestModel(selectedModel.getModelKey())
                                .requestType("chat")
                                .source(apiKeyId != null ? "api" : "web")
                                .promptTokens(promptTokens[0])
                                .completionTokens(completionTokens[0])
                                .totalTokens(totalTokens)
                                .cost(cost)
                                .duration((int) duration)
                                .status("success")
                                .routingStrategy(strategyType)
                                .isFallback(false)
                                .clientIp(clientIp)
                                .userAgent(userAgent)
                                .build());

                        // 收集监控指标
                        aiMetricsCollector.recordRequest(selectedModel.getModelKey(), userId, apiKeyId != null ? apiKeyId.toString() : null);
                        aiMetricsCollector.recordTokens(selectedModel.getModelKey(), totalTokens);
                        aiMetricsCollector.recordResponseTime(selectedModel.getModelKey(), duration);

                        // 聊天/API 中转消耗套餐次数，不消耗积分或余额
                        if (userId != null && !isByok[0]) {
                            entitlementService.recordChatUsage(userId, selectedModel);
                        }
                        if (userId != null && isByok[0]) {
                            log.info("BYOK 模式（流式）：用户 {} 使用自己的密钥，不扣减平台次数", userId);
                        }
                    }).doOnError(error -> {
                        // 流错误时记录日志
                        log.error("流式调用模型失败", error);
                        long duration = System.currentTimeMillis() - startTime;
                        requestLogService.logRequest(RequestLogDTO.builder()
                                .traceId(traceId)
                                .userId(userId)
                                .apiKeyId(apiKeyId)
                                .modelId(selectedModel.getId())
                                .requestModel(selectedModel.getModelKey())
                                .requestType("chat")
                                .source(apiKeyId != null ? "api" : "web")
                                .duration((int) duration)
                                .status("failed")
                                .errorMessage(error.getMessage())
                                .errorCode("STREAM_ERROR")
                                .routingStrategy(strategyType)
                                .isFallback(false)
                                .clientIp(clientIp)
                                .userAgent(userAgent)
                                .build());
                    });
        } catch (BusinessException e) {
            return Flux.error(e);
        } catch (Exception e) {
            log.error("流式调用模型失败", e);
            return Flux.error(new BusinessException(ErrorCode.SYSTEM_ERROR, "流式调用模型失败: " + e.getMessage()));
        }
    }

    /**
     * 转换响应格式
     */
    private ChatResponse convertResponse(org.springframework.ai.chat.model.ChatResponse aiResponse, String
            modelName) {
        String content = aiResponse.getResult().getOutput().getText();

        ChatResponse.Usage usage = ChatResponse.Usage.builder()
                .promptTokens(aiResponse.getMetadata().getUsage().getPromptTokens() != null ?
                        aiResponse.getMetadata().getUsage().getPromptTokens() : 0)
                .completionTokens(aiResponse.getMetadata().getUsage().getCompletionTokens() != null ?
                        aiResponse.getMetadata().getUsage().getCompletionTokens() : 0)
                .totalTokens(aiResponse.getMetadata().getUsage().getTotalTokens() != null ?
                        aiResponse.getMetadata().getUsage().getTotalTokens() : 0)
                .build();

        ChatResponse.Choice choice = ChatResponse.Choice.builder()
                .index(0)
                .message(new ChatMessage("assistant", content))
                .finishReason(aiResponse.getResult().getMetadata().getFinishReason())
                .build();

        return ChatResponse.builder()
                .id(IdUtil.simpleUUID())
                .object("chat.completion")
                .created(System.currentTimeMillis() / 1000)
                .model(modelName)
                .choices(List.of(choice))
                .usage(usage)
                .build();

    }

    private void checkChatPermission(Long userId, Model model) {
        if (userId == null) {
            return;
        }
        checkChatRole(userId);
        entitlementService.checkChatAccess(userId, model);
    }

    private void checkChatRole(Long userId) {
        if (userId != null && !rbacService.hasPermission(userId, PermissionConstant.CHAT_USE)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "当前角色无权使用聊天功能");
        }
    }

    private SelectedRoute selectEntitledRoute(Long userId, String strategyType, String requestedModel) {
        Model selectedModel = routingService.selectModel(strategyType, "chat", requestedModel);
        if (selectedModel == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "没有可用的模型");
        }
        List<Model> fallbackModels = routingService.getFallbackModels(strategyType, "chat", requestedModel);
        if (FIXED_ROUTING_STRATEGY.equals(strategyType)) {
            checkChatPermission(userId, selectedModel);
            return new SelectedRoute(selectedModel, filterEntitledModels(userId, fallbackModels, selectedModel.getModelKey()));
        }

        checkChatRole(userId);
        List<Model> candidates = new ArrayList<>();
        candidates.add(selectedModel);
        if (fallbackModels != null) {
            candidates.addAll(fallbackModels);
        }
        List<Model> entitledModels = filterEntitledModels(userId, candidates, null);
        if (entitledModels.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "今日聊天次数已用尽，请升级套餐或明日再试");
        }
        Model primary = entitledModels.getFirst();
        return new SelectedRoute(primary, entitledModels.stream()
                .skip(1)
                .filter(model -> !primary.getModelKey().equals(model.getModelKey()))
                .toList());
    }

    private List<Model> filterEntitledModels(Long userId, List<Model> models, String excludeModelKey) {
        if (models == null || models.isEmpty()) {
            return List.of();
        }
        List<Model> result = new ArrayList<>();
        for (Model model : models) {
            if (model == null || StrUtil.isBlank(model.getModelKey())) {
                continue;
            }
            if (StrUtil.isNotBlank(excludeModelKey) && excludeModelKey.equals(model.getModelKey())) {
                continue;
            }
            boolean exists = result.stream().anyMatch(item -> model.getModelKey().equals(item.getModelKey()));
            if (!exists && entitlementService.canUseChatAccess(userId, model)) {
                result.add(model);
            }
        }
        return result;
    }

    /**
     * 确定路由策略类型
     *
     * @param requestedStrategy 请求中指定的策略
     * @param requestedModel    请求中指定的模型
     * @return 最终使用的策略类型
     */
    private String determineStrategyType(String requestedStrategy, String requestedModel) {
        // 如果请求中指定了策略，优先使用
        if (StrUtil.isNotBlank(requestedStrategy)) {
            return requestedStrategy;
        }
        // 如果指定了模型但没有指定策略，使用固定模型策略
        if (StrUtil.isNotBlank(requestedModel)) {
            return "fixed";
        }
        // 默认使用自动路由策略
        return "auto";
    }

    /**
     * 执行插件并将结果注入到对话消息中
     *
     * @param chatRequest 原始请求
     * @param userId      用户ID
     * @return 注入插件上下文后的请求
     */
    private ChatRequest injectPluginContext(ChatRequest chatRequest, Long userId) {
        String pluginKey = chatRequest.getPluginKey();
        log.info("执行插件并注入上下文: {}", pluginKey);

        // 获取用户的最后一条消息作为 input
        String userInput = "";
        List<ChatMessage> messages = chatRequest.getMessages();
        if (messages != null && !messages.isEmpty()) {
            for (int i = messages.size() - 1; i >= 0; i--) {
                ChatMessage msg = messages.get(i);
                if ("user".equals(msg.getRole())) {
                    userInput = msg.getContent();
                    break;
                }
            }
        }

        // 构建插件执行请求
        PluginExecuteRequest pluginRequest = new PluginExecuteRequest();
        pluginRequest.setPluginKey(pluginKey);
        pluginRequest.setInput(userInput);
        pluginRequest.setFileUrl(chatRequest.getFileUrl());
        pluginRequest.setFileBytes(chatRequest.getFileBytes());
        pluginRequest.setFileType(chatRequest.getFileType());

        // 执行插件
        PluginExecuteVO pluginResult = pluginService.executePlugin(pluginRequest, userId);

        if (!pluginResult.isSuccess()) {
            log.warn("插件执行失败: {}", pluginResult.getErrorMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "插件执行失败: " + pluginResult.getErrorMessage());
        }

        // 构建插件上下文的 system message
        String pluginContext = buildPluginContextMessage(pluginKey, pluginResult.getContent());

        // 创建新的消息列表，将插件上下文作为 system message 注入
        List<ChatMessage> newMessages = new ArrayList<>();

        // 添加插件上下文作为 system message
        newMessages.add(new ChatMessage("system", pluginContext));

        // 添加原始消息
        if (messages != null) {
            newMessages.addAll(messages);
        }

        // 更新请求的消息列表
        chatRequest.setMessages(newMessages);

        log.info("插件上下文注入完成，新增 system message");
        return chatRequest;
    }

    /**
     * 根据插件类型构建上下文消息
     */
    private String buildPluginContextMessage(String pluginKey, String content) {
        return switch (pluginKey) {
            case "web_search" -> String.format("""
                    以下是实时网络搜索的结果，请根据这些信息回答用户的问题：
                    
                    %s
                    
                    请基于以上搜索结果，准确、简洁地回答用户的问题。如果搜索结果中没有相关信息，请如实告知。
                    """, content);
            case "pdf_parser" -> String.format("""
                    以下是用户上传的 PDF 文档内容：
                    
                    %s
                    
                    请基于以上文档内容，回答用户的问题。如果问题与文档内容无关，请如实告知。
                    """, content);
            case "image_recognition" -> String.format("""
                    以下是用户上传图片的识别结果：
                    
                    %s
                    
                    请基于以上图片识别结果，回答用户的问题。
                    """, content);
            default -> String.format("""
                    以下是插件返回的额外信息：
                    
                    %s
                    
                    请基于以上信息回答用户的问题。
                    """, content);
        };
    }
}
