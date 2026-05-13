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
@Table("point_transaction")
public class PointTransaction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("userId")
    private Long userId;

    @Column("changeAmount")
    private Long changeAmount;

    @Column("balanceBefore")
    private Long balanceBefore;

    @Column("balanceAfter")
    private Long balanceAfter;

    @Column("transactionType")
    private String transactionType;

    @Column("refType")
    private String refType;

    @Column("refId")
    private Long refId;

    private String description;

    @Column("createTime")
    private LocalDateTime createTime;
}
