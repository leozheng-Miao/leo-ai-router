package com.leo.airouterbackend.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("conversation_message")
public class ConversationMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("conversation_id")
    private Long conversationId;

    @Column("user_id")
    private Long userId;

    private String role;

    private String content;

    private Integer mode;

    @Column("token_count")
    private Integer tokenCount;

    private Long seq;

    @Column("created_at")
    private LocalDateTime createdAt;
}
