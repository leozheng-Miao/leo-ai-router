package com.leo.airouterbackend.auth;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JwtClaims {

    private Long userId;
    private String jti;
    private Integer tokenVersion;
    private List<String> roles;
    private long issuedAt;
    private long expiresAt;
}
