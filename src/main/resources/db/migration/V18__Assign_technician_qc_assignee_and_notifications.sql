-- V18 Add technician assignment to production_order and QC assignee to production_stage
ALTER TABLE production_order
    ADD COLUMN IF NOT EXISTS assigned_technician_id BIGINT NULL,
    ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMP NULL,
    ADD CONSTRAINT fk_po_assigned_technician FOREIGN KEY (assigned_technician_id) REFERENCES user(id);

ALTER TABLE production_stage
    ADD COLUMN IF NOT EXISTS qc_assignee_id BIGINT NULL,
    ADD CONSTRAINT fk_stage_qc_assignee FOREIGN KEY (qc_assignee_id) REFERENCES user(id);

-- No data backfill necessary; optional future backfill could set assigned_technician_id from created_by if role=TECHNICAL_STAFF.

