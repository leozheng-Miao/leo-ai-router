package com.leo.airouterbackend.model.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateResult implements Serializable {

    private Long recordId;

    private String paymentMethod;

    private String displayType;

    private String paymentId;

    private String redirectUrl;

    private String formHtml;
}
