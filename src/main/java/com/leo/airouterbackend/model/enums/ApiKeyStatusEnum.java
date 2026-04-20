package com.leo.airouterbackend.model.enums;

import lombok.Getter;

@Getter
public enum ApiKeyStatusEnum {

    ACTIVE("active", "正常可用"),
    INACTIVE("inactive", "未激活/禁用"),
    REVOKED("revoked", "已吊销");

    private final String value;
    private final String desc;

    ApiKeyStatusEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static ApiKeyStatusEnum fromValue(String value) {
        for (ApiKeyStatusEnum status : ApiKeyStatusEnum.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知状态: " + value);
    }
}