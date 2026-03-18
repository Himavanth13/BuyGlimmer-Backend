package com.buyglimmer.backend.service;

import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.repository.PaymentProcedureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentProcedureService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentProcedureService.class);

    private final PaymentProcedureRepository paymentProcedureRepository;

    public PaymentProcedureService(PaymentProcedureRepository paymentProcedureRepository) {
        this.paymentProcedureRepository = paymentProcedureRepository;
    }

    public FintechDtos.PaymentResponse createPayment(FintechDtos.PaymentCreateRequest request) {
        logger.info("Creating payment for orderId={}", request.orderId());
        return paymentProcedureRepository.createPayment(request);
    }

    public FintechDtos.PaymentResponse verifyPayment(FintechDtos.PaymentVerifyRequest request) {
        logger.info("Verifying payment paymentId={}", request.paymentId());
        return paymentProcedureRepository.verifyPayment(request);
    }
}
