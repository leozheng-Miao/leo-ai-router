package com.leo.airouterbackend.model.enums;

import lombok.Getter;

@Getter
public enum PaymentDisplayTypeEnum {

    REDIRECT_URL("redirect_url", "跳转链接"),
    FORM_HTML("form_html", "表单直出");

    private final String value;

    private final String text;

    PaymentDisplayTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }
}
