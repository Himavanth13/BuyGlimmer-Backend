-- BuyGlimmer MariaDB stored procedures
-- No table structure changes; procedures only.

DELIMITER $$

DROP PROCEDURE IF EXISTS sp_get_products $$
CREATE PROCEDURE sp_get_products()
BEGIN
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
  ORDER BY p.name;
END $$

DROP PROCEDURE IF EXISTS sp_get_product $$
CREATE PROCEDURE sp_get_product(IN p_product_id VARCHAR(36))
BEGIN
  SELECT p.id AS product_id,
         p.name,
         p.brand,
         p.description,
         v.price,
         v.mrp,
         v.stock,
         v.sku,
         CASE
           WHEN v.images IS NULL OR v.images = '' THEN NULL
           ELSE JSON_UNQUOTE(JSON_EXTRACT(v.images, '$[0]'))
         END AS image_url
  FROM product p
  JOIN product_variant v ON v.product_id = p.id
  WHERE p.id = p_product_id
  ORDER BY v.price
  LIMIT 1;
END $$

DROP PROCEDURE IF EXISTS sp_search_products $$
CREATE PROCEDURE sp_search_products(IN p_keyword VARCHAR(255))
BEGIN
  SELECT p.id AS product_id,
         p.name,
         p.brand,
         p.description,
         MIN(v.price) AS price,
         MIN(v.mrp) AS mrp,
         SUM(v.stock) AS stock
  FROM product p
  JOIN product_variant v ON v.product_id = p.id
  WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', p_keyword, '%'))
     OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', p_keyword, '%'))
  GROUP BY p.id, p.name, p.brand, p.description
  ORDER BY p.name;
END $$

DROP PROCEDURE IF EXISTS sp_add_to_cart $$
CREATE PROCEDURE sp_add_to_cart(
  IN p_customer_id VARCHAR(36),
  IN p_product_id VARCHAR(36),
  IN p_variant_id VARCHAR(36),
  IN p_quantity INT
)
BEGIN
  DECLARE v_cart_id VARCHAR(36);
  DECLARE v_variant_id VARCHAR(36);
  DECLARE v_price DECIMAL(12,2);
  DECLARE v_cart_item_id VARCHAR(36);

  SELECT id INTO v_cart_id
  FROM cart
  WHERE customer_id = p_customer_id AND LOWER(status) = 'active'
  ORDER BY created_at DESC
  LIMIT 1;

  IF v_cart_id IS NULL THEN
    SET v_cart_id = UUID();
    INSERT INTO cart(id, customer_id, status, created_at)
    VALUES (v_cart_id, p_customer_id, 'active', CURRENT_TIMESTAMP);
  END IF;

  SET v_variant_id = p_variant_id;
  IF v_variant_id IS NULL OR v_variant_id = '' THEN
    SELECT id INTO v_variant_id
    FROM product_variant
    WHERE product_id = p_product_id
    ORDER BY price
    LIMIT 1;
  END IF;

  SELECT price INTO v_price
  FROM product_variant
  WHERE id = v_variant_id
  LIMIT 1;

  SET v_cart_item_id = UUID();
  INSERT INTO cart_item(id, cart_id, variant_id, qty, price)
  VALUES (v_cart_item_id, v_cart_id, v_variant_id, p_quantity, v_price);

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
  WHERE ci.id = v_cart_item_id;
END $$

DROP PROCEDURE IF EXISTS sp_get_cart $$
CREATE PROCEDURE sp_get_cart(IN p_customer_id VARCHAR(36))
BEGIN
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
  WHERE c.customer_id = p_customer_id AND LOWER(c.status) = 'active'
  ORDER BY ci.id;
END $$

DROP PROCEDURE IF EXISTS sp_update_cart_item $$
CREATE PROCEDURE sp_update_cart_item(
  IN p_cart_item_id VARCHAR(36),
  IN p_quantity INT,
  IN p_actor_id VARCHAR(36)
)
BEGIN
  UPDATE cart_item ci
  JOIN cart c ON c.id = ci.cart_id
  SET ci.qty = p_quantity
  WHERE ci.id = p_cart_item_id
    AND c.customer_id = p_actor_id
    AND LOWER(c.status) = 'active';

  SELECT ROW_COUNT() AS affected_rows;
