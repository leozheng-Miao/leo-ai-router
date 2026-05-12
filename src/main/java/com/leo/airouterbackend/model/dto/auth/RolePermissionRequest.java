package com.leo.airouterbackend.model.dto.auth;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class RolePermissionRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String roleCode;
    private List<String> permissionCodes;
}
