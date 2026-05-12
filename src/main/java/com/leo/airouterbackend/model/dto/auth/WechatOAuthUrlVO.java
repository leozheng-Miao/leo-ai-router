package com.leo.airouterbackend.model.dto.auth;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class WechatOAuthUrlVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String url;
    private String state;
    private Boolean enabled;
    private String message;
}