END $$

DROP PROCEDURE IF EXISTS sp_remove_cart_item $$
CREATE PROCEDURE sp_remove_cart_item(
  IN p_cart_item_id VARCHAR(36),
  IN p_actor_id VARCHAR(36)
)
BEGIN
  DELETE ci
  FROM cart_item ci
  JOIN cart c ON c.id = ci.cart_id
  WHERE ci.id = p_cart_item_id
    AND c.customer_id = p_actor_id
    AND LOWER(c.status) = 'active';

  SELECT ROW_COUNT() AS affected_rows;
END $$

DROP PROCEDURE IF EXISTS sp_merge_guest_cart $$
CREATE PROCEDURE sp_merge_guest_cart(
  IN p_guest_id VARCHAR(36),
  IN p_customer_id VARCHAR(36)
)
BEGIN
  DECLARE v_guest_cart_id VARCHAR(36);
  DECLARE v_customer_cart_id VARCHAR(36);
  DECLARE v_merged_count INT DEFAULT 0;
  DECLARE v_moved_count INT DEFAULT 0;

  IF p_guest_id IS NULL OR TRIM(p_guest_id) = ''
     OR p_customer_id IS NULL OR TRIM(p_customer_id) = ''
     OR p_guest_id = p_customer_id THEN
    SELECT 0 AS affected_rows;
  ELSE
    SELECT id INTO v_guest_cart_id
    FROM cart
    WHERE customer_id = p_guest_id AND LOWER(status) = 'active'
    ORDER BY created_at DESC
    LIMIT 1;

    IF v_guest_cart_id IS NULL THEN
      SELECT 0 AS affected_rows;
    ELSE
      SELECT id INTO v_customer_cart_id
      FROM cart
      WHERE customer_id = p_customer_id AND LOWER(status) = 'active'
      ORDER BY created_at DESC
      LIMIT 1;

      IF v_customer_cart_id IS NULL THEN
        SET v_customer_cart_id = UUID();
        INSERT INTO cart(id, customer_id, status, created_at)
        VALUES (v_customer_cart_id, p_customer_id, 'active', CURRENT_TIMESTAMP);
      END IF;

      UPDATE cart_item ci_target
      JOIN cart_item ci_guest
        ON ci_guest.variant_id = ci_target.variant_id
       AND ci_guest.cart_id = v_guest_cart_id
      SET ci_target.qty = ci_target.qty + ci_guest.qty
      WHERE ci_target.cart_id = v_customer_cart_id;
      SET v_merged_count = ROW_COUNT();

      DELETE ci_guest
      FROM cart_item ci_guest
      JOIN cart_item ci_target
        ON ci_target.variant_id = ci_guest.variant_id
       AND ci_target.cart_id = v_customer_cart_id
      WHERE ci_guest.cart_id = v_guest_cart_id;

      UPDATE cart_item
      SET cart_id = v_customer_cart_id
      WHERE cart_id = v_guest_cart_id;
      SET v_moved_count = ROW_COUNT();

      UPDATE cart
      SET status = 'merged'
      WHERE id = v_guest_cart_id;

      SELECT (v_merged_count + v_moved_count) AS affected_rows;
    END IF;
  END IF;
END $$

DROP PROCEDURE IF EXISTS sp_create_order $$
CREATE PROCEDURE sp_create_order(
  IN p_customer_id VARCHAR(36),
  IN p_address_id VARCHAR(36),
  IN p_coupon_code VARCHAR(40),
  IN p_payment_method VARCHAR(20)
)
BEGIN
  DECLARE v_order_id VARCHAR(36);
  SET v_order_id = UUID();

  INSERT INTO orders(id, customer_id, address_id, total_amount, status, payment_status, meta, created_at)
  VALUES (
    v_order_id,
    p_customer_id,
    p_address_id,
    0,
    'pending',
    'pending',
    JSON_OBJECT('coupon', IFNULL(p_coupon_code, ''), 'paymentMethod', p_payment_method),
    CURRENT_TIMESTAMP
  );

  SELECT id AS order_id,
         customer_id,
         total_amount,
         status,
         payment_status,
         CAST(created_at AS CHAR) AS created_at
  FROM orders
  WHERE id = v_order_id;
