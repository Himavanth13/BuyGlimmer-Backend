CREATE TABLE IF NOT EXISTS customer (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150),
    mobile VARCHAR(15),
    password_hash VARCHAR(255),
    status TINYINT DEFAULT 1,
    meta CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS address (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36),
    type VARCHAR(20),
    address_line CLOB,
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(10),
    is_default BOOLEAN,
    FOREIGN KEY (customer_id) REFERENCES customer(id)
);

CREATE TABLE IF NOT EXISTS category (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(150),
    parent_id VARCHAR(36),
    meta CLOB,
    FOREIGN KEY (parent_id) REFERENCES category(id)
);

CREATE TABLE IF NOT EXISTS product (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200),
    description CLOB,
    brand VARCHAR(150),
    meta CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product_category (
    product_id VARCHAR(36) NOT NULL,
    category_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (product_id, category_id),
    FOREIGN KEY (product_id) REFERENCES product(id),
    FOREIGN KEY (category_id) REFERENCES category(id)
);

CREATE TABLE IF NOT EXISTS product_variant (
    id VARCHAR(36) PRIMARY KEY,
    product_id VARCHAR(36),
    sku VARCHAR(100),
    price DECIMAL(12,2),
    mrp DECIMAL(12,2),
    stock INT,
    attributes CLOB,
    images CLOB,
    status BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE IF NOT EXISTS cart (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36),
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customer(id)
);

CREATE TABLE IF NOT EXISTS cart_item (
    id VARCHAR(36) PRIMARY KEY,
    cart_id VARCHAR(36),
    variant_id VARCHAR(36),
    qty INT,
    price DECIMAL(12,2),
    FOREIGN KEY (cart_id) REFERENCES cart(id),
    FOREIGN KEY (variant_id) REFERENCES product_variant(id)
);

CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36),
    address_id VARCHAR(36),
    total_amount DECIMAL(12,2),
    status VARCHAR(20),
    payment_status VARCHAR(20),
    meta CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customer(id),
    FOREIGN KEY (address_id) REFERENCES address(id)
);

CREATE TABLE IF NOT EXISTS order_item (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36),
    variant_id VARCHAR(36),
    qty INT,
    price DECIMAL(12,2),
    total DECIMAL(12,2),
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (variant_id) REFERENCES product_variant(id)
);

CREATE TABLE IF NOT EXISTS payment (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36),
    method VARCHAR(20),
    gateway_txn_id VARCHAR(200),
    amount DECIMAL(12,2),
    status VARCHAR(20),
    meta CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE IF NOT EXISTS delivery (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    address_id VARCHAR(36),
    status VARCHAR(20) DEFAULT 'pending',
    tracking_number VARCHAR(100),
    carrier VARCHAR(100),
    estimated_delivery_date DATE,
    actual_delivery_date DATE,
    meta CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (address_id) REFERENCES address(id)
);

