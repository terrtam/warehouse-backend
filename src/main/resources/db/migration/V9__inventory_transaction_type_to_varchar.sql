-- Hibernate maps InventoryTransactionType as EnumType.STRING.
-- Legacy schemas used a PostgreSQL enum type, which causes bind errors on inserts.
ALTER TABLE IF EXISTS inventory_transaction
    ALTER COLUMN transaction_type TYPE VARCHAR(20)
    USING transaction_type::text;

ALTER TABLE IF EXISTS inventory_transaction
    ADD CONSTRAINT ck_inventory_transaction_type
        CHECK (transaction_type IN ('IN', 'OUT', 'ADJUST')) NOT VALID;
ALTER TABLE IF EXISTS inventory_transaction
    VALIDATE CONSTRAINT ck_inventory_transaction_type;