END $$

DROP PROCEDURE IF EXISTS sp_add_order_items $$
CREATE PROCEDURE sp_add_order_items(
  IN p_order_id VARCHAR(64),
  IN p_variant_id VARCHAR(36),
  IN p_quantity INT,
  IN p_price DECIMAL(12,2)
)
BEGIN
  DECLARE v_order_item_id VARCHAR(36);
  DECLARE v_order_exists INT DEFAULT 0;
  DECLARE v_variant_exists INT DEFAULT 0;

  SELECT COUNT(*) INTO v_order_exists
  FROM orders
  WHERE id = p_order_id;

  IF v_order_exists = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid orderId';
  END IF;

  SELECT COUNT(*) INTO v_variant_exists
  FROM product_variant
  WHERE id = p_variant_id;

  IF v_variant_exists = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid variantId';
  END IF;

  SET v_order_item_id = UUID();

  INSERT INTO order_item(id, order_id, variant_id, qty, price, total)
  VALUES (v_order_item_id, p_order_id, p_variant_id, p_quantity, p_price, p_price * p_quantity);

  UPDATE orders
  SET total_amount = (SELECT COALESCE(SUM(total), 0) FROM order_item WHERE order_id = p_order_id)
  WHERE id = p_order_id;

  SELECT v_order_item_id AS order_item_id;
END $$

DROP PROCEDURE IF EXISTS sp_get_orders $$
CREATE PROCEDURE sp_get_orders(IN p_customer_id VARCHAR(36))
BEGIN
  SELECT id AS order_id,
         customer_id,
         total_amount,
         status,
         payment_status,
         CAST(created_at AS CHAR) AS created_at
  FROM orders
  WHERE customer_id = p_customer_id
  ORDER BY created_at DESC;
END $$

DROP PROCEDURE IF EXISTS sp_get_order_detail $$
CREATE PROCEDURE sp_get_order_detail(IN p_order_id VARCHAR(64))
BEGIN
  SELECT o.id AS order_id,
         o.customer_id,
         o.total_amount,
         o.status,
         o.payment_status,
         CAST(o.created_at AS CHAR) AS created_at,
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
  WHERE o.id = p_order_id
  ORDER BY oi.id;
END $$

DROP PROCEDURE IF EXISTS sp_create_payment $$
CREATE PROCEDURE sp_create_payment(
  IN p_order_id VARCHAR(64),
  IN p_method VARCHAR(20),
  IN p_gateway_txn_id VARCHAR(200),
  IN p_amount DECIMAL(12,2)
)
BEGIN
  DECLARE v_payment_id VARCHAR(36);
  SET v_payment_id = UUID();

  INSERT INTO payment(id, order_id, method, gateway_txn_id, amount, status, meta, created_at)
  VALUES (v_payment_id, p_order_id, p_method, p_gateway_txn_id, p_amount, 'pending', NULL, CURRENT_TIMESTAMP);

  UPDATE orders SET payment_status = 'pending' WHERE id = p_order_id;

  SELECT id AS payment_id,
         order_id,
         method,
         gateway_txn_id,
         amount,
         status
  FROM payment
  WHERE id = v_payment_id;
END $$

DROP PROCEDURE IF EXISTS sp_verify_payment $$
CREATE PROCEDURE sp_verify_payment(
  IN p_payment_id VARCHAR(36),
  IN p_gateway_txn_id VARCHAR(200),
  IN p_status VARCHAR(20)
)
BEGIN
  UPDATE payment
  SET gateway_txn_id = p_gateway_txn_id,
      status = LOWER(p_status)
  WHERE id = p_payment_id;

  UPDATE orders
    SET payment_status = LOWER(p_status)
  WHERE id = (SELECT order_id FROM payment WHERE id = p_payment_id LIMIT 1);

  SELECT id AS payment_id,
         order_id,
         method,
         gateway_txn_id,
         amount,
         status
  FROM payment
  WHERE id = p_payment_id;
