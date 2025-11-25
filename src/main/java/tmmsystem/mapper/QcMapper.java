package tmmsystem.mapper;

import org.springframework.stereotype.Component;
import tmmsystem.dto.qc.*;
import tmmsystem.entity.*;

@Component
public class QcMapper {
    public QcCheckpointDto toDto(QcCheckpoint e) {
        if (e == null) return null;
        QcCheckpointDto dto = new QcCheckpointDto();
        dto.setId(e.getId());
        dto.setStageType(e.getStageType());
        dto.setCheckpointName(e.getCheckpointName());
        dto.setInspectionCriteria(e.getInspectionCriteria());
        dto.setSamplingPlan(e.getSamplingPlan());
        dto.setIsMandatory(e.getMandatory());
        dto.setDisplayOrder(e.getDisplayOrder());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        return dto;
    }

    public QcInspectionDto toDto(QcInspection e) {
        if (e == null) return null;
        QcInspectionDto dto = new QcInspectionDto();
        dto.setId(e.getId());
        dto.setProductionStageId(e.getProductionStage() != null ? e.getProductionStage().getId() : null);
        dto.setQcCheckpointId(e.getQcCheckpoint() != null ? e.getQcCheckpoint().getId() : null);
        dto.setInspectorId(e.getInspector() != null ? e.getInspector().getId() : null);
        dto.setSampleSize(e.getSampleSize());
        dto.setPassCount(e.getPassCount());
        dto.setFailCount(e.getFailCount());
        dto.setResult(e.getResult());
        dto.setNotes(e.getNotes());
        dto.setPhotoUrl(e.getPhotoUrl());
        dto.setCheckpointName(e.getQcCheckpoint() != null ? e.getQcCheckpoint().getCheckpointName() : null);
        dto.setInspectedAt(e.getInspectedAt());
        return dto;
    }

    public QcDefectDto toDto(QcDefect e) {
        if (e == null) return null;
        QcDefectDto dto = new QcDefectDto();
        dto.setId(e.getId());
        dto.setQcInspectionId(e.getQcInspection() != null ? e.getQcInspection().getId() : null);
        dto.setDefectType(e.getDefectType());
        dto.setDefectDescription(e.getDefectDescription());
        dto.setQuantityAffected(e.getQuantityAffected());
        dto.setSeverity(e.getSeverity());
        dto.setActionTaken(e.getActionTaken());
        return dto;
    }

    public QcPhotoDto toDto(QcPhoto e) {
        if (e == null) return null;
        QcPhotoDto dto = new QcPhotoDto();
        dto.setId(e.getId());
        dto.setQcInspectionId(e.getQcInspection() != null ? e.getQcInspection().getId() : null);
        dto.setPhotoUrl(e.getPhotoUrl());
        dto.setCaption(e.getCaption());
        dto.setUploadedAt(e.getUploadedAt());
        return dto;
    }

    public QcStandardDto toDto(QcStandard e) {
        if (e == null) return null;
        QcStandardDto dto = new QcStandardDto();
        dto.setId(e.getId());
        dto.setStandardName(e.getStandardName());
        dto.setStandardCode(e.getStandardCode());
        dto.setDescription(e.getDescription());
        dto.setApplicableStages(e.getApplicableStages());
        dto.setIsActive(e.getActive());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        return dto;
    }
}


