package com.buyglimmer.backend.controller;

import com.buyglimmer.backend.dto.ApiWrapperRequest;
import com.buyglimmer.backend.dto.ApiWrapperResponse;
import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.service.AuthService;
import com.buyglimmer.backend.service.ReturnService;
import com.buyglimmer.backend.util.ApiResponseFactory;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/returns")
public class ReturnController {

    private static final Logger logger = LoggerFactory.getLogger(ReturnController.class);

    private final AuthService authService;
    private final ReturnService returnService;
    private final ApiResponseFactory apiResponseFactory;

    public ReturnController(AuthService authService, ReturnService returnService, ApiResponseFactory apiResponseFactory) {
        this.authService = authService;
        this.returnService = returnService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/create")
    public ApiWrapperResponse<FintechDtos.ReturnResponse> create(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.ReturnCreateRequest> request) {
        authService.validateToken(request.token());
        logger.info("POST /api/v1/returns/create requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Return request created successfully", returnService.createReturn(request.data()));
    }

    @PostMapping("/detail")
    public ApiWrapperResponse<FintechDtos.ReturnResponse> detail(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.ReturnDetailRequest> request) {
        authService.validateToken(request.token());
        logger.info("POST /api/v1/returns/detail requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Return details fetched successfully", returnService.getReturn(request.data()));
    }

    @PostMapping("/list")
    public ApiWrapperResponse<List<FintechDtos.ReturnResponse>> list(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.ReturnListRequest> request) {
        authService.validateToken(request.token());
        logger.info("POST /api/v1/returns/list requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Returns fetched successfully", returnService.listReturns(request.data()));
    }
}
