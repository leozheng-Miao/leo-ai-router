package com.leo.airouterbackend.model.entity.rbac;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Table("role_plan_limit")
public class RolePlanLimit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("roleCode")
    private String roleCode;

    @Column("tokenQuota")
    private Long tokenQuota;

    @Column("dailyRequestLimit")
    private Long dailyRequestLimit;

    @Column("monthlyRequestLimit")
    private Long monthlyRequestLimit;

    @Column("dailyImageLimit")
    private Long dailyImageLimit;

    @Column("dailyPluginLimit")
    private Long dailyPluginLimit;

    @Column("apiKeyLimit")
    private Integer apiKeyLimit;

    @Column("allowByok")
    private Integer allowByok;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;
}
