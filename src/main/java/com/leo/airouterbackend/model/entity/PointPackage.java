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
@Table("point_package")
public class PointPackage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column("packageCode")
    private String packageCode;

    @Column("packageName")
    private String packageName;

    private Long points;

    private BigDecimal price;

    @Column("originalPrice")
    private BigDecimal originalPrice;

    private String description;

    private String badge;

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
