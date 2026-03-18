package com.buyglimmer.backend.util;

import com.buyglimmer.backend.dto.ApiWrapperResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ApiResponseFactory {

    public String resolveRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestId;
    }

    public <T> ApiWrapperResponse<T> success(String requestId, String message, T data) {
        return new ApiWrapperResponse<>(resolveRequestId(requestId), "SUCCESS", message, data);
    }

    public ApiWrapperResponse<Object> failed(String requestId, String message) {
        return new ApiWrapperResponse<>(resolveRequestId(requestId), "FAILED", message, null);
    }
}
