package com.leo.airouterbackend.model.dto.auth;

import com.leo.airouterbackend.model.vo.LoginUserVO;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class AuthLoginVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private LoginUserVO loginUser;
}
