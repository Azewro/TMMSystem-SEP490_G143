-- V7: add password (nullable) to customer for optional password-based login
ALTER TABLE customer ADD COLUMN IF NOT EXISTS password VARCHAR(255) NULL;

