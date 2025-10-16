// repository/ProductionStageRepository.java
package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tmmsystem.entity.ProductionStage;

import java.util.List;

@Repository
public interface ProductionStageRepository extends JpaRepository<ProductionStage, Long> {

    List<ProductionStage> findByWorkOrderDetailIdOrderByStageSequence(Long workOrderDetailId);

    List<ProductionStage> findByStatus(String status);

    List<ProductionStage> findByStageTypeAndStatus(String stageType, String status);

    List<ProductionStage> findByMachineId(Long machineId);

    List<ProductionStage> findByAssignedToId(Long userId);

    List<ProductionStage> findByAssignedLeaderId(Long leaderId);

    @Query("SELECT ps FROM ProductionStage ps WHERE ps.status = 'IN_PROGRESS' AND ps.assignedLeader.id = :leaderId")
    List<ProductionStage> findActiveStagesByLeader(@Param("leaderId") Long leaderId);
}
