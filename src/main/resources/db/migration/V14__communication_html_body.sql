-- Add optional HTML payload storage for styled email content.
ALTER TABLE IF EXISTS communication_outbox
    ADD COLUMN IF NOT EXISTS html_body TEXT;
