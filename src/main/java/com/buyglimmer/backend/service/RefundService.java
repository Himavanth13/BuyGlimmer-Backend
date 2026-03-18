package com.buyglimmer.backend.service;

import com.buyglimmer.backend.dto.FintechDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RefundService {

    private static final Logger logger = LoggerFactory.getLogger(RefundService.class);

    public FintechDtos.RefundResponse createRefund(FintechDtos.RefundCreateRequest request) {
        logger.info("Creating refund for returnId={} paymentId={}", request.returnId(), request.paymentId());
        return new FintechDtos.RefundResponse(
                "RFD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                request.returnId(),
                request.paymentId(),
                request.amount(),
                "INITIATED",
                OffsetDateTime.now().toString()
        );
    }

    public FintechDtos.RefundResponse getRefund(FintechDtos.RefundDetailRequest request) {
        logger.info("Fetching refund detail refundId={}", request.refundId());
        return new FintechDtos.RefundResponse(
                request.refundId(),
                "RTN-UNKNOWN",
                "PAY-UNKNOWN",
                null,
                "PROCESSING",
                OffsetDateTime.now().minusHours(4).toString()
        );
    }

    public List<FintechDtos.RefundResponse> listRefunds(FintechDtos.RefundListRequest request) {
        logger.info("Listing refunds for customerId={}", request.customerId());
        return List.of(
                new FintechDtos.RefundResponse(
                        "RFD-SAMPLE-001",
                        "RTN-SAMPLE-001",
                        "PAY-SAMPLE-001",
                        null,
                        "COMPLETED",
                        OffsetDateTime.now().minusDays(1).toString()
                )
        );
    }
}
