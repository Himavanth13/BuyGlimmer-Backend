package com.buyglimmer.backend.service;

import com.buyglimmer.backend.dto.FintechDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Service
public class DeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryService.class);

    public FintechDtos.DeliveryResponse createDelivery(FintechDtos.DeliveryCreateRequest request) {
        logger.info("Creating delivery record for orderId={}", request.orderId());
        String eta = resolveEstimatedDeliveryDate(request);
        return new FintechDtos.DeliveryResponse(
                "DLV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                request.orderId(),
                request.courierName(),
                request.trackingNumber(),
                "CREATED",
                null,
            eta,
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

    private String resolveEstimatedDeliveryDate(FintechDtos.DeliveryCreateRequest request) {
        if (request.estimatedDeliveryDate() != null && !request.estimatedDeliveryDate().isBlank()) {
            return request.estimatedDeliveryDate();
        }

        LocalDate baseDate = parseDateOrToday(request.dispatchDate());
        int transitDays = estimateTransitDays(request.courierName(), request.destinationPincode(), request.serviceType());
        return baseDate.plusDays(transitDays).toString();
    }

    private LocalDate parseDateOrToday(String dispatchDate) {
        if (dispatchDate == null || dispatchDate.isBlank()) {
            return LocalDate.now();
        }

        try {
            return LocalDate.parse(dispatchDate);
        } catch (DateTimeParseException ex) {
            logger.warn("Invalid dispatchDate format: {}. Falling back to today.", dispatchDate);
            return LocalDate.now();
        }
    }

    private int estimateTransitDays(String courierName, String destinationPincode, String serviceType) {
        int days = 5;

        String normalizedService = serviceType == null ? "" : serviceType.trim().toLowerCase();
        if ("same_day".equals(normalizedService)) {
            days = 0;
        } else if ("express".equals(normalizedService)) {
            days = 2;
        } else if ("standard".equals(normalizedService)) {
            days = 5;
        }

        String normalizedCourier = courierName == null ? "" : courierName.trim().toLowerCase();
        if (normalizedCourier.contains("bluedart") || normalizedCourier.contains("dhl")) {
            days = Math.max(1, days - 1);
        }

        if (destinationPincode != null && destinationPincode.matches("^\\d{6}$")) {
            if (destinationPincode.startsWith("56") || destinationPincode.startsWith("11")) {
                days = Math.max(1, days - 1);
            } else if (destinationPincode.startsWith("79") || destinationPincode.startsWith("17")) {
                days += 2;
            }
        }

        return days;
    }
}
