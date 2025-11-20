package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tmmsystem.entity.MachineAssignment;

import java.util.List;

public interface MachineAssignmentRepository extends JpaRepository<MachineAssignment, Long> {
    List<MachineAssignment> findByMachineIdOrderByAssignedAtDesc(Long machineId);
    List<MachineAssignment> findByProductionStageId(Long productionStageId);
    List<MachineAssignment> findByPlanStagePlanId(Long planId);
    void deleteByPlanStagePlanId(Long planId);
}


