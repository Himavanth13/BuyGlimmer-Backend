package com.buyglimmer.backend.service;

import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.repository.InvoiceProcedureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class InvoiceService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);
    private static final String INVOICE_PREFIX = "INV";

    private final InvoiceProcedureRepository invoiceProcedureRepository;
    private final OrderProcedureService orderProcedureService;

    public InvoiceService(InvoiceProcedureRepository invoiceProcedureRepository, OrderProcedureService orderProcedureService) {
        this.invoiceProcedureRepository = invoiceProcedureRepository;
        this.orderProcedureService = orderProcedureService;
    }

    @Transactional
    public FintechDtos.InvoiceDetailResponse generateInvoice(FintechDtos.InvoiceGenerateRequest request) {
        logger.info("Generating invoice for orderId={}", request.orderId());

        // Fetch order details
        FintechDtos.OrderDetailResponse orderDetail = orderProcedureService.getOrderDetail(
                new FintechDtos.OrderDetailRequest(request.orderId())
        );

        if (orderDetail == null) {
            throw new IllegalArgumentException("Order not found: " + request.orderId());
        }

        // Total amount is the order total (no tax calculation)
        BigDecimal totalAmount = orderDetail.totalAmount();
        BigDecimal discountAmount = BigDecimal.ZERO;

        // Generate invoice number
        String invoiceNumber = generateInvoiceNumber();

        // Create and persist invoice
        FintechDtos.InvoiceDetailResponse invoice = invoiceProcedureRepository.createInvoice(
                request.orderId(),
                invoiceNumber,
                totalAmount,
                discountAmount
        );

        // Fetch line items from order
        List<FintechDtos.InvoiceLineItem> lineItems = extractLineItems(orderDetail.items());
        invoice = new FintechDtos.InvoiceDetailResponse(
                invoice.invoiceId(),
                invoice.orderId(),
                invoice.invoiceNumber(),
                invoice.invoiceDate(),
                invoice.totalAmount(),
                invoice.discountAmount(),
                invoice.status(),
                lineItems
        );

        logger.info("Invoice generated successfully: invoiceId={}, invoiceNumber={}", invoice.invoiceId(), invoiceNumber);
        return invoice;
    }

    public FintechDtos.InvoiceDetailResponse getInvoice(FintechDtos.InvoiceDetailRequest request) {
        logger.info("Fetching invoice detail invoiceId={}", request.invoiceId());
        FintechDtos.InvoiceDetailResponse invoice = invoiceProcedureRepository.getInvoiceById(request.invoiceId());

        if (invoice == null) {
            throw new IllegalArgumentException("Invoice not found: " + request.invoiceId());
        }

        // Fetch line items for the invoice's order
        if (invoice.orderId() != null) {
            FintechDtos.OrderDetailResponse orderDetail = orderProcedureService.getOrderDetail(
                    new FintechDtos.OrderDetailRequest(invoice.orderId())
            );
            if (orderDetail != null) {
                List<FintechDtos.InvoiceLineItem> lineItems = extractLineItems(orderDetail.items());
                invoice = new FintechDtos.InvoiceDetailResponse(
                        invoice.invoiceId(),
                        invoice.orderId(),
                        invoice.invoiceNumber(),
                        invoice.invoiceDate(),
                        invoice.totalAmount(),
                        invoice.discountAmount(),
                        invoice.status(),
                        lineItems
                );
            }
        }

        return invoice;
    }

    public FintechDtos.InvoiceDetailResponse getInvoiceByOrder(FintechDtos.InvoiceByOrderRequest request) {
        logger.info("Fetching invoice by orderId={}", request.orderId());
        FintechDtos.InvoiceDetailResponse invoice = invoiceProcedureRepository.getInvoiceByOrderId(request.orderId());

        if (invoice == null) {
            throw new IllegalArgumentException("Invoice not found for order: " + request.orderId());
        }

        // Fetch line items
        FintechDtos.OrderDetailResponse orderDetail = orderProcedureService.getOrderDetail(
                new FintechDtos.OrderDetailRequest(invoice.orderId())
        );
        if (orderDetail != null) {
            List<FintechDtos.InvoiceLineItem> lineItems = extractLineItems(orderDetail.items());
            invoice = new FintechDtos.InvoiceDetailResponse(
                    invoice.invoiceId(),
                    invoice.orderId(),
                    invoice.invoiceNumber(),
                    invoice.invoiceDate(),
                    invoice.totalAmount(),
                    invoice.discountAmount(),
                    invoice.status(),
                    lineItems
            );
        }

        return invoice;
    }

    public FintechDtos.EmailNotificationResponse emailInvoice(FintechDtos.InvoiceEmailRequest request) {
        logger.info("Sending invoice email invoiceId={} to={}", request.invoiceId(), request.recipientEmail());

        // Fetch invoice details
        FintechDtos.InvoiceDetailResponse invoice = invoiceProcedureRepository.getInvoiceById(request.invoiceId());
        if (invoice == null) {
            throw new IllegalArgumentException("Invoice not found: " + request.invoiceId());
        }

        // TODO: Integrate with EmailNotificationService to send actual email with invoice PDF
        // For now, return success response
        return new FintechDtos.EmailNotificationResponse(
                "MAIL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                invoice.invoiceId(),
                request.recipientEmail(),
                "Your BuyGlimmer Invoice - " + invoice.invoiceNumber(),
                "INVOICE",
                "SENT",
                OffsetDateTime.now().toString()
        );
    }

    private String generateInvoiceNumber() {
        // Format: INV-YYYYMMDD-XXXXXX (e.g., INV-20260321-A7F2B1)
        String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return INVOICE_PREFIX + "-" + date + "-" + randomSuffix;
    }

    private List<FintechDtos.InvoiceLineItem> extractLineItems(List<FintechDtos.OrderItemResponse> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return List.of();
        }

        return orderItems.stream()
                .map(item -> new FintechDtos.InvoiceLineItem(
                        UUID.randomUUID().toString(),
                        item.variantId(),
                        item.productName(),
                        item.quantity(),
                        item.price(),
                        item.total(),
                        BigDecimal.ZERO
                ))
                .toList();
    }
}
