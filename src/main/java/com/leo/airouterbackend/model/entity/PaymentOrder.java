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
@Table("payment_order")
public class PaymentOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("orderNo")
    private String orderNo;

    @Column("userId")
    private Long userId;

    @Column("orderType")
    private String orderType;

    @Column("productCode")
    private String productCode;

    @Column("productName")
    private String productName;

    private BigDecimal amount;

    @Column("paymentMethod")
    private String paymentMethod;

    @Column("paymentId")
    private String paymentId;

    private String status;

    private String extra;

    @Column("paidTime")
    private LocalDateTime paidTime;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column("isDelete")
    private Integer isDelete;
}
