package com.leo.airouterbackend.controller;

import cn.hutool.extra.servlet.JakartaServletUtil;
import com.leo.airouterbackend.common.BaseResponse;
import com.leo.airouterbackend.common.ResultUtils;
import com.leo.airouterbackend.model.dto.conversation.CreateConversationRequest;
import com.leo.airouterbackend.model.dto.conversation.SendMessageRequest;
import com.leo.airouterbackend.model.vo.ConversationVO;
import com.leo.airouterbackend.model.vo.MessageVO;
import com.leo.airouterbackend.service.ConversationService;
import com.leo.airouterbackend.service.MessageService;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/conversations")
public class ConversationController {

    @Resource
    private ConversationService conversationService;

    @Resource
    private MessageService messageService;

    @PostMapping
    public BaseResponse<Map<String, Long>> createConversation(@RequestHeader("X-User-Id") Long userId,
                                                              @RequestBody(required = false) CreateConversationRequest request) {
        Long conversationId = conversationService.createConversation(userId, request);
        return ResultUtils.success(Map.of("conversationId", conversationId));
    }

    @GetMapping
    public BaseResponse<Page<ConversationVO>> listConversations(@RequestHeader("X-User-Id") Long userId,
                                                                @RequestParam(defaultValue = "0") long page,
                                                                @RequestParam(defaultValue = "20") long size) {
        return ResultUtils.success(conversationService.listConversations(userId, page, size));
    }

    @GetMapping("/{conversationId}/messages")
    public BaseResponse<Page<MessageVO>> listMessages(@RequestHeader("X-User-Id") Long userId,
                                                      @PathVariable Long conversationId,
                                                      @RequestParam(defaultValue = "0") long page,
                                                      @RequestParam(defaultValue = "30") long size) {
        return ResultUtils.success(messageService.listMessages(userId, conversationId, page, size));
    }

    @PostMapping("/{conversationId}/messages/send")
    public BaseResponse<MessageVO> sendMessage(@RequestHeader("X-User-Id") Long userId,
                                               @PathVariable Long conversationId,
                                               @RequestBody SendMessageRequest request,
                                               HttpServletRequest httpRequest) {
        String clientIp = JakartaServletUtil.getClientIP(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResultUtils.success(messageService.sendMessage(userId, conversationId, request, clientIp, userAgent));
    }

    @PostMapping(value = "/{conversationId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamMessage(@RequestHeader("X-User-Id") Long userId,
                                      @PathVariable Long conversationId,
                                      @RequestBody SendMessageRequest request,
                                      HttpServletRequest httpRequest) {
        String clientIp = JakartaServletUtil.getClientIP(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return messageService.streamMessage(userId, conversationId, request, clientIp, userAgent);
    }

    @DeleteMapping("/{conversationId}")
    public BaseResponse<Boolean> deleteConversation(@RequestHeader("X-User-Id") Long userId,
                                                    @PathVariable Long conversationId) {
        return ResultUtils.success(conversationService.deleteConversation(userId, conversationId));
    }
}
