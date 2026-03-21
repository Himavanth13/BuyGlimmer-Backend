package com.buyglimmer.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);
    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            // Check if invoice table exists
            String checkTableSQL = "SELECT 1 FROM information_schema.tables WHERE table_schema = 'sabbpeapparels' AND table_name = 'invoice'";
            Integer exists = jdbcTemplate.queryForObject(checkTableSQL, Integer.class);
            
            if (exists == null) {
                createInvoiceTable();
            }
        } catch (Exception e) {
            logger.warn("Invoice table check failed, attempting to create: {}", e.getMessage());
            createInvoiceTable();
        }
    }

    private void createInvoiceTable() {
        try {
            String sql = "CREATE TABLE IF NOT EXISTS invoice (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "order_id VARCHAR(36) NOT NULL, " +
                    "invoice_number VARCHAR(50) UNIQUE NOT NULL, " +
                    "invoice_date TIMESTAMP, " +
                    "total_amount DECIMAL(12,2), " +
                    "tax_amount DECIMAL(12,2), " +
                    "discount_amount DECIMAL(12,2), " +
                    "status VARCHAR(20) DEFAULT 'generated', " +
                    "meta LONGTEXT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (order_id) REFERENCES orders(id)" +
                    ")";
            
            jdbcTemplate.execute(sql);
            logger.info("Invoice table created successfully");
        } catch (Exception e) {
            logger.error("Failed to create invoice table: {}", e.getMessage());
        }
    }
}
