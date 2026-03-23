package com.buyglimmer.backend.service;

import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.repository.PaymentStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentStatusService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentStatusService.class);

    private final PaymentStatusRepository paymentStatusRepository;
    private final OrderProcedureService orderProcedureService;

    public PaymentStatusService(PaymentStatusRepository paymentStatusRepository, OrderProcedureService orderProcedureService) {
        this.paymentStatusRepository = paymentStatusRepository;
        this.orderProcedureService = orderProcedureService;
    }

    public FintechDtos.PaymentStatusUpdateResponse updateOrderPaymentStatusForCustomer(
            String authenticatedCustomerId,
            FintechDtos.PaymentStatusUpdateRequest request
    ) {
        // Enforce order ownership before applying payment status updates.
        orderProcedureService.getOrderDetailForCustomer(
                authenticatedCustomerId,
                new FintechDtos.OrderDetailRequest(request.orderId())
        );

        String normalizedStatus = request.status().trim().toUpperCase();
        logger.info("Updating payment status orderId={} status={}", request.orderId(), normalizedStatus);
        return paymentStatusRepository.updateOrderPaymentStatus(request.orderId(), normalizedStatus, request.gatewayTxnId());
    }
}
