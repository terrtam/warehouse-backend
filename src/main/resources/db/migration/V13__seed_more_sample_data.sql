-- Add additional sample data to better populate local/demo environments.
-- Idempotent via stable IDs and ON CONFLICT upserts.

-- Additional suppliers
INSERT INTO supplier (id, name, contact_info, address, status, notes, email, phone, created_at, updated_at, version)
VALUES
    ('10101010-1010-1010-1010-101010101010', 'Harbor Tech Supply', 'Electronics and peripherals wholesaler', '900 Harbor Blvd, Long Beach, CA', 'ACTIVE', 'Secondary electronics supplier', 'orders@harbortech.test', '+1-562-555-1002', now(), now(), 0),
    ('20202020-2020-2020-2020-202020202020', 'Summit Office Wholesale', 'Office goods distributor', '450 Commerce St, Denver, CO', 'ACTIVE', 'Paper and stationery bulk source', 'sales@summitoffice.test', '+1-303-555-2030', now(), now(), 0),
    ('30303030-3030-3030-3030-303030303030', 'RapidPack Logistics Supplies', 'Packaging and labels', '77 Logistics Way, Columbus, OH', 'ACTIVE', 'Fast turnaround on packaging SKUs', 'contact@rapidpack.test', '+1-614-555-3077', now(), now(), 0)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    contact_info = EXCLUDED.contact_info,
    address = EXCLUDED.address,
    status = EXCLUDED.status,
    notes = EXCLUDED.notes,
    email = EXCLUDED.email,
    phone = EXCLUDED.phone,
    updated_at = now();

-- Additional customers
INSERT INTO customers (id, name, email, phone, status, address, notes, created_at, updated_at, version)
VALUES
    ('40404040-4040-4040-4040-404040404040', 'Northstar Market', 'purchasing@northstar.test', '+1-503-555-4040', 'ACTIVE', '790 Broadway St, Portland, OR', 'Retail customer', now(), now(), 0),
    ('50505050-5050-5050-5050-505050505050', 'Pinnacle Books & More', 'ops@pinnaclebooks.test', '+1-617-555-5050', 'ACTIVE', '220 Beacon St, Boston, MA', 'Frequent office supply buyer', now(), now(), 0),
    ('60606060-6060-6060-6060-606060606060', 'Urban Office Network', 'buyer@urbanoffice.test', '+1-917-555-6060', 'ACTIVE', '320 Lexington Ave, New York, NY', 'Multi-branch enterprise account', now(), now(), 0),
    ('70707070-7070-7070-7070-707070707070', 'Delta Discount Stores', 'procurement@deltadiscount.test', '+1-702-555-7070', 'ACTIVE', '111 Fremont St, Las Vegas, NV', 'Price-sensitive volume buyer', now(), now(), 0)
ON CONFLICT (email) DO UPDATE SET
    name = EXCLUDED.name,
    phone = EXCLUDED.phone,
    status = EXCLUDED.status,
    address = EXCLUDED.address,
    notes = EXCLUDED.notes,
    updated_at = now();

-- Additional products
INSERT INTO product (id, name, sku, category_id, unit, default_sale_price, cost_price, reorder_threshold, status, description, created_at, updated_at, version)
VALUES
    ('80808080-8080-8080-8080-808080808080', 'USB-C Docking Station', 'SKU-DS-5000', '11111111-1111-1111-1111-111111111111', 'EA', 139.00, 92.00, 20, 'ACTIVE', 'Universal USB-C docking station', now(), now(), 0),
    ('90919191-9091-9091-9091-909191919191', '27in Monitor', 'SKU-MN-5100', '11111111-1111-1111-1111-111111111111', 'EA', 229.00, 160.00, 15, 'ACTIVE', '27-inch IPS office monitor', now(), now(), 0),
    ('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1', 'Thermal Shipping Labels', 'SKU-LB-5200', '33333333-3333-3333-3333-333333333333', 'ROLL', 18.50, 9.75, 80, 'ACTIVE', '4x6 thermal labels', now(), now(), 0),
    ('b2b2b2b2-b2b2-b2b2-b2b2-b2b2b2b2b2b2', 'Packing Tape 6-pack', 'SKU-TP-5300', '33333333-3333-3333-3333-333333333333', 'PACK', 14.25, 7.20, 60, 'ACTIVE', 'Clear packaging tape', now(), now(), 0),
    ('c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3', 'Sticky Notes Assorted', 'SKU-SN-5400', '22222222-2222-2222-2222-222222222222', 'PACK', 6.40, 3.10, 100, 'ACTIVE', 'Assorted sticky note pack', now(), now(), 0),
    ('d4d4d4d4-d4d4-d4d4-d4d4-d4d4d4d4d4d4', 'Gel Pens 12-pack', 'SKU-GP-5500', '22222222-2222-2222-2222-222222222222', 'PACK', 11.99, 5.95, 90, 'ACTIVE', 'Blue/black gel pen pack', now(), now(), 0)
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

