package com.leo.airouterbackend.service;

import com.leo.airouterbackend.model.entity.rbac.Permission;
import com.leo.airouterbackend.model.entity.rbac.Role;
import com.leo.airouterbackend.model.entity.rbac.RolePlanLimit;

import java.util.List;
import java.util.Set;

public interface RbacService {

    List<Role> listRoles();

    boolean saveRole(Role role);

    boolean updateRole(Role role);

    List<Permission> listPermissions();

    Set<String> getUserRoleCodes(Long userId);

    Set<String> getUserPermissionCodes(Long userId);

    boolean hasRole(Long userId, String roleCode);

    boolean hasPermission(Long userId, String permissionCode);

    void assignUserRoles(Long userId, List<String> roleCodes);

    void setRolePermissions(String roleCode, List<String> permissionCodes);

    List<String> listRolePermissions(String roleCode);

    void ensureDefaultRole(Long userId, String roleCode);

    RolePlanLimit getBestPlanLimit(Long userId);
}
