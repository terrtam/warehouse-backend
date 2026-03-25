-- =============================
-- Master data foundation updates
-- =============================
ALTER TABLE IF EXISTS category
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

UPDATE category
SET status = UPPER(BTRIM(status))
WHERE status IS NOT NULL
  AND status <> UPPER(BTRIM(status));

ALTER TABLE IF EXISTS category
    ADD CONSTRAINT ck_category_status
        CHECK (status IN ('ACTIVE', 'INACTIVE')) NOT VALID;
ALTER TABLE IF EXISTS category VALIDATE CONSTRAINT ck_category_status;

ALTER TABLE IF EXISTS customers
    ADD COLUMN IF NOT EXISTS address TEXT,
    ADD COLUMN IF NOT EXISTS notes TEXT;

UPDATE customers
SET status = UPPER(BTRIM(status))
WHERE status IS NOT NULL
  AND status <> UPPER(BTRIM(status));

ALTER TABLE IF EXISTS customers
    ADD CONSTRAINT ck_customers_status
        CHECK (status IN ('ACTIVE', 'INACTIVE')) NOT VALID;
ALTER TABLE IF EXISTS customers VALIDATE CONSTRAINT ck_customers_status;

ALTER TABLE IF EXISTS supplier
    ADD COLUMN IF NOT EXISTS notes TEXT;

UPDATE supplier
SET status = UPPER(BTRIM(status))
WHERE status IS NOT NULL
  AND status <> UPPER(BTRIM(status));

ALTER TABLE IF EXISTS supplier
    ADD CONSTRAINT ck_supplier_status
        CHECK (status IN ('ACTIVE', 'INACTIVE')) NOT VALID;
ALTER TABLE IF EXISTS supplier VALIDATE CONSTRAINT ck_supplier_status;

UPDATE product
SET status = UPPER(BTRIM(status))
WHERE status IS NOT NULL
  AND status <> UPPER(BTRIM(status));

ALTER TABLE IF EXISTS product
    ADD CONSTRAINT ck_product_status
        CHECK (status IN ('ACTIVE', 'INACTIVE')) NOT VALID;
ALTER TABLE IF EXISTS product VALIDATE CONSTRAINT ck_product_status;

-- =============================
-- Order and inventory foundation
-- =============================
ALTER TABLE IF EXISTS sales_order
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

UPDATE sales_order
SET status = UPPER(BTRIM(status))
WHERE status IS NOT NULL
  AND status <> UPPER(BTRIM(status));

ALTER TABLE IF EXISTS sales_order
    ADD CONSTRAINT ck_sales_order_status
        CHECK (status IN ('DRAFT', 'PROCESSING', 'PARTIALLY_SHIPPED', 'SHIPPED', 'CANCELLED')) NOT VALID;
ALTER TABLE IF EXISTS sales_order VALIDATE CONSTRAINT ck_sales_order_status;

CREATE INDEX IF NOT EXISTS ix_sales_order_status_order_date ON sales_order (status, order_date);
CREATE INDEX IF NOT EXISTS ix_sales_order_updated_at ON sales_order (updated_at);

ALTER TABLE IF EXISTS sales_order_line
    ADD COLUMN IF NOT EXISTS quantity_reserved INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE IF EXISTS sales_order_line
    ADD CONSTRAINT ck_sales_order_line_reserved_non_negative CHECK (quantity_reserved >= 0) NOT VALID;
ALTER TABLE IF EXISTS sales_order_line VALIDATE CONSTRAINT ck_sales_order_line_reserved_non_negative;

ALTER TABLE IF EXISTS sales_order_line
    ADD CONSTRAINT ck_sales_order_line_shipped_non_negative CHECK (quantity_shipped >= 0) NOT VALID;
ALTER TABLE IF EXISTS sales_order_line VALIDATE CONSTRAINT ck_sales_order_line_shipped_non_negative;

ALTER TABLE IF EXISTS sales_order_line
    ADD CONSTRAINT ck_sales_order_line_ordered_positive CHECK (quantity_ordered > 0) NOT VALID;
ALTER TABLE IF EXISTS sales_order_line VALIDATE CONSTRAINT ck_sales_order_line_ordered_positive;

ALTER TABLE IF EXISTS sales_order_line
    ADD CONSTRAINT ck_sales_order_line_invariant
        CHECK (quantity_reserved + quantity_shipped <= quantity_ordered) NOT VALID;
ALTER TABLE IF EXISTS sales_order_line VALIDATE CONSTRAINT ck_sales_order_line_invariant;

CREATE INDEX IF NOT EXISTS ix_sales_order_line_product_id ON sales_order_line (product_id);

ALTER TABLE IF EXISTS purchase_order
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

