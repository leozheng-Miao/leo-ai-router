package com.leo.airouterbackend.service.impl.payment;

import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.model.dto.payment.PaymentCreateResult;
import com.leo.airouterbackend.model.enums.PaymentMethodEnum;
import com.leo.airouterbackend.service.payment.PaymentProvider;
import com.leo.airouterbackend.service.payment.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final Map<PaymentMethodEnum, PaymentProvider> providerMap = new EnumMap<>(PaymentMethodEnum.class);

    @Autowired
    public void setProviders(List<PaymentProvider> providers) {
        for (PaymentProvider provider : providers) {
            providerMap.put(provider.getPaymentMethod(), provider);
        }
    }

    @Override
    public PaymentCreateResult createRecharge(Long userId, BigDecimal amount, PaymentMethodEnum paymentMethod) {
        return getProvider(paymentMethod).createRecharge(userId, amount);
    }

    @Override
    public boolean handleAsyncNotify(PaymentMethodEnum paymentMethod, Map<String, String> params) {
        return getProvider(paymentMethod).handleAsyncNotify(params);
    }

    @Override
    public boolean handleSyncReturn(PaymentMethodEnum paymentMethod, Map<String, String> params) {
        return getProvider(paymentMethod).handleSyncReturn(params);
    }

    private PaymentProvider getProvider(PaymentMethodEnum paymentMethod) {
        PaymentProvider provider = providerMap.get(paymentMethod);
        if (provider == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的支付方式");
        }
        return provider;
    }
}
