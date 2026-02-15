-- =============================
-- Enable UUID extension safely
-- =============================
DO $$
    BEGIN
        CREATE EXTENSION IF NOT EXISTS "pgcrypto";
    EXCEPTION
        WHEN duplicate_object THEN null;
    END$$;

-- =============================
-- Create Enums
-- =============================
DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role') THEN
            CREATE TYPE user_role AS ENUM ('MANAGER', 'STAFF');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'transaction_type') THEN
            CREATE TYPE transaction_type AS ENUM ('IN', 'OUT', 'ADJUST');
        END IF;
    END$$;

-- =============================
-- Independent Tables
-- =============================
CREATE TABLE IF NOT EXISTS users (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     username VARCHAR(100) UNIQUE NOT NULL,
                                     password_hash TEXT NOT NULL,
                                     role user_role NOT NULL
);

CREATE TABLE IF NOT EXISTS category (
                                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                        name VARCHAR(150) NOT NULL,
                                        status VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS customer (
                                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                        name VARCHAR(200) NOT NULL,
                                        contact_info TEXT,
                                        address TEXT,
                                        status VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS supplier (
                                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                        name VARCHAR(200) NOT NULL,
                                        contact_info TEXT,
                                        address TEXT,
                                        status VARCHAR(50)
);

-- =============================
-- Dependent Tables
-- =============================
CREATE TABLE IF NOT EXISTS product (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                       name VARCHAR(200) NOT NULL,
                                       sku VARCHAR(100) UNIQUE NOT NULL,
                                       category_id UUID REFERENCES category(id),
                                       unit VARCHAR(50),
                                       default_sale_price NUMERIC(12,2),
                                       cost_price NUMERIC(12,2),
                                       reorder_threshold INT,
                                       status VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS inventory (
                                         product_id UUID PRIMARY KEY REFERENCES product(id) ON DELETE CASCADE,
                                         current_quantity INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sales_order (
                                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                           customer_id UUID REFERENCES customer(id),
                                           order_date DATE NOT NULL,
                                           status VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS sales_order_line (
                                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                sales_order_id UUID REFERENCES sales_order(id) ON DELETE CASCADE,
                                                product_id UUID REFERENCES product(id),
                                                quantity_ordered INT NOT NULL,
                                                quantity_shipped INT DEFAULT 0,
                                                unit_price NUMERIC(12,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS purchase_order (
                                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                              supplier_id UUID REFERENCES supplier(id),
                                              order_date DATE NOT NULL,
                                              status VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS purchase_order_line (
                                                   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                   purchase_order_id UUID REFERENCES purchase_order(id) ON DELETE CASCADE,
                                                   product_id UUID REFERENCES product(id),
                                                   quantity_ordered INT NOT NULL,
                                                   quantity_received INT DEFAULT 0,
                                                   unit_price NUMERIC(12,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS inventory_transaction (
                                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                     product_id UUID REFERENCES product(id),
                                                     transaction_type transaction_type NOT NULL,
                                                     quantity INT NOT NULL,
                                                     reference_type VARCHAR(50),
                                                     reference_id UUID,
                                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                     performed_by UUID REFERENCES users(id)
);
