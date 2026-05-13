package com.leo.airouterbackend.model.dto.membership;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class PointPackageSaveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String packageCode;

    private String packageName;

    private Long points;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private String description;

    private String badge;

    private Integer priority;

    private Integer visible;

    private String status;
}
