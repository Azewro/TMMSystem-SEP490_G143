
package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tmmsystem.entity.MachineAssignment;

import java.util.List;

public interface MachineAssignmentRepository extends JpaRepository<MachineAssignment, Long> {
        List<MachineAssignment> findByMachineIdOrderByAssignedAtDesc(Long machineId);

        List<MachineAssignment> findByProductionStageId(Long productionStageId);

        List<MachineAssignment> findByPlanStagePlanId(Long planId);

        List<MachineAssignment> findByProductionStageIdAndReleasedAtIsNull(Long productionStageId);

        void deleteByPlanStagePlanId(Long planId);

        List<MachineAssignment> findByProductionStageAndReservationStatus(
                        tmmsystem.entity.ProductionStage productionStage,
                        String reservationStatus);

        org.springframework.data.domain.Page<MachineAssignment> findByMachineIdOrderByAssignedAtDesc(Long machineId,
                        org.springframework.data.domain.Pageable pageable);

        java.util.Optional<MachineAssignment> findByMachineAndProductionStage(tmmsystem.entity.Machine machine,
                        tmmsystem.entity.ProductionStage productionStage);
}
