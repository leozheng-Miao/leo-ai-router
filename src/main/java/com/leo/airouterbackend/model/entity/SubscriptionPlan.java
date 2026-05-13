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
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("subscription_plan")
public class SubscriptionPlan implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("planCode")
    private String planCode;

    @Column("planName")
    private String planName;

    private String description;

    private BigDecimal price;

    @Column("originalPrice")
    private BigDecimal originalPrice;

    @Column("durationDays")
    private Integer durationDays;

    private Integer lifetime;

    @Column("dailyProLimit")
    private Long dailyProLimit;

    @Column("dailyAdvancedLimit")
    private Long dailyAdvancedLimit;

    @Column("monthlyProLimit")
    private Long monthlyProLimit;

    @Column("monthlyAdvancedLimit")
    private Long monthlyAdvancedLimit;

    @Column("bonusPoints")
    private Long bonusPoints;

    @Column("apiKeyLimit")
    private Integer apiKeyLimit;

    @Column("allowByok")
    private Integer allowByok;

    private Integer priority;

    private Integer visible;

    private String status;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column("isDelete")
    private Integer isDelete;
}
