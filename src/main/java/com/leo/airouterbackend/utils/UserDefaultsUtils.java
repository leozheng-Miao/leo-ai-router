package com.leo.airouterbackend.utils;

import com.leo.airouterbackend.model.entity.User;
import com.leo.airouterbackend.model.enums.UserStatusEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class UserDefaultsUtils {

    private static final long DEFAULT_TOKEN_QUOTA = 100000L;

    private UserDefaultsUtils() {
    }

    public static void fillNewUserDefaults(User user) {
        LocalDateTime now = LocalDateTime.now();
        if (user.getEditTime() == null) {
            user.setEditTime(now);
        }
        if (user.getCreateTime() == null) {
            user.setCreateTime(now);
        }
        if (user.getUpdateTime() == null) {
            user.setUpdateTime(now);
        }
        if (user.getIsDelete() == null) {
            user.setIsDelete(0);
        }
        if (user.getTokenQuota() == null) {
            user.setTokenQuota(DEFAULT_TOKEN_QUOTA);
        }
        if (user.getUsedTokens() == null) {
            user.setUsedTokens(0L);
        }
        if (user.getUserStatus() == null) {
            user.setUserStatus(UserStatusEnum.ACTIVE.getValue());
        }
        if (user.getBalance() == null) {
            user.setBalance(BigDecimal.ZERO);
        }
        if (user.getTokenVersion() == null) {
            user.setTokenVersion(0);
        }
        if (user.getPhoneVerified() == null) {
            user.setPhoneVerified(0);
        }
    }
}