END $$

DROP PROCEDURE IF EXISTS sp_update_order_payment_status $$
CREATE PROCEDURE sp_update_order_payment_status(
  IN p_order_id VARCHAR(64),
  IN p_status VARCHAR(20),
  IN p_gateway_txn_id VARCHAR(200)
)
BEGIN
  DECLARE v_payment_count INT DEFAULT 0;
  DECLARE v_payment_method VARCHAR(20) DEFAULT 'UPI';
  DECLARE v_payment_amount DECIMAL(12,2) DEFAULT 0;
  DECLARE v_mapped_payment_status VARCHAR(20) DEFAULT 'pending';
  DECLARE v_orders_payment_supports_paid INT DEFAULT 0;
  DECLARE v_payment_table_supports_paid INT DEFAULT 0;

  SET v_mapped_payment_status = CASE UPPER(IFNULL(p_status, ''))
    WHEN 'SUCCESS' THEN 'paid'
    WHEN 'PAID' THEN 'paid'
    WHEN 'FAILED' THEN 'failed'
    WHEN 'PENDING' THEN 'pending'
    ELSE LOWER(IFNULL(p_status, 'pending'))
  END;

  IF v_mapped_payment_status = 'paid' THEN
    SELECT COUNT(*)
      INTO v_orders_payment_supports_paid
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'payment_status'
      AND LOWER(COLUMN_TYPE) LIKE '%paid%';

    SELECT COUNT(*)
      INTO v_payment_table_supports_paid
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'payment'
      AND COLUMN_NAME = 'status'
      AND LOWER(COLUMN_TYPE) LIKE '%paid%';

    IF v_orders_payment_supports_paid = 0 OR v_payment_table_supports_paid = 0 THEN
      SET v_mapped_payment_status = 'success';
    END IF;
  END IF;

  UPDATE orders
  SET payment_status = v_mapped_payment_status
  WHERE id = p_order_id;

  SELECT COUNT(*)
  INTO v_payment_count
  FROM payment
  WHERE order_id = p_order_id;

  IF v_payment_count = 0 THEN
    SELECT COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(meta, '$.paymentMethod')), ''), 'UPI'),
           COALESCE(total_amount, 0)
    INTO v_payment_method, v_payment_amount
    FROM orders
    WHERE id = p_order_id
    LIMIT 1;

    INSERT INTO payment(id, order_id, method, gateway_txn_id, amount, status, meta, created_at)
    VALUES (UUID(), p_order_id, v_payment_method, p_gateway_txn_id, v_payment_amount, v_mapped_payment_status, JSON_OBJECT('source', 'payments/update-status'), CURRENT_TIMESTAMP);
  ELSE
    UPDATE payment
    SET status = v_mapped_payment_status,
        gateway_txn_id = IFNULL(p_gateway_txn_id, gateway_txn_id)
    WHERE order_id = p_order_id;
  END IF;

  SELECT id AS order_id,
         payment_status
  FROM orders
  WHERE id = p_order_id;
END $$

DROP PROCEDURE IF EXISTS sp_get_profile $$
CREATE PROCEDURE sp_get_profile(IN p_customer_id VARCHAR(36))
BEGIN
  SELECT id AS customer_id,
         name,
         email,
         mobile,
         status,
         CAST(created_at AS CHAR) AS created_at
  FROM customer
  WHERE id = p_customer_id;
END $$

DROP PROCEDURE IF EXISTS sp_update_profile $$
CREATE PROCEDURE sp_update_profile(
  IN p_customer_id VARCHAR(36),
  IN p_name VARCHAR(150),
  IN p_email VARCHAR(150),
  IN p_mobile VARCHAR(15)
)
BEGIN
  UPDATE customer
  SET name = p_name,
      email = p_email,
      mobile = p_mobile
  WHERE id = p_customer_id;

  CALL sp_get_profile(p_customer_id);
END $$