-- Additional inventory balances
INSERT INTO inventory (product_id, current_quantity)
VALUES
    ('80808080-8080-8080-8080-808080808080', 45),
    ('90919191-9091-9091-9091-909191919191', 32),
    ('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1', 210),
    ('b2b2b2b2-b2b2-b2b2-b2b2-b2b2b2b2b2b2', 185),
    ('c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3', 260),
    ('d4d4d4d4-d4d4-d4d4-d4d4-d4d4d4d4d4d4', 170)
ON CONFLICT (product_id) DO UPDATE SET
    current_quantity = EXCLUDED.current_quantity;

-- Additional purchase orders
INSERT INTO purchase_order (id, supplier_id, order_date, status, version, created_at, updated_at)
VALUES
    ('e5e5e5e5-e5e5-e5e5-e5e5-e5e5e5e5e5e5', '10101010-1010-1010-1010-101010101010', CURRENT_DATE - INTERVAL '12 days', 'RECEIVED', 0, now(), now()),
    ('f6f6f6f6-f6f6-f6f6-f6f6-f6f6f6f6f6f6', '30303030-3030-3030-3030-303030303030', CURRENT_DATE - INTERVAL '3 days', 'ORDERED', 0, now(), now())
ON CONFLICT (id) DO UPDATE SET
    supplier_id = EXCLUDED.supplier_id,
    order_date = EXCLUDED.order_date,
    status = EXCLUDED.status,
    updated_at = now();

