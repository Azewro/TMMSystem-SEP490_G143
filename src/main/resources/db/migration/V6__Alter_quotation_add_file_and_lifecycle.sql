-- V6: Add file path and lifecycle timestamps to quotation
ALTER TABLE `quotation`
    ADD COLUMN `file_path` VARCHAR(500) NULL AFTER `total_amount`,
    ADD COLUMN `sent_at` DATETIME NULL AFTER `status`,
    ADD COLUMN `accepted_at` DATETIME NULL AFTER `is_accepted`,
    ADD COLUMN `rejected_at` DATETIME NULL AFTER `is_canceled`,
    ADD COLUMN `reject_reason` TEXT NULL AFTER `rejected_at`;

CREATE INDEX `idx_quotation_lifecycle` ON `quotation`(`status`,`sent_at`);

