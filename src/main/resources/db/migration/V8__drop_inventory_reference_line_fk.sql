-- inventory_transaction.reference_line_id is used by both sales and purchase flows.
-- The previous FK to sales_order_line blocks purchase-order receipts.
ALTER TABLE IF EXISTS inventory_transaction
    DROP CONSTRAINT IF EXISTS fk_inventory_transaction_reference_line;
