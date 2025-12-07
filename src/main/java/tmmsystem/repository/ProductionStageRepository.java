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

    // NEW: Find active stages on a specific machine
    List<ProductionStage> findByMachineIdAndExecutionStatus(Long machineId, String executionStatus);

    // NEW: Find stages by type and execution status (for Rework Preemption)
    List<ProductionStage> findByStageTypeAndExecutionStatus(String stageType, String executionStatus);

    // NEW: Find stages by type and status (for Rework Preemption)
    List<ProductionStage> findByStageTypeAndStatus(String stageType, String status);

    // NEW: Check if stages exist for a specific order
    boolean existsByProductionOrderId(Long productionOrderId);
}
