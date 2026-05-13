package com.leo.airouterbackend.model.vo;

import com.leo.airouterbackend.model.dto.payment.PaymentCreateResult;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentOrderVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String orderNo;

    private String orderType;

    private String productCode;

    private String productName;

    private BigDecimal amount;

    private String paymentMethod;

    private String paymentId;

    private String status;

    private LocalDateTime paidTime;

    private LocalDateTime createTime;

    private String displayType;

    private String redirectUrl;

    private String formHtml;

    private String checkoutUrl;

    private String sessionId;

    public void applyPaymentResult(PaymentCreateResult result) {
        if (result == null) {
            return;
        }
        this.paymentMethod = result.getPaymentMethod();
        this.paymentId = result.getPaymentId();
        this.displayType = result.getDisplayType();
        this.redirectUrl = result.getRedirectUrl();
        this.formHtml = result.getFormHtml();
        this.checkoutUrl = result.getRedirectUrl();
        this.sessionId = result.getPaymentId();
    }
}
