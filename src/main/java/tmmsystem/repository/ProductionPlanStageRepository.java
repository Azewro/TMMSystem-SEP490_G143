package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tmmsystem.entity.ProductionPlanStage;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductionPlanStageRepository extends JpaRepository<ProductionPlanStage, Long> {
       // New: find by plan
       List<ProductionPlanStage> findByPlanIdOrderBySequenceNo(Long planId);

       // Tìm các công đoạn theo loại công đoạn
       List<ProductionPlanStage> findByStageType(String stageType);

       // Tìm các công đoạn theo máy được gán
       List<ProductionPlanStage> findByAssignedMachineId(Long machineId);

       // Tìm các công đoạn theo người phụ trách
       List<ProductionPlanStage> findByInChargeUserId(Long userId);

       // Kiểm tra xung đột máy móc trong khoảng thời gian
       @Query("SELECT pps FROM ProductionPlanStage pps WHERE pps.assignedMachine.id = :machineId " +
                     "AND ((pps.plannedStartTime BETWEEN :startTime AND :endTime) OR " +
                     "(pps.plannedEndTime BETWEEN :startTime AND :endTime) OR " +
                     "(pps.plannedStartTime <= :startTime AND pps.plannedEndTime >= :endTime))")
       List<ProductionPlanStage> findConflictingMachineTime(@Param("machineId") Long machineId,
                     @Param("startTime") LocalDateTime startTime,
                     @Param("endTime") LocalDateTime endTime);

       // Count production plan stages for a leader (used for auto-assign to balance
       // workload)
       long countByInChargeUserId(Long userId);
}