DROP PROCEDURE IF EXISTS sp_add_address $$
CREATE PROCEDURE sp_add_address(
  IN p_customer_id VARCHAR(36),
  IN p_type VARCHAR(20),
  IN p_address_line TEXT,
  IN p_city VARCHAR(100),
  IN p_state VARCHAR(100),
  IN p_pincode VARCHAR(10),
  IN p_is_default BOOLEAN
)
BEGIN
  DECLARE v_address_id VARCHAR(36);

  IF p_is_default THEN
    UPDATE address SET is_default = FALSE WHERE customer_id = p_customer_id;
  END IF;

  SET v_address_id = UUID();
  INSERT INTO address(id, customer_id, type, address_line, city, state, pincode, is_default)
  VALUES (v_address_id, p_customer_id, p_type, p_address_line, p_city, p_state, p_pincode, IFNULL(p_is_default, FALSE));

  SELECT id AS address_id,
         customer_id,
         type,
         address_line,
         city,
         state,
         pincode,
         is_default
  FROM address
  WHERE id = v_address_id;
END $$

DROP PROCEDURE IF EXISTS sp_get_addresses $$
CREATE PROCEDURE sp_get_addresses(IN p_customer_id VARCHAR(36))
BEGIN
  SELECT id AS address_id,
         customer_id,
         type,
         address_line,
         city,
         state,
         pincode,
         is_default
  FROM address
  WHERE customer_id = p_customer_id
  ORDER BY is_default DESC, id DESC;
END $$

DROP PROCEDURE IF EXISTS sp_validate_coupon $$
CREATE PROCEDURE sp_validate_coupon(
  IN p_customer_id VARCHAR(36),
  IN p_coupon_code VARCHAR(40),
  IN p_order_amount DECIMAL(12,2)
)
BEGIN
  DECLARE v_discount_type VARCHAR(20);
  DECLARE v_discount_value DECIMAL(12,2);
  DECLARE v_min_order_amount DECIMAL(12,2) DEFAULT 0;
  DECLARE v_active BOOLEAN;
  DECLARE v_is_valid BOOLEAN DEFAULT FALSE;
  DECLARE v_discount_amount DECIMAL(12,2) DEFAULT 0;
  DECLARE v_message VARCHAR(255) DEFAULT 'Invalid coupon';

  SELECT discount_type, discount_value, active
  INTO v_discount_type, v_discount_value, v_active
  FROM coupon
  WHERE LOWER(code) = LOWER(p_coupon_code)
  LIMIT 1;

  IF v_discount_type IS NOT NULL THEN
    IF NOT v_active THEN
      SET v_message = 'Coupon is inactive';
    ELSEIF p_order_amount < v_min_order_amount THEN
      SET v_message = CONCAT('Minimum order amount is ', v_min_order_amount);
    ELSE
      SET v_is_valid = TRUE;
      IF UPPER(v_discount_type) = 'PERCENT' THEN
        SET v_discount_amount = ROUND((p_order_amount * v_discount_value) / 100, 2);
      ELSE
        SET v_discount_amount = v_discount_value;
      END IF;
      IF v_discount_amount < 0 THEN
        SET v_discount_amount = 0;
      END IF;
      SET v_message = 'Coupon applied successfully';
    END IF;
  END IF;

  SELECT v_is_valid AS is_valid,
         v_discount_amount AS discount_amount,
         v_message AS message;
END $$

DROP PROCEDURE IF EXISTS sp_fetch_user_profile $$
CREATE PROCEDURE sp_fetch_user_profile(IN p_user_id VARCHAR(36))
BEGIN
  SELECT c.id,
         c.name,
         c.email,
         c.mobile AS phone,
         a.address_line AS address,
         UPPER(LEFT(c.name, 1)) AS avatar
  FROM customer c
  LEFT JOIN address a ON a.customer_id = c.id AND a.is_default = TRUE
  WHERE c.id = p_user_id
  LIMIT 1;
END $$

DROP PROCEDURE IF EXISTS sp_fetch_user_by_email $$
CREATE PROCEDURE sp_fetch_user_by_email(IN p_email VARCHAR(150))
BEGIN
  SELECT c.id,
         c.name,
         c.email,
         c.password_hash AS password,
         c.mobile AS phone,
         a.address_line AS address,
         UPPER(LEFT(c.name, 1)) AS avatar
  FROM customer c
  LEFT JOIN address a ON a.customer_id = c.id AND a.is_default = TRUE
  WHERE LOWER(c.email) = LOWER(p_email)
  LIMIT 1;
