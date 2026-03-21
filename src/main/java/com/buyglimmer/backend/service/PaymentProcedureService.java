package com.buyglimmer.backend.service;

import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.repository.PaymentProcedureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentProcedureService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentProcedureService.class);

    private final PaymentProcedureRepository paymentProcedureRepository;
    private final InvoiceService invoiceService;
    private final OrderProcedureService orderProcedureService;

    public PaymentProcedureService(PaymentProcedureRepository paymentProcedureRepository, 
                                   InvoiceService invoiceService, 
                                   OrderProcedureService orderProcedureService) {
        this.paymentProcedureRepository = paymentProcedureRepository;
        this.invoiceService = invoiceService;
        this.orderProcedureService = orderProcedureService;
    }

    public FintechDtos.PaymentResponse createPayment(FintechDtos.PaymentCreateRequest request) {
        logger.info("Creating payment for orderId={}", request.orderId());
        return paymentProcedureRepository.createPayment(request);
    }

    @Transactional
    public FintechDtos.PaymentResponse verifyPayment(FintechDtos.PaymentVerifyRequest request) {
        logger.info("Verifying payment paymentId={} status={}", request.paymentId(), request.status());
        FintechDtos.PaymentResponse paymentResponse = paymentProcedureRepository.verifyPayment(request);
        
        // Auto-generate invoice when payment is successfully verified
        if ("success".equalsIgnoreCase(paymentResponse.status())) {
            try {
                String orderId = paymentResponse.orderId();
                logger.info("Payment verified successfully for orderId={}. Auto-generating invoice...", orderId);
                
                // Fetch order details to get customerId
                FintechDtos.OrderDetailResponse orderDetail = orderProcedureService.getOrderDetail(
                        new FintechDtos.OrderDetailRequest(orderId)
                );
                
                // Generate invoice automatically with order data
                String customerId = orderDetail != null ? orderDetail.customerId() : "UNKNOWN";
                FintechDtos.InvoiceGenerateRequest invoiceRequest = new FintechDtos.InvoiceGenerateRequest(
                        orderId,
                        customerId,
                        "" // Email optional/can be fetched from user profile separately
                );
                
                FintechDtos.InvoiceDetailResponse invoice = invoiceService.generateInvoice(invoiceRequest);
                logger.info("Invoice auto-generated after payment verification: invoiceId={}, invoiceNumber={}, orderId={}", 
                        invoice.invoiceId(), invoice.invoiceNumber(), orderId);
            } catch (Exception ex) {
                // Log but don't fail payment verification if invoice generation fails
                logger.warn("Failed to auto-generate invoice after payment verification for orderId={}", 
                        paymentResponse.orderId(), ex);
            }
        }
        
        return paymentResponse;
    }
}
