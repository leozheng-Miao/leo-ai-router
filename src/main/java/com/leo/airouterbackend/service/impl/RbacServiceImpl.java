package com.leo.airouterbackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.leo.airouterbackend.constant.UserConstant;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.mapper.UserMapper;
import com.leo.airouterbackend.mapper.rbac.PermissionMapper;
import com.leo.airouterbackend.mapper.rbac.RoleMapper;
import com.leo.airouterbackend.mapper.rbac.RolePermissionMapper;
import com.leo.airouterbackend.mapper.rbac.RolePlanLimitMapper;
import com.leo.airouterbackend.mapper.rbac.UserRoleMapper;
import com.leo.airouterbackend.model.entity.User;
import com.leo.airouterbackend.model.entity.rbac.Permission;
import com.leo.airouterbackend.model.entity.rbac.Role;
import com.leo.airouterbackend.model.entity.rbac.RolePermission;
import com.leo.airouterbackend.model.entity.rbac.RolePlanLimit;
import com.leo.airouterbackend.model.entity.rbac.UserRole;
import com.leo.airouterbackend.service.RbacService;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RbacServiceImpl implements RbacService {

    private static final String PERM_CACHE_PREFIX = "rbac:permissions:";

    @Resource
    private UserMapper userMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private PermissionMapper permissionMapper;

    @Resource
    private RolePermissionMapper rolePermissionMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private RolePlanLimitMapper rolePlanLimitMapper;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public List<Role> listRoles() {
        return roleMapper.selectListByQuery(QueryWrapper.create()
                .eq("status", "active")
                .orderBy("priority", false));
    }

    @Override
    public boolean saveRole(Role role) {
        if (role == null || StrUtil.isBlank(role.getRoleCode()) || StrUtil.isBlank(role.getRoleName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        role.setStatus(StrUtil.blankToDefault(role.getStatus(), "active"));
        role.setPriority(role.getPriority() == null ? 0 : role.getPriority());
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        return roleMapper.insert(role) > 0;
    }

    @Override
    public boolean updateRole(Role role) {
        if (role == null || role.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        role.setUpdateTime(LocalDateTime.now());
        return roleMapper.update(role) > 0;
    }

    @Override
    public List<Permission> listPermissions() {
        return permissionMapper.selectListByQuery(QueryWrapper.create()
                .eq("status", "active")
                .orderBy("permissionCode", true));
    }

    @Override
    public Set<String> getUserRoleCodes(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        List<UserRole> userRoles = userRoleMapper.selectListByQuery(QueryWrapper.create().eq("userId", userId));
        Set<String> roleCodes = userRoles.stream()
                .map(UserRole::getRoleCode)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (roleCodes.isEmpty()) {
            User user = userMapper.selectOneById(userId);
            if (user != null && StrUtil.isNotBlank(user.getUserRole())) {
                roleCodes.add(user.getUserRole());
            } else {
                roleCodes.add(UserConstant.DEFAULT_ROLE);
            }
        }
        return roleCodes;
    }

    @Override
    public Set<String> getUserPermissionCodes(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        RBucket<Set<String>> bucket = redissonClient.getBucket(PERM_CACHE_PREFIX + userId);
        Set<String> cached = bucket.get();
        if (cached != null) {
            return cached;
        }
        Set<String> roleCodes = getUserRoleCodes(userId);
        Set<String> permissions = new HashSet<>();
        if (roleCodes.contains(UserConstant.ADMIN_ROLE)) {
            permissions.add("*");
        } else if (!roleCodes.isEmpty()) {
            List<RolePermission> rolePermissions = rolePermissionMapper.selectListByQuery(QueryWrapper.create()
                    .in("roleCode", roleCodes));
            rolePermissions.stream()
                    .map(RolePermission::getPermissionCode)
                    .filter(StrUtil::isNotBlank)
                    .forEach(permissions::add);
        }
        bucket.set(permissions, 5, TimeUnit.MINUTES);
        return permissions;
    }

    @Override
    public boolean hasRole(Long userId, String roleCode) {
        if (StrUtil.isBlank(roleCode)) {
            return true;
        }
        Set<String> roleCodes = getUserRoleCodes(userId);
        if (roleCodes.contains(UserConstant.ADMIN_ROLE)) {
            return true;
        }
        if (UserConstant.DEFAULT_ROLE.equals(roleCode)) {
            return !roleCodes.isEmpty();
        }
        return roleCodes.contains(roleCode);
    }

    @Override
    public boolean hasPermission(Long userId, String permissionCode) {
        if (StrUtil.isBlank(permissionCode)) {
            return true;
        }
        Set<String> permissions = getUserPermissionCodes(userId);
        if (permissions.contains("*") || permissions.contains(permissionCode)) {
            return true;
        }
        for (String permission : permissions) {
            if (permission.endsWith(":*")) {
                String prefix = permission.substring(0, permission.length() - 1);
                if (permissionCode.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignUserRoles(Long userId, List<String> roleCodes) {
        if (userId == null || CollUtil.isEmpty(roleCodes)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        userRoleMapper.deleteByQuery(QueryWrapper.create().eq("userId", userId));
        for (String roleCode : roleCodes.stream().filter(StrUtil::isNotBlank).distinct().toList()) {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleCode(roleCode);
            userRole.setCreateTime(LocalDateTime.now());
            userRoleMapper.insert(userRole);
        }
        User user = new User();
        user.setId(userId);
        user.setUserRole(roleCodes.get(0));
        userMapper.update(user);
        invalidate(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setRolePermissions(String roleCode, List<String> permissionCodes) {
        if (StrUtil.isBlank(roleCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        rolePermissionMapper.deleteByQuery(QueryWrapper.create().eq("roleCode", roleCode));
        if (permissionCodes != null) {
            for (String permissionCode : permissionCodes.stream().filter(StrUtil::isNotBlank).distinct().toList()) {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRoleCode(roleCode);
                rolePermission.setPermissionCode(permissionCode);
                rolePermission.setCreateTime(LocalDateTime.now());
                rolePermissionMapper.insert(rolePermission);
            }
        }
        redissonClient.getKeys().deleteByPattern(PERM_CACHE_PREFIX + "*");
    }

    @Override
    public List<String> listRolePermissions(String roleCode) {
        if (StrUtil.isBlank(roleCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return rolePermissionMapper.selectListByQuery(QueryWrapper.create().eq("roleCode", roleCode))
                .stream()
                .map(RolePermission::getPermissionCode)
                .filter(StrUtil::isNotBlank)
                .toList();
    }

    @Override
    public void ensureDefaultRole(Long userId, String roleCode) {
        if (userId == null) {
            return;
        }
        long count = userRoleMapper.selectCountByQuery(QueryWrapper.create().eq("userId", userId));
        if (count > 0) {
            return;
        }
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleCode(StrUtil.blankToDefault(roleCode, UserConstant.DEFAULT_ROLE));
        userRole.setCreateTime(LocalDateTime.now());
        userRoleMapper.insert(userRole);
        invalidate(userId);
    }

    @Override
    public RolePlanLimit getBestPlanLimit(Long userId) {
        Set<String> roleCodes = getUserRoleCodes(userId);
        List<RolePlanLimit> limits = roleCodes.isEmpty()
                ? List.of()
                : rolePlanLimitMapper.selectListByQuery(QueryWrapper.create().in("roleCode", roleCodes));
        RolePlanLimit best = new RolePlanLimit();
        best.setTokenQuota(0L);
        best.setDailyRequestLimit(0L);
        best.setMonthlyRequestLimit(0L);
        best.setDailyImageLimit(0L);
        best.setDailyPluginLimit(0L);
        best.setApiKeyLimit(0);
        best.setAllowByok(0);
        for (RolePlanLimit limit : limits) {
            best.setTokenQuota(maxLimit(best.getTokenQuota(), limit.getTokenQuota()));
            best.setDailyRequestLimit(maxLimit(best.getDailyRequestLimit(), limit.getDailyRequestLimit()));
            best.setMonthlyRequestLimit(maxLimit(best.getMonthlyRequestLimit(), limit.getMonthlyRequestLimit()));
            best.setDailyImageLimit(maxLimit(best.getDailyImageLimit(), limit.getDailyImageLimit()));
            best.setDailyPluginLimit(maxLimit(best.getDailyPluginLimit(), limit.getDailyPluginLimit()));
            best.setApiKeyLimit(Math.max(best.getApiKeyLimit(), limit.getApiKeyLimit() == null ? 0 : limit.getApiKeyLimit()));
            best.setAllowByok(Math.max(best.getAllowByok(), limit.getAllowByok() == null ? 0 : limit.getAllowByok()));
        }
        return best;
    }

    private Long maxLimit(Long current, Long next) {
        if (current != null && current == -1L) {
            return -1L;
        }
        if (next != null && next == -1L) {
            return -1L;
        }
        return Math.max(current == null ? 0L : current, next == null ? 0L : next);
    }

    private void invalidate(Long userId) {
        redissonClient.getBucket(PERM_CACHE_PREFIX + userId).delete();
    }
}
