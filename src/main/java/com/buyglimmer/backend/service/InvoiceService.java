package com.buyglimmer.backend.service;

import com.buyglimmer.backend.dto.FintechDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class InvoiceService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);

    public FintechDtos.InvoiceResponse generateInvoice(FintechDtos.InvoiceGenerateRequest request) {
        logger.info("Generating invoice for orderId={}", request.orderId());
        String invoiceId = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new FintechDtos.InvoiceResponse(
                invoiceId,
                request.orderId(),
                request.customerId(),
                BigDecimal.ZERO,
                "GENERATED",
                "/api/v1/invoices/file/" + invoiceId,
                OffsetDateTime.now().toString()
        );
    }

    public FintechDtos.InvoiceResponse getInvoice(FintechDtos.InvoiceDetailRequest request) {
        logger.info("Fetching invoice detail invoiceId={}", request.invoiceId());
        return new FintechDtos.InvoiceResponse(
                request.invoiceId(),
                "ORDER-UNKNOWN",
                "CUSTOMER-UNKNOWN",
                BigDecimal.ZERO,
                "GENERATED",
                "/api/v1/invoices/file/" + request.invoiceId(),
                OffsetDateTime.now().toString()
        );
    }

    public FintechDtos.EmailNotificationResponse emailInvoice(FintechDtos.InvoiceEmailRequest request) {
        logger.info("Sending invoice email invoiceId={} to={}", request.invoiceId(), request.recipientEmail());
        return new FintechDtos.EmailNotificationResponse(
                "MAIL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                "NA",
                request.recipientEmail(),
                "Your BuyGlimmer Invoice",
                "INVOICE",
                "SENT",
                OffsetDateTime.now().toString()
        );
    }
}
