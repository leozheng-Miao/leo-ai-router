package com.leo.airouterbackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum PaymentMethodEnum {

    STRIPE("stripe", "Stripe"),
    ALIPAY("alipay", "支付宝");

    private final String value;

    private final String text;

    PaymentMethodEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static PaymentMethodEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (PaymentMethodEnum anEnum : PaymentMethodEnum.values()) {
            if (anEnum.value.equalsIgnoreCase(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
