package com.leo.airouterbackend.service.payment;

import com.leo.airouterbackend.model.dto.payment.PaymentCreateResult;
import com.leo.airouterbackend.model.enums.PaymentMethodEnum;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentProvider {

    PaymentMethodEnum getPaymentMethod();

    PaymentCreateResult createRecharge(Long userId, BigDecimal amount);

    default boolean handleAsyncNotify(Map<String, String> params) {
        throw new UnsupportedOperationException("当前支付方式未实现异步通知");
    }

    default boolean handleSyncReturn(Map<String, String> params) {
        throw new UnsupportedOperationException("当前支付方式未实现同步回跳");
    }
}
