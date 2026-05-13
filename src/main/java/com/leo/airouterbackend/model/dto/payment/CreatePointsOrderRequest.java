package com.leo.airouterbackend.model.dto.payment;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class CreatePointsOrderRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String packageCode;

    private String paymentMethod;
}
