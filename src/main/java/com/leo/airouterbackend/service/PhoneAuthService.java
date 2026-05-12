package com.leo.airouterbackend.service;

import com.leo.airouterbackend.model.dto.auth.AuthLoginVO;
import com.leo.airouterbackend.model.dto.auth.PhoneCodeVO;
import jakarta.servlet.http.HttpServletRequest;

public interface PhoneAuthService {

    PhoneCodeVO sendLoginCode(String phone);

    PhoneCodeVO sendBindCode(String phone);

    AuthLoginVO loginByPhone(String phone, String code, HttpServletRequest request);

    boolean bindPhone(Long userId, String phone, String code);
}
