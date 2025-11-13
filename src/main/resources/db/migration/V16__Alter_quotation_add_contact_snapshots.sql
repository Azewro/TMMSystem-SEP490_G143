-- Add contact snapshot fields to quotation
ALTER TABLE quotation
    ADD COLUMN IF NOT EXISTS contact_person_snapshot VARCHAR(150),
    ADD COLUMN IF NOT EXISTS contact_email_snapshot VARCHAR(150),
    ADD COLUMN IF NOT EXISTS contact_phone_snapshot VARCHAR(30),
    ADD COLUMN IF NOT EXISTS contact_address_snapshot TEXT,
    ADD COLUMN IF NOT EXISTS contact_method VARCHAR(10);

-- Optional: no index needed; these are read-only snapshot fields

