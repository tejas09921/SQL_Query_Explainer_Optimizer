package com.sqlexplainer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnalyzeRequest(
        @Valid @NotNull ConnectionRequest connection,
        @NotBlank String sql,
        boolean allowNonSelect,
        boolean includeBuffers
) {
}
