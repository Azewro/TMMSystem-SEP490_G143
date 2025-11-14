package tmmsystem.mapper;

import org.springframework.stereotype.Component;
import tmmsystem.dto.production.*;
import tmmsystem.entity.*;

@Component
public class ProductionMapper {
    public ProductionOrderDto toDto(ProductionOrder e) {
        if (e == null) return null;
        ProductionOrderDto dto = new ProductionOrderDto();
        dto.setId(e.getId());
        dto.setPoNumber(e.getPoNumber());
        dto.setContractId(e.getContract() != null ? e.getContract().getId() : null);
        dto.setTotalQuantity(e.getTotalQuantity());
        dto.setPlannedStartDate(e.getPlannedStartDate());
        dto.setPlannedEndDate(e.getPlannedEndDate());
        dto.setStatus(e.getStatus());
        dto.setPriority(e.getPriority());
        dto.setNotes(e.getNotes());
        dto.setCreatedById(e.getCreatedBy() != null ? e.getCreatedBy().getId() : null);
        dto.setApprovedById(e.getApprovedBy() != null ? e.getApprovedBy().getId() : null);
        dto.setApprovedAt(e.getApprovedAt());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        dto.setAssignedTechnicianId(e.getAssignedTechnician()!=null? e.getAssignedTechnician().getId(): null);
        dto.setAssignedAt(e.getAssignedAt());
        return dto;
    }

    public ProductionOrderDetailDto toDto(ProductionOrderDetail d) {
        if (d == null) return null;
        ProductionOrderDetailDto dto = new ProductionOrderDetailDto();
        dto.setId(d.getId());
        dto.setProductionOrderId(d.getProductionOrder() != null ? d.getProductionOrder().getId() : null);
        dto.setProductId(d.getProduct() != null ? d.getProduct().getId() : null);
        dto.setBomId(d.getBom() != null ? d.getBom().getId() : null);
        dto.setBomVersion(d.getBomVersion());
        dto.setQuantity(d.getQuantity());
        dto.setUnit(d.getUnit());
        dto.setNoteColor(d.getNoteColor());
        return dto;
    }

    public TechnicalSheetDto toDto(TechnicalSheet t) {
        if (t == null) return null;
        TechnicalSheetDto dto = new TechnicalSheetDto();
        dto.setId(t.getId());
        dto.setProductionOrderId(t.getProductionOrder() != null ? t.getProductionOrder().getId() : null);
        dto.setSheetNumber(t.getSheetNumber());
        dto.setYarnSpecifications(t.getYarnSpecifications());
        dto.setMachineSettings(t.getMachineSettings());
        dto.setQualityStandards(t.getQualityStandards());
        dto.setSpecialInstructions(t.getSpecialInstructions());
        dto.setCreatedById(t.getCreatedBy() != null ? t.getCreatedBy().getId() : null);
        dto.setApprovedById(t.getApprovedBy() != null ? t.getApprovedBy().getId() : null);
        dto.setCreatedAt(t.getCreatedAt());
        dto.setUpdatedAt(t.getUpdatedAt());
        return dto;
    }

    public WorkOrderDto toDto(WorkOrder w) {
        if (w == null) return null;
        WorkOrderDto dto = new WorkOrderDto();
        dto.setId(w.getId());
        dto.setProductionOrderId(w.getProductionOrder() != null ? w.getProductionOrder().getId() : null);
        dto.setWoNumber(w.getWoNumber());
        dto.setDeadline(w.getDeadline());
        dto.setStatus(w.getStatus());
        dto.setSendStatus(w.getSendStatus());
        dto.setIsProduction(w.getProduction());
        dto.setCreatedById(w.getCreatedBy() != null ? w.getCreatedBy().getId() : null);
        dto.setApprovedById(w.getApprovedBy() != null ? w.getApprovedBy().getId() : null);
        dto.setCreatedAt(w.getCreatedAt());
        dto.setUpdatedAt(w.getUpdatedAt());
        return dto;
    }

    public WorkOrderDetailDto toDto(WorkOrderDetail d) {
        if (d == null) return null;
        WorkOrderDetailDto dto = new WorkOrderDetailDto();
        dto.setId(d.getId());
        dto.setWorkOrderId(d.getWorkOrder() != null ? d.getWorkOrder().getId() : null);
        dto.setProductionOrderDetailId(d.getProductionOrderDetail() != null ? d.getProductionOrderDetail().getId() : null);
        dto.setStageSequence(d.getStageSequence());
        dto.setPlannedStartAt(d.getPlannedStartAt());
        dto.setPlannedEndAt(d.getPlannedEndAt());
        dto.setStartAt(d.getStartAt());
        dto.setCompleteAt(d.getCompleteAt());
        dto.setWorkStatus(d.getWorkStatus());
        dto.setNotes(d.getNotes());
        return dto;
    }

    public ProductionStageDto toDto(ProductionStage s) {
        if (s == null) return null;
        ProductionStageDto dto = new ProductionStageDto();
        dto.setId(s.getId());
        dto.setWorkOrderDetailId(s.getWorkOrderDetail() != null ? s.getWorkOrderDetail().getId() : null);
        dto.setStageType(s.getStageType());
        dto.setStageSequence(s.getStageSequence());
        dto.setMachineId(s.getMachine() != null ? s.getMachine().getId() : null);
        dto.setAssignedToId(s.getAssignedTo() != null ? s.getAssignedTo().getId() : null);
        dto.setAssignedLeaderId(s.getAssignedLeader() != null ? s.getAssignedLeader().getId() : null);
        dto.setBatchNumber(s.getBatchNumber());
        dto.setPlannedOutput(s.getPlannedOutput());
        dto.setActualOutput(s.getActualOutput());
        dto.setStartAt(s.getStartAt());
        dto.setCompleteAt(s.getCompleteAt());
        dto.setStatus(s.getStatus());
        dto.setIsOutsourced(s.getOutsourced());
        dto.setOutsourceVendor(s.getOutsourceVendor());
        dto.setNotes(s.getNotes());
        dto.setCreatedAt(s.getCreatedAt());
        dto.setUpdatedAt(s.getUpdatedAt());
        dto.setPlannedStartAt(s.getPlannedStartAt());
        dto.setPlannedEndAt(s.getPlannedEndAt());
        dto.setPlannedDurationHours(s.getPlannedDurationHours());
        dto.setQrToken(s.getQrToken());
        dto.setQcLastResult(s.getQcLastResult());
        dto.setQcLastCheckedAt(s.getQcLastCheckedAt());
        dto.setQcAssigneeId(s.getQcAssignee()!=null? s.getQcAssignee().getId(): null);
        return dto;
    }
}
