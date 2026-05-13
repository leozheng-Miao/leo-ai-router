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
@Table("user_subscription")
public class UserSubscription implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("userId")
    private Long userId;

    @Column("planCode")
    private String planCode;

    @Column("planName")
    private String planName;

    private Integer priority;

    @Column("startTime")
    private LocalDateTime startTime;

    @Column("endTime")
    private LocalDateTime endTime;

    private Integer lifetime;

    private String status;

    @Column("paymentOrderId")
    private Long paymentOrderId;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column("isDelete")
    private Integer isDelete;
}
