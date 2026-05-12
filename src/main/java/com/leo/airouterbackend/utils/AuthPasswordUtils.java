package com.leo.airouterbackend.utils;

import cn.hutool.core.util.IdUtil;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

public final class AuthPasswordUtils {

    private static final String SALT = "cecilia";
    private static final String UNSET_PASSWORD_PREFIX = "UNSET_PASSWORD:";

    private AuthPasswordUtils() {
    }

    public static String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex((SALT + password).getBytes(StandardCharsets.UTF_8));
    }

    public static String createUnsetPassword() {
        return UNSET_PASSWORD_PREFIX + IdUtil.fastSimpleUUID();
    }

    public static boolean hasPassword(String storedPassword) {
        return storedPassword != null && !storedPassword.startsWith(UNSET_PASSWORD_PREFIX);
    }
}
