-- Add assigned_sales_id and assigned_planning_id to quotation to mirror RFQ assignments
ALTER TABLE quotation
    ADD COLUMN IF NOT EXISTS assigned_sales_id BIGINT NULL,
    ADD COLUMN IF NOT EXISTS assigned_planning_id BIGINT NULL;

-- Add FKs (assume user table name is `user`)
ALTER TABLE quotation
    ADD CONSTRAINT IF NOT EXISTS fk_quotation_assigned_sales FOREIGN KEY (assigned_sales_id) REFERENCES "user"(id);
ALTER TABLE quotation
    ADD CONSTRAINT IF NOT EXISTS fk_quotation_assigned_planning FOREIGN KEY (assigned_planning_id) REFERENCES "user"(id);

-- Indexes for filtering
CREATE INDEX IF NOT EXISTS idx_quotation_assigned_sales ON quotation(assigned_sales_id);
CREATE INDEX IF NOT EXISTS idx_quotation_assigned_planning ON quotation(assigned_planning_id);

