package com.leo.airouterbackend.service;

import com.leo.airouterbackend.model.dto.conversation.SendMessageRequest;
import com.leo.airouterbackend.model.entity.ConversationMessage;
import com.leo.airouterbackend.model.vo.MessageVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import reactor.core.publisher.Flux;

public interface MessageService extends IService<ConversationMessage> {

    Page<MessageVO> listMessages(Long userId, Long conversationId, long page, long size);

    MessageVO sendMessage(Long userId, Long conversationId, SendMessageRequest request, String clientIp, String userAgent);

    Flux<String> streamMessage(Long userId, Long conversationId, SendMessageRequest request, String clientIp, String userAgent);
}
