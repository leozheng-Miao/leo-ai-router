package com.leo.airouterbackend.service;

import com.leo.airouterbackend.model.dto.auth.AuthLoginVO;
import com.leo.airouterbackend.model.dto.auth.WechatOAuthUrlVO;
import jakarta.servlet.http.HttpServletRequest;

public interface WechatOAuthService {

    WechatOAuthUrlVO buildAuthorizeUrl();

    AuthLoginVO callback(String code, String state, HttpServletRequest request);
}
