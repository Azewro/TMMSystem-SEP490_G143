package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tmmsystem.entity.MaterialRequisition;

import java.util.List;

public interface MaterialRequisitionRepository extends JpaRepository<MaterialRequisition, Long> {
    List<MaterialRequisition> findByStatus(String status);

    List<MaterialRequisition> findByRequestedById(Long requestedById);

    List<MaterialRequisition> findByProductionStageIdAndStatus(Long stageId, String status);
}