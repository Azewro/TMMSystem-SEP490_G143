-- V10: introduce production_lot & production_lot_order; modify production_plan & production_plan_stage; drop production_plan_detail

-- 1. New table production_lot
CREATE TABLE IF NOT EXISTS production_lot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    lot_code VARCHAR(50) NOT NULL UNIQUE,
    product_id BIGINT,
    size_snapshot VARCHAR(200),
    total_quantity DECIMAL(12,2),
    delivery_date_target DATE,
    contract_date_min DATE,
    contract_date_max DATE,
    status VARCHAR(30),
    material_requirements_json JSON,
    CONSTRAINT fk_lot_product FOREIGN KEY (product_id) REFERENCES product(id)
) ENGINE=InnoDB;

-- 2. New table production_lot_order
CREATE TABLE IF NOT EXISTS production_lot_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lot_id BIGINT NOT NULL,
    contract_id BIGINT NOT NULL,
    quotation_detail_id BIGINT,
    allocated_quantity DECIMAL(12,2),
    CONSTRAINT fk_lot_order_lot FOREIGN KEY (lot_id) REFERENCES production_lot(id),
    CONSTRAINT fk_lot_order_contract FOREIGN KEY (contract_id) REFERENCES contract(id),
    CONSTRAINT fk_lot_order_qdetail FOREIGN KEY (quotation_detail_id) REFERENCES quotation_detail(id)
) ENGINE=InnoDB;

-- 3. Alter production_plan: add lot_id, version_no, is_current_version; keep existing columns
ALTER TABLE production_plan ADD COLUMN IF NOT EXISTS lot_id BIGINT;
ALTER TABLE production_plan ADD COLUMN IF NOT EXISTS version_no INT DEFAULT 1;
ALTER TABLE production_plan ADD COLUMN IF NOT EXISTS is_current_version TINYINT(1) DEFAULT 1;
ALTER TABLE production_plan ADD CONSTRAINT fk_plan_lot FOREIGN KEY (lot_id) REFERENCES production_lot(id);

-- 4. Alter production_plan_stage: add extended tracking columns
ALTER TABLE production_plan_stage ADD COLUMN IF NOT EXISTS qc_user_id BIGINT;
ALTER TABLE production_plan_stage ADD COLUMN IF NOT EXISTS stage_status VARCHAR(20) DEFAULT 'PENDING';
ALTER TABLE production_plan_stage ADD COLUMN IF NOT EXISTS setup_time_minutes INT;
ALTER TABLE production_plan_stage ADD COLUMN IF NOT EXISTS teardown_time_minutes INT;
ALTER TABLE production_plan_stage ADD COLUMN IF NOT EXISTS actual_start_time DATETIME;
ALTER TABLE production_plan_stage ADD COLUMN IF NOT EXISTS actual_end_time DATETIME;
ALTER TABLE production_plan_stage ADD COLUMN IF NOT EXISTS downtime_minutes INT;
ALTER TABLE production_plan_stage ADD COLUMN IF NOT EXISTS downtime_reason VARCHAR(200);
ALTER TABLE production_plan_stage ADD COLUMN IF NOT EXISTS quantity_input DECIMAL(12,2);
ALTER TABLE production_plan_stage ADD COLUMN IF NOT EXISTS quantity_output DECIMAL(12,2);
ALTER TABLE production_plan_stage ADD COLUMN IF NOT EXISTS plan_id BIGINT;
ALTER TABLE production_plan_stage ADD CONSTRAINT fk_stage_qc_user FOREIGN KEY (qc_user_id) REFERENCES user(id);

-- 5. Migrate stage.plan_id from production_plan_detail before dropping it
-- This update will succeed only if production_plan_detail exists
UPDATE production_plan_stage s
JOIN production_plan_detail d ON s.plan_detail_id = d.id
SET s.plan_id = d.plan_id
WHERE s.plan_id IS NULL;

ALTER TABLE production_plan_stage ADD CONSTRAINT fk_stage_plan FOREIGN KEY (plan_id) REFERENCES production_plan(id);

-- 6. Drop old plan_detail FK column on stage
ALTER TABLE production_plan_stage DROP COLUMN IF EXISTS plan_detail_id;

-- 7. Drop production_plan_detail table
DROP TABLE IF EXISTS production_plan_detail;