CREATE TABLE IF NOT EXISTS invoice (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    invoice_date TIMESTAMP,
    total_amount DECIMAL(12,2),
    tax_amount DECIMAL(12,2),
    discount_amount DECIMAL(12,2),
    status VARCHAR(20) DEFAULT 'generated',
    meta CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE IF NOT EXISTS returns (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    reason VARCHAR(100),
    status VARCHAR(20) DEFAULT 'initiated',
    return_date TIMESTAMP,
    notes CLOB,
    meta CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE IF NOT EXISTS refunds (
    id VARCHAR(36) PRIMARY KEY,
    return_id VARCHAR(36) NOT NULL,
    amount DECIMAL(12,2),
    status VARCHAR(20) DEFAULT 'pending',
    refund_date TIMESTAMP,
    gateway_txn_id VARCHAR(200),
    meta CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (return_id) REFERENCES returns(id)
);

CREATE TABLE IF NOT EXISTS email_notifications (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36),
    order_id VARCHAR(36),
    email_type VARCHAR(50),
    recipient_email VARCHAR(150) NOT NULL,
    subject VARCHAR(255),
    status VARCHAR(20) DEFAULT 'pending',
    sent_at TIMESTAMP,
    error_message CLOB,
    meta CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customer(id),
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE IF NOT EXISTS coupon (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(40) UNIQUE NOT NULL,
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(12,2) NOT NULL,
    min_order_amount DECIMAL(12,2) DEFAULT 0,
    active BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS wishlist (
    user_id VARCHAR(36) NOT NULL,
    product_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (user_id, product_id),
    FOREIGN KEY (user_id) REFERENCES customer(id),
    FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE ALIAS IF NOT EXISTS SP_FETCH_CATEGORIES FOR "com.buyglimmer.backend.config.H2StoredProcedures.spFetchCategories";
CREATE ALIAS IF NOT EXISTS SP_FETCH_PRODUCTS FOR "com.buyglimmer.backend.config.H2StoredProcedures.spFetchProducts";
CREATE ALIAS IF NOT EXISTS SP_FETCH_PRODUCT_BY_ID FOR "com.buyglimmer.backend.config.H2StoredProcedures.spFetchProductById";
CREATE ALIAS IF NOT EXISTS SP_FETCH_PRODUCT_IMAGES FOR "com.buyglimmer.backend.config.H2StoredProcedures.spFetchProductImages";
CREATE ALIAS IF NOT EXISTS SP_FETCH_PRODUCT_SIZES FOR "com.buyglimmer.backend.config.H2StoredProcedures.spFetchProductSizes";
CREATE ALIAS IF NOT EXISTS SP_FETCH_PRODUCT_COLORS FOR "com.buyglimmer.backend.config.H2StoredProcedures.spFetchProductColors";
CREATE ALIAS IF NOT EXISTS SP_FETCH_PRODUCT_SPECS FOR "com.buyglimmer.backend.config.H2StoredProcedures.spFetchProductSpecs";
CREATE ALIAS IF NOT EXISTS SP_FETCH_PRODUCT_REVIEWS FOR "com.buyglimmer.backend.config.H2StoredProcedures.spFetchProductReviews";
CREATE ALIAS IF NOT EXISTS SP_FETCH_USER_PROFILE FOR "com.buyglimmer.backend.config.H2StoredProcedures.spFetchUserProfile";
CREATE ALIAS IF NOT EXISTS SP_FETCH_USER_BY_EMAIL FOR "com.buyglimmer.backend.config.H2StoredProcedures.spFetchUserByEmail";
CREATE ALIAS IF NOT EXISTS SP_REGISTER_USER FOR "com.buyglimmer.backend.config.H2StoredProcedures.spRegisterUser";
CREATE ALIAS IF NOT EXISTS SP_UPDATE_USER_PROFILE FOR "com.buyglimmer.backend.config.H2StoredProcedures.spUpdateUserProfile";
CREATE ALIAS IF NOT EXISTS SP_FETCH_WISHLIST FOR "com.buyglimmer.backend.config.H2StoredProcedures.spFetchWishlist";
CREATE ALIAS IF NOT EXISTS SP_TOGGLE_WISHLIST FOR "com.buyglimmer.backend.config.H2StoredProcedures.spToggleWishlist";
CREATE ALIAS IF NOT EXISTS SP_FETCH_CART FOR "com.buyglimmer.backend.config.H2StoredProcedures.spFetchCart";
CREATE ALIAS IF NOT EXISTS SP_ADD_CART_ITEM FOR "com.buyglimmer.backend.config.H2StoredProcedures.spAddCartItem";
CREATE ALIAS IF NOT EXISTS SP_DELETE_CART_ITEM FOR "com.buyglimmer.backend.config.H2StoredProcedures.spDeleteCartItem";
CREATE ALIAS IF NOT EXISTS SP_CLEAR_CART FOR "com.buyglimmer.backend.config.H2StoredProcedures.spClearCart";
CREATE ALIAS IF NOT EXISTS SP_UPSERT_ADDRESS FOR "com.buyglimmer.backend.config.H2StoredProcedures.spUpsertAddress";
CREATE ALIAS IF NOT EXISTS SP_CREATE_ORDER FOR "com.buyglimmer.backend.config.H2StoredProcedures.spCreateOrder";
CREATE ALIAS IF NOT EXISTS SP_TRANSFER_CART_TO_ORDER FOR "com.buyglimmer.backend.config.H2StoredProcedures.spTransferCartToOrder";
CREATE ALIAS IF NOT EXISTS SP_FETCH_ORDERS FOR "com.buyglimmer.backend.config.H2StoredProcedures.spFetchOrders";
CREATE ALIAS IF NOT EXISTS SP_FETCH_ORDER_BY_ID FOR "com.buyglimmer.backend.config.H2StoredProcedures.spFetchOrderById";
CREATE ALIAS IF NOT EXISTS SP_FETCH_ORDER_ITEMS FOR "com.buyglimmer.backend.config.H2StoredProcedures.spFetchOrderItems";

CREATE ALIAS IF NOT EXISTS sp_get_products FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spGetProducts";
CREATE ALIAS IF NOT EXISTS sp_get_product FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spGetProduct";
CREATE ALIAS IF NOT EXISTS sp_search_products FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spSearchProducts";
CREATE ALIAS IF NOT EXISTS sp_add_to_cart FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spAddToCart";
CREATE ALIAS IF NOT EXISTS sp_get_cart FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spGetCart";
CREATE ALIAS IF NOT EXISTS sp_update_cart_item FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spUpdateCartItem";
CREATE ALIAS IF NOT EXISTS sp_remove_cart_item FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spRemoveCartItem";
CREATE ALIAS IF NOT EXISTS sp_create_order FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spCreateOrder";
CREATE ALIAS IF NOT EXISTS sp_add_order_items FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spAddOrderItems";
CREATE ALIAS IF NOT EXISTS sp_get_orders FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spGetOrders";
CREATE ALIAS IF NOT EXISTS sp_get_order_detail FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spGetOrderDetail";
CREATE ALIAS IF NOT EXISTS sp_create_payment FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spCreatePayment";
CREATE ALIAS IF NOT EXISTS sp_verify_payment FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spVerifyPayment";
CREATE ALIAS IF NOT EXISTS sp_get_profile FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spGetProfile";
CREATE ALIAS IF NOT EXISTS sp_update_profile FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spUpdateProfile";
CREATE ALIAS IF NOT EXISTS sp_add_address FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spAddAddress";
CREATE ALIAS IF NOT EXISTS sp_validate_coupon FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spValidateCoupon";

CREATE ALIAS IF NOT EXISTS sp_fetch_user_profile FOR "com.buyglimmer.backend.config.H2StoredProcedures.spFetchUserProfile";
CREATE ALIAS IF NOT EXISTS sp_fetch_user_by_email FOR "com.buyglimmer.backend.config.H2StoredProcedures.spFetchUserByEmail";
CREATE ALIAS IF NOT EXISTS sp_register_user FOR "com.buyglimmer.backend.config.H2StoredProcedures.spRegisterUser";
CREATE ALIAS IF NOT EXISTS sp_update_user_profile FOR "com.buyglimmer.backend.config.H2StoredProcedures.spUpdateUserProfile";

-- Delivery procedures
CREATE ALIAS IF NOT EXISTS sp_create_delivery FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spCreateDelivery";
CREATE ALIAS IF NOT EXISTS sp_update_delivery_status FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spUpdateDeliveryStatus";
CREATE ALIAS IF NOT EXISTS sp_get_delivery_by_order FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spGetDeliveryByOrder";

-- Invoice procedures
CREATE ALIAS IF NOT EXISTS sp_create_invoice FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spCreateInvoice";
CREATE ALIAS IF NOT EXISTS sp_get_invoice_by_order FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spGetInvoiceByOrder";

-- Returns procedures
CREATE ALIAS IF NOT EXISTS sp_initiate_return FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spInitiateReturn";
CREATE ALIAS IF NOT EXISTS sp_update_return_status FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spUpdateReturnStatus";
CREATE ALIAS IF NOT EXISTS sp_get_returns_by_order FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spGetReturnsByOrder";

-- Refunds procedures
CREATE ALIAS IF NOT EXISTS sp_create_refund FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spCreateRefund";
CREATE ALIAS IF NOT EXISTS sp_update_refund_status FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spUpdateRefundStatus";
CREATE ALIAS IF NOT EXISTS sp_get_refund_by_return FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spGetRefundByReturn";

-- Email notification procedures
CREATE ALIAS IF NOT EXISTS sp_send_email_notification FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spSendEmailNotification";
CREATE ALIAS IF NOT EXISTS sp_update_email_notification_status FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spUpdateEmailNotificationStatus";
CREATE ALIAS IF NOT EXISTS sp_get_notifications_by_order FOR "com.buyglimmer.backend.config.H2FintechStoredProcedures.spGetNotificationsByOrder";