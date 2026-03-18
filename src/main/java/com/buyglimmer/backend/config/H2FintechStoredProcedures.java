package com.buyglimmer.backend.config;

import org.h2.tools.SimpleResultSet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class H2FintechStoredProcedures {

    private H2FintechStoredProcedures() {
    }

    public static ResultSet spGetProducts(Connection connection) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("""
                SELECT p.id AS product_id,
                       p.name,
                       p.brand,
                       p.description,
                       MIN(v.price) AS price,
                       MIN(v.mrp) AS mrp,
                       SUM(v.stock) AS stock
                FROM product p
                JOIN product_variant v ON v.product_id = p.id
                GROUP BY p.id, p.name, p.brand, p.description
                ORDER BY p.name
                """);
        return statement.executeQuery();
    }

    public static ResultSet spGetProduct(Connection connection, String productId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("""
                SELECT p.id AS product_id,
                       p.name,
                       p.brand,
                       p.description,
                       v.price,
                       v.mrp,
                       v.stock,
                       v.sku,
                       v.images
                FROM product p
                JOIN product_variant v ON v.product_id = p.id
                WHERE p.id = ?
                ORDER BY v.price
                """);
        statement.setString(1, productId);
        ResultSet rows = statement.executeQuery();

        SimpleResultSet resultSet = new SimpleResultSet();
        resultSet.addColumn("product_id", Types.VARCHAR, 36, 0);
        resultSet.addColumn("name", Types.VARCHAR, 255, 0);
        resultSet.addColumn("brand", Types.VARCHAR, 255, 0);
        resultSet.addColumn("description", Types.VARCHAR, 2000, 0);
        resultSet.addColumn("price", Types.DECIMAL, 12, 2);
        resultSet.addColumn("mrp", Types.DECIMAL, 12, 2);
        resultSet.addColumn("stock", Types.INTEGER, 10, 0);
        resultSet.addColumn("sku", Types.VARCHAR, 100, 0);
        resultSet.addColumn("image_url", Types.VARCHAR, 2000, 0);

        if (rows.next()) {
            resultSet.addRow(
                    rows.getString("product_id"),
                    rows.getString("name"),
                    rows.getString("brand"),
                    rows.getString("description"),
                    rows.getBigDecimal("price"),
                    rows.getBigDecimal("mrp"),
                    rows.getInt("stock"),
                    rows.getString("sku"),
                    firstImage(rows.getString("images"))
            );
        }
        return resultSet;
    }

    public static ResultSet spSearchProducts(Connection connection, String keyword) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("""
                SELECT p.id AS product_id,
                       p.name,
                       p.brand,
                       p.description,
                       MIN(v.price) AS price,
                       MIN(v.mrp) AS mrp,
                       SUM(v.stock) AS stock
                FROM product p
                JOIN product_variant v ON v.product_id = p.id
                WHERE LOWER(p.name) LIKE LOWER(?) OR LOWER(COALESCE(p.description, '')) LIKE LOWER(?)
                GROUP BY p.id, p.name, p.brand, p.description
                ORDER BY p.name
                """);
        String like = "%" + keyword + "%";
        statement.setString(1, like);
        statement.setString(2, like);
        return statement.executeQuery();
    }

    public static ResultSet spAddToCart(Connection connection, String customerId, String productId, String variantId, Integer quantity) throws SQLException {
        String cartId = ensureActiveCart(connection, customerId);
        String finalVariantId = variantId == null || variantId.isBlank() ? findCheapestVariant(connection, productId) : variantId;

        PreparedStatement variantStmt = connection.prepareStatement("SELECT price FROM product_variant WHERE id = ?");
        variantStmt.setString(1, finalVariantId);
        ResultSet variantRows = variantStmt.executeQuery();
        if (!variantRows.next()) {
            return singleMessageResult("cart_item_id", null);
        }

        BigDecimal unitPrice = variantRows.getBigDecimal("price");
        String cartItemId = UUID.randomUUID().toString();

        PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO cart_item(id, cart_id, variant_id, qty, price)
                VALUES (?, ?, ?, ?, ?)
                """);
        insert.setString(1, cartItemId);
        insert.setString(2, cartId);
        insert.setString(3, finalVariantId);
        insert.setInt(4, quantity);
        insert.setBigDecimal(5, unitPrice);
        insert.executeUpdate();

        PreparedStatement rowQuery = connection.prepareStatement("""
                SELECT ci.id AS cart_item_id,
                       c.customer_id,
                       pv.product_id,
                       ci.variant_id,
                       p.name AS product_name,
                       ci.qty AS quantity,
                       ci.price AS unit_price,
                       ci.price * ci.qty AS line_total
                FROM cart_item ci
                JOIN cart c ON c.id = ci.cart_id
                JOIN product_variant pv ON pv.id = ci.variant_id
                JOIN product p ON p.id = pv.product_id
                WHERE ci.id = ?
                """);
        rowQuery.setString(1, cartItemId);
        return rowQuery.executeQuery();
    }

    public static ResultSet spGetCart(Connection connection, String customerId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("""
                SELECT ci.id AS cart_item_id,
                       c.customer_id,
                       pv.product_id,
                       ci.variant_id,
                       p.name AS product_name,
                       ci.qty AS quantity,
                       ci.price AS unit_price,
                       ci.price * ci.qty AS line_total
                FROM cart c
                JOIN cart_item ci ON ci.cart_id = c.id
                JOIN product_variant pv ON pv.id = ci.variant_id
                JOIN product p ON p.id = pv.product_id
                WHERE c.customer_id = ? AND c.status = 'active'
                ORDER BY ci.id
                """);
        statement.setString(1, customerId);
        return statement.executeQuery();
    }

    public static ResultSet spUpdateCartItem(Connection connection, String cartItemId, Integer quantity) throws SQLException {
        PreparedStatement update = connection.prepareStatement("UPDATE cart_item SET qty = ? WHERE id = ?");
        update.setInt(1, quantity);
        update.setString(2, cartItemId);
        int updated = update.executeUpdate();
        return singleIntResult(updated);
    }

    public static ResultSet spRemoveCartItem(Connection connection, String cartItemId) throws SQLException {
        PreparedStatement delete = connection.prepareStatement("DELETE FROM cart_item WHERE id = ?");
        delete.setString(1, cartItemId);
        int removed = delete.executeUpdate();
        return singleIntResult(removed);
    }

    public static ResultSet spCreateOrder(Connection connection, String customerId, String addressId, String couponCode, String paymentMethod) throws SQLException {
        String orderId = UUID.randomUUID().toString();
        PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO orders(id, customer_id, address_id, total_amount, status, payment_status, meta, created_at)
                VALUES (?, ?, ?, 0, 'CREATED', 'PENDING', ?, CURRENT_TIMESTAMP)
                """);
        statement.setString(1, orderId);
        statement.setString(2, customerId);
        statement.setString(3, addressId);
        statement.setString(4, "{\"coupon\":\"" + (couponCode == null ? "" : couponCode) + "\",\"paymentMethod\":\"" + paymentMethod + "\"}");
        statement.executeUpdate();

        SimpleResultSet resultSet = new SimpleResultSet();
        resultSet.addColumn("order_id", Types.VARCHAR, 36, 0);
        resultSet.addColumn("customer_id", Types.VARCHAR, 36, 0);
        resultSet.addColumn("total_amount", Types.DECIMAL, 12, 2);
        resultSet.addColumn("status", Types.VARCHAR, 20, 0);
        resultSet.addColumn("payment_status", Types.VARCHAR, 20, 0);
        resultSet.addColumn("created_at", Types.VARCHAR, 30, 0);
        resultSet.addRow(orderId, customerId, BigDecimal.ZERO, "CREATED", "PENDING", nowString());
        return resultSet;
    }

    public static ResultSet spAddOrderItems(Connection connection, String orderId, String variantId, Integer quantity, BigDecimal price) throws SQLException {
        String orderItemId = UUID.randomUUID().toString();
        BigDecimal total = price.multiply(BigDecimal.valueOf(quantity));

        PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO order_item(id, order_id, variant_id, qty, price, total)
                VALUES (?, ?, ?, ?, ?, ?)
                """);
        insert.setString(1, orderItemId);
        insert.setString(2, orderId);
        insert.setString(3, variantId);
        insert.setInt(4, quantity);
        insert.setBigDecimal(5, price);
        insert.setBigDecimal(6, total);
        insert.executeUpdate();

        PreparedStatement updateOrder = connection.prepareStatement("""
                UPDATE orders
                SET total_amount = (SELECT COALESCE(SUM(total), 0) FROM order_item WHERE order_id = ?)
                WHERE id = ?
                """);
        updateOrder.setString(1, orderId);
        updateOrder.setString(2, orderId);
        updateOrder.executeUpdate();

        return singleMessageResult("order_item_id", orderItemId);
    }

    public static ResultSet spGetOrders(Connection connection, String customerId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("""
                SELECT id AS order_id,
                       customer_id,
                       total_amount,
                       status,
                       payment_status,
                       CAST(created_at AS VARCHAR(30)) AS created_at
                FROM orders
                WHERE customer_id = ?
                ORDER BY created_at DESC
                """);
        statement.setString(1, customerId);
        return statement.executeQuery();
    }

    public static ResultSet spGetOrderDetail(Connection connection, String orderId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("""
                SELECT o.id AS order_id,
                       o.customer_id,
                       o.total_amount,
                       o.status,
                       o.payment_status,
                       CAST(o.created_at AS VARCHAR(30)) AS created_at,
                       oi.id AS order_item_id,
                       oi.variant_id,
                       p.name AS product_name,
                       oi.qty AS quantity,
                       oi.price,
                       oi.total
                FROM orders o
                LEFT JOIN order_item oi ON oi.order_id = o.id
                LEFT JOIN product_variant pv ON pv.id = oi.variant_id
                LEFT JOIN product p ON p.id = pv.product_id
                WHERE o.id = ?
                ORDER BY oi.id
                """);
        statement.setString(1, orderId);
        return statement.executeQuery();
    }

    public static ResultSet spCreatePayment(Connection connection, String orderId, String method, String gatewayTxnId, BigDecimal amount) throws SQLException {
        String paymentId = UUID.randomUUID().toString();
        PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO payment(id, order_id, method, gateway_txn_id, amount, status, meta, created_at)
                VALUES (?, ?, ?, ?, ?, 'CREATED', NULL, CURRENT_TIMESTAMP)
                """);
        statement.setString(1, paymentId);
        statement.setString(2, orderId);
        statement.setString(3, method);
        statement.setString(4, gatewayTxnId);
        statement.setBigDecimal(5, amount);
        statement.executeUpdate();

        PreparedStatement orderStatus = connection.prepareStatement("UPDATE orders SET payment_status = 'CREATED' WHERE id = ?");
        orderStatus.setString(1, orderId);
        orderStatus.executeUpdate();

        return paymentById(connection, paymentId);
    }

    public static ResultSet spVerifyPayment(Connection connection, String paymentId, String gatewayTxnId, String status) throws SQLException {
        PreparedStatement update = connection.prepareStatement("""
                UPDATE payment
                SET gateway_txn_id = ?, status = ?
                WHERE id = ?
                """);
        update.setString(1, gatewayTxnId);
        update.setString(2, status);
        update.setString(3, paymentId);
        update.executeUpdate();

        PreparedStatement orderStatus = connection.prepareStatement("""
                UPDATE orders
                SET payment_status = ?
                WHERE id = (SELECT order_id FROM payment WHERE id = ?)
                """);
        orderStatus.setString(1, status);
        orderStatus.setString(2, paymentId);
        orderStatus.executeUpdate();

        return paymentById(connection, paymentId);
    }

    public static ResultSet spGetProfile(Connection connection, String customerId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("""
                SELECT id AS customer_id,
                       name,
                       email,
                       mobile,
                       status,
                       CAST(created_at AS VARCHAR(30)) AS created_at
                FROM customer
                WHERE id = ?
                """);
        statement.setString(1, customerId);
        return statement.executeQuery();
    }

    public static ResultSet spUpdateProfile(Connection connection, String customerId, String name, String email, String mobile) throws SQLException {
        PreparedStatement update = connection.prepareStatement("""
                UPDATE customer
                SET name = ?, email = ?, mobile = ?
                WHERE id = ?
                """);
        update.setString(1, name);
        update.setString(2, email);
        update.setString(3, mobile);
        update.setString(4, customerId);
        update.executeUpdate();
        return spGetProfile(connection, customerId);
    }

    public static ResultSet spAddAddress(Connection connection, String customerId, String type, String addressLine, String city, String state, String pincode, Boolean isDefault) throws SQLException {
        if (Boolean.TRUE.equals(isDefault)) {
            PreparedStatement clearDefaults = connection.prepareStatement("UPDATE address SET is_default = FALSE WHERE customer_id = ?");
            clearDefaults.setString(1, customerId);
            clearDefaults.executeUpdate();
        }

        String addressId = UUID.randomUUID().toString();
        PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO address(id, customer_id, type, address_line, city, state, pincode, is_default)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """);
        insert.setString(1, addressId);
        insert.setString(2, customerId);
        insert.setString(3, type);
        insert.setString(4, addressLine);
        insert.setString(5, city);
        insert.setString(6, state);
        insert.setString(7, pincode);
        insert.setBoolean(8, Boolean.TRUE.equals(isDefault));
        insert.executeUpdate();

        PreparedStatement row = connection.prepareStatement("""
                SELECT id AS address_id, customer_id, type, address_line, city, state, pincode, is_default
                FROM address
                WHERE id = ?
                """);
        row.setString(1, addressId);
        return row.executeQuery();
    }

    public static ResultSet spValidateCoupon(Connection connection, String customerId, String couponCode, BigDecimal orderAmount) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("""
                SELECT code, discount_type, discount_value, min_order_amount, active
                FROM coupon
                WHERE LOWER(code) = LOWER(?)
                """);
        statement.setString(1, couponCode);
        ResultSet rows = statement.executeQuery();

        boolean valid = false;
        BigDecimal discount = BigDecimal.ZERO;
        String message = "Invalid coupon";

        if (rows.next()) {
            boolean active = rows.getBoolean("active");
            BigDecimal minOrderAmount = rows.getBigDecimal("min_order_amount");
            if (active && orderAmount.compareTo(minOrderAmount) >= 0) {
                valid = true;
                String discountType = rows.getString("discount_type");
                BigDecimal discountValue = rows.getBigDecimal("discount_value");
                if ("PERCENT".equalsIgnoreCase(discountType)) {
                    discount = orderAmount.multiply(discountValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                } else {
                    discount = discountValue;
                }
                message = "Coupon applied successfully";
            } else if (!active) {
                message = "Coupon is inactive";
            } else {
                message = "Order amount does not meet coupon minimum value";
            }
        }

        SimpleResultSet resultSet = new SimpleResultSet();
        resultSet.addColumn("is_valid", Types.BOOLEAN, 1, 0);
        resultSet.addColumn("discount_amount", Types.DECIMAL, 12, 2);
        resultSet.addColumn("message", Types.VARCHAR, 255, 0);
        resultSet.addRow(valid, discount, message);
        return resultSet;
    }

    private static ResultSet paymentById(Connection connection, String paymentId) throws SQLException {
        PreparedStatement payment = connection.prepareStatement("""
                SELECT id AS payment_id, order_id, method, gateway_txn_id, amount, status
                FROM payment
                WHERE id = ?
                """);
        payment.setString(1, paymentId);
        return payment.executeQuery();
    }

    private static String ensureActiveCart(Connection connection, String customerId) throws SQLException {
        PreparedStatement existing = connection.prepareStatement("SELECT id FROM cart WHERE customer_id = ? AND status = 'active' ORDER BY created_at DESC");
        existing.setString(1, customerId);
        ResultSet rows = existing.executeQuery();
        if (rows.next()) {
            return rows.getString("id");
        }

        String cartId = UUID.randomUUID().toString();
        PreparedStatement insert = connection.prepareStatement("INSERT INTO cart(id, customer_id, status, created_at) VALUES (?, ?, 'active', CURRENT_TIMESTAMP)");
        insert.setString(1, cartId);
        insert.setString(2, customerId);
        insert.executeUpdate();
        return cartId;
    }

    private static String findCheapestVariant(Connection connection, String productId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT id FROM product_variant WHERE product_id = ? AND status = TRUE ORDER BY price LIMIT 1");
        statement.setString(1, productId);
        ResultSet rows = statement.executeQuery();
        if (!rows.next()) {
            throw new SQLException("No variant found for productId=" + productId);
        }
        return rows.getString("id");
    }

    private static ResultSet singleIntResult(int value) {
        SimpleResultSet resultSet = new SimpleResultSet();
        resultSet.addColumn("result", Types.INTEGER, 10, 0);
        resultSet.addRow(value);
        return resultSet;
    }

    private static ResultSet singleMessageResult(String column, String value) {
        SimpleResultSet resultSet = new SimpleResultSet();
        resultSet.addColumn(column, Types.VARCHAR, 100, 0);
        resultSet.addRow(value);
        return resultSet;
    }

    private static String firstImage(String imagesJson) {
        if (imagesJson == null || imagesJson.isBlank()) {
            return null;
        }
        String cleaned = imagesJson.trim();
        if (cleaned.startsWith("[")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.endsWith("]")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        if (cleaned.isBlank()) {
            return null;
        }
        String first = cleaned.split(",")[0].trim();
        if (first.startsWith("\"")) {
            first = first.substring(1);
        }
        if (first.endsWith("\"")) {
            first = first.substring(0, first.length() - 1);
        }
        return first;
    }

    private static String nowString() {
        return Timestamp.from(java.time.Instant.now()).toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