UPDATE purchase_order
SET status = UPPER(BTRIM(status))
WHERE status IS NOT NULL
  AND status <> UPPER(BTRIM(status));

ALTER TABLE IF EXISTS purchase_order
    ADD CONSTRAINT ck_purchase_order_status
        CHECK (status IN ('DRAFT', 'ORDERED', 'PARTIALLY_RECEIVED', 'RECEIVED', 'CANCELLED')) NOT VALID;
ALTER TABLE IF EXISTS purchase_order VALIDATE CONSTRAINT ck_purchase_order_status;

CREATE INDEX IF NOT EXISTS ix_purchase_order_status_order_date ON purchase_order (status, order_date);
CREATE INDEX IF NOT EXISTS ix_purchase_order_updated_at ON purchase_order (updated_at);

ALTER TABLE IF EXISTS purchase_order_line
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE IF EXISTS purchase_order_line
    ADD CONSTRAINT ck_purchase_order_line_received_non_negative CHECK (quantity_received >= 0) NOT VALID;
ALTER TABLE IF EXISTS purchase_order_line VALIDATE CONSTRAINT ck_purchase_order_line_received_non_negative;

ALTER TABLE IF EXISTS purchase_order_line
    ADD CONSTRAINT ck_purchase_order_line_ordered_positive CHECK (quantity_ordered > 0) NOT VALID;
ALTER TABLE IF EXISTS purchase_order_line VALIDATE CONSTRAINT ck_purchase_order_line_ordered_positive;

ALTER TABLE IF EXISTS purchase_order_line
    ADD CONSTRAINT ck_purchase_order_line_invariant
        CHECK (quantity_received <= quantity_ordered) NOT VALID;
ALTER TABLE IF EXISTS purchase_order_line VALIDATE CONSTRAINT ck_purchase_order_line_invariant;

CREATE INDEX IF NOT EXISTS ix_purchase_order_line_product_id ON purchase_order_line (product_id);

ALTER TABLE IF EXISTS inventory_transaction
    ADD COLUMN IF NOT EXISTS unit_price NUMERIC(12,2),
    ADD COLUMN IF NOT EXISTS reason TEXT,
    ADD COLUMN IF NOT EXISTS reference_line_id UUID,
    ADD COLUMN IF NOT EXISTS created_by_username VARCHAR(100),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE IF EXISTS inventory_transaction
    ADD CONSTRAINT fk_inventory_transaction_reference_line
        FOREIGN KEY (reference_line_id) REFERENCES sales_order_line(id) DEFERRABLE INITIALLY IMMEDIATE;

CREATE INDEX IF NOT EXISTS ix_inventory_transaction_product_created
    ON inventory_transaction (product_id, created_at);
CREATE INDEX IF NOT EXISTS ix_inventory_transaction_reference
    ON inventory_transaction (reference_type, reference_id);

-- =============================
-- Audit and communication
-- =============================
CREATE TABLE IF NOT EXISTS entity_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    action VARCHAR(50) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    performed_by UUID REFERENCES users(id),
    performed_by_username VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_entity_audit_log_entity_created
    ON entity_audit_log (entity_type, entity_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_entity_audit_log_created_at
    ON entity_audit_log (created_at DESC);

CREATE TABLE IF NOT EXISTS communication_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_type VARCHAR(50) NOT NULL,
    document_id UUID NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    channel VARCHAR(30) NOT NULL DEFAULT 'EMAIL',
    subject VARCHAR(255),
    body TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_error TEXT,
    created_by UUID REFERENCES users(id),
    created_by_username VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE IF EXISTS communication_outbox
    ADD CONSTRAINT ck_communication_outbox_status
        CHECK (status IN ('PENDING', 'SENT', 'FAILED')) NOT VALID;
ALTER TABLE IF EXISTS communication_outbox VALIDATE CONSTRAINT ck_communication_outbox_status;

CREATE INDEX IF NOT EXISTS ix_communication_outbox_status_next
    ON communication_outbox (status, next_attempt_at);

CREATE TABLE IF NOT EXISTS communication_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    outbox_id UUID REFERENCES communication_outbox(id),
    document_type VARCHAR(50) NOT NULL,
    document_id UUID NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    channel VARCHAR(30) NOT NULL DEFAULT 'EMAIL',
    status VARCHAR(30) NOT NULL,
    sender_username VARCHAR(100),
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_communication_log_document_created
    ON communication_log (document_type, document_id, created_at DESC);

CREATE TABLE IF NOT EXISTS auth_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100),
    event_type VARCHAR(30) NOT NULL,
    success BOOLEAN NOT NULL,
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_auth_audit_log_created_at
    ON auth_audit_log (created_at DESC);
