-- Add planned time fields to production_stage
ALTER TABLE production_stage
    ADD COLUMN IF NOT EXISTS planned_start_at TIMESTAMP NULL,
    ADD COLUMN IF NOT EXISTS planned_end_at TIMESTAMP NULL,
    ADD COLUMN IF NOT EXISTS planned_duration_hours DECIMAL(10,2) NULL;

