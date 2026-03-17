package com.buyglimmer.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record ShippingAddressRequest(
            @NotBlank String name,
            @NotBlank String address,
            @NotBlank String city,
            @NotBlank String state,
            @NotBlank String pincode,
            @NotBlank String phone,
            @Email @NotBlank String email
    ) {
    }

    public record CheckoutRequest(
            @NotNull @Valid ShippingAddressRequest shippingAddress,
            @NotBlank String paymentMethod,
            String couponCode
    ) {
    }

    public record OrderItemResponse(
            String name,
            BigDecimal price,
            String image,
            String color,
            Integer quantity
    ) {
    }

    public record TrackingDetailsResponse(
            String trackingNumber,
            String carrier,
            String estimatedDelivery
    ) {
    }

    public record OrderResponse(
            String id,
            String date,
            String status,
            List<OrderItemResponse> items,
            BigDecimal total,
            String paymentMethod,
            ShippingAddressRequest shippingAddress,
            TrackingDetailsResponse tracking
    ) {
    }
}