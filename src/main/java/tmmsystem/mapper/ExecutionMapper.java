package tmmsystem.mapper;

import org.springframework.stereotype.Component;
import tmmsystem.dto.execution.*;
import tmmsystem.entity.*;

@Component
public class ExecutionMapper {
    private final tmmsystem.repository.MaterialRequisitionDetailRepository reqDetailRepo;

    public ExecutionMapper(tmmsystem.repository.MaterialRequisitionDetailRepository reqDetailRepo) {
        this.reqDetailRepo = reqDetailRepo;
    }

    public StageTrackingDto toDto(StageTracking e) {
        if (e == null)
            return null;
        StageTrackingDto dto = new StageTrackingDto();
        dto.setId(e.getId());
        dto.setProductionStageId(e.getProductionStage() != null ? e.getProductionStage().getId() : null);
        if (e.getOperator() != null) {
            dto.setOperatorId(e.getOperator().getId());
            dto.setOperatorName(e.getOperator().getName());
        }
        dto.setAction(e.getAction());
        dto.setQuantityCompleted(e.getQuantityCompleted());
        dto.setNotes(e.getNotes());
        dto.setTimestamp(e.getTimestamp());
        dto.setEvidencePhotoUrl(e.getEvidencePhotoUrl());
        dto.setIsRework(e.getIsRework());
        return dto;
    }

    public StagePauseLogDto toDto(StagePauseLog e) {
        if (e == null)
            return null;
        StagePauseLogDto dto = new StagePauseLogDto();
        dto.setId(e.getId());
        dto.setProductionStageId(e.getProductionStage() != null ? e.getProductionStage().getId() : null);
        dto.setPausedById(e.getPausedBy() != null ? e.getPausedBy().getId() : null);
        dto.setResumedById(e.getResumedBy() != null ? e.getResumedBy().getId() : null);
        dto.setPauseReason(e.getPauseReason());
        dto.setPauseNotes(e.getPauseNotes());
        dto.setPausedAt(e.getPausedAt());
        dto.setResumedAt(e.getResumedAt());
        dto.setDurationMinutes(e.getDurationMinutes());
        return dto;
    }

    public OutsourcingTaskDto toDto(OutsourcingTask e) {
        if (e == null)
            return null;
        OutsourcingTaskDto dto = new OutsourcingTaskDto();
        dto.setId(e.getId());
        dto.setProductionStageId(e.getProductionStage() != null ? e.getProductionStage().getId() : null);
        dto.setVendorName(e.getVendorName());
        dto.setDeliveryNoteNumber(e.getDeliveryNoteNumber());
        dto.setWeightSent(e.getWeightSent());
        dto.setWeightReturned(e.getWeightReturned());
        dto.setShrinkageRate(e.getShrinkageRate());
        dto.setExpectedQuantity(e.getExpectedQuantity());
        dto.setReturnedQuantity(e.getReturnedQuantity());
        dto.setUnitCost(e.getUnitCost());
        dto.setTotalCost(e.getTotalCost());
        dto.setSentAt(e.getSentAt());
        dto.setExpectedReturnDate(e.getExpectedReturnDate());
        dto.setActualReturnDate(e.getActualReturnDate());
        dto.setStatus(e.getStatus());
        dto.setNotes(e.getNotes());
        dto.setCreatedById(e.getCreatedBy() != null ? e.getCreatedBy().getId() : null);
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        return dto;
    }

    public ProductionLossDto toDto(ProductionLoss e) {
        if (e == null)
            return null;
        ProductionLossDto dto = new ProductionLossDto();
        dto.setId(e.getId());
        dto.setProductionOrderId(e.getProductionOrder() != null ? e.getProductionOrder().getId() : null);
        dto.setMaterialId(e.getMaterial() != null ? e.getMaterial().getId() : null);
        dto.setQuantityLost(e.getQuantityLost());
        dto.setLossType(e.getLossType());
        dto.setProductionStageId(e.getProductionStage() != null ? e.getProductionStage().getId() : null);
        dto.setNotes(e.getNotes());
        dto.setRecordedById(e.getRecordedBy() != null ? e.getRecordedBy().getId() : null);
        dto.setRecordedAt(e.getRecordedAt());
        return dto;
    }

    public MaterialRequisitionDto toDto(MaterialRequisition e) {
        if (e == null)
            return null;
        MaterialRequisitionDto dto = new MaterialRequisitionDto();
        dto.setId(e.getId());
        dto.setRequisitionNumber(e.getRequisitionNumber());
        dto.setProductionStageId(e.getProductionStage() != null ? e.getProductionStage().getId() : null);
        dto.setRequestedById(e.getRequestedBy() != null ? e.getRequestedBy().getId() : null);
        dto.setApprovedById(e.getApprovedBy() != null ? e.getApprovedBy().getId() : null);
        dto.setStatus(e.getStatus());
        dto.setRequestedAt(e.getRequestedAt());
        dto.setApprovedAt(e.getApprovedAt());
        dto.setIssuedAt(e.getIssuedAt());
        dto.setNotes(e.getNotes());
        dto.setQuantityRequested(e.getQuantityRequested());
        dto.setQuantityApproved(e.getQuantityApproved());
        dto.setRequisitionType(e.getRequisitionType());

        if (e.getProductionStage() != null) {
            dto.setStageType(e.getProductionStage().getStageType());
        }
        if (e.getRequestedBy() != null) {
            dto.setRequestedByName(e.getRequestedBy().getName());
        }
        if (e.getApprovedBy() != null) {
            dto.setApprovedByName(e.getApprovedBy().getName());
        }

        // Map details
        java.util.List<tmmsystem.entity.MaterialRequisitionDetail> details = reqDetailRepo
                .findByRequisitionId(e.getId());
        if (details != null && !details.isEmpty()) {
            dto.setDetails(details.stream().map(this::toDto).collect(java.util.stream.Collectors.toList()));
        }

        return dto;
    }

    public MaterialRequisitionDetailDto toDto(MaterialRequisitionDetail e) {
        if (e == null)
            return null;
        MaterialRequisitionDetailDto dto = new MaterialRequisitionDetailDto();
        dto.setId(e.getId());
        dto.setRequisitionId(e.getRequisition() != null ? e.getRequisition().getId() : null);
        dto.setMaterialId(e.getMaterial() != null ? e.getMaterial().getId() : null);
        dto.setMaterialName(e.getMaterial() != null ? e.getMaterial().getName() : null);
        dto.setQuantityRequested(e.getQuantityRequested());
        dto.setQuantityApproved(e.getQuantityApproved());
        dto.setQuantityIssued(e.getQuantityIssued());
        dto.setUnit(e.getUnit());
        dto.setNotes(e.getNotes());
        return dto;
    }
}