END $$

DROP PROCEDURE IF EXISTS sp_register_user $$
CREATE PROCEDURE sp_register_user(
  IN p_name VARCHAR(150),
  IN p_email VARCHAR(150),
  IN p_password VARCHAR(255),
  IN p_phone VARCHAR(15)
)
BEGIN
  DECLARE v_user_id VARCHAR(36);

  IF p_password IS NULL OR p_password NOT REGEXP '^\\$2[aby]\\$[0-9]{2}\\$[./A-Za-z0-9]{53}$' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid password hash format (BCrypt required)';
  END IF;

  SET v_user_id = UUID();

  INSERT INTO customer(id, name, email, mobile, password_hash, status, meta, created_at)
  VALUES (v_user_id, p_name, p_email, p_phone, p_password, 1, NULL, CURRENT_TIMESTAMP);

  SELECT v_user_id AS user_id;
END $$

DROP PROCEDURE IF EXISTS sp_update_user_profile $$
CREATE PROCEDURE sp_update_user_profile(
  IN p_user_id VARCHAR(36),
  IN p_name VARCHAR(150),
  IN p_email VARCHAR(150),
  IN p_phone VARCHAR(15),
  IN p_address VARCHAR(500),
  IN p_avatar VARCHAR(10)
)
BEGIN
  DECLARE v_address_id VARCHAR(36);

  UPDATE customer
  SET name = p_name,
      email = p_email,
      mobile = p_phone
  WHERE id = p_user_id;

  SELECT id INTO v_address_id
  FROM address
  WHERE customer_id = p_user_id AND is_default = TRUE
  LIMIT 1;

  IF v_address_id IS NULL THEN
    SET v_address_id = UUID();
    INSERT INTO address(id, customer_id, type, address_line, city, state, pincode, is_default)
    VALUES (v_address_id, p_user_id, 'home', IFNULL(p_address, ''), '', '', '', TRUE);
  ELSE
    UPDATE address
    SET address_line = IFNULL(p_address, address_line)
    WHERE id = v_address_id;
  END IF;

  SELECT p_user_id AS user_id;
END $$

DROP PROCEDURE IF EXISTS sp_create_delivery $$
CREATE PROCEDURE sp_create_delivery(
  IN p_order_id VARCHAR(64),
  IN p_address_id VARCHAR(36),
  IN p_tracking_number VARCHAR(100),
  IN p_carrier VARCHAR(100),
  IN p_estimated_delivery_date DATE
)
BEGIN
  DECLARE v_delivery_id VARCHAR(36);
  SET v_delivery_id = UUID();

  INSERT INTO delivery(id, order_id, address_id, status, tracking_number, carrier, estimated_delivery_date, created_at)
  VALUES (v_delivery_id, p_order_id, p_address_id, 'pending', p_tracking_number, p_carrier, p_estimated_delivery_date, CURRENT_TIMESTAMP);

  SELECT id AS delivery_id,
         order_id,
         status,
         tracking_number,
         carrier,
         estimated_delivery_date,
         CAST(created_at AS CHAR) AS created_at
  FROM delivery
  WHERE id = v_delivery_id;
END $$

DROP PROCEDURE IF EXISTS sp_update_delivery_status $$
CREATE PROCEDURE sp_update_delivery_status(
  IN p_delivery_id VARCHAR(36),
  IN p_status VARCHAR(20),
  IN p_actual_delivery_date DATE
)
BEGIN
  UPDATE delivery
  SET status = p_status,
      actual_delivery_date = p_actual_delivery_date
  WHERE id = p_delivery_id;

  SELECT id AS delivery_id,
         order_id,
         status,
         tracking_number,
         carrier,
         estimated_delivery_date,
         actual_delivery_date,
         CAST(created_at AS CHAR) AS created_at
  FROM delivery
  WHERE id = p_delivery_id;
END $$

