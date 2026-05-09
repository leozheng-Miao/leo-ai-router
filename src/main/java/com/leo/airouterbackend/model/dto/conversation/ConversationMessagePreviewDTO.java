package com.leo.airouterbackend.model.dto.conversation;

import lombok.Data;

@Data
public class ConversationMessagePreviewDTO {

    private Long conversationId;

    private String content;
}
