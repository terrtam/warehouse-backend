ALTER TABLE IF EXISTS supplier
    ADD COLUMN IF NOT EXISTS email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS phone VARCHAR(30),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

UPDATE supplier
SET email = CASE
                WHEN contact_info ~* '^[^,\s]+@[^,\s]+\.[^,\s]+\s*,\s*.+$'
                    THEN NULLIF(BTRIM(split_part(contact_info, ',', 1)), '')
                ELSE NULL
    END,
    phone = CASE
                WHEN contact_info ~* '^[^,\s]+@[^,\s]+\.[^,\s]+\s*,\s*.+$'
                    THEN NULLIF(BTRIM(substring(contact_info FROM '^[^,]*,\s*(.*)$')), '')
                ELSE NULLIF(BTRIM(contact_info), '')
    END
WHERE contact_info IS NOT NULL
  AND BTRIM(contact_info) <> ''
  AND (email IS NULL OR BTRIM(email) = '')
  AND (phone IS NULL OR BTRIM(phone) = '');

UPDATE supplier
SET status = 'ACTIVE'
WHERE status IS NULL OR BTRIM(status) = '';

CREATE INDEX IF NOT EXISTS ix_supplier_updated_at ON supplier (updated_at);
