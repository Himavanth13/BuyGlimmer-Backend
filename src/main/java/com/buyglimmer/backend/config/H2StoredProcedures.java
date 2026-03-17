package com.buyglimmer.backend.config;

import org.h2.tools.SimpleResultSet;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class H2StoredProcedures {

    private H2StoredProcedures() {
    }

    public static ResultSet spFetchCategories(Connection connection) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("""
                SELECT c.name, COUNT(DISTINCT pc.product_id) AS product_count
                FROM category c
                LEFT JOIN product_category pc ON pc.category_id = c.id
                GROUP BY c.id, c.name
                ORDER BY c.name
                """);
        return statement.executeQuery();
    }

    public static ResultSet spFetchProducts(Connection connection, String category) throws SQLException {
        boolean hasCategory = category != null && !category.isBlank() && !"All".equalsIgnoreCase(category);
        String sql = """
                SELECT p.id, p.name, MIN(v.price) AS price, COALESCE(MIN(c.name), 'All') AS category, p.description
                FROM product p
                JOIN product_variant v ON v.product_id = p.id
                LEFT JOIN product_category pc ON pc.product_id = p.id
                LEFT JOIN category c ON c.id = pc.category_id
                """ + (hasCategory ? " WHERE c.name = ? " : "") + " GROUP BY p.id, p.name, p.description ORDER BY p.name";
        PreparedStatement statement = connection.prepareStatement(sql);
        if (hasCategory) {
            statement.setString(1, category);
        }
        return statement.executeQuery();
    }

    public static ResultSet spFetchProductById(Connection connection, String productId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("""
                SELECT p.id, p.name, MIN(v.price) AS price, COALESCE(MIN(c.name), 'All') AS category, p.description
                FROM product p
                JOIN product_variant v ON v.product_id = p.id
                LEFT JOIN product_category pc ON pc.product_id = p.id
                LEFT JOIN category c ON c.id = pc.category_id
                WHERE p.id = ?
                GROUP BY p.id, p.name, p.description
                """);
        statement.setString(1, productId);
        return statement.executeQuery();
    }

    public static ResultSet spFetchProductImages(Connection connection, String productId) throws SQLException {
        SimpleResultSet resultSet = singleStringColumn("image_url");
        PreparedStatement statement = connection.prepareStatement("SELECT images FROM product_variant WHERE product_id = ? ORDER BY price");
        statement.setString(1, productId);
        ResultSet rows = statement.executeQuery();
        while (rows.next()) {
            for (String image : parseJsonArray(rows.getString("images"))) {
                resultSet.addRow(image);
            }
        }
        return resultSet;
    }

    public static ResultSet spFetchProductSizes(Connection connection, String productId) throws SQLException {
        SimpleResultSet resultSet = singleStringColumn("size_value");
        PreparedStatement statement = connection.prepareStatement("SELECT attributes FROM product_variant WHERE product_id = ? ORDER BY price");
        statement.setString(1, productId);
        ResultSet rows = statement.executeQuery();
        List<String> seen = new ArrayList<>();
        while (rows.next()) {
            String size = jsonValue(rows.getString("attributes"), "size");
            if (size != null && !seen.contains(size)) {
                seen.add(size);
                resultSet.addRow(size);
            }
        }
        return resultSet;
    }

    public static ResultSet spFetchProductColors(Connection connection, String productId) throws SQLException {
        SimpleResultSet resultSet = new SimpleResultSet();
        resultSet.addColumn("color_name", Types.VARCHAR, 100, 0);
        resultSet.addColumn("hex_code", Types.VARCHAR, 20, 0);
        resultSet.addColumn("image_url", Types.VARCHAR, 1000, 0);

        PreparedStatement statement = connection.prepareStatement("SELECT attributes, images FROM product_variant WHERE product_id = ? ORDER BY price");
        statement.setString(1, productId);
        ResultSet rows = statement.executeQuery();
        List<String> seen = new ArrayList<>();
        while (rows.next()) {
            String color = jsonValue(rows.getString("attributes"), "color");
            if (color != null && !seen.contains(color)) {
                seen.add(color);
                String firstImage = parseJsonArray(rows.getString("images")).stream().findFirst().orElse(null);
                resultSet.addRow(color, colorHex(color), firstImage);
            }
        }
        return resultSet;
    }

    public static ResultSet spFetchProductSpecs(Connection connection, String productId) throws SQLException {
        SimpleResultSet resultSet = new SimpleResultSet();
        resultSet.addColumn("spec_key", Types.VARCHAR, 100, 0);
        resultSet.addColumn("spec_value", Types.VARCHAR, 255, 0);

        PreparedStatement statement = connection.prepareStatement("SELECT brand, meta FROM product WHERE id = ?");
        statement.setString(1, productId);
        ResultSet rows = statement.executeQuery();
        if (rows.next()) {
            resultSet.addRow("Brand", rows.getString("brand"));
            Map<String, String> metaValues = parseJsonObject(rows.getString("meta"));
            for (Map.Entry<String, String> entry : metaValues.entrySet()) {
                resultSet.addRow(capitalize(entry.getKey()), entry.getValue());
            }
        }
        return resultSet;
    }

    public static ResultSet spFetchProductReviews(Connection connection, String productId) {
        SimpleResultSet resultSet = new SimpleResultSet();
        resultSet.addColumn("user_name", Types.VARCHAR, 100, 0);
        resultSet.addColumn("rating", Types.INTEGER, 10, 0);
        resultSet.addColumn("comment_text", Types.VARCHAR, 1000, 0);
        resultSet.addColumn("review_date", Types.DATE, 10, 0);
        return resultSet;
    }

    public static ResultSet spFetchUserProfile(Connection connection, String userId) throws SQLException {
        SimpleResultSet resultSet = new SimpleResultSet();
        resultSet.addColumn("id", Types.VARCHAR, 36, 0);
        resultSet.addColumn("name", Types.VARCHAR, 150, 0);
        resultSet.addColumn("email", Types.VARCHAR, 150, 0);
        resultSet.addColumn("phone", Types.VARCHAR, 15, 0);
        resultSet.addColumn("address", Types.VARCHAR, 255, 0);
        resultSet.addColumn("avatar", Types.VARCHAR, 10, 0);

        PreparedStatement statement = connection.prepareStatement("""
                SELECT c.id, c.name, c.email, c.mobile, a.address_line
                FROM customer c
                LEFT JOIN address a ON a.customer_id = c.id AND a.is_default = TRUE
                WHERE c.id = ?
                """);
        statement.setString(1, userId);
        ResultSet rows = statement.executeQuery();
        if (rows.next()) {
            resultSet.addRow(
                    rows.getString("id"),
                    rows.getString("name"),
                    rows.getString("email"),
                    rows.getString("mobile"),
                    rows.getString("address_line"),
                    initials(rows.getString("name"))
            );
        }
        return resultSet;
    }

    public static ResultSet spFetchUserByEmail(Connection connection, String email) throws SQLException {
        SimpleResultSet resultSet = new SimpleResultSet();
        resultSet.addColumn("id", Types.VARCHAR, 36, 0);
        resultSet.addColumn("name", Types.VARCHAR, 150, 0);
        resultSet.addColumn("email", Types.VARCHAR, 150, 0);
        resultSet.addColumn("password", Types.VARCHAR, 255, 0);
        resultSet.addColumn("phone", Types.VARCHAR, 15, 0);
        resultSet.addColumn("address", Types.VARCHAR, 255, 0);
        resultSet.addColumn("avatar", Types.VARCHAR, 10, 0);

        PreparedStatement statement = connection.prepareStatement("""
                SELECT c.id, c.name, c.email, c.password_hash, c.mobile, a.address_line
                FROM customer c
                LEFT JOIN address a ON a.customer_id = c.id AND a.is_default = TRUE
                WHERE lower(c.email) = lower(?)
                """);
        statement.setString(1, email);
        ResultSet rows = statement.executeQuery();
        if (rows.next()) {
            resultSet.addRow(
                    rows.getString("id"),
                    rows.getString("name"),
                    rows.getString("email"),
                    rows.getString("password_hash"),
                    rows.getString("mobile"),
                    rows.getString("address_line"),
                    initials(rows.getString("name"))
            );
        }
        return resultSet;
    }

    public static String spRegisterUser(Connection connection, String name, String email, String password, String phone) throws SQLException {
        String userId = UUID.randomUUID().toString();
        PreparedStatement insertCustomer = connection.prepareStatement("""
                INSERT INTO customer(id, name, email, mobile, password_hash, status, meta, created_at)
                VALUES (?, ?, ?, ?, ?, 1, NULL, CURRENT_TIMESTAMP)
                """);
        insertCustomer.setString(1, userId);
        insertCustomer.setString(2, name);
        insertCustomer.setString(3, email);
        insertCustomer.setString(4, phone);
        insertCustomer.setString(5, password);
        insertCustomer.executeUpdate();

        PreparedStatement insertAddress = connection.prepareStatement("""
                INSERT INTO address(id, customer_id, type, address_line, city, state, pincode, is_default)
                VALUES (?, ?, 'home', '', '', '', '', TRUE)
                """);
        insertAddress.setString(1, UUID.randomUUID().toString());
        insertAddress.setString(2, userId);
        insertAddress.executeUpdate();
        return userId;
    }

    public static String spUpdateUserProfile(Connection connection, String userId, String name, String email, String phone, String address, String avatar) throws SQLException {
        PreparedStatement customerUpdate = connection.prepareStatement("""
                UPDATE customer
                SET name = ?, email = ?, mobile = ?
                WHERE id = ?
                """);
        customerUpdate.setString(1, name);
        customerUpdate.setString(2, email);
        customerUpdate.setString(3, phone);
        customerUpdate.setString(4, userId);
        customerUpdate.executeUpdate();

        PreparedStatement findAddress = connection.prepareStatement("SELECT id FROM address WHERE customer_id = ? AND is_default = TRUE");
        findAddress.setString(1, userId);
        ResultSet addressRows = findAddress.executeQuery();
        if (addressRows.next()) {
            PreparedStatement addressUpdate = connection.prepareStatement("UPDATE address SET address_line = ? WHERE id = ?");
            addressUpdate.setString(1, address);
            addressUpdate.setString(2, addressRows.getString("id"));
            addressUpdate.executeUpdate();
        } else {
            PreparedStatement insertAddress = connection.prepareStatement("""
                    INSERT INTO address(id, customer_id, type, address_line, city, state, pincode, is_default)
                    VALUES (?, ?, 'home', ?, '', '', '', TRUE)
                    """);
            insertAddress.setString(1, UUID.randomUUID().toString());
            insertAddress.setString(2, userId);
            insertAddress.setString(3, address);
            insertAddress.executeUpdate();
        }
        return userId;
    }

    public static ResultSet spFetchWishlist(Connection connection, String userId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT product_id FROM wishlist WHERE user_id = ? ORDER BY product_id");
        statement.setString(1, userId);
        return statement.executeQuery();
    }

    public static Integer spToggleWishlist(Connection connection, String userId, String productId) throws SQLException {
        PreparedStatement exists = connection.prepareStatement("SELECT COUNT(*) FROM wishlist WHERE user_id = ? AND product_id = ?");
        exists.setString(1, userId);
        exists.setString(2, productId);
        ResultSet rows = exists.executeQuery();
        rows.next();
        if (rows.getInt(1) > 0) {
            PreparedStatement delete = connection.prepareStatement("DELETE FROM wishlist WHERE user_id = ? AND product_id = ?");
            delete.setString(1, userId);
            delete.setString(2, productId);
            delete.executeUpdate();
        } else {
            PreparedStatement insert = connection.prepareStatement("INSERT INTO wishlist(user_id, product_id) VALUES (?, ?)");
            insert.setString(1, userId);
            insert.setString(2, productId);
            insert.executeUpdate();
        }
        return 1;
    }

    public static ResultSet spFetchCart(Connection connection, String userId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("""
                SELECT ci.id AS cart_item_id, v.product_id, ci.qty AS quantity, v.attributes
                FROM cart c
                JOIN cart_item ci ON ci.cart_id = c.id
                JOIN product_variant v ON v.id = ci.variant_id
                WHERE c.customer_id = ? AND c.status = 'active'
                ORDER BY c.created_at DESC, ci.id DESC
                """);
        statement.setString(1, userId);
        ResultSet rows = statement.executeQuery();

        SimpleResultSet resultSet = new SimpleResultSet();
        resultSet.addColumn("cart_item_id", Types.VARCHAR, 36, 0);
        resultSet.addColumn("product_id", Types.VARCHAR, 36, 0);
        resultSet.addColumn("quantity", Types.INTEGER, 10, 0);
        resultSet.addColumn("selected_size", Types.VARCHAR, 50, 0);
        resultSet.addColumn("selected_color", Types.VARCHAR, 50, 0);
        while (rows.next()) {
            String attributes = rows.getString("attributes");
            resultSet.addRow(
                    rows.getString("cart_item_id"),
                    rows.getString("product_id"),
                    rows.getInt("quantity"),
                    jsonValue(attributes, "size"),
                    jsonValue(attributes, "color")
            );
        }
        return resultSet;
    }

    public static String spAddCartItem(Connection connection, String userId, String productId, Integer quantity, String selectedSize, String selectedColor) throws SQLException {
        String cartId = ensureActiveCart(connection, userId);
        VariantChoice variant = findVariant(connection, productId, selectedSize, selectedColor);
        String cartItemId = UUID.randomUUID().toString();
        PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO cart_item(id, cart_id, variant_id, qty, price)
                VALUES (?, ?, ?, ?, ?)
                """);
        insert.setString(1, cartItemId);
        insert.setString(2, cartId);
        insert.setString(3, variant.variantId());
        insert.setInt(4, quantity);
        insert.setBigDecimal(5, variant.price());
        insert.executeUpdate();
        return cartItemId;
    }

    public static Integer spDeleteCartItem(Connection connection, String cartItemId) throws SQLException {
        PreparedStatement delete = connection.prepareStatement("DELETE FROM cart_item WHERE id = ?");
        delete.setString(1, cartItemId);
        return delete.executeUpdate();
    }

    public static Integer spClearCart(Connection connection, String userId) throws SQLException {
        String cartId = activeCartId(connection, userId);
        if (cartId == null) {
            return 0;
        }
        PreparedStatement delete = connection.prepareStatement("DELETE FROM cart_item WHERE cart_id = ?");
        delete.setString(1, cartId);
        return delete.executeUpdate();
    }

    public static String spUpsertAddress(Connection connection, String userId, String name, String addressLine, String city, String state, String pincode, String phone) throws SQLException {
        PreparedStatement existing = connection.prepareStatement("SELECT id FROM address WHERE customer_id = ? AND is_default = TRUE");
        existing.setString(1, userId);
        ResultSet rows = existing.executeQuery();
        String addressId;
        if (rows.next()) {
            addressId = rows.getString("id");
            PreparedStatement update = connection.prepareStatement("""
                    UPDATE address
                    SET address_line = ?, city = ?, state = ?, pincode = ?, type = 'home', is_default = TRUE
                    WHERE id = ?
                    """);
            update.setString(1, addressLine);
            update.setString(2, city);
            update.setString(3, state);
            update.setString(4, pincode);
            update.setString(5, addressId);
            update.executeUpdate();
        } else {
            addressId = UUID.randomUUID().toString();
            PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO address(id, customer_id, type, address_line, city, state, pincode, is_default)
                    VALUES (?, ?, 'home', ?, ?, ?, ?, TRUE)
                    """);
            insert.setString(1, addressId);
            insert.setString(2, userId);
            insert.setString(3, addressLine);
            insert.setString(4, city);
            insert.setString(5, state);
            insert.setString(6, pincode);
            insert.executeUpdate();
        }

        PreparedStatement updateCustomer = connection.prepareStatement("UPDATE customer SET name = ?, mobile = ? WHERE id = ?");
        updateCustomer.setString(1, name);
        updateCustomer.setString(2, phone);
        updateCustomer.setString(3, userId);
        updateCustomer.executeUpdate();
        return addressId;
    }

    public static String spCreateOrder(Connection connection, String customerId, String addressId) throws SQLException {
        String orderId = UUID.randomUUID().toString();
        PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO orders(id, customer_id, address_id, total_amount, status, payment_status, meta, created_at)
                VALUES (?, ?, ?, 0, 'pending', 'pending', NULL, CURRENT_TIMESTAMP)
                """);
        insert.setString(1, orderId);
        insert.setString(2, customerId);
        insert.setString(3, addressId);
        insert.executeUpdate();
        return orderId;
    }

    public static Integer spTransferCartToOrder(Connection connection, String userId, String orderId, String paymentMethod) throws SQLException {
        String cartId = activeCartId(connection, userId);
        if (cartId == null) {
            return 0;
        }

        PreparedStatement cartItems = connection.prepareStatement("""
                SELECT ci.variant_id, ci.qty, ci.price
                FROM cart_item ci
                WHERE ci.cart_id = ?
                """);
        cartItems.setString(1, cartId);
        ResultSet rows = cartItems.executeQuery();
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        while (rows.next()) {
            BigDecimal linePrice = rows.getBigDecimal("price");
            int qty = rows.getInt("qty");
            BigDecimal lineTotal = linePrice.multiply(BigDecimal.valueOf(qty));
            PreparedStatement insertItem = connection.prepareStatement("""
                    INSERT INTO order_item(id, order_id, variant_id, qty, price, total)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """);
            insertItem.setString(1, UUID.randomUUID().toString());
            insertItem.setString(2, orderId);
            insertItem.setString(3, rows.getString("variant_id"));
            insertItem.setInt(4, qty);
            insertItem.setBigDecimal(5, linePrice);
            insertItem.setBigDecimal(6, lineTotal);
            insertItem.executeUpdate();
            total = total.add(lineTotal);
            count++;
        }

        if (count == 0) {
            return 0;
        }

        PreparedStatement updateOrder = connection.prepareStatement("UPDATE orders SET total_amount = ?, meta = ? WHERE id = ?");
        updateOrder.setBigDecimal(1, total);
        updateOrder.setString(2, "{\"paymentMethod\":\"" + paymentMethod + "\"}");
        updateOrder.setString(3, orderId);
        updateOrder.executeUpdate();

        PreparedStatement paymentInsert = connection.prepareStatement("""
                INSERT INTO payment(id, order_id, method, gateway_txn_id, amount, status, meta, created_at)
                VALUES (?, ?, ?, NULL, ?, 'pending', NULL, CURRENT_TIMESTAMP)
                """);
        paymentInsert.setString(1, UUID.randomUUID().toString());
        paymentInsert.setString(2, orderId);
        paymentInsert.setString(3, paymentMethod);
        paymentInsert.setBigDecimal(4, total);
        paymentInsert.executeUpdate();

        PreparedStatement clearCart = connection.prepareStatement("DELETE FROM cart_item WHERE cart_id = ?");
        clearCart.setString(1, cartId);
        clearCart.executeUpdate();

        PreparedStatement markCart = connection.prepareStatement("UPDATE cart SET status = 'ordered' WHERE id = ?");
        markCart.setString(1, cartId);
        markCart.executeUpdate();
        return count;
    }

    public static ResultSet spFetchOrders(Connection connection, String userId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("""
                SELECT o.id, CAST(o.created_at AS DATE) AS order_date, o.status, o.total_amount AS total,
                       COALESCE(p.method, 'pending') AS payment_method,
                       c.name AS shipping_name, a.address_line AS shipping_address, a.city, a.state, a.pincode, c.mobile AS phone, c.email,
                       '' AS tracking_number, '' AS carrier, DATEADD('DAY', 4, CAST(o.created_at AS DATE)) AS estimated_delivery
                FROM orders o
                JOIN customer c ON c.id = o.customer_id
                LEFT JOIN address a ON a.id = o.address_id
                LEFT JOIN payment p ON p.order_id = o.id
                WHERE o.customer_id = ?
                ORDER BY o.created_at DESC
                """);
        statement.setString(1, userId);
        return statement.executeQuery();
    }

    public static ResultSet spFetchOrderById(Connection connection, String orderId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("""
                SELECT o.id, CAST(o.created_at AS DATE) AS order_date, o.status, o.total_amount AS total,
                       COALESCE(p.method, 'pending') AS payment_method,
                       c.name AS shipping_name, a.address_line AS shipping_address, a.city, a.state, a.pincode, c.mobile AS phone, c.email,
                       '' AS tracking_number, '' AS carrier, DATEADD('DAY', 4, CAST(o.created_at AS DATE)) AS estimated_delivery
                FROM orders o
                JOIN customer c ON c.id = o.customer_id
                LEFT JOIN address a ON a.id = o.address_id
                LEFT JOIN payment p ON p.order_id = o.id
                WHERE o.id = ?
                """);
        statement.setString(1, orderId);
        return statement.executeQuery();
    }

    public static ResultSet spFetchOrderItems(Connection connection, String orderId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("""
                SELECT p.name, oi.price, '' AS image, v.attributes, oi.qty AS quantity
                FROM order_item oi
                JOIN product_variant v ON v.id = oi.variant_id
                JOIN product p ON p.id = v.product_id
                WHERE oi.order_id = ?
                ORDER BY oi.id
                """);
        statement.setString(1, orderId);
        ResultSet rows = statement.executeQuery();
        SimpleResultSet resultSet = new SimpleResultSet();
        resultSet.addColumn("name", Types.VARCHAR, 200, 0);
        resultSet.addColumn("price", Types.DECIMAL, 12, 2);
        resultSet.addColumn("image", Types.VARCHAR, 1000, 0);
        resultSet.addColumn("color", Types.VARCHAR, 50, 0);
        resultSet.addColumn("quantity", Types.INTEGER, 10, 0);
        while (rows.next()) {
            resultSet.addRow(
                    rows.getString("name"),
                    rows.getBigDecimal("price"),
                    rows.getString("image"),
                    jsonValue(rows.getString("attributes"), "color"),
                    rows.getInt("quantity")
            );
        }
        return resultSet;
    }

    private static String ensureActiveCart(Connection connection, String userId) throws SQLException {
        String existing = activeCartId(connection, userId);
        if (existing != null) {
            return existing;
        }
        String cartId = UUID.randomUUID().toString();
        PreparedStatement insert = connection.prepareStatement("INSERT INTO cart(id, customer_id, status, created_at) VALUES (?, ?, 'active', CURRENT_TIMESTAMP)");
        insert.setString(1, cartId);
        insert.setString(2, userId);
        insert.executeUpdate();
        return cartId;
    }

    private static String activeCartId(Connection connection, String userId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT id FROM cart WHERE customer_id = ? AND status = 'active' ORDER BY created_at DESC");
        statement.setString(1, userId);
        ResultSet rows = statement.executeQuery();
        return rows.next() ? rows.getString("id") : null;
    }

    private static VariantChoice findVariant(Connection connection, String productId, String size, String color) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT id, price, attributes FROM product_variant WHERE product_id = ? AND status = TRUE ORDER BY price");
        statement.setString(1, productId);
        ResultSet rows = statement.executeQuery();
        VariantChoice fallback = null;
        while (rows.next()) {
            String attributes = rows.getString("attributes");
            String variantSize = jsonValue(attributes, "size");
            String variantColor = jsonValue(attributes, "color");
            VariantChoice current = new VariantChoice(rows.getString("id"), rows.getBigDecimal("price"));
            if (fallback == null) {
                fallback = current;
            }
            boolean sizeMatches = size == null || size.isBlank() || size.equalsIgnoreCase(variantSize);
            boolean colorMatches = color == null || color.isBlank() || color.equalsIgnoreCase(variantColor);
            if (sizeMatches && colorMatches) {
                return current;
            }
        }
        if (fallback == null) {
            throw new SQLException("No variant found for product " + productId);
        }
        return fallback;
    }

    private static SimpleResultSet singleStringColumn(String columnName) {
        SimpleResultSet resultSet = new SimpleResultSet();
        resultSet.addColumn(columnName, Types.VARCHAR, 1000, 0);
        return resultSet;
    }

    private static List<String> parseJsonArray(String json) {
        List<String> values = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return values;
        }
        String cleaned = json.trim();
        if (cleaned.startsWith("[")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.endsWith("]")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        if (cleaned.isBlank()) {
            return values;
        }
        for (String part : cleaned.split(",")) {
            String value = unquote(part);
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private static Map<String, String> parseJsonObject(String json) {
        Map<String, String> values = new LinkedHashMap<>();
        if (json == null || json.isBlank()) {
            return values;
        }
        String cleaned = json.trim();
        if (cleaned.startsWith("{")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.endsWith("}")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        if (cleaned.isBlank()) {
            return values;
        }
        for (String pair : cleaned.split(",")) {
            String[] parts = pair.split(":", 2);
            if (parts.length == 2) {
                values.put(unquote(parts[0]), unquote(parts[1]));
            }
        }
        return values;
    }

    private static String jsonValue(String json, String key) {
        return parseJsonObject(json).get(key);
    }

    private static String unquote(String value) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.startsWith("\"")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned;
    }

    private static String colorHex(String color) {
        return switch (color == null ? "" : color.toLowerCase()) {
            case "black" -> "#000000";
            case "white" -> "#ffffff";
            case "red" -> "#ff0000";
            case "blue" -> "#0000ff";
            default -> "#666666";
        };
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "BG";
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < parts.length && index < 2; index++) {
            builder.append(parts[index], 0, 1);
        }
        return builder.toString().toUpperCase();
    }

    private record VariantChoice(
            String variantId,
            BigDecimal price
    ) {
    }
}