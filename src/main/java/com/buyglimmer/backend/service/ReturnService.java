package com.buyglimmer.backend.service;

import com.buyglimmer.backend.dto.FintechDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReturnService {

    private static final Logger logger = LoggerFactory.getLogger(ReturnService.class);

    public FintechDtos.ReturnResponse createReturn(FintechDtos.ReturnCreateRequest request) {
        logger.info("Creating return for orderId={} customerId={}", request.orderId(), request.customerId());
        return new FintechDtos.ReturnResponse(
                "RTN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                request.orderId(),
                request.customerId(),
                request.reason(),
                "REQUESTED",
                OffsetDateTime.now().toString()
        );
    }

    public FintechDtos.ReturnResponse getReturn(FintechDtos.ReturnDetailRequest request) {
        logger.info("Fetching return detail returnId={}", request.returnId());
        return new FintechDtos.ReturnResponse(
                request.returnId(),
                "ORDER-UNKNOWN",
                "CUSTOMER-UNKNOWN",
                "NA",
                "UNDER_REVIEW",
                OffsetDateTime.now().minusDays(1).toString()
        );
    }

    public List<FintechDtos.ReturnResponse> listReturns(FintechDtos.ReturnListRequest request) {
        logger.info("Listing returns for customerId={}", request.customerId());
        return List.of(
                new FintechDtos.ReturnResponse(
                        "RTN-SAMPLE-001",
                        "ORDER-SAMPLE-001",
                        request.customerId(),
                        "DAMAGED_ITEM",
                        "APPROVED",
                        OffsetDateTime.now().minusDays(2).toString()
                )
        );
    }
}
