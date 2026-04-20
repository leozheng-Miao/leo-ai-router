package com.leo.airouterbackend.service;

import com.leo.airouterbackend.model.dto.user.UserQueryRequest;
import com.leo.airouterbackend.model.entity.User;
import com.leo.airouterbackend.model.vo.LoginUserVO;
import com.leo.airouterbackend.model.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface UserService extends IService<User> {

    /**
     * 用户注册
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     */
    User getLoginUser(HttpServletRequest request);

    LoginUserVO userLoginByEmail(String email, HttpServletRequest request);

    long userRegisterByEmail(String email, String code, String userPassword, String checkPassword);

    boolean resetPassword(String email, String newPassword, String checkPassword);

    /**
     * 获取脱敏的登录用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    List<UserVO> getUserVOList(List<User> userList);

    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 用户注销
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏的用户信息
     */
    UserVO getUserVO(User user);

    /**
     * 是否为管理员
     */
    boolean isAdmin(User user);

    String getEncryptedPassword(String password);

    boolean isUserDisabled(Long userId);

    boolean disableUser(Long userId);

    boolean enableUser(Long userId);
}