package tmmsystem.mapper;

import org.springframework.stereotype.Component;
import tmmsystem.dto.production.*;
import tmmsystem.entity.*;
import java.time.Instant;

@Component
public class ProductionMapper {
    public ProductionOrderDto toDto(ProductionOrder e) {
        if (e == null)
            return null;
        ProductionOrderDto dto = new ProductionOrderDto();
        dto.setId(e.getId());
        dto.setPoNumber(e.getPoNumber());
        dto.setContractId(e.getContract() != null ? e.getContract().getId() : null);
        dto.setTotalQuantity(e.getTotalQuantity());
        dto.setPlannedStartDate(e.getPlannedStartDate());
        dto.setPlannedEndDate(e.getPlannedEndDate());
        dto.setStatus(e.getStatus());
        dto.setExecutionStatus(e.getExecutionStatus()); // Map executionStatus
        dto.setPriority(e.getPriority());
        dto.setAssignedTechnicianId(e.getAssignedTechnician() != null ? e.getAssignedTechnician().getId() : null);
        dto.setAssignedAt(e.getAssignedAt());
        if (e.getContractIds() != null && !e.getContractIds().isBlank()) {
            try {
                String clean = e.getContractIds().replace("[", "").replace("]", "").replace(" ", "");
                if (!clean.isEmpty()) {
                    dto.setContractIds(java.util.Arrays.stream(clean.split(","))
                            .map(Long::valueOf).collect(java.util.stream.Collectors.toList()));
                }
            } catch (Exception ex) {
                /* ignore parse error */ }
        }
        return dto;
    }

    public ProductionOrderDetailDto toDto(ProductionOrderDetail d) {
        if (d == null)
            return null;
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
        if (t == null)
            return null;
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
        if (w == null)
            return null;
        WorkOrderDto dto = new WorkOrderDto();
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
        if (d == null)
            return null;
        WorkOrderDetailDto dto = new WorkOrderDetailDto();
        dto.setId(d.getId());
        dto.setWorkOrderId(d.getWorkOrder() != null ? d.getWorkOrder().getId() : null);
        dto.setProductionOrderDetailId(
                d.getProductionOrderDetail() != null ? d.getProductionOrderDetail().getId() : null);
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
        if (s == null)
            return null;
        ProductionStageDto dto = new ProductionStageDto();
        dto.setId(s.getId());
        dto.setProductionOrderId(s.getProductionOrder() != null ? s.getProductionOrder().getId() : null); // NEW
        // REMOVED: dto.setWorkOrderDetailId() - field workOrderDetail đã bị xóa khỏi
        // entity
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
        dto.setQcAssigneeId(s.getQcAssignee() != null ? s.getQcAssignee().getId() : null);
        dto.setExecutionStatus(s.getExecutionStatus());
        dto.setProgressPercent(s.getProgressPercent());
        dto.setIsRework(s.getIsRework());

        // Enrich fields for frontend
        dto.setStageCode(s.getStageType()); // stageType là mã công đoạn
        dto.setStageName(mapStageTypeToName(s.getStageType())); // Map mã sang tên
        dto.setAssigneeName(getAssigneeName(s)); // Lấy tên người phụ trách
        dto.setStatusLabel(mapStageStatusToLabel(s.getExecutionStatus(), s.getStatus())); // Map status sang label

        // Map defect info
        dto.setDefectSeverity(s.getDefectLevel());
        dto.setDefectDescription(s.getDefectDescription());

        // Format start and end times for Leader
        if (s.getStartAt() != null) {
            dto.setStartTimeFormatted(formatInstant(s.getStartAt()));
        }
        if (s.getCompleteAt() != null) {
            dto.setEndTimeFormatted(formatInstant(s.getCompleteAt()));
        }

        return dto;
    }

    /**
     * Format Instant to Vietnamese datetime string
     */
    private String formatInstant(Instant instant) {
        if (instant == null)
            return null;
        return java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(java.time.ZoneId.systemDefault())
                .format(instant);
    }

    /**
     * Map stageType sang tên công đoạn để hiển thị
     */
    private String mapStageTypeToName(String stageType) {
        if (stageType == null)
            return "Không xác định";
        switch (stageType.toUpperCase()) {
            case "WARPING":
            case "CUONG_MAC":
                return "Cuồng mắc";
            case "WEAVING":
            case "DET":
                return "Dệt";
            case "DYEING":
            case "NHUOM":
                return "Nhuộm";
            case "CUTTING":
            case "CAT":
                return "Cắt";
            case "HEMMING":
            case "MAY":
                return "May";
            case "PACKAGING":
            case "DONG_GOI":
                return "Đóng gói";
            default:
                return stageType;
        }
    }

    /**
     * Lấy tên người phụ trách
     */
    private String getAssigneeName(ProductionStage s) {
        // Ưu tiên assignedLeader, nếu không có thì lấy assignedTo
        if (s.getAssignedLeader() != null) {
            return s.getAssignedLeader().getName() != null ? s.getAssignedLeader().getName()
                    : (s.getAssignedLeader().getEmail() != null ? s.getAssignedLeader().getEmail() : "N/A");
        }
        if (s.getAssignedTo() != null) {
            return s.getAssignedTo().getName() != null ? s.getAssignedTo().getName()
                    : (s.getAssignedTo().getEmail() != null ? s.getAssignedTo().getEmail() : "N/A");
        }
        // Nếu là công đoạn nhuộm và outsourced, trả về "Production Manager"
        if ("DYEING".equalsIgnoreCase(s.getStageType()) || "NHUOM".equalsIgnoreCase(s.getStageType())) {
            if (Boolean.TRUE.equals(s.getOutsourced())) {
                return "Production Manager";
            }
        }
        return "Chưa phân công";
    }

    /**
     * Map executionStatus và status sang statusLabel để hiển thị
     */
    private String mapStageStatusToLabel(String executionStatus, String status) {
        if (executionStatus != null) {
            switch (executionStatus) {
                case "PENDING":
                    return "Đợi";
                case "WAITING":
                    return "Chờ làm";
                case "IN_PROGRESS":
                    return "Đang làm";
                case "WAITING_QC":
                    return "Chờ kiểm tra";
                case "QC_IN_PROGRESS":
                    return "Đang kiểm tra";
                case "QC_PASSED":
                    return "Đạt";
                case "QC_FAILED":
                    return "Không đạt";
                case "WAITING_REWORK":
                    return "Chờ sửa";
                case "REWORK_IN_PROGRESS":
                    return "Đang sửa";
                case "COMPLETED":
                    return "Hoàn thành";
                default:
                    break;
            }
        }
        // Fallback về status nếu executionStatus không có
        if (status != null) {
            switch (status) {
                case "PENDING":
                    return "Đợi";
                case "WAITING":
                    return "Chờ làm";
                case "IN_PROGRESS":
                    return "Đang làm";
                case "WAITING_QC":
                    return "Chờ kiểm tra";
                case "COMPLETED":
                    return "Hoàn thành";
                case "FAILED":
                    return "Không đạt";
                default:
                    return status;
            }
        }
        return "Không xác định";
    }
}
