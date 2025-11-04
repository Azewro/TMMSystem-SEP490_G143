-- Add new columns to rfq
ALTER TABLE `rfq`
    ADD COLUMN `source_type` VARCHAR(30) NULL AFTER `customer_id`,
    ADD COLUMN `assigned_sales_id` BIGINT NULL AFTER `created_by`,
    ADD COLUMN `assigned_planning_id` BIGINT NULL AFTER `assigned_sales_id`,
    ADD COLUMN `approval_date` TIMESTAMP NULL AFTER `approved_by`;

-- Add indexes
CREATE INDEX `idx_rfq_assigned_sales` ON `rfq`(`assigned_sales_id`);
CREATE INDEX `idx_rfq_assigned_planning` ON `rfq`(`assigned_planning_id`);

-- Optional: tighten status length
ALTER TABLE `rfq` MODIFY `status` VARCHAR(100) NULL;

-- Add foreign keys (if not already present)
ALTER TABLE `rfq`
    ADD CONSTRAINT `fk_rfq_assigned_sales` FOREIGN KEY (`assigned_sales_id`) REFERENCES `user`(`id`) ON DELETE SET NULL,
    ADD CONSTRAINT `fk_rfq_assigned_planning` FOREIGN KEY (`assigned_planning_id`) REFERENCES `user`(`id`) ON DELETE SET NULL;

