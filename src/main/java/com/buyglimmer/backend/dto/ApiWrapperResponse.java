package com.buyglimmer.backend.dto;

public record ApiWrapperResponse<T>(
        String requestId,
        String status,
        String message,
        T data
) {
}
