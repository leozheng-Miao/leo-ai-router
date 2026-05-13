package com.leo.airouterbackend.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class MembershipVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String planCode;

    private String planName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer lifetime;

    private String status;

    private Long dailyProLimit;

    private Long dailyProUsed;

    private Long dailyProRemaining;

    private Long dailyAdvancedLimit;

    private Long dailyAdvancedUsed;

    private Long dailyAdvancedRemaining;

    private Long pointBalance;
}
