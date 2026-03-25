-- Seed representative sample data for local/demo environments.
-- This migration is idempotent and can be reapplied safely.

-- Categories
INSERT INTO category (id, name, description, status, version, created_at, updated_at)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'Electronics', 'Consumer electronics and accessories', 'ACTIVE', 0, now(), now()),
    ('22222222-2222-2222-2222-222222222222', 'Office Supplies', 'Operational office essentials', 'ACTIVE', 0, now(), now()),
    ('33333333-3333-3333-3333-333333333333', 'Packaging', 'Boxes, wraps, and packing materials', 'ACTIVE', 0, now(), now())
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    status = EXCLUDED.status,
    updated_at = now();

-- Suppliers
INSERT INTO supplier (id, name, contact_info, address, status, notes, email, phone, created_at, updated_at, version)
VALUES
    ('44444444-4444-4444-4444-444444444444', 'Prime Source Distributors', 'Regional wholesale supplier', '1200 Trade Ave, Dallas, TX', 'ACTIVE', 'Primary supplier for electronics', 'sales@primesource.test', '+1-214-555-1001', now(), now(), 0),
    ('55555555-5555-5555-5555-555555555555', 'North Ridge Packaging', 'Packaging material specialist', '88 Industrial Rd, Phoenix, AZ', 'ACTIVE', 'Preferred for cartons and labels', 'support@northridge.test', '+1-602-555-2044', now(), now(), 0)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    contact_info = EXCLUDED.contact_info,
    address = EXCLUDED.address,
    status = EXCLUDED.status,
    notes = EXCLUDED.notes,
    email = EXCLUDED.email,
    phone = EXCLUDED.phone,
    updated_at = now();

-- Customers
INSERT INTO customers (id, name, email, phone, status, address, notes, created_at, updated_at, version)
VALUES
    ('66666666-6666-6666-6666-666666666666', 'Acme Retail Group', 'buyer@acmeretail.test', '+1-415-555-3111', 'ACTIVE', '10 Market St, San Francisco, CA', 'Key account', now(), now(), 0),
    ('77777777-7777-7777-7777-777777777777', 'Bluebird Stores', 'ops@bluebirdstores.test', '+1-206-555-4222', 'ACTIVE', '501 Pine Ave, Seattle, WA', 'Regional chain', now(), now(), 0),
    ('88888888-8888-8888-8888-888888888888', 'Metro Office Co', 'procurement@metrooffice.test', '+1-312-555-5333', 'ACTIVE', '250 Lake Shore Dr, Chicago, IL', 'Office supplies account', now(), now(), 0)
ON CONFLICT (email) DO UPDATE SET
    name = EXCLUDED.name,
    phone = EXCLUDED.phone,
    status = EXCLUDED.status,
    address = EXCLUDED.address,
    notes = EXCLUDED.notes,
    updated_at = now();

-- Products
INSERT INTO product (id, name, sku, category_id, unit, default_sale_price, cost_price, reorder_threshold, status, description, created_at, updated_at, version)
VALUES
    ('99999999-9999-9999-9999-999999999999', 'Wireless Mouse', 'SKU-WM-1000', '11111111-1111-1111-1111-111111111111', 'EA', 29.99, 15.00, 40, 'ACTIVE', 'Ergonomic wireless mouse', now(), now(), 0),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Mechanical Keyboard', 'SKU-KB-2000', '11111111-1111-1111-1111-111111111111', 'EA', 89.00, 52.00, 25, 'ACTIVE', 'Backlit mechanical keyboard', now(), now(), 0),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Cardboard Box Medium', 'SKU-BX-3000', '33333333-3333-3333-3333-333333333333', 'EA', 2.40, 1.10, 200, 'ACTIVE', 'Medium corrugated shipping box', now(), now(), 0),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'A4 Printer Paper 500ct', 'SKU-PP-4000', '22222222-2222-2222-2222-222222222222', 'PACK', 8.99, 4.80, 120, 'ACTIVE', 'A4 white paper, 500 sheets', now(), now(), 0)
ON CONFLICT (sku) DO UPDATE SET
    name = EXCLUDED.name,
    category_id = EXCLUDED.category_id,
    unit = EXCLUDED.unit,
    default_sale_price = EXCLUDED.default_sale_price,
    cost_price = EXCLUDED.cost_price,
    reorder_threshold = EXCLUDED.reorder_threshold,
    status = EXCLUDED.status,
    description = EXCLUDED.description,
    updated_at = now();

-- Current inventory
INSERT INTO inventory (product_id, current_quantity)
VALUES
    ('99999999-9999-9999-9999-999999999999', 120),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 60),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 540),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 300)
ON CONFLICT (product_id) DO UPDATE SET
    current_quantity = EXCLUDED.current_quantity;

