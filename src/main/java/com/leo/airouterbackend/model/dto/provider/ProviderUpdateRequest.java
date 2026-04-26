package com.leo.airouterbackend.model.dto.provider;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ProviderUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String displayName;

    private String baseUrl;

    private String apiKey;

    private String status;

    private Integer priority;

    private String config;
}
