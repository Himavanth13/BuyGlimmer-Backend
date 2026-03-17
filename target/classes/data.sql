INSERT INTO customer (id, name, email, mobile, password_hash, status, meta, created_at) VALUES
('39225d99-1f70-11f1-9651-ed7fb304f8d2', 'Ravi Kumar', 'ravi@test.com', '9876543210', 'password123', 1, NULL, CURRENT_TIMESTAMP);

INSERT INTO address (id, customer_id, type, address_line, city, state, pincode, is_default) VALUES
('3929205e-1f70-11f1-9651-ed7fb304f8d2', '39225d99-1f70-11f1-9651-ed7fb304f8d2', 'home', 'Banjara Hills', 'Hyderabad', 'Telangana', '500034', TRUE);

INSERT INTO category (id, name, parent_id, meta) VALUES
('38de0e23-1f70-11f1-9651-ed7fb304f8d2', 'Men', NULL, NULL),
('38de18b9-1f70-11f1-9651-ed7fb304f8d2', 'Women', NULL, NULL),
('38de1a69-1f70-11f1-9651-ed7fb304f8d2', 'Shirts', NULL, NULL),
('38de1acc-1f70-11f1-9651-ed7fb304f8d2', 'Tshirts', NULL, NULL),
('38de1b0f-1f70-11f1-9651-ed7fb304f8d2', 'Jeans', NULL, NULL);

INSERT INTO product (id, name, description, brand, meta, created_at) VALUES
('38e45a96-1f70-11f1-9651-ed7fb304f8d2', 'Classic Cotton Shirt', 'Formal slim fit cotton shirt', 'Arrow', '{"fabric":"cotton","fit":"slim"}', CURRENT_TIMESTAMP),
('38e46519-1f70-11f1-9651-ed7fb304f8d2', 'Casual Polo Tshirt', 'Polo tshirt for daily wear', 'UCB', '{"fabric":"poly-cotton","fit":"regular"}', CURRENT_TIMESTAMP),
('38e4669e-1f70-11f1-9651-ed7fb304f8d2', 'Denim Jeans', 'Stretchable denim jeans', 'Levis', '{"fit":"skinny","stretch":"yes"}', CURRENT_TIMESTAMP);

INSERT INTO product_category (product_id, category_id) VALUES
('38e45a96-1f70-11f1-9651-ed7fb304f8d2', '38de1a69-1f70-11f1-9651-ed7fb304f8d2'),
('38e46519-1f70-11f1-9651-ed7fb304f8d2', '38de1acc-1f70-11f1-9651-ed7fb304f8d2'),
('38e4669e-1f70-11f1-9651-ed7fb304f8d2', '38de1b0f-1f70-11f1-9651-ed7fb304f8d2');

INSERT INTO product_variant (id, product_id, sku, price, mrp, stock, attributes, images, status) VALUES
('38fdce2b-1f70-11f1-9651-ed7fb304f8d2', '38e45a96-1f70-11f1-9651-ed7fb304f8d2', 'SHIRT-BLACK-M', 1999.00, 2499.00, 50, '{"color":"Black","size":"M"}', '["https://images.unsplash.com/photo-1603252109303-2751441dd157?w=800"]', TRUE),
('7ba10e8d-1f70-11f1-9651-ed7fb304f8d2', '38e45a96-1f70-11f1-9651-ed7fb304f8d2', 'SHIRT-BLACK-L', 1999.00, 2499.00, 40, '{"color":"Black","size":"L"}', '["https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=800"]', TRUE),
('7ba7fac9-1f70-11f1-9651-ed7fb304f8d2', '38e45a96-1f70-11f1-9651-ed7fb304f8d2', 'SHIRT-WHITE-M', 1899.00, 2399.00, 60, '{"color":"White","size":"M"}', '["https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=800"]', TRUE),
('7bae7076-1f70-11f1-9651-ed7fb304f8d2', '38e46519-1f70-11f1-9651-ed7fb304f8d2', 'TSHIRT-RED-M', 999.00, 1299.00, 80, '{"color":"Red","size":"M"}', '["https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=800"]', TRUE),
('7bb4d502-1f70-11f1-9651-ed7fb304f8d2', '38e46519-1f70-11f1-9651-ed7fb304f8d2', 'TSHIRT-BLUE-L', 1099.00, 1399.00, 70, '{"color":"Blue","size":"L"}', '["https://images.unsplash.com/photo-1576566588028-4147f3842f27?w=800"]', TRUE),
('7bbb54f6-1f70-11f1-9651-ed7fb304f8d2', '38e4669e-1f70-11f1-9651-ed7fb304f8d2', 'JEANS-BLUE-32', 2999.00, 3499.00, 30, '{"color":"Blue","size":"32"}', '["https://images.unsplash.com/photo-1541099649105-f69ad21f3246?w=800"]', TRUE);

INSERT INTO orders (id, customer_id, address_id, total_amount, status, payment_status, meta, created_at) VALUES
('e11a12c2-1f70-11f1-9651-ed7fb304f8d2', '39225d99-1f70-11f1-9651-ed7fb304f8d2', '3929205e-1f70-11f1-9651-ed7fb304f8d2', 0.00, 'pending', 'pending', '{"paymentMethod":"upi"}', CURRENT_TIMESTAMP);

INSERT INTO payment (id, order_id, method, gateway_txn_id, amount, status, meta, created_at) VALUES
('f11a12c2-1f70-11f1-9651-ed7fb304f8d2', 'e11a12c2-1f70-11f1-9651-ed7fb304f8d2', 'upi', NULL, 0.00, 'pending', NULL, CURRENT_TIMESTAMP);