DROP PROCEDURE IF EXISTS sp_get_delivery_by_order $$
CREATE PROCEDURE sp_get_delivery_by_order(IN p_order_id VARCHAR(64))
BEGIN
  SELECT id AS delivery_id,
         order_id,
         status,
         tracking_number,
         carrier,
         estimated_delivery_date,
         actual_delivery_date,
         CAST(created_at AS CHAR) AS created_at
  FROM delivery
  WHERE order_id = p_order_id
  ORDER BY created_at DESC;
END $$

DROP PROCEDURE IF EXISTS sp_create_invoice $$
CREATE PROCEDURE sp_create_invoice(
  IN p_order_id VARCHAR(64),
  IN p_invoice_number VARCHAR(50),
  IN p_total_amount DECIMAL(12,2),
  IN p_tax_amount DECIMAL(12,2),
  IN p_discount_amount DECIMAL(12,2)
)
BEGIN
  DECLARE v_invoice_id VARCHAR(36);
  SET v_invoice_id = UUID();

  INSERT INTO invoice(id, order_id, invoice_number, invoice_date, total_amount, tax_amount, discount_amount, status, created_at)
  VALUES (v_invoice_id, p_order_id, p_invoice_number, CURRENT_TIMESTAMP, p_total_amount, p_tax_amount, p_discount_amount, 'generated', CURRENT_TIMESTAMP);

  SELECT id AS invoice_id,
         order_id,
         invoice_number,
         CAST(invoice_date AS CHAR) AS invoice_date,
         total_amount,
         tax_amount,
         discount_amount,
         status
  FROM invoice
  WHERE id = v_invoice_id;
END $$

DROP PROCEDURE IF EXISTS sp_get_invoice_by_order $$
CREATE PROCEDURE sp_get_invoice_by_order(IN p_order_id VARCHAR(64))
BEGIN
  SELECT id AS invoice_id,
         order_id,
         invoice_number,
         CAST(invoice_date AS CHAR) AS invoice_date,
         total_amount,
         tax_amount,
         discount_amount,
         status
  FROM invoice
  WHERE order_id = p_order_id
  ORDER BY invoice_date DESC;
END $$

DROP PROCEDURE IF EXISTS sp_initiate_return $$
CREATE PROCEDURE sp_initiate_return(
  IN p_order_id VARCHAR(64),
  IN p_reason VARCHAR(100),
  IN p_notes VARCHAR(1000)
)
BEGIN
  DECLARE v_return_id VARCHAR(36);
  SET v_return_id = UUID();

  INSERT INTO returns(id, order_id, reason, status, return_date, notes, created_at)
  VALUES (v_return_id, p_order_id, p_reason, 'initiated', CURRENT_TIMESTAMP, p_notes, CURRENT_TIMESTAMP);

  SELECT id AS return_id,
         order_id,
         reason,
         status,
         CAST(return_date AS CHAR) AS return_date,
         CAST(created_at AS CHAR) AS created_at
  FROM returns
  WHERE id = v_return_id;
END $$

DROP PROCEDURE IF EXISTS sp_update_return_status $$
CREATE PROCEDURE sp_update_return_status(
  IN p_return_id VARCHAR(36),
  IN p_status VARCHAR(20)
)
BEGIN
  UPDATE returns
  SET status = p_status
  WHERE id = p_return_id;

  SELECT id AS return_id,
         order_id,
         reason,
         status,
         CAST(return_date AS CHAR) AS return_date,
         CAST(created_at AS CHAR) AS created_at
  FROM returns
  WHERE id = p_return_id;
END $$

DROP PROCEDURE IF EXISTS sp_get_returns_by_order $$
CREATE PROCEDURE sp_get_returns_by_order(IN p_order_id VARCHAR(64))
BEGIN
  SELECT id AS return_id,
         order_id,
         reason,
         status,
         CAST(return_date AS CHAR) AS return_date,
         CAST(created_at AS CHAR) AS created_at
  FROM returns
  WHERE order_id = p_order_id
  ORDER BY return_date DESC;
END $$

