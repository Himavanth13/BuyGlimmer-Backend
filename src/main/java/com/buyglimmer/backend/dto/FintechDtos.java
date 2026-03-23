package com.buyglimmer.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class FintechDtos {

    private FintechDtos() {
    }

    public record ProductListRequest(
            String category
    ) {
    }

    public record ProductDetailRequest(
            @NotBlank String productId
    ) {
    }

    public record ProductSearchRequest(
            @NotBlank String keyword
    ) {
    }

    public record ProductSummaryResponse(
            String productId,
            String name,
            String brand,
            String description,
            BigDecimal price,
            BigDecimal mrp,
            Integer stock
    ) {
    }

    public record ProductDetailResponse(
            String productId,
            String name,
            String brand,
            String description,
            BigDecimal price,
            BigDecimal mrp,
            Integer stock,
            String sku,
            String imageUrl
    ) {
    }

    public record CartAddRequest(
            String customerId,
            String guestId,
            @NotBlank String productId,
            String variantId,
            @NotNull @Min(1) Integer quantity
    ) {
    }

    public record CartGetRequest(
            String customerId,
            String guestId
    ) {
    }

    public record CartUpdateRequest(
            String customerId,
            String guestId,
            @NotBlank String cartItemId,
            @NotNull @Min(1) Integer quantity
    ) {
    }

    public record CartRemoveRequest(
            String customerId,
            String guestId,
            @NotBlank String cartItemId
    ) {
    }

    public record CartItemResponse(
            String cartItemId,
            String customerId,
            String productId,
            String variantId,
            String productName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
    }

    public record OrderCreateRequest(
            @NotBlank String customerId,
            @NotBlank String addressId,
            String couponCode,
            @NotBlank String paymentMethod,
            @NotEmpty List<@Valid OrderItemInput> items
    ) {
    }

    public record InstantBuyRequest(
            @NotBlank String customerId,
            @NotBlank String addressId,
            @NotBlank String variantId,
            @NotNull @Min(1) Integer quantity,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
            String couponCode,
            @NotBlank String paymentMethod
    ) {
    }

    public record OrderItemInput(
            @NotBlank String variantId,
            @NotNull @Min(1) Integer quantity,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price
    ) {
    }

    public record OrderListRequest(
            @NotBlank String customerId
    ) {
    }

    public record OrderDetailRequest(
            @NotBlank String orderId
    ) {
    }

    public record OrderSummaryResponse(
            String orderId,
            String customerId,
            BigDecimal totalAmount,
            String status,
            String paymentStatus,
            String createdAt
    ) {
    }

    public record OrderItemResponse(
            String orderItemId,
            String variantId,
            String productName,
            Integer quantity,
            BigDecimal price,
            BigDecimal total
    ) {
    }

    public record OrderDetailResponse(
            String orderId,
            String customerId,
            BigDecimal totalAmount,
            String status,
            String paymentStatus,
            String createdAt,
            List<OrderItemResponse> items
    ) {
    }

    public record PaymentCreateRequest(
            @NotBlank String orderId,
            @NotBlank String method,
            String gatewayTxnId,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount
    ) {
    }

    public record PaymentVerifyRequest(
            @NotBlank String paymentId,
            @NotBlank String gatewayTxnId,
            @NotBlank String status
    ) {
    }

    public record PaymentCallbackRequest(
            @NotBlank String paymentId,
            @NotBlank String gatewayTxnId,
            @NotBlank String status
    ) {
    }

    public record PaymentResponse(
            String paymentId,
            String orderId,
            String method,
            String gatewayTxnId,
            BigDecimal amount,
            String status
    ) {
    }

    public record UserProfileRequest(
            @NotBlank String customerId
    ) {
    }

    public record UserUpdateRequest(
            @NotBlank String customerId,
            @NotBlank String name,
            @Email @NotBlank String email,
            @NotBlank String mobile
    ) {
    }

    public record UserProfileResponse(
            String customerId,
            String name,
            String email,
            String mobile,
            Integer status,
            String createdAt
    ) {
    }

    public record AddressAddRequest(
            @NotBlank String customerId,
            @NotBlank String type,
            @NotBlank String addressLine,
            @NotBlank String city,
            @NotBlank String state,
            @NotBlank String pincode,
            Boolean isDefault
    ) {
    }

    public record AddressResponse(
            String addressId,
            String customerId,
            String type,
            String addressLine,
            String city,
            String state,
            String pincode,
            Boolean isDefault
    ) {
    }

    public record CouponValidateRequest(
            @NotBlank String customerId,
            @NotBlank String couponCode,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal orderAmount
    ) {
    }

    public record CouponValidationResponse(
            Boolean valid,
            BigDecimal discountAmount,
            String message
    ) {
    }

    public record InvoiceGenerateRequest(
            @NotBlank String orderId,
            @NotBlank String customerId,
            @Email @NotBlank String billingEmail
    ) {
    }

    public record InvoiceDetailRequest(
            @NotBlank String invoiceId
    ) {
    }

    public record InvoiceByOrderRequest(
            @NotBlank String orderId
    ) {
    }

    public record InvoiceEmailRequest(
            @NotBlank String invoiceId,
            @Email @NotBlank String recipientEmail
    ) {
    }

    public record InvoiceLineItem(
            String lineItemId,
            String productId,
            String productName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal itemTotal,
            BigDecimal discountAmount
    ) {
    }

    public record InvoiceDetailResponse(
            String invoiceId,
            String orderId,
            String invoiceNumber,
            String invoiceDate,
            BigDecimal totalAmount,
            BigDecimal discountAmount,
            String status,
            List<InvoiceLineItem> lineItems
    ) {
    }

    public record InvoiceResponse(
            String invoiceId,
            String orderId,
            String customerId,
            BigDecimal amount,
            String status,
            String invoiceUrl,
            String generatedAt
    ) {
    }

    public record EmailNotificationSendRequest(
            @NotBlank String customerId,
            @Email @NotBlank String toEmail,
            @NotBlank String subject,
            @NotBlank String messageType,
            @NotBlank String body
    ) {
    }

    public record EmailNotificationHistoryRequest(
            @NotBlank String customerId
    ) {
    }

    public record EmailNotificationResponse(
            String notificationId,
            String customerId,
            String toEmail,
            String subject,
            String messageType,
            String status,
            String sentAt
    ) {
    }

    public record DeliveryCreateRequest(
            @NotBlank String orderId,
            @NotBlank String courierName,
            @NotBlank String trackingNumber,
            String estimatedDeliveryDate,
            String destinationPincode,
            String serviceType,
            String dispatchDate
    ) {
    }

    public record DeliveryDetailRequest(
            @NotBlank String deliveryId
    ) {
    }

    public record DeliveryStatusUpdateRequest(
            @NotBlank String deliveryId,
            @NotBlank String status,
            String currentLocation,
            String remarks
    ) {
    }

    public record DeliveryResponse(
            String deliveryId,
            String orderId,
            String courierName,
            String trackingNumber,
            String status,
            String currentLocation,
            String estimatedDeliveryDate,
            String updatedAt
    ) {
    }

    public record ReturnCreateRequest(
            @NotBlank String orderId,
            @NotBlank String customerId,
            @NotBlank String reason,
            String comments
    ) {
    }

    public record ReturnDetailRequest(
            @NotBlank String returnId
    ) {
    }

    public record ReturnListRequest(
            @NotBlank String customerId
    ) {
    }

    public record ReturnResponse(
            String returnId,
            String orderId,
            String customerId,
            String reason,
            String status,
            String createdAt
    ) {
    }

    public record RefundCreateRequest(
            @NotBlank String returnId,
            @NotBlank String paymentId,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
            @NotBlank String reason
    ) {
    }

    public record RefundDetailRequest(
            @NotBlank String refundId
    ) {
    }

    public record RefundListRequest(
            @NotBlank String customerId
    ) {
    }

    public record RefundResponse(
            String refundId,
            String returnId,
            String paymentId,
            BigDecimal amount,
            String status,
            String processedAt
    ) {
    }
}
