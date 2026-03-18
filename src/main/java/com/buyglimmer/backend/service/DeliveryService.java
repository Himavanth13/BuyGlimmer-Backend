package com.buyglimmer.backend.service;

import com.buyglimmer.backend.dto.FintechDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class DeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryService.class);

    public FintechDtos.DeliveryResponse createDelivery(FintechDtos.DeliveryCreateRequest request) {
        logger.info("Creating delivery record for orderId={}", request.orderId());
        return new FintechDtos.DeliveryResponse(
                "DLV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                request.orderId(),
                request.courierName(),
                request.trackingNumber(),
                "CREATED",
                null,
                request.estimatedDeliveryDate(),
                OffsetDateTime.now().toString()
        );
    }

    public FintechDtos.DeliveryResponse getDelivery(FintechDtos.DeliveryDetailRequest request) {
        logger.info("Fetching delivery detail deliveryId={}", request.deliveryId());
        return new FintechDtos.DeliveryResponse(
                request.deliveryId(),
                "ORDER-UNKNOWN",
                "UNKNOWN",
                "UNKNOWN",
                "IN_TRANSIT",
                "Hub",
                null,
                OffsetDateTime.now().toString()
        );
    }

    public FintechDtos.DeliveryResponse updateStatus(FintechDtos.DeliveryStatusUpdateRequest request) {
        logger.info("Updating delivery status deliveryId={} status={}", request.deliveryId(), request.status());
        return new FintechDtos.DeliveryResponse(
                request.deliveryId(),
                "ORDER-UNKNOWN",
                "UNKNOWN",
                "UNKNOWN",
                request.status(),
                request.currentLocation(),
                null,
                OffsetDateTime.now().toString()
        );
    }
}
