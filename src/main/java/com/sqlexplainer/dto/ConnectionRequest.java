package com.sqlexplainer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConnectionRequest(
        @NotBlank String host,
        @NotNull @Min(1) @Max(65535) Integer port,
        @NotBlank String database,
        @NotBlank String username,
        String password,
        String sslMode
) {
}
