package com.leo.airouterbackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.mapper.ConversationMessageMapper;
import com.leo.airouterbackend.model.dto.chat.ChatMessage;
import com.leo.airouterbackend.model.dto.chat.ChatRequest;
import com.leo.airouterbackend.model.dto.chat.ChatResponse;
import com.leo.airouterbackend.model.dto.chat.StreamResponse;
import com.leo.airouterbackend.model.dto.conversation.SendMessageRequest;
import com.leo.airouterbackend.model.entity.Conversation;
import com.leo.airouterbackend.model.entity.ConversationMessage;
import com.leo.airouterbackend.model.entity.Model;
import com.leo.airouterbackend.model.entity.ModelProvider;
import com.leo.airouterbackend.model.vo.MessageVO;
import com.leo.airouterbackend.service.ChatService;
import com.leo.airouterbackend.service.ConversationService;
import com.leo.airouterbackend.service.MessageService;
import com.leo.airouterbackend.service.ModelProviderService;
import com.leo.airouterbackend.service.RoutingService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class MessageServiceImpl extends ServiceImpl<ConversationMessageMapper, ConversationMessage> implements MessageService {

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final int DEFAULT_MODE = 1;
    private static final int REASONING_MODE = 2;
    private static final int CONTEXT_LIMIT = 20;
    private static final String DEFAULT_ROUTING_STRATEGY = "auto";
    private static final String FIXED_ROUTING_STRATEGY = "fixed";
    private static final String FAILED_ASSISTANT_CONTENT = "消息发送失败，请重试";

    @Resource
    private ConversationService conversationService;

    @Resource
    private ChatService chatService;

    @Resource
    private RoutingService routingService;

    @Resource
    private ModelProviderService modelProviderService;

    @Resource
    private ConversationMessageMapper conversationMessageMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Page<MessageVO> listMessages(Long userId, Long conversationId, long page, long size) {
        conversationService.validateOwner(userId, conversationId);
        if (page < 0 || size <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<ConversationMessage> messagePage = this.page(Page.of(page + 1, size), QueryWrapper.create()
                .eq("conversation_id", conversationId)
                .eq("user_id", userId)
                .orderBy("seq", true));

        Page<MessageVO> result = new Page<>(page + 1, size, messagePage.getTotalRow());
        result.setRecords(messagePage.getRecords().stream().map(this::toMessageVO).toList());
        return result;
    }

    @Override
    public MessageVO sendMessage(Long userId, Long conversationId, SendMessageRequest request, String clientIp, String userAgent) {
        Conversation conversation = conversationService.validateOwner(userId, conversationId);
        int mode = normalizeMode(request);
        String userContent = normalizeContent(request);
        Model selectedModel = resolveModel(request);

        saveMessage(conversationId, userId, ROLE_USER, userContent, mode);
        ChatRequest chatRequest = buildChatRequest(conversationId, mode, selectedModel, false);
        ChatResponse response = chatService.chat(chatRequest, userId, null, clientIp, userAgent);
        String assistantContent = extractAssistantContent(response);

        ConversationMessage assistantMessage = saveMessage(conversationId, userId, ROLE_ASSISTANT, assistantContent, mode);
        conversationService.updateAfterMessage(conversation, userContent);
        return toMessageVO(assistantMessage);
    }

    @Override
    public Flux<String> streamMessage(Long userId, Long conversationId, SendMessageRequest request, String clientIp, String userAgent) {
        return Flux.defer(() -> {
            Conversation conversation = conversationService.validateOwner(userId, conversationId);
            int mode = normalizeMode(request);
            String userContent = normalizeContent(request);
            Model selectedModel = resolveModel(request);

            saveMessage(conversationId, userId, ROLE_USER, userContent, mode);
            ChatRequest chatRequest = buildChatRequest(conversationId, mode, selectedModel, true);
            StringBuilder assistantContent = new StringBuilder();
            AtomicBoolean assistantPersisted = new AtomicBoolean(false);

            try {
                return chatService.chatStream(chatRequest, userId, null, clientIp, userAgent)
                        .flatMap(response -> {
                            String text = extractDeltaContent(response);
                            if (StrUtil.isBlank(text)) {
                                return Flux.empty();
                            }
                            assistantContent.append(text);
                            return Flux.just("data: " + text + "\n\n");
                        })
                        .concatWith(Flux.defer(() -> {
                            saveMessage(conversationId, userId, ROLE_ASSISTANT, assistantContent.toString(), mode);
                            assistantPersisted.set(true);
                            conversationService.updateAfterMessage(conversation, userContent);
                            return Flux.just("data: [DONE]\n\n");
                        }))
                        .doOnError(error -> persistStreamFailureIfNecessary(
                                assistantPersisted,
                                conversation,
                                conversationId,
                                userId,
                                mode,
                                userContent,
                                error
                        ));
            } catch (Exception e) {
                persistStreamFailureIfNecessary(assistantPersisted, conversation, conversationId, userId, mode, userContent, e);
                return Flux.error(e);
            }
        });
    }

    private ChatRequest buildChatRequest(Long conversationId, int mode, Model selectedModel, boolean stream) {
        boolean deepSeekModel = isDeepSeekModel(selectedModel);
        List<ConversationMessage> recentMessages = conversationMessageMapper.selectRecentContext(conversationId, CONTEXT_LIMIT);
        List<ChatMessage> chatMessages = recentMessages.stream()
                .map(message -> toChatMessage(message, deepSeekModel))
                .filter(Objects::nonNull)
                .toList();

        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setModel(selectedModel.getModelKey());
        chatRequest.setRoutingStrategy(FIXED_ROUTING_STRATEGY);
        chatRequest.setMessages(chatMessages);
        chatRequest.setStream(stream);
        chatRequest.setEnableReasoning(mode == REASONING_MODE);
        return chatRequest;
    }

    private ChatMessage toChatMessage(ConversationMessage message, boolean deepSeekModel) {
        if (ROLE_USER.equals(message.getRole())) {
            return new ChatMessage(ROLE_USER, message.getContent());
        }
        if (ROLE_ASSISTANT.equals(message.getRole())) {
            if (deepSeekModel) {
                return new ChatMessage(ROLE_USER, "[AI之前的回复]: " + message.getContent());
            }
            return new ChatMessage(ROLE_ASSISTANT, message.getContent());
        }
        return null;
    }

    private Model resolveModel(SendMessageRequest request) {
        String requestedModel = request == null ? null : request.getModel();
        String requestedStrategy = StrUtil.isNotBlank(requestedModel)
                ? FIXED_ROUTING_STRATEGY
                : StrUtil.blankToDefault(request == null ? null : request.getRoutingStrategy(), DEFAULT_ROUTING_STRATEGY);
        Model selectedModel = routingService.selectModel(requestedStrategy, "chat", requestedModel);
        if (selectedModel == null || StrUtil.isBlank(selectedModel.getModelKey())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "没有可用的模型");
        }
        return selectedModel;
    }

    private boolean isDeepSeekModel(Model selectedModel) {
        if (selectedModel == null) {
            return false;
        }
        String modelKey = StrUtil.blankToDefault(selectedModel.getModelKey(), "").toLowerCase(Locale.ROOT);
        if (modelKey.startsWith("deepseek")) {
            return true;
        }
        ModelProvider provider = modelProviderService.getById(selectedModel.getProviderId());
        String providerName = provider == null ? "" : StrUtil.blankToDefault(provider.getProviderName(), "").toLowerCase(Locale.ROOT);
        return "deepseek".equals(providerName);
    }

    private ConversationMessage saveMessage(Long conversationId, Long userId, String role, String content, int mode) {
        ConversationMessage message = ConversationMessage.builder()
                .conversationId(conversationId)
                .userId(userId)
                .role(role)
                .content(StrUtil.nullToEmpty(content))
                .mode(mode)
                .tokenCount(null)
                .seq(nextSeq(conversationId))
                .createdAt(LocalDateTime.now())
                .build();
        boolean saved = this.save(message);
        if (!saved || message.getId() == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存消息失败");
        }
        return message;
    }

    private Long nextSeq(Long conversationId) {
        String key = "conv:seq:" + conversationId;
        Boolean hasKey = stringRedisTemplate.hasKey(key);
        if (!Boolean.TRUE.equals(hasKey)) {
            Long maxSeq = conversationMessageMapper.selectMaxSeq(conversationId);
            stringRedisTemplate.opsForValue().setIfAbsent(key, String.valueOf(maxSeq == null ? 0L : maxSeq));
        }
        Long seq = stringRedisTemplate.opsForValue().increment(key);
        if (seq == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成消息序号失败");
        }
        return seq;
    }

    private int normalizeMode(SendMessageRequest request) {
        Integer mode = request == null ? null : request.getMode();
        if (mode == null) {
            return DEFAULT_MODE;
        }
        if (mode != DEFAULT_MODE && mode != REASONING_MODE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "mode 参数不合法");
        }
        return mode;
    }

    private String normalizeContent(SendMessageRequest request) {
        String content = request == null ? null : request.getContent();
        if (StrUtil.isBlank(content)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        }
        return content.trim();
    }

    private String extractAssistantContent(ChatResponse response) {
        if (response == null || CollUtil.isEmpty(response.getChoices())) {
            return "";
        }
        ChatResponse.Choice choice = response.getChoices().getFirst();
        if (choice == null || choice.getMessage() == null) {
            return "";
        }
        return StrUtil.nullToEmpty(choice.getMessage().getContent());
    }

    private String extractDeltaContent(StreamResponse response) {
        if (response == null || CollUtil.isEmpty(response.getChoices())) {
            return "";
        }
        StreamResponse.StreamChoice choice = response.getChoices().getFirst();
        if (choice == null || choice.getDelta() == null) {
            return "";
        }
        return StrUtil.nullToEmpty(choice.getDelta().getContent());
    }

    private void persistStreamFailureIfNecessary(AtomicBoolean assistantPersisted,
                                                 Conversation conversation,
                                                 Long conversationId,
                                                 Long userId,
                                                 int mode,
                                                 String userContent,
                                                 Throwable error) {
        if (!assistantPersisted.compareAndSet(false, true)) {
            return;
        }
        try {
            log.warn("流式会话消息发送失败，conversationId={}", conversationId, error);
            saveMessage(conversationId, userId, ROLE_ASSISTANT, FAILED_ASSISTANT_CONTENT, mode);
            conversationService.updateAfterMessage(conversation, userContent);
        } catch (Exception persistException) {
            log.error("持久化流式失败消息失败，conversationId={}", conversationId, persistException);
        }
    }

    private MessageVO toMessageVO(ConversationMessage message) {
        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setMode(message.getMode());
        vo.setCreatedAt(message.getCreatedAt());
        vo.setSeq(message.getSeq());
        return vo;
    }
}
