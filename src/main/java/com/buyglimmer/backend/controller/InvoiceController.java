package com.buyglimmer.backend.controller;

import com.buyglimmer.backend.dto.ApiWrapperRequest;
import com.buyglimmer.backend.dto.ApiWrapperResponse;
import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.service.AuthService;
import com.buyglimmer.backend.service.InvoiceService;
import com.buyglimmer.backend.util.ApiResponseFactory;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceController.class);

    private final AuthService authService;
    private final InvoiceService invoiceService;
    private final ApiResponseFactory apiResponseFactory;

    public InvoiceController(AuthService authService, InvoiceService invoiceService, ApiResponseFactory apiResponseFactory) {
        this.authService = authService;
        this.invoiceService = invoiceService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/generate")
    public ApiWrapperResponse<FintechDtos.InvoiceDetailResponse> generate(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.InvoiceGenerateRequest> request) {
        String authenticatedCustomerId = authService.getAuthenticatedCustomerId(request.token());
        logger.info("POST /api/v1/invoices/generate requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Invoice generated successfully", invoiceService.generateInvoiceForCustomer(authenticatedCustomerId, request.data()));
    }

    @PostMapping("/detail")
    public ApiWrapperResponse<FintechDtos.InvoiceDetailResponse> detail(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.InvoiceDetailRequest> request) {
        String authenticatedCustomerId = authService.getAuthenticatedCustomerId(request.token());
        logger.info("POST /api/v1/invoices/detail requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Invoice fetched successfully", invoiceService.getInvoiceForCustomer(authenticatedCustomerId, request.data()));
    }

    @PostMapping("/by-order")
    public ApiWrapperResponse<FintechDtos.InvoiceDetailResponse> getByOrder(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.InvoiceByOrderRequest> request) {
        String authenticatedCustomerId = authService.getAuthenticatedCustomerId(request.token());
        logger.info("POST /api/v1/invoices/by-order requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Invoice fetched by order successfully", invoiceService.getInvoiceByOrderForCustomer(authenticatedCustomerId, request.data()));
    }

    @PostMapping("/email")
    public ApiWrapperResponse<FintechDtos.EmailNotificationResponse> email(
            @Valid @RequestBody ApiWrapperRequest<FintechDtos.InvoiceEmailRequest> request) {
        String authenticatedCustomerId = authService.getAuthenticatedCustomerId(request.token());
        logger.info("POST /api/v1/invoices/email requestId={}", request.requestId());
        return apiResponseFactory.success(request.requestId(), "Invoice email sent successfully", invoiceService.emailInvoiceForCustomer(authenticatedCustomerId, request.data()));
    }
}
