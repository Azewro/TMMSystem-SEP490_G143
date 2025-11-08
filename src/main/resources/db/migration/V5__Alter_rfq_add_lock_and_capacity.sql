-- V5: Add RFQ locking & capacity evaluation columns
ALTER TABLE `rfq`
    ADD COLUMN `sales_confirmed_at` DATETIME NULL AFTER `approval_date`,
    ADD COLUMN `sales_confirmed_by` BIGINT NULL AFTER `sales_confirmed_at`,
    ADD COLUMN `is_locked` TINYINT(1) NOT NULL DEFAULT 0 AFTER `sales_confirmed_by`,
    ADD COLUMN `capacity_status` VARCHAR(20) NULL AFTER `is_locked`,
    ADD COLUMN `capacity_reason` TEXT NULL AFTER `capacity_status`,
    ADD COLUMN `proposed_new_delivery_date` DATE NULL AFTER `capacity_reason`;

ALTER TABLE `rfq`
    ADD CONSTRAINT `fk_rfq_sales_confirmed_by` FOREIGN KEY (`sales_confirmed_by`) REFERENCES `user`(`id`) ON DELETE SET NULL;

CREATE INDEX `idx_rfq_locked_status` ON `rfq`(`is_locked`,`status`);