DROP PROCEDURE IF EXISTS sp_create_refund $$
CREATE PROCEDURE sp_create_refund(
  IN p_return_id VARCHAR(36),
  IN p_amount DECIMAL(12,2),
  IN p_gateway_txn_id VARCHAR(200)
)
BEGIN
  DECLARE v_refund_id VARCHAR(36);
  SET v_refund_id = UUID();

  INSERT INTO refunds(id, return_id, amount, status, gateway_txn_id, created_at)
  VALUES (v_refund_id, p_return_id, p_amount, 'pending', p_gateway_txn_id, CURRENT_TIMESTAMP);

  SELECT id AS refund_id,
         return_id,
         amount,
         status,
         CAST(created_at AS CHAR) AS created_at
  FROM refunds
  WHERE id = v_refund_id;
END $$

DROP PROCEDURE IF EXISTS sp_update_refund_status $$
CREATE PROCEDURE sp_update_refund_status(
  IN p_refund_id VARCHAR(36),
  IN p_status VARCHAR(20),
  IN p_refund_date TIMESTAMP
)
BEGIN
  UPDATE refunds
  SET status = p_status,
      refund_date = p_refund_date
  WHERE id = p_refund_id;

  SELECT id AS refund_id,
         return_id,
         amount,
         status,
         CAST(refund_date AS CHAR) AS refund_date,
         CAST(created_at AS CHAR) AS created_at
  FROM refunds
  WHERE id = p_refund_id;
END $$

DROP PROCEDURE IF EXISTS sp_get_refund_by_return $$
CREATE PROCEDURE sp_get_refund_by_return(IN p_return_id VARCHAR(36))
BEGIN
  SELECT id AS refund_id,
         return_id,
         amount,
         status,
         CAST(refund_date AS CHAR) AS refund_date,
         CAST(created_at AS CHAR) AS created_at
  FROM refunds
  WHERE return_id = p_return_id
  ORDER BY created_at DESC;
END $$

DROP PROCEDURE IF EXISTS sp_send_email_notification $$
CREATE PROCEDURE sp_send_email_notification(
  IN p_customer_id VARCHAR(36),
  IN p_order_id VARCHAR(64),
  IN p_email_type VARCHAR(50),
  IN p_recipient_email VARCHAR(150),
  IN p_subject VARCHAR(255)
)
BEGIN
  DECLARE v_notification_id VARCHAR(36);
  SET v_notification_id = UUID();

  INSERT INTO email_notifications(id, customer_id, order_id, email_type, recipient_email, subject, status, created_at)
  VALUES (v_notification_id, p_customer_id, p_order_id, p_email_type, p_recipient_email, p_subject, 'pending', CURRENT_TIMESTAMP);

  SELECT id AS notification_id,
         customer_id,
         order_id,
         email_type,
         recipient_email,
         status,
         CAST(created_at AS CHAR) AS created_at
  FROM email_notifications
  WHERE id = v_notification_id;
END $$

DROP PROCEDURE IF EXISTS sp_update_email_notification_status $$
CREATE PROCEDURE sp_update_email_notification_status(
  IN p_notification_id VARCHAR(36),
  IN p_status VARCHAR(20),
  IN p_error_message VARCHAR(1000)
)
BEGIN
  UPDATE email_notifications
  SET status = p_status,
      sent_at = CURRENT_TIMESTAMP,
      error_message = p_error_message
  WHERE id = p_notification_id;

  SELECT id AS notification_id,
         customer_id,
         order_id,
         email_type,
         recipient_email,
         status,
         CAST(sent_at AS CHAR) AS sent_at,
         CAST(created_at AS CHAR) AS created_at
  FROM email_notifications
  WHERE id = p_notification_id;
END $$

DROP PROCEDURE IF EXISTS sp_get_notifications_by_order $$
CREATE PROCEDURE sp_get_notifications_by_order(IN p_order_id VARCHAR(64))
BEGIN
  SELECT id AS notification_id,
         customer_id,
         order_id,
         email_type,
         recipient_email,
         status,
         CAST(sent_at AS CHAR) AS sent_at,
         CAST(created_at AS CHAR) AS created_at
  FROM email_notifications
  WHERE order_id = p_order_id
  ORDER BY created_at DESC;
END $$

DELIMITER ;
