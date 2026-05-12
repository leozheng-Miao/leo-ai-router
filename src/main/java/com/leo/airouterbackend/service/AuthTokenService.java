package com.leo.airouterbackend.service;

import com.leo.airouterbackend.auth.JwtClaims;
import com.leo.airouterbackend.model.dto.auth.AuthLoginVO;
import com.leo.airouterbackend.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthTokenService {

    AuthLoginVO createLoginSession(User user, HttpServletRequest request);

    AuthLoginVO refresh(String refreshToken, HttpServletRequest request);

    void logout(String accessToken, String refreshToken);

    JwtClaims parseAndValidateAccessToken(String accessToken);
}
