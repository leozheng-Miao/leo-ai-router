package com.leo.airouterbackend.model.dto.auth;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class AssignUserRolesRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private List<String> roleCodes;
}
