package com.leo.airouterbackend.model.dto.auth;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class PhoneCodeRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
