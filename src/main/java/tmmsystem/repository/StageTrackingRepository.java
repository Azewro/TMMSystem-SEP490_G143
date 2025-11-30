package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tmmsystem.entity.StageTracking;

import java.util.List;

public interface StageTrackingRepository extends JpaRepository<StageTracking, Long> {
    List<StageTracking> findByProductionStageIdOrderByTimestampAsc(Long productionStageId);

    List<StageTracking> findByProductionStageIdOrderByTimestampDesc(Long productionStageId);
}
