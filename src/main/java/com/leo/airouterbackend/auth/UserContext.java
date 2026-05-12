package com.leo.airouterbackend.auth;

import com.leo.airouterbackend.model.entity.User;

public final class UserContext {

    private static final ThreadLocal<User> USER_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<JwtClaims> CLAIMS_HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(User user, JwtClaims claims) {
        USER_HOLDER.set(user);
        CLAIMS_HOLDER.set(claims);
    }

    public static User getUser() {
        return USER_HOLDER.get();
    }

    public static JwtClaims getClaims() {
        return CLAIMS_HOLDER.get();
    }

    public static void clear() {
        USER_HOLDER.remove();
        CLAIMS_HOLDER.remove();
    }
}
