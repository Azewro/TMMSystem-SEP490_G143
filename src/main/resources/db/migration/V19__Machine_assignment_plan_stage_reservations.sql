ALTER TABLE machine_assignment
    ADD COLUMN plan_stage_id BIGINT NULL,
    ADD COLUMN reservation_type VARCHAR(20) NOT NULL DEFAULT 'PRODUCTION',
    ADD COLUMN reservation_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE machine_assignment
    ADD CONSTRAINT fk_machine_assignment_plan_stage
        FOREIGN KEY (plan_stage_id) REFERENCES production_plan_stage(id);

CREATE UNIQUE INDEX idx_machine_assignment_plan_stage_unique
    ON machine_assignment(plan_stage_id)
    WHERE plan_stage_id IS NOT NULL;

CREATE INDEX idx_machine_assignment_plan_stage
    ON machine_assignment(plan_stage_id);