-- Purchase order and lines
INSERT INTO purchase_order (id, supplier_id, order_date, status, version, created_at, updated_at)
VALUES
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', '44444444-4444-4444-4444-444444444444', CURRENT_DATE - INTERVAL '5 days', 'PARTIALLY_RECEIVED', 0, now(), now())
ON CONFLICT (id) DO UPDATE SET
    supplier_id = EXCLUDED.supplier_id,
    order_date = EXCLUDED.order_date,
    status = EXCLUDED.status,
    updated_at = now();

INSERT INTO purchase_order_line (id, purchase_order_id, product_id, quantity_ordered, quantity_received, unit_price, version, created_at, updated_at)
VALUES
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'dddddddd-dddd-dddd-dddd-dddddddddddd', '99999999-9999-9999-9999-999999999999', 80, 50, 14.50, 0, now(), now()),
    ('ffffffff-ffff-ffff-ffff-ffffffffffff', 'dddddddd-dddd-dddd-dddd-dddddddddddd', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 40, 40, 50.00, 0, now(), now())
ON CONFLICT (id) DO UPDATE SET
    purchase_order_id = EXCLUDED.purchase_order_id,
    product_id = EXCLUDED.product_id,
    quantity_ordered = EXCLUDED.quantity_ordered,
    quantity_received = EXCLUDED.quantity_received,
    unit_price = EXCLUDED.unit_price,
    updated_at = now();

-- Sales order and lines
INSERT INTO sales_order (id, customer_id, order_date, status, version, created_at, updated_at)
VALUES
    ('12121212-1212-1212-1212-121212121212', '66666666-6666-6666-6666-666666666666', CURRENT_DATE - INTERVAL '1 day', 'PARTIALLY_SHIPPED', 0, now(), now())
ON CONFLICT (id) DO UPDATE SET
    customer_id = EXCLUDED.customer_id,
    order_date = EXCLUDED.order_date,
    status = EXCLUDED.status,
    updated_at = now();

INSERT INTO sales_order_line (id, sales_order_id, product_id, quantity_ordered, quantity_reserved, quantity_shipped, unit_price, supplier_id, version, created_at, updated_at)
VALUES
    ('34343434-3434-3434-3434-343434343434', '12121212-1212-1212-1212-121212121212', '99999999-9999-9999-9999-999999999999', 30, 5, 20, 29.99, '44444444-4444-4444-4444-444444444444', 0, now(), now()),
    ('56565656-5656-5656-5656-565656565656', '12121212-1212-1212-1212-121212121212', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 120, 0, 120, 2.40, '55555555-5555-5555-5555-555555555555', 0, now(), now())
ON CONFLICT (id) DO UPDATE SET
    sales_order_id = EXCLUDED.sales_order_id,
    product_id = EXCLUDED.product_id,
    quantity_ordered = EXCLUDED.quantity_ordered,
    quantity_reserved = EXCLUDED.quantity_reserved,
    quantity_shipped = EXCLUDED.quantity_shipped,
    unit_price = EXCLUDED.unit_price,
    supplier_id = EXCLUDED.supplier_id,
    updated_at = now();

-- Inventory transactions
INSERT INTO inventory_transaction (
    id,
    product_id,
    transaction_type,
    quantity,
    reference_type,
    reference_id,
    reference_line_id,
    unit_price,
    reason,
    performed_by,
    created_by_username,
    created_at,
    updated_at,
    version
)
SELECT
    '78787878-7878-7878-7878-787878787878',
    '99999999-9999-9999-9999-999999999999',
    'IN',
    50,
    'PURCHASE_ORDER',
    'dddddddd-dddd-dddd-dddd-dddddddddddd',
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
    14.50,
    'Initial purchase receive',
    u.id,
    u.username,
    now() - INTERVAL '4 days',
    now() - INTERVAL '4 days',
    0
FROM users u
WHERE u.username = 'manager1'
ON CONFLICT (id) DO UPDATE SET
    quantity = EXCLUDED.quantity,
    updated_at = now();

INSERT INTO inventory_transaction (
    id,
    product_id,
    transaction_type,
    quantity,
    reference_type,
    reference_id,
    reference_line_id,
    unit_price,
    reason,
    performed_by,
    created_by_username,
    created_at,
    updated_at,
    version
)
SELECT
    '90909090-9090-9090-9090-909090909090',
    '99999999-9999-9999-9999-999999999999',
    'OUT',
    20,
    'SALES_ORDER',
    '12121212-1212-1212-1212-121212121212',
    '34343434-3434-3434-3434-343434343434',
    29.99,
    'Shipment allocation',
    u.id,
    u.username,
    now() - INTERVAL '1 day',
    now() - INTERVAL '1 day',
    0
FROM users u
WHERE u.username = 'staff1'
ON CONFLICT (id) DO UPDATE SET
    quantity = EXCLUDED.quantity,
    updated_at = now();
