package com.buyglimmer.backend.service;

import com.buyglimmer.backend.dto.FintechDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EmailNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    public FintechDtos.EmailNotificationResponse send(FintechDtos.EmailNotificationSendRequest request) {
        logger.info("Sending email notification customerId={} type={}", request.customerId(), request.messageType());
        return new FintechDtos.EmailNotificationResponse(
                "MAIL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                request.customerId(),
                request.toEmail(),
                request.subject(),
                request.messageType(),
                "SENT",
                OffsetDateTime.now().toString()
        );
    }

    public List<FintechDtos.EmailNotificationResponse> history(FintechDtos.EmailNotificationHistoryRequest request) {
        logger.info("Fetching email notification history customerId={}", request.customerId());
        return List.of(
                new FintechDtos.EmailNotificationResponse(
                        "MAIL-SAMPLE-001",
                        request.customerId(),
                        "customer@example.com",
                        "Order Confirmation",
                        "ORDER",
                        "SENT",
                        OffsetDateTime.now().minusDays(1).toString()
                )
        );
    }
}
