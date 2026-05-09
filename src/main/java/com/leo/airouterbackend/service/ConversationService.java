package com.leo.airouterbackend.service;

import com.leo.airouterbackend.model.dto.conversation.CreateConversationRequest;
import com.leo.airouterbackend.model.entity.Conversation;
import com.leo.airouterbackend.model.vo.ConversationVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

public interface ConversationService extends IService<Conversation> {

    Long createConversation(Long userId, CreateConversationRequest request);

    Page<ConversationVO> listConversations(Long userId, long page, long size);

    Conversation validateOwner(Long userId, Long conversationId);

    void updateAfterMessage(Conversation conversation, String userContent);

    boolean deleteConversation(Long userId, Long conversationId);

    void evictUserConversationListCache(Long userId);
}
