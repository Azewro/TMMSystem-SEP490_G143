package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tmmsystem.entity.ProductionStage;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionStageRepository extends JpaRepository<ProductionStage, Long> {
        // REMOVED: findByWorkOrderDetailIdOrderByStageSequenceAsc - property
        // workOrderDetailId không còn tồn tại

        // NEW: Query trực tiếp theo ProductionOrder
        List<ProductionStage> findByProductionOrderIdOrderByStageSequenceAsc(Long productionOrderId);

        Optional<ProductionStage> findByQrToken(String qrToken);

        List<ProductionStage> findByAssignedLeaderIdAndStatusIn(Long leaderId, List<String> statuses);

        List<ProductionStage> findByStatus(String status);

        List<ProductionStage> findByExecutionStatus(String executionStatus);

        // Query stages by execution status with JOIN FETCH to avoid
        // LazyLoadingException
        @Query("select s from ProductionStage s join fetch s.productionOrder po where s.executionStatus in :statuses")
        List<ProductionStage> findByExecutionStatusIn(@Param("statuses") List<String> executionStatuses);

        List<ProductionStage> findByQcAssigneeIdAndExecutionStatusIn(Long qcUserId, List<String> statuses);

        // Query tất cả stages được assign cho QA (không filter theo status)
        // Sử dụng JOIN FETCH để load productionOrder ngay lập tức, tránh
        // LazyLoadingException
        @Query("select s from ProductionStage s join fetch s.productionOrder po where s.qcAssignee.id = :qcUserId")
        List<ProductionStage> findByQcAssigneeId(@Param("qcUserId") Long qcUserId);

        @Query("select s from ProductionStage s join fetch s.productionOrder po where s.assignedLeader.id = :leaderId and s.executionStatus in :statuses order by po.priority desc, s.id asc")
        List<ProductionStage> findByAssignedLeaderIdAndExecutionStatusIn(@Param("leaderId") Long leaderId,
                        @Param("statuses") List<String> statuses);

        // NEW: Count active stages for Leader workload balancing
        long countByAssignedLeaderIdAndExecutionStatusIn(Long leaderId, List<String> executionStatuses);

        // NEW: Count active stages for QC workload balancing (using qcAssignee)
        long countByQcAssigneeIdAndExecutionStatusIn(Long qcAssigneeId, List<String> executionStatuses);

        // Query tất cả stages được assign cho leader (không filter theo status)
        // Sử dụng JOIN FETCH để load productionOrder ngay lập tức, tránh
        // LazyLoadingException
        @Query("select s from ProductionStage s join fetch s.productionOrder po where s.assignedLeader.id = :leaderId")
        List<ProductionStage> findByAssignedLeaderId(Long leaderId);

        // NEW: Query trực tiếp theo ProductionOrder (thay thế query cũ)
        @Query("select s from ProductionStage s where s.productionOrder.id = :orderId order by s.stageSequence asc")
        List<ProductionStage> findStagesByOrderId(Long orderId);

        // REMOVED: findStagesByOrderIdOld - query dùng workOrderDetail không còn hợp lệ
        // NEW: Count active stages for capacity check
        long countByStageTypeAndExecutionStatusIn(String stageType, List<String> executionStatuses);

        // NEW: Find active stages for capacity check (with order info)
        @Query("select s from ProductionStage s join fetch s.productionOrder po where s.stageType = :stageType and s.executionStatus in :statuses")
        List<ProductionStage> findByStageTypeAndExecutionStatusIn(@Param("stageType") String stageType,
                        @Param("statuses") List<String> executionStatuses);

        // NEW: Count active (progress < 100) stages of same type excluding a specific
        // stage
        @Query("select count(s) from ProductionStage s where s.stageType = :stageType and s.id <> :stageId and (s.progressPercent is null or s.progressPercent < 100) and s.executionStatus in :executionStatuses")
        long countActiveByStageTypeExcludingStage(@Param("stageType") String stageType, @Param("stageId") Long stageId,
                        @Param("executionStatuses") List<String> executionStatuses);

        // NEW: Find active stages on a specific machine
        List<ProductionStage> findByMachineIdAndExecutionStatus(Long machineId, String executionStatus);

        // NEW: Find stages by type and execution status (for Rework Preemption)
        List<ProductionStage> findByStageTypeAndExecutionStatus(String stageType, String executionStatus);

        // NEW: Find stages by type and status (for Rework Preemption)
        List<ProductionStage> findByStageTypeAndStatus(String stageType, String status);

        // NEW: Check if stages exist for a specific order
        boolean existsByProductionOrderId(Long productionOrderId);

        // NEW: Count active stages where progress < 100% for Leader strictly assignment
        @Query("select count(s) from ProductionStage s where s.assignedLeader.id = :leaderId and (s.progressPercent is null or s.progressPercent < 100) and (s.executionStatus is null or s.executionStatus in :executionStatuses)")
        long countByAssignedLeaderIdAndProgressPercentLessThan100(@Param("leaderId") Long leaderId,
                        @Param("executionStatuses") List<String> executionStatuses);

        // NEW: Find pending stages by type, ordered by PO priority then creation date
        // (for cross-PO promotion)
        @Query("SELECT s FROM ProductionStage s " +
                        "JOIN s.productionOrder po " +
                        "WHERE s.stageType = :stageType AND s.executionStatus = 'PENDING' " +
                        "ORDER BY po.priority DESC, po.createdAt ASC")
        List<ProductionStage> findPendingByStageTypeOrderByPriority(@Param("stageType") String stageType);

        // NEW: Count distinct Production Orders for a leader (for workload balancing by
        // PO count)
        @Query("SELECT COUNT(DISTINCT s.productionOrder.id) FROM ProductionStage s WHERE s.assignedLeader.id = :leaderId")
        long countDistinctProductionOrdersByLeaderId(@Param("leaderId") Long leaderId);
}
