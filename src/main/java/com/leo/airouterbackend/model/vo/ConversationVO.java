package com.leo.airouterbackend.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ConversationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String title;

    private Integer convType;

    private LocalDateTime lastMessageAt;

    private String lastMessagePreview;
}
