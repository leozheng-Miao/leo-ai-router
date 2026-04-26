package com.leo.airouterbackend.controller;

import cn.hutool.extra.servlet.JakartaServletUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leo.airouterbackend.annotation.RateLimit;
import com.leo.airouterbackend.common.ResultUtils;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.model.dto.chat.ChatRequest;
import com.leo.airouterbackend.model.dto.chat.ChatResponse;
import com.leo.airouterbackend.model.entity.User;
import com.leo.airouterbackend.service.ChatService;
import com.leo.airouterbackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/chat")
@Slf4j
public class InternalChatController {

    @Resource
    private ChatService chatService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private UserService userService;

    /**
     * 内部聊天接口（网页端对话）
     * 支持流式和非流式响应
     */
    @PostMapping(value = "/completions", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
//    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @Operation(summary = "内部聊天接口")
    @RateLimit(type = RateLimit.LimitType.IP, limit = 30)
    public Object chatCompletions(@RequestBody ChatRequest request,
                                  HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);

        // 1. 参数校验
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "messages 不能为空");
        }
        // 2. 获取客户端IP和User-Agent
        String clientIp = JakartaServletUtil.getClientIP(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // 3. 判断是否为流式请求
        Boolean stream = request.getStream();
        if (stream != null && stream) {
            // 流式响应（网页端调用，不传 apiKeyId）
            return chatService.chatStream(request, loginUser.getId(), null, clientIp, userAgent)
                    .map(streamResponse -> {
                        try {
                            // 将 StreamResponse 转换为 JSON
                            String json = objectMapper.writeValueAsString(streamResponse);
                            return json + "\n\n";
                        } catch (Exception e) {
                            log.error("Failed to serialize stream response", e);
                            return "";
                        }
                    });
        } else {
            // 非流式响应（网页端调用，不传 apiKeyId）
            ChatResponse response = chatService.chat(request, loginUser.getId(), null, clientIp, userAgent);
            return ResultUtils.success(response);
        }
    }
}