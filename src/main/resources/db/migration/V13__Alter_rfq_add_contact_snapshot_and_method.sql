-- V13: Add RFQ contact snapshots and contact method
ALTER TABLE `rfq`
    ADD COLUMN `contact_person_snapshot` VARCHAR(150) NULL AFTER `proposed_new_delivery_date`,
    ADD COLUMN `contact_email_snapshot` VARCHAR(150) NULL AFTER `contact_person_snapshot`,
    ADD COLUMN `contact_phone_snapshot` VARCHAR(30) NULL AFTER `contact_email_snapshot`,
    ADD COLUMN `contact_address_snapshot` TEXT NULL AFTER `contact_phone_snapshot`,
    ADD COLUMN `contact_method` VARCHAR(10) NULL AFTER `contact_address_snapshot`;

-- Optional index for analytics
CREATE INDEX `idx_rfq_contact_method` ON `rfq`(`contact_method`);

