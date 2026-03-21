package com.buyglimmer.backend.repository;

import com.buyglimmer.backend.dto.FintechDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class InvoiceProcedureRepository {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceProcedureRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public InvoiceProcedureRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public FintechDtos.InvoiceDetailResponse createInvoice(
            String orderId,
            String invoiceNumber,
            BigDecimal totalAmount,
            BigDecimal discountAmount) {
        try {
            List<FintechDtos.InvoiceDetailResponse> rows = jdbcTemplate.query(
                    "CALL sp_create_invoice(?, ?, ?, ?, ?)",
                    ps -> {
                        ps.setString(1, orderId);
                        ps.setString(2, invoiceNumber);
                        ps.setBigDecimal(3, totalAmount);
                        ps.setBigDecimal(4, BigDecimal.ZERO);  // tax_amount = 0
                        ps.setBigDecimal(5, discountAmount);
                    },
                    (rs, rowNum) -> new FintechDtos.InvoiceDetailResponse(
                            rs.getString("invoice_id"),
                            rs.getString("order_id"),
                            invoiceNumber,
                            rs.getString("invoice_date"),
                            rs.getBigDecimal("total_amount"),
                            rs.getBigDecimal("discount_amount"),
                            rs.getString("status"),
                            null  // lineItems populated separately
                    )
            );
            if (rows.isEmpty()) {
                throw new IllegalStateException("Invoice not created");
            }
            return rows.get(0);
        } catch (DataAccessException ex) {
            logger.warn("sp_create_invoice failed; using SQL fallback. orderId={}", orderId, ex);
            return createInvoiceFallback(orderId, invoiceNumber, totalAmount, discountAmount);
        }
    }

    private FintechDtos.InvoiceDetailResponse createInvoiceFallback(
            String orderId,
            String invoiceNumber,
            BigDecimal totalAmount,
            BigDecimal discountAmount) {
        String invoiceId = java.util.UUID.randomUUID().toString();
        
        jdbcTemplate.update(
                "INSERT INTO invoice(id, order_id, invoice_number, invoice_date, total_amount, tax_amount, discount_amount, status, created_at) " +
                        "VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, 'generated', CURRENT_TIMESTAMP)",
                invoiceId,
                orderId,
                invoiceNumber,
                totalAmount,
                BigDecimal.ZERO,  // tax_amount = 0
                discountAmount
        );

        return new FintechDtos.InvoiceDetailResponse(
                invoiceId,
                orderId,
                invoiceNumber,
                java.time.OffsetDateTime.now().toString(),
                totalAmount,
                discountAmount,
                "generated",
                null
        );
    }

    public FintechDtos.InvoiceDetailResponse getInvoiceById(String invoiceId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id AS invoice_id, order_id, invoice_number, CAST(invoice_date AS CHAR) AS invoice_date, " +
                            "total_amount, discount_amount, status FROM invoice WHERE id = ?",
                    (rs, rowNum) -> new FintechDtos.InvoiceDetailResponse(
                            rs.getString("invoice_id"),
                            rs.getString("order_id"),
                            rs.getString("invoice_number"),
                            rs.getString("invoice_date"),
                            rs.getBigDecimal("total_amount"),
                            rs.getBigDecimal("discount_amount"),
                            rs.getString("status"),
                            null  // lineItems populated separately
                    ),
                    invoiceId
            );
        } catch (DataAccessException ex) {
            logger.error("Failed to fetch invoice by id: {}", invoiceId, ex);
            return null;
        }
    }

    public FintechDtos.InvoiceDetailResponse getInvoiceByOrderId(String orderId) {
        try {
            List<FintechDtos.InvoiceDetailResponse> rows = jdbcTemplate.query(
                    "CALL sp_get_invoice_by_order(?)",
                    ps -> ps.setString(1, orderId),
                    (rs, rowNum) -> new FintechDtos.InvoiceDetailResponse(
                            rs.getString("invoice_id"),
                            rs.getString("order_id"),
                            rs.getString("invoice_number"),
                            rs.getString("invoice_date"),
                            rs.getBigDecimal("total_amount"),
                            rs.getBigDecimal("discount_amount"),
                            rs.getString("status"),
                            null  // lineItems populated separately
                    )
            );
            return rows.isEmpty() ? null : rows.get(0);
        } catch (DataAccessException ex) {
            logger.warn("sp_get_invoice_by_order failed; using SQL fallback. orderId={}", orderId, ex);
            return getInvoiceByOrderIdFallback(orderId);
        }
    }

    private FintechDtos.InvoiceDetailResponse getInvoiceByOrderIdFallback(String orderId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id AS invoice_id, order_id, invoice_number, CAST(invoice_date AS CHAR) AS invoice_date, " +
                            "total_amount, discount_amount, status FROM invoice WHERE order_id = ? " +
                            "ORDER BY invoice_date DESC LIMIT 1",
                    (rs, rowNum) -> new FintechDtos.InvoiceDetailResponse(
                            rs.getString("invoice_id"),
                            rs.getString("order_id"),
                            rs.getString("invoice_number"),
                            rs.getString("invoice_date"),
                            rs.getBigDecimal("total_amount"),
                            rs.getBigDecimal("discount_amount"),
                            rs.getString("status"),
                            null
                    ),
                    orderId
            );
        } catch (DataAccessException ex) {
            logger.warn("Failed to fetch invoice by orderId: {}", orderId, ex);
            return null;
        }
    }

    public void updateInvoiceStatus(String invoiceId, String status) {
        try {
            jdbcTemplate.update(
                    "UPDATE invoice SET status = ? WHERE id = ?",
                    status,
                    invoiceId
            );
            logger.info("Updated invoice status: invoiceId={}, status={}", invoiceId, status);
        } catch (DataAccessException ex) {
            logger.error("Failed to update invoice status: invoiceId={}", invoiceId, ex);
        }
    }
}
