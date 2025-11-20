package tmmsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.entity.*;
import tmmsystem.repository.*;

import java.util.List;

@Service
public class MachineOpsService {
    private final MachineAssignmentRepository assignmentRepo;
    private final MachineMaintenanceRepository maintenanceRepo;

    public MachineOpsService(MachineAssignmentRepository assignmentRepo, MachineMaintenanceRepository maintenanceRepo) {
        this.assignmentRepo = assignmentRepo; this.maintenanceRepo = maintenanceRepo;
    }

    // Assignments
    public List<MachineAssignment> listAssignmentsByMachine(Long machineId) { return assignmentRepo.findByMachineIdOrderByAssignedAtDesc(machineId); }
    public List<MachineAssignment> listAssignmentsByStage(Long stageId) { return assignmentRepo.findByProductionStageId(stageId); }
    public MachineAssignment getAssignment(Long id) { return assignmentRepo.findById(id).orElseThrow(); }
    @Transactional public MachineAssignment createAssignment(MachineAssignment e) { return assignmentRepo.save(e); }
    @Transactional public MachineAssignment releaseAssignment(Long id, java.time.Instant releasedAt) {
        MachineAssignment e = assignmentRepo.findById(id).orElseThrow();
        e.setReleasedAt(releasedAt);
        e.setReservationStatus("RELEASED");
        return e;
    }
    public void deleteAssignment(Long id) { assignmentRepo.deleteById(id); }

    // Maintenance
    public List<MachineMaintenance> listMaintenances(Long machineId) { return maintenanceRepo.findByMachineIdOrderByReportedAtDesc(machineId); }
    public MachineMaintenance getMaintenance(Long id) { return maintenanceRepo.findById(id).orElseThrow(); }
    @Transactional public MachineMaintenance createMaintenance(MachineMaintenance e) { return maintenanceRepo.save(e); }
    @Transactional public MachineMaintenance updateMaintenance(Long id, MachineMaintenance upd) { MachineMaintenance e = maintenanceRepo.findById(id).orElseThrow(); e.setMachine(upd.getMachine()); e.setMaintenanceType(upd.getMaintenanceType()); e.setIssueDescription(upd.getIssueDescription()); e.setResolution(upd.getResolution()); e.setReportedBy(upd.getReportedBy()); e.setAssignedTo(upd.getAssignedTo()); e.setReportedAt(upd.getReportedAt()); e.setStartedAt(upd.getStartedAt()); e.setCompletedAt(upd.getCompletedAt()); e.setStatus(upd.getStatus()); e.setCost(upd.getCost()); e.setDowntimeMinutes(upd.getDowntimeMinutes()); return e; }
    public void deleteMaintenance(Long id) { maintenanceRepo.deleteById(id); }
}
