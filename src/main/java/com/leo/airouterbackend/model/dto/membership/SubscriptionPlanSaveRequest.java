package com.leo.airouterbackend.model.dto.membership;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class SubscriptionPlanSaveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String planCode;

    private String planName;

    private String description;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer durationDays;

    private Integer lifetime;

    private Long dailyProLimit;

    private Long dailyAdvancedLimit;

    private Long monthlyProLimit;

    private Long monthlyAdvancedLimit;

    private Long bonusPoints;

    private Integer apiKeyLimit;

    private Integer allowByok;

    private Integer priority;

    private Integer visible;

    private String status;
}
