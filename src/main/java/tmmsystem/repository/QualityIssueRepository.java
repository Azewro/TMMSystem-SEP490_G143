package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tmmsystem.entity.QualityIssue;

import java.util.List;

@Repository
public interface QualityIssueRepository extends JpaRepository<QualityIssue, Long> {
    List<QualityIssue> findByStatus(String status);

    List<QualityIssue> findByProductionStageId(Long stageId);

    List<QualityIssue> findByProductionOrderId(Long orderId);
}
