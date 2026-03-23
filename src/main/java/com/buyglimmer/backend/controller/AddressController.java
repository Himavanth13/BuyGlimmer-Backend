package com.buyglimmer.backend.controller;

import com.buyglimmer.backend.dto.ApiWrapperRequest;
import com.buyglimmer.backend.dto.ApiWrapperResponse;
import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.service.AuthService;
import com.buyglimmer.backend.service.UserProcedureService;
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
@RequestMapping("/api/v1/address")
public class AddressController {

    private static final Logger logger = LoggerFactory.getLogger(AddressController.class);

    private final AuthService authService;
    private final UserProcedureService userProcedureService;
    private final ApiResponseFactory apiResponseFactory;

    public AddressController(AuthService authService, UserProcedureService userProcedureService, ApiResponseFactory apiResponseFactory) {
        this.authService = authService;
        this.userProcedureService = userProcedureService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/add")
    public ApiWrapperResponse<FintechDtos.AddressResponse> addAddress(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.AddressAddRequest> request) {
        authService.assertCustomerOwnership(request.token(), request.data().customerId());
        logger.info("POST /api/v1/address/add requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Address added successfully", userProcedureService.addAddress(request.data()));
    }

    @PostMapping("/list")
    public ApiWrapperResponse<List<FintechDtos.AddressResponse>> listAddress(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.AddressListRequest> request) {
        authService.assertCustomerOwnership(request.token(), request.data().customerId());
        logger.info("POST /api/v1/address/list requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Address list fetched successfully", userProcedureService.listAddresses(request.data()));
    }
}
