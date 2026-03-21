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