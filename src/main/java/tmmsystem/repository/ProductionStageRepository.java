package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tmmsystem.entity.ProductionStage;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionStageRepository extends JpaRepository<ProductionStage, Long> {
    // REMOVED: findByWorkOrderDetailIdOrderByStageSequenceAsc - property workOrderDetailId không còn tồn tại
    
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
    
    // REMOVED: findStagesByOrderIdOld - query dùng workOrderDetail không còn hợp lệ
}
