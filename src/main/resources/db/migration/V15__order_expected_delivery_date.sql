-- Add optional expected delivery date for order planning and notifications.
ALTER TABLE IF EXISTS sales_order
    ADD COLUMN IF NOT EXISTS expected_delivery_date DATE;

ALTER TABLE IF EXISTS purchase_order
    ADD COLUMN IF NOT EXISTS expected_delivery_date DATE;
