package com.leo.airouterbackend.auth;

import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.model.entity.User;
import com.leo.airouterbackend.model.enums.UserStatusEnum;
import com.leo.airouterbackend.service.AuthTokenService;
import com.leo.airouterbackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    private AuthTokenService authTokenService;

    @Resource
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String authorization = request.getHeader("Authorization");
            if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
                String token = authorization.substring(7);
                JwtClaims claims = authTokenService.parseAndValidateAccessToken(token);
                User user = userService.getById(claims.getUserId());
                if (user != null && !UserStatusEnum.DISABLED.getValue().equals(user.getUserStatus())
                        && (user.getTokenVersion() == null || user.getTokenVersion().equals(claims.getTokenVersion()))) {
                    UserContext.set(user, claims);
                }
            }
            filterChain.doFilter(request, response);
        } catch (BusinessException e) {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
