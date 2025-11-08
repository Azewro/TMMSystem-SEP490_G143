-- V9: add force_password_change flag to customer
ALTER TABLE customer ADD COLUMN IF NOT EXISTS force_password_change BOOLEAN DEFAULT FALSE;

