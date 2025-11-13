-- Add assigned_sales_id and assigned_planning_id to contract to mirror quotation assignments
ALTER TABLE contract
    ADD COLUMN IF NOT EXISTS assigned_sales_id BIGINT NULL,
    ADD COLUMN IF NOT EXISTS assigned_planning_id BIGINT NULL;

ALTER TABLE contract
    ADD CONSTRAINT IF NOT EXISTS fk_contract_assigned_sales FOREIGN KEY (assigned_sales_id) REFERENCES "user"(id);
ALTER TABLE contract
    ADD CONSTRAINT IF NOT EXISTS fk_contract_assigned_planning FOREIGN KEY (assigned_planning_id) REFERENCES "user"(id);

CREATE INDEX IF NOT EXISTS idx_contract_assigned_sales ON contract(assigned_sales_id);
CREATE INDEX IF NOT EXISTS idx_contract_assigned_planning ON contract(assigned_planning_id);

