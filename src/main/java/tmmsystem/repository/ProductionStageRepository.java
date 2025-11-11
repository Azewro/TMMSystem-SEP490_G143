package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tmmsystem.entity.ProductionStage;

import java.util.List;
import java.util.Optional;

public interface ProductionStageRepository extends JpaRepository<ProductionStage, Long> {
    List<ProductionStage> findByWorkOrderDetailIdOrderByStageSequenceAsc(Long workOrderDetailId);
    Optional<ProductionStage> findByQrToken(String qrToken);
    List<ProductionStage> findByAssignedLeaderIdAndStatusIn(Long leaderId, List<String> statuses);
    List<ProductionStage> findByStatus(String status);
}
