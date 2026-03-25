ALTER TABLE IF EXISTS sales_order_line
    ADD COLUMN IF NOT EXISTS supplier_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_sales_order_line_supplier'
    ) THEN
        ALTER TABLE sales_order_line
            ADD CONSTRAINT fk_sales_order_line_supplier
                FOREIGN KEY (supplier_id) REFERENCES supplier(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_sales_order_line_supplier_id
    ON sales_order_line (supplier_id);
