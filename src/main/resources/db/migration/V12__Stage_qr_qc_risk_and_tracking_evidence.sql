-- Add QR & QC fields to production_stage
ALTER TABLE production_stage
    ADD COLUMN IF NOT EXISTS qr_token VARCHAR(64) UNIQUE,
    ADD COLUMN IF NOT EXISTS qc_last_result VARCHAR(20),
    ADD COLUMN IF NOT EXISTS qc_last_checked_at TIMESTAMP NULL;

-- Add evidence photo URL to stage_tracking
ALTER TABLE stage_tracking
    ADD COLUMN IF NOT EXISTS evidence_photo_url VARCHAR(500);

-- Stage Risk Assessment table
CREATE TABLE IF NOT EXISTS stage_risk_assessment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    production_stage_id BIGINT NOT NULL,
    severity VARCHAR(10) NOT NULL, -- MINOR / MAJOR
    description TEXT,
    root_cause TEXT,
    solution_proposal TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN', -- OPEN/IN_REVIEW/APPROVED/REJECTED/CLOSED
    approved_by BIGINT NULL,
    approved_at TIMESTAMP NULL,
    impacted_delivery BOOLEAN DEFAULT FALSE,
    proposed_new_date DATE NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_risk_stage FOREIGN KEY (production_stage_id) REFERENCES production_stage(id),
    CONSTRAINT fk_risk_approved_by FOREIGN KEY (approved_by) REFERENCES user(id)
) ENGINE=InnoDB;

-- Stage Risk Attachment table
CREATE TABLE IF NOT EXISTS stage_risk_attachment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    risk_assessment_id BIGINT NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    caption VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_risk_attachment FOREIGN KEY (risk_assessment_id) REFERENCES stage_risk_assessment(id)
) ENGINE=InnoDB;

-- Seed QC checkpoints (idempotent inserts) for 6 stages
INSERT INTO qc_checkpoint(stage_type, checkpoint_name, inspection_criteria, sampling_plan, is_mandatory, display_order)
SELECT 'WARPING','Chủng loại sợi đúng BOM','Đối chiếu BOM','AQL','1',1 WHERE NOT EXISTS(SELECT 1 FROM qc_checkpoint WHERE stage_type='WARPING' AND checkpoint_name='Chủng loại sợi đúng BOM');
INSERT INTO qc_checkpoint(stage_type, checkpoint_name, inspection_criteria, sampling_plan, is_mandatory, display_order)
SELECT 'WARPING','Không lẫn cỡ sợi','Quan sát','AQL','1',2 WHERE NOT EXISTS(SELECT 1 FROM qc_checkpoint WHERE stage_type='WARPING' AND checkpoint_name='Không lẫn cỡ sợi');
INSERT INTO qc_checkpoint(stage_type, checkpoint_name, inspection_criteria, sampling_plan, is_mandatory, display_order)
SELECT 'WARPING','Không sợi đứt/chùng','Quan sát','AQL','1',3 WHERE NOT EXISTS(SELECT 1 FROM qc_checkpoint WHERE stage_type='WARPING' AND checkpoint_name='Không sợi đứt/chùng');
INSERT INTO qc_checkpoint(stage_type, checkpoint_name, inspection_criteria, sampling_plan, is_mandatory, display_order)
SELECT 'WEAVING','Mật độ dệt','Đo & so sánh','AQL','1',1 WHERE NOT EXISTS(SELECT 1 FROM qc_checkpoint WHERE stage_type='WEAVING' AND checkpoint_name='Mật độ dệt');
INSERT INTO qc_checkpoint(stage_type, checkpoint_name, inspection_criteria, sampling_plan, is_mandatory, display_order)
SELECT 'WEAVING','Độ rộng vải','Đo','AQL','1',2 WHERE NOT EXISTS(SELECT 1 FROM qc_checkpoint WHERE stage_type='WEAVING' AND checkpoint_name='Độ rộng vải');
INSERT INTO qc_checkpoint(stage_type, checkpoint_name, inspection_criteria, sampling_plan, is_mandatory, display_order)
SELECT 'DYEING','Không lem/loang','Quan sát','AQL','1',1 WHERE NOT EXISTS(SELECT 1 FROM qc_checkpoint WHERE stage_type='DYEING' AND checkpoint_name='Không lem/loang');
INSERT INTO qc_checkpoint(stage_type, checkpoint_name, inspection_criteria, sampling_plan, is_mandatory, display_order)
SELECT 'DYEING','Độ đều màu','So màu','AQL','1',2 WHERE NOT EXISTS(SELECT 1 FROM qc_checkpoint WHERE stage_type='DYEING' AND checkpoint_name='Độ đều màu');
INSERT INTO qc_checkpoint(stage_type, checkpoint_name, inspection_criteria, sampling_plan, is_mandatory, display_order)
SELECT 'CUTTING','Kích thước chuẩn','Đo kích thước','AQL','1',1 WHERE NOT EXISTS(SELECT 1 FROM qc_checkpoint WHERE stage_type='CUTTING' AND checkpoint_name='Kích thước chuẩn');
INSERT INTO qc_checkpoint(stage_type, checkpoint_name, inspection_criteria, sampling_plan, is_mandatory, display_order)
SELECT 'CUTTING','Đường cắt thẳng','Quan sát','AQL','1',2 WHERE NOT EXISTS(SELECT 1 FROM qc_checkpoint WHERE stage_type='CUTTING' AND checkpoint_name='Đường cắt thẳng');
INSERT INTO qc_checkpoint(stage_type, checkpoint_name, inspection_criteria, sampling_plan, is_mandatory, display_order)
SELECT 'HEMMING','Đường may thẳng','Quan sát','AQL','1',1 WHERE NOT EXISTS(SELECT 1 FROM qc_checkpoint WHERE stage_type='HEMMING' AND checkpoint_name='Đường may thẳng');
INSERT INTO qc_checkpoint(stage_type, checkpoint_name, inspection_criteria, sampling_plan, is_mandatory, display_order)
SELECT 'HEMMING','Mũi chỉ đều','Quan sát','AQL','1',2 WHERE NOT EXISTS(SELECT 1 FROM qc_checkpoint WHERE stage_type='HEMMING' AND checkpoint_name='Mũi chỉ đều');
INSERT INTO qc_checkpoint(stage_type, checkpoint_name, inspection_criteria, sampling_plan, is_mandatory, display_order)
SELECT 'PACKAGING','Gấp đúng quy cách','Quan sát','AQL','1',1 WHERE NOT EXISTS(SELECT 1 FROM qc_checkpoint WHERE stage_type='PACKAGING' AND checkpoint_name='Gấp đúng quy cách');
INSERT INTO qc_checkpoint(stage_type, checkpoint_name, inspection_criteria, sampling_plan, is_mandatory, display_order)
SELECT 'PACKAGING','Đủ số lượng hàng','Đếm','AQL','1',2 WHERE NOT EXISTS(SELECT 1 FROM qc_checkpoint WHERE stage_type='PACKAGING' AND checkpoint_name='Đủ số lượng hàng');

