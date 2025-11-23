package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tmmsystem.entity.ProductionStage;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionStageRepository extends JpaRepository<ProductionStage, Long> {
    List<ProductionStage> findByWorkOrderDetailIdOrderByStageSequenceAsc(Long workOrderDetailId);
    Optional<ProductionStage> findByQrToken(String qrToken);
    List<ProductionStage> findByAssignedLeaderIdAndStatusIn(Long leaderId, List<String> statuses);
    List<ProductionStage> findByStatus(String status);
    List<ProductionStage> findByExecutionStatus(String executionStatus);
    List<ProductionStage> findByExecutionStatusIn(List<String> executionStatuses);
    List<ProductionStage> findByQcAssigneeIdAndExecutionStatusIn(Long qcUserId, List<String> statuses);
    List<ProductionStage> findByAssignedLeaderIdAndExecutionStatusIn(Long leaderId, List<String> statuses);

    @Query("select s from ProductionStage s where s.workOrderDetail.productionOrderDetail.productionOrder.id = :orderId")
    List<ProductionStage> findStagesByOrderId(Long orderId);
}
