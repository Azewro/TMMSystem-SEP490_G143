package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tmmsystem.entity.ProductionStage;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionStageRepository extends JpaRepository<ProductionStage, Long> {
    // OLD: Query theo WorkOrderDetail (deprecated, chỉ dùng cho migration)
    @Deprecated
    List<ProductionStage> findByWorkOrderDetailIdOrderByStageSequenceAsc(Long workOrderDetailId);
    
    // NEW: Query trực tiếp theo ProductionOrder
    List<ProductionStage> findByProductionOrderIdOrderByStageSequenceAsc(Long productionOrderId);
    
    Optional<ProductionStage> findByQrToken(String qrToken);
    List<ProductionStage> findByAssignedLeaderIdAndStatusIn(Long leaderId, List<String> statuses);
    List<ProductionStage> findByStatus(String status);
    List<ProductionStage> findByExecutionStatus(String executionStatus);
    List<ProductionStage> findByExecutionStatusIn(List<String> executionStatuses);
    List<ProductionStage> findByQcAssigneeIdAndExecutionStatusIn(Long qcUserId, List<String> statuses);
    List<ProductionStage> findByAssignedLeaderIdAndExecutionStatusIn(Long leaderId, List<String> statuses);

    // NEW: Query trực tiếp theo ProductionOrder (thay thế query cũ)
    @Query("select s from ProductionStage s where s.productionOrder.id = :orderId order by s.stageSequence asc")
    List<ProductionStage> findStagesByOrderId(Long orderId);
    
    // OLD: Query qua WorkOrderDetail (deprecated, giữ lại để backward compatibility tạm thời)
    @Deprecated
    @Query("select s from ProductionStage s where s.workOrderDetail.productionOrderDetail.productionOrder.id = :orderId")
    List<ProductionStage> findStagesByOrderIdOld(Long orderId);
}
