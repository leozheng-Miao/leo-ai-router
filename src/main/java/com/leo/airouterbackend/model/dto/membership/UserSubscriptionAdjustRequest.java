package com.leo.airouterbackend.model.dto.membership;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserSubscriptionAdjustRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String planCode;

    private LocalDateTime endTime;

    private Integer lifetime;
}
