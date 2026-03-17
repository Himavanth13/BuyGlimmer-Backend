package com.buyglimmer.backend.dto;

public record ApiResponse<T>(
        String operation,
        String status,
        T payload
) {
}