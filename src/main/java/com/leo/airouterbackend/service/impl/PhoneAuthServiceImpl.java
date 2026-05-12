package com.leo.airouterbackend.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.leo.airouterbackend.config.TencentSmsProperties;
import com.leo.airouterbackend.constant.UserConstant;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.mapper.UserMapper;
import com.leo.airouterbackend.model.dto.auth.AuthLoginVO;
import com.leo.airouterbackend.model.dto.auth.PhoneCodeVO;
import com.leo.airouterbackend.model.entity.User;
import com.leo.airouterbackend.model.enums.UserRoleEnum;
import com.leo.airouterbackend.service.AuthTokenService;
import com.leo.airouterbackend.service.PhoneAuthService;
import com.leo.airouterbackend.service.RbacService;
import com.leo.airouterbackend.utils.AuthPasswordUtils;
import com.leo.airouterbackend.utils.UserDefaultsUtils;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class PhoneAuthServiceImpl implements PhoneAuthService {

    private static final String PHONE_PATTERN = "^1[3-9]\\d{9}$";
    private static final String CODE_PREFIX = "sms:code:login:";
    private static final String COOL_DOWN_PREFIX = "sms:cooldown:login:";
    private static final String FAIL_PREFIX = "sms:fail:login:";
    private static final String BIND_SCENE = "bind";
    private static final String LOGIN_SCENE = "login";

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private TencentSmsClient tencentSmsClient;

    @Resource
    private TencentSmsProperties smsProperties;

    @Resource
    private UserMapper userMapper;

    @Resource
    private AuthTokenService authTokenService;

    @Resource
    private RbacService rbacService;

    @Override
    public PhoneCodeVO sendLoginCode(String phone) {
        return sendCode(phone, LOGIN_SCENE);
    }

    @Override
    public PhoneCodeVO sendBindCode(String phone) {
        return sendCode(phone, BIND_SCENE);
    }

    private PhoneCodeVO sendCode(String phone, String scene) {
        validatePhone(phone);
        RBucket<Boolean> cooldown = redissonClient.getBucket(COOL_DOWN_PREFIX + scene + ":" + phone);
        if (Boolean.TRUE.equals(cooldown.get())) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "验证码发送过于频繁");
        }
        String code = RandomUtil.randomNumbers(6);
        redissonClient.<String>getBucket(CODE_PREFIX + scene + ":" + phone)
                .set(code, smsProperties.getCodeExpireMinutes(), TimeUnit.MINUTES);
        cooldown.set(true, smsProperties.getCodeCoolDownSeconds(), TimeUnit.SECONDS);
        tencentSmsClient.sendLoginCode(phone, code);
        boolean mockMode = !smsProperties.isEnabled();
        return PhoneCodeVO.builder()
                .sent(smsProperties.isEnabled())
                .mockMode(mockMode)
                .devCode(mockMode && smsProperties.isLocalCodeVisible() ? code : null)
                .message(mockMode ? "短信服务未启用，已生成本地开发验证码" : "验证码已发送")
                .expireMinutes(smsProperties.getCodeExpireMinutes())
                .coolDownSeconds(smsProperties.getCodeCoolDownSeconds())
                .build();
    }

    @Override
    public AuthLoginVO loginByPhone(String phone, String code, HttpServletRequest request) {
        validatePhone(phone);
        if (StrUtil.isBlank(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码不能为空");
        }
        RAtomicLong failCounter = redissonClient.getAtomicLong(FAIL_PREFIX + LOGIN_SCENE + ":" + phone);
        if (failCounter.get() >= 5) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "验证码错误次数过多，请稍后重试");
        }
        RBucket<String> codeBucket = redissonClient.getBucket(CODE_PREFIX + LOGIN_SCENE + ":" + phone);
        String cachedCode = codeBucket.get();
        if (!code.equals(cachedCode)) {
            long count = failCounter.incrementAndGet();
            if (count == 1) {
                failCounter.expire(10, TimeUnit.MINUTES);
            }
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误或已过期");
        }
        codeBucket.delete();
        failCounter.delete();
        User user = userMapper.selectOneByQuery(QueryWrapper.create().eq("userPhone", phone));
        if (user == null) {
            user = new User();
            user.setUserPhone(phone);
            user.setPhoneVerified(1);
            user.setUserAccount("phone_" + phone + "_" + RandomUtil.randomNumbers(4));
            user.setUserPassword(AuthPasswordUtils.createUnsetPassword());
            user.setUserName("用户" + phone.substring(7));
            user.setUserRole(UserRoleEnum.USER.getValue());
            user.setTokenVersion(0);
            UserDefaultsUtils.fillNewUserDefaults(user);
            int inserted = userMapper.insert(user);
            if (inserted <= 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "账号创建失败");
            }
            rbacService.ensureDefaultRole(user.getId(), UserConstant.DEFAULT_ROLE);
        }
        return authTokenService.createLoginSession(user, request);
    }

    @Override
    public boolean bindPhone(Long userId, String phone, String code) {
        validatePhone(phone);
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        if (StrUtil.isBlank(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码不能为空");
        }
        RAtomicLong failCounter = redissonClient.getAtomicLong(FAIL_PREFIX + BIND_SCENE + ":" + phone);
        if (failCounter.get() >= 5) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "验证码错误次数过多，请稍后重试");
        }
        RBucket<String> codeBucket = redissonClient.getBucket(CODE_PREFIX + BIND_SCENE + ":" + phone);
        String cachedCode = codeBucket.get();
        if (!code.equals(cachedCode)) {
            long count = failCounter.incrementAndGet();
            if (count == 1) {
                failCounter.expire(10, TimeUnit.MINUTES);
            }
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误或已过期");
        }
        User existing = userMapper.selectOneByQuery(QueryWrapper.create().eq("userPhone", phone));
        if (existing != null && !userId.equals(existing.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该手机号已绑定其他账号");
        }
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setUserPhone(phone);
        updateUser.setPhoneVerified(1);
        int updated = userMapper.update(updateUser);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "手机号绑定失败");
        }
        codeBucket.delete();
        failCounter.delete();
        return true;
    }

    private void validatePhone(String phone) {
        if (StrUtil.isBlank(phone) || !phone.matches(PHONE_PATTERN)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式不正确");
        }
    }
}
