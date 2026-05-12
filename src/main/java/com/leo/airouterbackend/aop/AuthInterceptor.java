package com.leo.airouterbackend.aop;


import com.leo.airouterbackend.annotation.AuthCheck;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.model.entity.User;
import com.leo.airouterbackend.service.RbacService;
import com.leo.airouterbackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 权限校验 AOP
 *
 */
@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private RbacService rbacService;

    /**
     * 执行拦截
     *
     * @param joinPoint 切入点
     * @param authCheck 权限校验注解
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        if (!rbacService.hasRole(loginUser.getId(), mustRole)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        String[] mustPermissions = authCheck.mustPermissions();
        if (mustPermissions != null && mustPermissions.length > 0) {
            boolean passed = authCheck.requireAll();
            for (String permission : mustPermissions) {
                boolean hasPermission = rbacService.hasPermission(loginUser.getId(), permission);
                if (authCheck.requireAll() && !hasPermission) {
                    passed = false;
                    break;
                }
                if (!authCheck.requireAll() && hasPermission) {
                    passed = true;
                    break;
                }
            }
            if (!passed) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        // 通过权限校验，放行
        return joinPoint.proceed();
    }
}
