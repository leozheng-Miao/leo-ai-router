package com.leo.airouterbackend.service.payment;

import com.leo.airouterbackend.model.dto.payment.PaymentCreateResult;
import com.leo.airouterbackend.model.entity.PaymentOrder;
import com.leo.airouterbackend.model.enums.PaymentMethodEnum;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentService {

    PaymentCreateResult createRecharge(Long userId, BigDecimal amount, PaymentMethodEnum paymentMethod);

    PaymentCreateResult createOrder(PaymentOrder order, PaymentMethodEnum paymentMethod);

    boolean handleAsyncNotify(PaymentMethodEnum paymentMethod, Map<String, String> params);

    boolean handleSyncReturn(PaymentMethodEnum paymentMethod, Map<String, String> params);
}
