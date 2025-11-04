-- Add employee_code column (nullable first for backfill)
ALTER TABLE `user`
    ADD COLUMN `employee_code` VARCHAR(30) NULL AFTER `id`;

-- Backfill existing rows with generated codes based on created_at order
SET @rownum := 0;
SET @prefix := DATE_FORMAT(UTC_TIMESTAMP(), 'EMP-%Y%m-');
UPDATE `user`
JOIN (
    SELECT id, (@rownum := @rownum + 1) AS rn
    FROM `user`
    ORDER BY COALESCE(created_at, NOW()) ASC, id ASC
) AS t ON t.id = `user`.id
SET `user`.employee_code = CONCAT(@prefix, LPAD(t.rn, 3, '0'))
WHERE `user`.employee_code IS NULL OR `user`.employee_code = '';

-- Make column NOT NULL and add unique constraint
ALTER TABLE `user` MODIFY `employee_code` VARCHAR(30) NOT NULL;
ALTER TABLE `user` ADD CONSTRAINT uq_user_employee_code UNIQUE (`employee_code`);
