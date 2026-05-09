package com.leo.airouterbackend.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class MessageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String role;

    private String content;

    private Integer mode;

    private LocalDateTime createdAt;

    private Long seq;
}
