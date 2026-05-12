package com.leo.airouterbackend.controller;

import cn.hutool.core.bean.BeanUtil;

import com.leo.airouterbackend.annotation.AuthCheck;
import com.leo.airouterbackend.common.BaseResponse;
import com.leo.airouterbackend.common.DeleteRequest;
import com.leo.airouterbackend.common.ResultUtils;
import com.leo.airouterbackend.constant.UserConstant;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.exception.ThrowUtils;
import com.leo.airouterbackend.model.dto.auth.AuthLoginVO;
import com.leo.airouterbackend.model.dto.auth.PhoneBindRequest;
import com.leo.airouterbackend.model.dto.auth.PhoneCodeVO;
import com.leo.airouterbackend.model.dto.auth.PhoneCodeRequest;
import com.leo.airouterbackend.model.dto.auth.PhoneLoginRequest;
import com.leo.airouterbackend.model.dto.auth.RefreshTokenRequest;
import com.leo.airouterbackend.model.dto.auth.WechatOAuthUrlVO;
import com.leo.airouterbackend.model.dto.email.ResetPasswordRequest;
import com.leo.airouterbackend.model.dto.email.SendEmailCodeRequest;
import com.leo.airouterbackend.model.dto.email.UserEmailBindRequest;
import com.leo.airouterbackend.model.dto.email.UserEmailLoginRequest;
import com.leo.airouterbackend.model.dto.email.UserEmailRegisterRequest;
import com.leo.airouterbackend.model.dto.user.UserAddRequest;
import com.leo.airouterbackend.model.dto.user.UserLoginRequest;
import com.leo.airouterbackend.model.dto.user.UserProfileUpdateRequest;
import com.leo.airouterbackend.model.dto.user.UserQueryRequest;
import com.leo.airouterbackend.model.dto.user.UserRegisterRequest;
import com.leo.airouterbackend.model.dto.user.UserSetPasswordRequest;
import com.leo.airouterbackend.model.dto.user.UserUpdateRequest;
import com.leo.airouterbackend.model.entity.User;
import com.leo.airouterbackend.model.vo.LoginUserVO;
import com.leo.airouterbackend.model.vo.UserVO;
import com.leo.airouterbackend.service.BillingService;
import com.leo.airouterbackend.service.AuthTokenService;
import com.leo.airouterbackend.service.PhoneAuthService;
import com.leo.airouterbackend.service.QuotaService;
import com.leo.airouterbackend.service.RbacService;
import com.leo.airouterbackend.service.RequestLogService;
import com.leo.airouterbackend.service.UserService;
import com.leo.airouterbackend.service.WechatOAuthService;
import com.leo.airouterbackend.service.impl.EmailService;
import com.leo.airouterbackend.utils.UserDefaultsUtils;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

