package com.leo.airouterbackend.model.dto.conversation;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class CreateConversationRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer convType;
}
