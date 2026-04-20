package com.leo.airouterbackend.service;

import com.leo.airouterbackend.model.dto.chat.ChatRequest;
import com.leo.airouterbackend.model.dto.chat.ChatResponse;
import com.leo.airouterbackend.model.dto.chat.StreamResponse;
import reactor.core.publisher.Flux;

public interface ChatService {

    /**
     * 非流式聊天
     */
    ChatResponse chat(ChatRequest chatRequest, Long userId, Long apiKeyId, String clientIp, String userAgent);

    /**
     * 流式聊天
     *  返回 OpenAI SSE 格式
     */
    Flux<StreamResponse> chatStream(ChatRequest chatRequest, Long userId, Long apiKeyId, String clientIp, String userAgent);
}