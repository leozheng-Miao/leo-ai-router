package com.leo.airouterbackend.model.vo;

import com.mybatisflex.annotation.Column;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 脱敏后的登录用户信息
 */
@Data
public class LoginUserVO implements Serializable {

    /**
     * 用户 id
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户邮箱
     */
    private String userEmail;

    private String userPhone;

    private Integer phoneVerified;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    /**
     * 剩余配额
     */
    private Integer quota;

    /**
     * 成为会员时间
     */
    private LocalDateTime vipTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * token限额
     */
    private Long tokenQuota;

    /**
     * 用户已使用的token
     */
    private Long usedTokens;

    /**
     * 用户状态 active-正常， disabled-禁用
     */
    private String userStatus;

    /**
     * 用户余额
     */
    private BigDecimal balance;

    private Integer tokenVersion;

    private Boolean hasPassword;

    private Boolean needSetPassword;

    private static final long serialVersionUID = 1L;
}
