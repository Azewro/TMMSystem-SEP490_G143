package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tmmsystem.entity.QcInspection;

import java.util.List;

public interface QcInspectionRepository extends JpaRepository<QcInspection, Long> {
    List<QcInspection> findByProductionStageId(Long productionStageId);

    List<QcInspection> findByProductionStageIdAndResult(Long productionStageId, String result);
}