import static com.leo.airouterbackend.constant.EmailConstant.SCENE_LOGIN;
import static com.leo.airouterbackend.constant.EmailConstant.SCENE_REGISTER;
import static com.leo.airouterbackend.constant.EmailConstant.SCENE_RESET;
import static com.leo.airouterbackend.constant.EmailConstant.SCENE_BIND;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private EmailService emailService;

    @Resource
    private QuotaService quotaService;

    @Resource
    private RequestLogService requestLogService;

    @Resource
    private BillingService billingService;

    @Resource
    private AuthTokenService authTokenService;

    @Resource
    private PhoneAuthService phoneAuthService;

    @Resource
    private WechatOAuthService wechatOAuthService;

    @Resource
    private RbacService rbacService;

    // ───────────────── 账号密码 登录/注册 ─────────────────

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        long id = userService.userRegister(
                userRegisterRequest.getUserAccount(),
                userRegisterRequest.getUserPassword(),
                userRegisterRequest.getCheckPassword());
        return ResultUtils.success(id);
    }

    @PostMapping("/login")
    public BaseResponse<AuthLoginVO> userLogin(
            @RequestBody UserLoginRequest req,
            HttpServletRequest request) {
        if (req == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        AuthLoginVO loginUserVO = userService.userLogin(
                req.getUserAccount(), req.getUserPassword(), request);
        return ResultUtils.success(loginUserVO);
    }

    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        return ResultUtils.success(userService.getLoginUserVO(userService.getLoginUser(request)));
    }

    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        return ResultUtils.success(userService.userLogout(request));
    }

    // ───────────────── 邮箱验证码 ─────────────────

    @PostMapping("/email/send-code")
    public BaseResponse<Boolean> sendEmailCode(
            @RequestBody @Validated SendEmailCodeRequest request) {
        emailService.sendVerifyCode(request.getEmail(), request.getScene());
        return ResultUtils.success(true);
    }

    @PostMapping("/email/login")
    public BaseResponse<AuthLoginVO> userLoginByEmail(
            @RequestBody @Validated UserEmailLoginRequest request,
            HttpServletRequest httpRequest) {
        emailService.verifyCode(request.getEmail(), request.getCode(), SCENE_LOGIN);
        AuthLoginVO vo = userService.userLoginByEmail(request.getEmail(), httpRequest);
        return ResultUtils.success(vo);
    }

    @PostMapping("/email/bind/send-code")
    public BaseResponse<Boolean> sendEmailBindCode(
            @RequestBody @Validated SendEmailCodeRequest request,
            HttpServletRequest httpRequest) {
        userService.getLoginUser(httpRequest);
        emailService.sendVerifyCode(request.getEmail(), SCENE_BIND);
        return ResultUtils.success(true);
    }

    @PostMapping("/email/bind")
    public BaseResponse<LoginUserVO> bindEmail(
            @RequestBody @Validated UserEmailBindRequest request,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        boolean result = userService.bindEmail(loginUser.getId(), request.getEmail(), request.getCode());
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "邮箱绑定失败");
        }
        return ResultUtils.success(userService.getLoginUserVO(userService.getById(loginUser.getId())));
    }

    @PostMapping("/phone/send-code")
    public BaseResponse<PhoneCodeVO> sendPhoneCode(@RequestBody @Validated PhoneCodeRequest request) {
        return ResultUtils.success(phoneAuthService.sendLoginCode(request.getPhone()));
    }

    @PostMapping("/phone/login")
    public BaseResponse<AuthLoginVO> userLoginByPhone(
            @RequestBody @Validated PhoneLoginRequest request,
            HttpServletRequest httpRequest) {
        return ResultUtils.success(phoneAuthService.loginByPhone(request.getPhone(), request.getCode(), httpRequest));
    }

    @PostMapping("/phone/bind/send-code")
    public BaseResponse<PhoneCodeVO> sendPhoneBindCode(@RequestBody @Validated PhoneCodeRequest request,
                                                       HttpServletRequest httpRequest) {
        userService.getLoginUser(httpRequest);
        return ResultUtils.success(phoneAuthService.sendBindCode(request.getPhone()));
    }

    @PostMapping("/phone/bind")
    public BaseResponse<LoginUserVO> bindPhone(@RequestBody @Validated PhoneBindRequest request,
                                               HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        boolean result = phoneAuthService.bindPhone(loginUser.getId(), request.getPhone(), request.getCode());
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "手机号绑定失败");
        }
        return ResultUtils.success(userService.getLoginUserVO(userService.getById(loginUser.getId())));
    }

    @GetMapping("/wechat/oauth/url")
    public BaseResponse<WechatOAuthUrlVO> getWechatOAuthUrl() {
        return ResultUtils.success(wechatOAuthService.buildAuthorizeUrl());
    }

    @GetMapping("/wechat/oauth/callback")
    public BaseResponse<AuthLoginVO> wechatOAuthCallback(@RequestParam String code,
                                                         @RequestParam String state,
                                                         HttpServletRequest request) {
        return ResultUtils.success(wechatOAuthService.callback(code, state, request));
    }

    @PostMapping("/token/refresh")
    public BaseResponse<AuthLoginVO> refreshToken(@RequestBody @Validated RefreshTokenRequest request,
                                                  HttpServletRequest httpRequest) {
        return ResultUtils.success(authTokenService.refresh(request.getRefreshToken(), httpRequest));
    }

    @PostMapping("/password/set")
    public BaseResponse<Boolean> setPassword(@RequestBody @Validated UserSetPasswordRequest request,
                                             HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        return ResultUtils.success(userService.setPassword(
                loginUser.getId(), request.getUserPassword(), request.getCheckPassword()));
    }

    @PostMapping("/profile/update")
    public BaseResponse<LoginUserVO> updateMyProfile(@RequestBody UserProfileUpdateRequest request,
                                                     HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        boolean updated = userService.updateMyProfile(
                loginUser.getId(), request.getUserName(), request.getUserAvatar(), request.getUserProfile());
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "个人资料更新失败");
        }
        return ResultUtils.success(userService.getLoginUserVO(userService.getById(loginUser.getId())));
    }

    @PostMapping("/email/register")
    public BaseResponse<Long> userRegisterByEmail(
            @RequestBody @Validated UserEmailRegisterRequest request) {
        emailService.verifyCode(request.getEmail(), request.getCode(), SCENE_REGISTER);
        long id = userService.userRegisterByEmail(
                request.getEmail(), request.getCode(),
                request.getUserPassword(), request.getCheckPassword());
        return ResultUtils.success(id);
    }

    @PostMapping("/email/reset-password")
    public BaseResponse<Boolean> resetPassword(
            @RequestBody @Validated ResetPasswordRequest request) {
        emailService.verifyCode(request.getEmail(), request.getCode(), SCENE_RESET);
        boolean result = userService.resetPassword(
                request.getEmail(), request.getNewPassword(), request.getCheckPassword());
        return ResultUtils.success(result);
    }

    // ───────────────── 管理员接口 ─────────────────

    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(Long id) {
        if (id <= 0) throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        User user = userService.getById(id);
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        return ResultUtils.success(user);
    }

    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(Long id) {
        User user = getUserById(id).getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        if (userAddRequest == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        user.setUserPassword(userService.getEncryptedPassword("12345678"));
        UserDefaultsUtils.fillNewUserDefaults(user);
        boolean save = userService.save(user);
        if (!save) throw new BusinessException(ErrorCode.OPERATION_ERROR, "添加用户失败");
        rbacService.ensureDefaultRole(user.getId(), user.getUserRole());
        return ResultUtils.success(user.getId());
    }

    @DeleteMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        return ResultUtils.success(userService.removeById(deleteRequest.getId()));
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean b = userService.updateById(user);
        if (!b) throw new BusinessException(ErrorCode.OPERATION_ERROR);
        if (userUpdateRequest.getUserRole() != null) {
            rbacService.assignUserRoles(userUpdateRequest.getId(), java.util.List.of(userUpdateRequest.getUserRole()));
        }
        return ResultUtils.success(b);
    }

    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        long pageSize = userQueryRequest.getPageSize();
        long pageNum = userQueryRequest.getPageNum();
        Page<User> userPage = userService.page(
                Page.of(pageNum, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        userVOPage.setRecords(userService.getUserVOList(userPage.getRecords()));
        return ResultUtils.success(userVOPage);
    }

    /**
     * 获取我的配额信息
     */
    @GetMapping("/quota/my")
    @Operation(summary = "获取我的配额信息")
    public BaseResponse<QuotaVO> getMyQuota(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        long remainingQuota = quotaService.getRemainingQuota(loginUser.getId());

        QuotaVO quotaVO = new QuotaVO();
        quotaVO.setTokenQuota(loginUser.getTokenQuota());
        quotaVO.setUsedTokens(loginUser.getUsedTokens() != null ? loginUser.getUsedTokens() : 0L);
        quotaVO.setRemainingQuota(remainingQuota);

        return ResultUtils.success(quotaVO);
    }

    /**
     * 设置用户配额（仅管理员）
     */
    @PostMapping("/quota/set")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "设置用户配额")
    public BaseResponse<Boolean> setUserQuota(@RequestBody QuotaUpdateRequest quotaUpdateRequest) {
        ThrowUtils.throwIf(quotaUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(quotaUpdateRequest.getUserId() == null, ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        ThrowUtils.throwIf(quotaUpdateRequest.getTokenQuota() == null, ErrorCode.PARAMS_ERROR, "配额不能为空");

        User user = new User();
        user.setId(quotaUpdateRequest.getUserId());
        user.setTokenQuota(quotaUpdateRequest.getTokenQuota());

        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 重置用户已使用配额（仅管理员）
     */
    @PostMapping("/quota/reset")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "重置用户已使用配额")
    public BaseResponse<Boolean> resetUserQuota(@RequestParam Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不合法");

        User user = new User();
        user.setId(userId);
        user.setUsedTokens(0L);

        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 禁用用户（仅管理员）
     */
    @PostMapping("/disable")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "禁用用户")
    public BaseResponse<Boolean> disableUser(@RequestParam Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不合法");

        // 不能禁用自己
        // 可以在这里添加更多校验

        boolean result = userService.disableUser(userId);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 启用用户（仅管理员）
     */
    @PostMapping("/enable")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "启用用户")
    public BaseResponse<Boolean> enableUser(@RequestParam Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不合法");

        boolean result = userService.enableUser(userId);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 获取用户使用分析数据（仅管理员）
     */
    @GetMapping("/analysis")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "获取用户使用分析数据")
    public BaseResponse<UserAnalysisVO> getUserAnalysis(@RequestParam Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不合法");

        User user = userService.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        UserAnalysisVO analysisVO = new UserAnalysisVO();
        analysisVO.setUserId(userId);
        analysisVO.setUserAccount(user.getUserAccount());
        analysisVO.setUserName(user.getUserName());
        analysisVO.setUserStatus(user.getUserStatus());
        analysisVO.setUserRole(user.getUserRole());

        // 配额信息
        analysisVO.setTokenQuota(user.getTokenQuota());
        analysisVO.setUsedTokens(user.getUsedTokens() != null ? user.getUsedTokens() : 0L);
        analysisVO.setRemainingQuota(quotaService.getRemainingQuota(userId));

        // 请求统计
        analysisVO.setTotalRequests(requestLogService.countUserRequests(userId));
        analysisVO.setSuccessRequests(requestLogService.countUserSuccessRequests(userId));
        analysisVO.setTotalTokens(requestLogService.countUserTokens(userId));

        // 费用统计
        analysisVO.setTotalCost(billingService.getUserTotalCost(userId));
        analysisVO.setTodayCost(billingService.getUserTodayCost(userId));

        return ResultUtils.success(analysisVO);
    }

    // endregion

    /**
     * 用户使用分析视图对象
     */
    @lombok.Data
    public static class UserAnalysisVO implements java.io.Serializable {
        private Long userId;
        private String userAccount;
        private String userName;
        private String userStatus;
        private String userRole;
        private Long tokenQuota;
        private Long usedTokens;
        private Long remainingQuota;
        private Long totalRequests;
        private Long successRequests;
        private Long totalTokens;
        private BigDecimal totalCost;
        private BigDecimal todayCost;
    }

    /**
     * 配额信息视图对象
     */
    @lombok.Data
    public static class QuotaVO implements java.io.Serializable {
        /**
         * Token配额（-1表示无限制）
         */
        private Long tokenQuota;

        /**
         * 已使用Token数
         */
        private Long usedTokens;

        /**
         * 剩余配额（-1表示无限制）
         */
        private Long remainingQuota;
    }

    /**
     * 配额更新请求
     */
    @lombok.Data
    public static class QuotaUpdateRequest implements java.io.Serializable {
        /**
         * 用户ID
         */
        private Long userId;

        /**
         * Token配额（-1表示无限制）
         */
        private Long tokenQuota;
    }

}
