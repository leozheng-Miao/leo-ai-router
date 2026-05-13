package com.leo.airouterbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.leo.airouterbackend.auth.UserContext;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.mapper.UserMapper;
import com.leo.airouterbackend.model.dto.auth.AuthLoginVO;
import com.leo.airouterbackend.model.dto.user.UserQueryRequest;
import com.leo.airouterbackend.model.entity.User;
import com.leo.airouterbackend.model.enums.UserRoleEnum;
import com.leo.airouterbackend.model.enums.UserStatusEnum;
import com.leo.airouterbackend.model.vo.LoginUserVO;
import com.leo.airouterbackend.model.vo.UserVO;
import com.leo.airouterbackend.service.AuthTokenService;
import com.leo.airouterbackend.service.PointService;
import com.leo.airouterbackend.service.RbacService;
import com.leo.airouterbackend.service.UserService;
import com.leo.airouterbackend.utils.AuthPasswordUtils;
import com.leo.airouterbackend.utils.UserDefaultsUtils;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.leo.airouterbackend.constant.EmailConstant.SCENE_BIND;
import static com.leo.airouterbackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @program: leo-ai-router-backend
 * @description:
 * @author: Miao Zheng
 * @date: 2026-04-07 14:32
 **/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private AuthTokenService authTokenService;

    @Resource
    private RbacService rbacService;

    @Resource
    private EmailService emailService;

    @Resource
    private PointService pointService;

    private static final long REGISTER_BONUS_POINTS = 10L;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名长度不能小于4");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能小于8");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码不一致");
        }
        QueryWrapper queryWrapper = QueryWrapper.create().eq("userAccount", userAccount);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名已存在");
        }
        String encryptedPassword = getEncryptedPassword(userPassword);
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptedPassword);
        user.setUserName("用户" + userAccount);
        user.setUserRole(UserRoleEnum.USER.getValue());
        user.setTokenVersion(0);
        UserDefaultsUtils.fillNewUserDefaults(user);
        boolean save = this.save(user);
        if (!save) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户注册失败");
        }
        rbacService.ensureDefaultRole(user.getId(), UserRoleEnum.USER.getValue());
        grantRegisterBonus(user.getId());
        return user.getId();
    }

    @Override
    public AuthLoginVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名长度不能小于4");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能小于8");
        }
        String encryptedPassword = getEncryptedPassword(userPassword);
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("userAccount", userAccount)
                .eq("userPassword", encryptedPassword);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名不存在或密码错误");
        }
        if (UserStatusEnum.DISABLED.getValue().equals(user.getUserStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账号已被禁用，请联系管理员");
        }
        rbacService.ensureDefaultRole(user.getId(), user.getUserRole());
        return authTokenService.createLoginSession(user, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthLoginVO userLoginByEmail(String email, HttpServletRequest request) {
        QueryWrapper queryWrapper = QueryWrapper.create().eq("userEmail", email);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        if (user == null) {
            user = new User();
            user.setUserEmail(email);
            String prefix = email.substring(0, email.indexOf('@'));
            user.setUserAccount("email_" + prefix + "_" + RandomUtil.randomNumbers(4));
            user.setUserPassword(AuthPasswordUtils.createUnsetPassword());
            user.setUserName("用户" + RandomUtil.randomNumbers(6));
            user.setUserRole(UserRoleEnum.USER.getValue());
            user.setTokenVersion(0);
            UserDefaultsUtils.fillNewUserDefaults(user);
            boolean save = this.save(user);
            if (!save) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "账号创建失败");
            }
            rbacService.ensureDefaultRole(user.getId(), UserRoleEnum.USER.getValue());
            grantRegisterBonus(user.getId());
        }
        rbacService.ensureDefaultRole(user.getId(), user.getUserRole());
        return authTokenService.createLoginSession(user, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long userRegisterByEmail(String email, String code, String userPassword, String checkPassword) {
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码不一致");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能小于8位");
        }
        QueryWrapper queryWrapper = QueryWrapper.create().eq("userEmail", email);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邮箱已注册");
        }
        String prefix = email.substring(0, email.indexOf('@'));
        User user = new User();
        user.setUserEmail(email);
        user.setUserAccount("email_" + prefix + "_" + RandomUtil.randomNumbers(4));
        user.setUserPassword(AuthPasswordUtils.encryptPassword(userPassword));
        user.setUserName("用户" + RandomUtil.randomNumbers(6));
        user.setUserRole(UserRoleEnum.USER.getValue());
        user.setTokenVersion(0);
        UserDefaultsUtils.fillNewUserDefaults(user);
        boolean save = this.save(user);
        if (!save) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "注册失败");
        }
        rbacService.ensureDefaultRole(user.getId(), UserRoleEnum.USER.getValue());
        grantRegisterBonus(user.getId());
        return user.getId();
    }

    private void grantRegisterBonus(Long userId) {
        pointService.grantPoints(userId, REGISTER_BONUS_POINTS, "register_bonus", "user", userId, "注册赠送积分");
    }

    @Override
    public boolean resetPassword(String email, String newPassword, String checkPassword) {
        if (!newPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码不一致");
        }
        if (newPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能小于8位");
        }
        QueryWrapper queryWrapper = QueryWrapper.create().eq("userEmail", email);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "该邮箱未注册");
        }
        user.setUserPassword(getEncryptedPassword(newPassword));
        return this.updateById(user);
    }

    @Override
    public boolean setPassword(Long userId, String userPassword, String checkPassword) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        if (StrUtil.hasBlank(userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码不一致");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能小于8位");
        }
        User user = new User();
        user.setId(userId);
        user.setUserPassword(AuthPasswordUtils.encryptPassword(userPassword));
        return this.updateById(user);
    }

    @Override
    public boolean updateMyProfile(Long userId, String userName, String userAvatar, String userProfile) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        User user = new User();
        user.setId(userId);
        user.setUserName(userName);
        user.setUserAvatar(userAvatar);
        user.setUserProfile(userProfile);
        return this.updateById(user);
    }

    @Override
    public boolean bindEmail(Long userId, String email, String code) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        if (StrUtil.hasBlank(email, code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱或验证码不能为空");
        }
        User existing = this.mapper.selectOneByQuery(QueryWrapper.create().eq("userEmail", email));
        if (existing != null && !userId.equals(existing.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该邮箱已绑定其他账号");
        }
        emailService.verifyCode(email, code, SCENE_BIND);
        User user = new User();
        user.setId(userId);
        user.setUserEmail(email);
        return this.updateById(user);
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) return null;
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        boolean hasPassword = AuthPasswordUtils.hasPassword(user.getUserPassword());
        loginUserVO.setHasPassword(hasPassword);
        loginUserVO.setNeedSetPassword(!hasPassword);
        return loginUserVO;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        User contextUser = UserContext.getUser();
        if (contextUser != null && contextUser.getId() != null) {
            User current = this.getById(contextUser.getId());
            if (current == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
            }
            return current;
        }
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currUser = (User) userObj;
        if (userObj == null || currUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        currUser = this.getById(currUser.getId());
        if (currUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        return currUser;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) return null;
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        boolean hasPassword = AuthPasswordUtils.hasPassword(user.getUserPassword());
        userVO.setHasPassword(hasPassword);
        userVO.setNeedSetPassword(!hasPassword);
        return userVO;
    }

    @Override
    public boolean isAdmin(User user) {
        UserRoleEnum role = UserRoleEnum.getEnumByValue(user.getUserRole());
        return role == UserRoleEnum.ADMIN;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollectionUtil.isEmpty(userList)) return new ArrayList<>();
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .eq("userRole", userRole)
                .like("userAccount", userAccount)
                .like("userName", userName)
                .like("userProfile", userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        String accessToken = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : null;
        String refreshToken = request.getHeader("X-Refresh-Token");
        authTokenService.logout(accessToken, refreshToken);
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null && accessToken == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户未登录");
        }
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public String getEncryptedPassword(String password) {
        return AuthPasswordUtils.encryptPassword(password);
    }

    @Override
    public boolean isUserDisabled(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = this.getById(userId);
        if (user == null) {
            return true; // 用户不存在，视为禁用
        }
        return UserStatusEnum.DISABLED.getValue().equals(user.getUserStatus());
    }

    @Override
    public boolean disableUser(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = new User();
        user.setId(userId);
        user.setUserStatus(UserStatusEnum.DISABLED.getValue());
        return this.updateById(user);
    }

    @Override
    public boolean enableUser(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = new User();
        user.setId(userId);
        user.setUserStatus(UserStatusEnum.ACTIVE.getValue());
        return this.updateById(user);
    }

}
