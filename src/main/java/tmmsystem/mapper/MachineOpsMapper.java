package tmmsystem.mapper;

import org.springframework.stereotype.Component;
import tmmsystem.dto.machine.*;
import tmmsystem.entity.*;

@Component
public class MachineOpsMapper {
    public MachineAssignmentDto toDto(MachineAssignment e) {
        if (e == null) return null;
        MachineAssignmentDto dto = new MachineAssignmentDto();
        dto.setId(e.getId());
        dto.setMachineId(e.getMachine() != null ? e.getMachine().getId() : null);
        dto.setProductionStageId(e.getProductionStage() != null ? e.getProductionStage().getId() : null);
        dto.setPlanStageId(e.getPlanStage() != null ? e.getPlanStage().getId() : null);
        dto.setAssignedAt(e.getAssignedAt());
        dto.setReleasedAt(e.getReleasedAt());
        dto.setReservationType(e.getReservationType());
        dto.setReservationStatus(e.getReservationStatus());
        return dto;
    }

    public MachineMaintenanceDto toDto(MachineMaintenance e) {
        if (e == null) return null;
        MachineMaintenanceDto dto = new MachineMaintenanceDto();
        dto.setId(e.getId());
        dto.setMachineId(e.getMachine() != null ? e.getMachine().getId() : null);
        dto.setMaintenanceType(e.getMaintenanceType());
        dto.setIssueDescription(e.getIssueDescription());
        dto.setResolution(e.getResolution());
        dto.setReportedById(e.getReportedBy() != null ? e.getReportedBy().getId() : null);
        dto.setAssignedToId(e.getAssignedTo() != null ? e.getAssignedTo().getId() : null);
        dto.setReportedAt(e.getReportedAt());
        dto.setStartedAt(e.getStartedAt());
        dto.setCompletedAt(e.getCompletedAt());
        dto.setStatus(e.getStatus());
        dto.setCost(e.getCost());
        dto.setDowntimeMinutes(e.getDowntimeMinutes());
        return dto;
    }
}