INSERT INTO purchase_order_line (id, purchase_order_id, product_id, quantity_ordered, quantity_received, unit_price, version, created_at, updated_at)
VALUES
    ('1111aaaa-aaaa-aaaa-aaaa-aaaaaaaa1111', 'e5e5e5e5-e5e5-e5e5-e5e5-e5e5e5e5e5e5', '80808080-8080-8080-8080-808080808080', 25, 25, 89.00, 0, now(), now()),
    ('2222bbbb-bbbb-bbbb-bbbb-bbbbbbbb2222', 'e5e5e5e5-e5e5-e5e5-e5e5-e5e5e5e5e5e5', '90919191-9091-9091-9091-909191919191', 20, 20, 152.00, 0, now(), now()),
    ('3333cccc-cccc-cccc-cccc-cccccccc3333', 'f6f6f6f6-f6f6-f6f6-f6f6-f6f6f6f6f6f6', 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1', 150, 0, 8.80, 0, now(), now()),
    ('4444dddd-dddd-dddd-dddd-dddddddd4444', 'f6f6f6f6-f6f6-f6f6-f6f6-f6f6f6f6f6f6', 'b2b2b2b2-b2b2-b2b2-b2b2-b2b2b2b2b2b2', 120, 0, 6.95, 0, now(), now())
ON CONFLICT (id) DO UPDATE SET
    purchase_order_id = EXCLUDED.purchase_order_id,
    product_id = EXCLUDED.product_id,
    quantity_ordered = EXCLUDED.quantity_ordered,
    quantity_received = EXCLUDED.quantity_received,
    unit_price = EXCLUDED.unit_price,
    updated_at = now();

-- Additional sales orders
INSERT INTO sales_order (id, customer_id, order_date, status, version, created_at, updated_at)
VALUES
    ('5555eeee-eeee-eeee-eeee-eeeeeeee5555', '40404040-4040-4040-4040-404040404040', CURRENT_DATE - INTERVAL '2 days', 'PROCESSING', 0, now(), now()),
    ('6666ffff-ffff-ffff-ffff-ffffffff6666', '50505050-5050-5050-5050-505050505050', CURRENT_DATE - INTERVAL '8 days', 'SHIPPED', 0, now(), now()),
    ('77771111-1111-1111-1111-111111117777', '60606060-6060-6060-6060-606060606060', CURRENT_DATE, 'DRAFT', 0, now(), now())
ON CONFLICT (id) DO UPDATE SET
    customer_id = EXCLUDED.customer_id,
    order_date = EXCLUDED.order_date,
    status = EXCLUDED.status,
    updated_at = now();

INSERT INTO sales_order_line (id, sales_order_id, product_id, quantity_ordered, quantity_reserved, quantity_shipped, unit_price, supplier_id, version, created_at, updated_at)
VALUES
    ('88882222-2222-2222-2222-222222228888', '5555eeee-eeee-eeee-eeee-eeeeeeee5555', '80808080-8080-8080-8080-808080808080', 8, 8, 0, 139.00, '10101010-1010-1010-1010-101010101010', 0, now(), now()),
    ('99993333-3333-3333-3333-333333339999', '5555eeee-eeee-eeee-eeee-eeeeeeee5555', 'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3', 30, 12, 18, 6.40, '20202020-2020-2020-2020-202020202020', 0, now(), now()),
    ('aaaa4444-4444-4444-4444-44444444aaaa', '6666ffff-ffff-ffff-ffff-ffffffff6666', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 80, 0, 80, 2.40, '30303030-3030-3030-3030-303030303030', 0, now(), now()),
    ('bbbb5555-5555-5555-5555-55555555bbbb', '6666ffff-ffff-ffff-ffff-ffffffff6666', 'd4d4d4d4-d4d4-d4d4-d4d4-d4d4d4d4d4d4', 40, 0, 40, 11.99, '20202020-2020-2020-2020-202020202020', 0, now(), now()),
    ('cccc6666-6666-6666-6666-66666666cccc', '77771111-1111-1111-1111-111111117777', '90919191-9091-9091-9091-909191919191', 5, 0, 0, 229.00, '10101010-1010-1010-1010-101010101010', 0, now(), now())
ON CONFLICT (id) DO UPDATE SET
    sales_order_id = EXCLUDED.sales_order_id,
    product_id = EXCLUDED.product_id,
    quantity_ordered = EXCLUDED.quantity_ordered,
    quantity_reserved = EXCLUDED.quantity_reserved,
    quantity_shipped = EXCLUDED.quantity_shipped,
    unit_price = EXCLUDED.unit_price,
    supplier_id = EXCLUDED.supplier_id,
    updated_at = now();

-- Additional inventory transactions
INSERT INTO inventory_transaction (
    id, product_id, transaction_type, quantity, reference_type, reference_id, reference_line_id,
    unit_price, reason, performed_by, created_by_username, created_at, updated_at, version
)
SELECT
    'dddd7777-7777-7777-7777-77777777dddd',
    '80808080-8080-8080-8080-808080808080',
    'IN',
    25,
    'PURCHASE_ORDER',
    'e5e5e5e5-e5e5-e5e5-e5e5-e5e5e5e5e5e5',
    '1111aaaa-aaaa-aaaa-aaaa-aaaaaaaa1111',
    89.00,
    'Purchase order fully received',
    u.id,
    u.username,
    now() - INTERVAL '11 days',
    now() - INTERVAL '11 days',
    0
FROM users u
WHERE u.username = 'manager1'
ON CONFLICT (id) DO UPDATE SET
    quantity = EXCLUDED.quantity,
    updated_at = now();

INSERT INTO inventory_transaction (
    id, product_id, transaction_type, quantity, reference_type, reference_id, reference_line_id,
    unit_price, reason, performed_by, created_by_username, created_at, updated_at, version
)
SELECT
    'eeee8888-8888-8888-8888-88888888eeee',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'OUT',
    80,
    'SALES_ORDER',
    '6666ffff-ffff-ffff-ffff-ffffffff6666',
    'aaaa4444-4444-4444-4444-44444444aaaa',
    2.40,
    'Historical shipment',
    u.id,
    u.username,
    now() - INTERVAL '7 days',
    now() - INTERVAL '7 days',
    0
FROM users u
WHERE u.username = 'staff1'
ON CONFLICT (id) DO UPDATE SET
    quantity = EXCLUDED.quantity,
    updated_at = now();

INSERT INTO inventory_transaction (
    id, product_id, transaction_type, quantity, reference_type, reference_id, reference_line_id,
    unit_price, reason, performed_by, created_by_username, created_at, updated_at, version
)
SELECT
    'ffff9999-9999-9999-9999-99999999ffff',
    'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3',
    'OUT',
    18,
    'SALES_ORDER',
    '5555eeee-eeee-eeee-eeee-eeeeeeee5555',
    '99993333-3333-3333-3333-333333339999',
    6.40,
    'Partial shipment',
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
