package com.leo.airouterbackend.model.dto.auth;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class PhoneCodeVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Boolean sent;
    private Boolean mockMode;
    private String devCode;
    private String message;
    private Integer expireMinutes;
    private Integer coolDownSeconds;
}
