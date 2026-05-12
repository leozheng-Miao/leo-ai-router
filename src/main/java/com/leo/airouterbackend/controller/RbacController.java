package com.leo.airouterbackend.controller;

import com.leo.airouterbackend.annotation.AuthCheck;
import com.leo.airouterbackend.common.BaseResponse;
import com.leo.airouterbackend.common.ResultUtils;
import com.leo.airouterbackend.constant.UserConstant;
import com.leo.airouterbackend.model.dto.auth.AssignUserRolesRequest;
import com.leo.airouterbackend.model.dto.auth.RolePermissionRequest;
import com.leo.airouterbackend.model.entity.rbac.Permission;
import com.leo.airouterbackend.model.entity.rbac.Role;
import com.leo.airouterbackend.service.RbacService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/rbac")
public class RbacController {

    @Resource
    private RbacService rbacService;

    @GetMapping("/roles")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE, mustPermissions = "admin:rbac")
    public BaseResponse<List<Role>> listRoles() {
        return ResultUtils.success(rbacService.listRoles());
    }

    @PostMapping("/roles")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE, mustPermissions = "admin:rbac")
    public BaseResponse<Boolean> addRole(@RequestBody Role role) {
        return ResultUtils.success(rbacService.saveRole(role));
    }

    @PostMapping("/roles/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE, mustPermissions = "admin:rbac")
    public BaseResponse<Boolean> updateRole(@RequestBody Role role) {
        return ResultUtils.success(rbacService.updateRole(role));
    }

    @GetMapping("/permissions")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE, mustPermissions = "admin:rbac")
    public BaseResponse<List<Permission>> listPermissions() {
        return ResultUtils.success(rbacService.listPermissions());
    }

    @PostMapping("/roles/permissions")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE, mustPermissions = "admin:rbac")
    public BaseResponse<Boolean> setRolePermissions(@RequestBody RolePermissionRequest request) {
        rbacService.setRolePermissions(request.getRoleCode(), request.getPermissionCodes());
        return ResultUtils.success(true);
    }

    @GetMapping("/roles/permissions")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE, mustPermissions = "admin:rbac")
    public BaseResponse<List<String>> listRolePermissions(@RequestParam String roleCode) {
        return ResultUtils.success(rbacService.listRolePermissions(roleCode));
    }

    @PostMapping("/users/roles")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE, mustPermissions = "admin:rbac")
    public BaseResponse<Boolean> assignUserRoles(@RequestBody AssignUserRolesRequest request) {
        rbacService.assignUserRoles(request.getUserId(), request.getRoleCodes());
        return ResultUtils.success(true);
    }
}
