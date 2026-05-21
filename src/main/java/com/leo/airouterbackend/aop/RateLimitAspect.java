package com.leo.airouterbackend.aop;

import cn.hutool.extra.servlet.JakartaServletUtil;
import com.leo.airouterbackend.annotation.RateLimit;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.service.RateLimitService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    @Resource
    private RateLimitService rateLimitService;

    @Around("@annotation(rateLimit)")
    public Object doRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 获取请求对象
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }
        HttpServletRequest request = attributes.getRequest();

        LimitCheck limitCheck = checkLimit(rateLimit, request);
        if (limitCheck == null) {
            return joinPoint.proceed();
        }

        if (!limitCheck.allowed()) {
            log.warn("Rate limit exceeded, key: {}, limit: {}/{} {}",
                    limitCheck.logKey(), rateLimit.limit(), rateLimit.window(), rateLimit.timeUnit());
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "请求过于频繁，请稍后再试");
        }

        return joinPoint.proceed();
    }

    /**
     * 根据限流类型获取限流 key
     */
    private LimitCheck checkLimit(RateLimit rateLimit, HttpServletRequest request) {
        Duration duration = toDuration(rateLimit.window(), rateLimit.timeUnit());
        switch (rateLimit.type()) {
            case API_KEY:
                String authorization = request.getHeader("Authorization");
                if (authorization != null && authorization.startsWith("Bearer ")) {
                    String apiKey = authorization.substring(7);
                    return new LimitCheck("rate:api_key", rateLimitService.checkApiKeyRateLimit(apiKey, rateLimit.limit()));
                }
                return null;
            case IP:
                String ip = JakartaServletUtil.getClientIP(request);
                return new LimitCheck("rate:ip:" + ip, rateLimitService.tryAcquire("rate:ip:" + ip, rateLimit.limit(), duration));
            default:
                return null;
        }
    }

    /**
     * 将 TimeUnit 转换为 Duration
     */
    private Duration toDuration(long amount, TimeUnit timeUnit) {
        return switch (timeUnit) {
            case NANOSECONDS -> Duration.ofNanos(amount);
            case MICROSECONDS -> Duration.ofNanos(amount * 1000);
            case MILLISECONDS -> Duration.ofMillis(amount);
            case SECONDS -> Duration.ofSeconds(amount);
            case MINUTES -> Duration.ofMinutes(amount);
            case HOURS -> Duration.ofHours(amount);
            case DAYS -> Duration.ofDays(amount);
        };
    }

    private record LimitCheck(String logKey, boolean allowed) {
    }
}
