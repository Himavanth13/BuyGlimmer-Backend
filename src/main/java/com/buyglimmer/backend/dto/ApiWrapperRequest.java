package com.buyglimmer.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ApiWrapperRequest<T>(
        String token,
        String requestId,
        @NotNull @Valid T data
) {
}
