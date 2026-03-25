-- Align sales_order.customer_id to canonical customers(id) table.
-- This migration is idempotent and preserves existing sales_order references.
DO $$
DECLARE
    sales_order_exists BOOLEAN;
    customers_exists BOOLEAN;
    legacy_customer_exists BOOLEAN;
    orphan_count BIGINT;
    fk_name TEXT;
BEGIN
    SELECT to_regclass('public.sales_order') IS NOT NULL INTO sales_order_exists;
    SELECT to_regclass('public.customers') IS NOT NULL INTO customers_exists;
    SELECT to_regclass('public.customer') IS NOT NULL INTO legacy_customer_exists;

    IF NOT sales_order_exists THEN
        RAISE NOTICE 'Skipping V10: sales_order table does not exist';
        RETURN;
    END IF;

    IF NOT customers_exists THEN
        RAISE NOTICE 'Skipping V10: customers table does not exist';
        RETURN;
    END IF;

    SELECT COUNT(*)
    INTO orphan_count
    FROM sales_order so
    WHERE so.customer_id IS NOT NULL
      AND NOT EXISTS (
          SELECT 1
          FROM customers c
          WHERE c.id = so.customer_id
      );

    RAISE NOTICE 'V10 pre-check: sales_order.customer_id rows missing in customers = %', orphan_count;

    IF legacy_customer_exists THEN
        -- Backfill only legacy rows that are referenced by sales_order but missing in customers.
        INSERT INTO customers (id, name, email, phone, status)
        SELECT
            lc.id,
            COALESCE(NULLIF(BTRIM(lc.name), ''), 'Legacy Customer ' || lc.id::text),
            ('legacy-customer-' || lc.id::text || '@legacy.local')::varchar(255),
            NULL,
            CASE
                WHEN UPPER(BTRIM(COALESCE(lc.status, ''))) IN ('ACTIVE', 'INACTIVE')
                    THEN UPPER(BTRIM(lc.status))
                ELSE 'ACTIVE'
            END
        FROM customer lc
        WHERE EXISTS (
            SELECT 1
            FROM sales_order so
            WHERE so.customer_id = lc.id
        )
          AND NOT EXISTS (
            SELECT 1
            FROM customers c
            WHERE c.id = lc.id
        );

        GET DIAGNOSTICS orphan_count = ROW_COUNT;
        RAISE NOTICE 'V10 backfill: inserted % legacy referenced customer rows into customers', orphan_count;
    END IF;

    -- Drop any FK constraints on sales_order.customer_id (name can vary per environment).
    FOR fk_name IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
        JOIN pg_attribute att ON att.attrelid = rel.oid
            AND att.attnum = ANY (con.conkey)
        WHERE con.contype = 'f'
          AND nsp.nspname = 'public'
          AND rel.relname = 'sales_order'
          AND att.attname = 'customer_id'
    LOOP
        EXECUTE format('ALTER TABLE public.sales_order DROP CONSTRAINT IF EXISTS %I', fk_name);
        RAISE NOTICE 'V10: dropped FK constraint %', fk_name;
    END LOOP;

    -- Recreate FK to canonical customers table if missing.
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
        JOIN pg_class refrel ON refrel.oid = con.confrelid
        JOIN pg_namespace refnsp ON refnsp.oid = refrel.relnamespace
        JOIN pg_attribute att ON att.attrelid = rel.oid
            AND att.attnum = ANY (con.conkey)
        WHERE con.contype = 'f'
          AND nsp.nspname = 'public'
          AND rel.relname = 'sales_order'
          AND att.attname = 'customer_id'
          AND refnsp.nspname = 'public'
          AND refrel.relname = 'customers'
    ) THEN
        ALTER TABLE public.sales_order
            ADD CONSTRAINT fk_sales_order_customer_id_customers
            FOREIGN KEY (customer_id) REFERENCES public.customers(id)
            NOT VALID;

        ALTER TABLE public.sales_order
            VALIDATE CONSTRAINT fk_sales_order_customer_id_customers;

        RAISE NOTICE 'V10: added FK fk_sales_order_customer_id_customers -> customers(id)';
    ELSE
        RAISE NOTICE 'V10: FK from sales_order.customer_id to customers(id) already present';
    END IF;
END $$;
