package tmmsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springframework.web.bind.annotation.*;
import tmmsystem.dto.machine.*;
import tmmsystem.entity.*;
import tmmsystem.mapper.MachineOpsMapper;
import tmmsystem.service.MachineOpsService;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/machines")
public class MachineOpsController {
    private final MachineOpsService service;
    private final MachineOpsMapper mapper;

    public MachineOpsController(MachineOpsService service, MachineOpsMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    // Assignments
    // @GetMapping("/{machineId}/assignments")
    // public List<MachineAssignmentDto> listAssignmentsByMachine(@PathVariable Long
    // machineId) { return
    // service.listAssignmentsByMachine(machineId).stream().map(mapper::toDto).collect(Collectors.toList());
    // }
    @GetMapping("/stages/{stageId}/assignments")
    public List<MachineAssignmentDto> listAssignmentsByStage(@PathVariable Long stageId) {
        return service.listAssignmentsByStage(stageId).stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @GetMapping("/assignments/{id}")
    public MachineAssignmentDto getAssignment(@PathVariable Long id) {
        return mapper.toDto(service.getAssignment(id));
    }

    @Operation(summary = "Gán máy cho stage")
    @PostMapping("/assignments")
    public MachineAssignmentDto createAssignment(
            @RequestBody(description = "Payload gán máy", required = true, content = @Content(schema = @Schema(implementation = MachineAssignmentDto.class))) @org.springframework.web.bind.annotation.RequestBody MachineAssignmentDto body) {
        MachineAssignment e = new MachineAssignment();
        if (body.getMachineId() != null) {
            Machine m = new Machine();
            m.setId(body.getMachineId());
            e.setMachine(m);
        }
        if (body.getProductionStageId() != null) {
            ProductionStage s = new ProductionStage();
            s.setId(body.getProductionStageId());
            e.setProductionStage(s);
        }
        if (body.getPlanStageId() != null) {
            ProductionPlanStage ps = new ProductionPlanStage();
            ps.setId(body.getPlanStageId());
            e.setPlanStage(ps);
        }
        e.setReservationType(body.getReservationType() != null ? body.getReservationType() : e.getReservationType());
        e.setReservationStatus(
                body.getReservationStatus() != null ? body.getReservationStatus() : e.getReservationStatus());
        e.setAssignedAt(body.getAssignedAt());
        e.setReleasedAt(body.getReleasedAt());
        return mapper.toDto(service.createAssignment(e));
    }

    @Operation(summary = "Release assignment")
    @PostMapping("/assignments/{id}/release")
    public MachineAssignmentDto releaseAssignment(@PathVariable Long id,
            @RequestParam(required = false) Instant releasedAt) {
        return mapper.toDto(service.releaseAssignment(id, releasedAt != null ? releasedAt : Instant.now()));
    }

    @DeleteMapping("/assignments/{id}")
    public void deleteAssignment(@PathVariable Long id) {
        service.deleteAssignment(id);
    }

    // Maintenance
    @GetMapping("/{machineId}/maintenances")
    public List<MachineMaintenanceDto> listMaintenances(@PathVariable Long machineId) {
        return service.listMaintenances(machineId).stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @GetMapping("/maintenances/{id}")
    public MachineMaintenanceDto getMaintenance(@PathVariable Long id) {
        return mapper.toDto(service.getMaintenance(id));
    }

    @Operation(summary = "Tạo maintenance")
    @PostMapping("/maintenances")
    public MachineMaintenanceDto createMaintenance(
            @RequestBody(description = "Payload maintenance", required = true, content = @Content(schema = @Schema(implementation = MachineMaintenanceDto.class))) @org.springframework.web.bind.annotation.RequestBody MachineMaintenanceDto body) {
        MachineMaintenance e = new MachineMaintenance();
        if (body.getMachineId() != null) {
            Machine m = new Machine();
            m.setId(body.getMachineId());
            e.setMachine(m);
        }
        e.setMaintenanceType(body.getMaintenanceType());
        e.setIssueDescription(body.getIssueDescription());
        e.setResolution(body.getResolution());
        if (body.getReportedById() != null) {
            User u = new User();
            u.setId(body.getReportedById());
            e.setReportedBy(u);
        }
        if (body.getAssignedToId() != null) {
            User u2 = new User();
            u2.setId(body.getAssignedToId());
            e.setAssignedTo(u2);
        }
        e.setReportedAt(body.getReportedAt());
        e.setStartedAt(body.getStartedAt());
        e.setCompletedAt(body.getCompletedAt());
        e.setStatus(body.getStatus());
        e.setCost(body.getCost());
        e.setDowntimeMinutes(body.getDowntimeMinutes());
        return mapper.toDto(service.createMaintenance(e));
    }

    @PutMapping("/maintenances/{id}")
    public MachineMaintenanceDto updateMaintenance(@PathVariable Long id, @RequestBody MachineMaintenanceDto body) {
        MachineMaintenance e = new MachineMaintenance();
        if (body.getMachineId() != null) {
            Machine m = new Machine();
            m.setId(body.getMachineId());
            e.setMachine(m);
        }
        e.setMaintenanceType(body.getMaintenanceType());
        e.setIssueDescription(body.getIssueDescription());
        e.setResolution(body.getResolution());
        if (body.getReportedById() != null) {
            User u = new User();
            u.setId(body.getReportedById());
            e.setReportedBy(u);
        }
        if (body.getAssignedToId() != null) {
            User u2 = new User();
            u2.setId(body.getAssignedToId());
            e.setAssignedTo(u2);
        }
        e.setReportedAt(body.getReportedAt());
        e.setStartedAt(body.getStartedAt());
        e.setCompletedAt(body.getCompletedAt());
        e.setStatus(body.getStatus());
        e.setCost(body.getCost());
        e.setDowntimeMinutes(body.getDowntimeMinutes());
        return mapper.toDto(service.updateMaintenance(id, e));
    }

    @DeleteMapping("/maintenances/{id}")
    public void deleteMaintenance(@PathVariable Long id) {
        service.deleteMaintenance(id);
    }
}
