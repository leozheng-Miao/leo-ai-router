// ResetPasswordRequest.java
package com.leo.airouterbackend.model.dto.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class ResetPasswordRequest implements Serializable {
    @NotBlank @Email
    private String email;

    @NotBlank
    private String code;

    @NotBlank
    private String newPassword;

    @NotBlank
    private String checkPassword;
}