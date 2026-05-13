package com.leo.airouterbackend.model.dto.membership;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class PointAdjustRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    private Long points;

    private String description;
}
