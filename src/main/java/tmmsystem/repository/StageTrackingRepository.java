package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tmmsystem.entity.StageTracking;

import java.time.Instant;
import java.util.List;

public interface StageTrackingRepository extends JpaRepository<StageTracking, Long> {
    List<StageTracking> findByProductionStageIdOrderByTimestampAsc(Long productionStageId);

    List<StageTracking> findByProductionStageIdOrderByTimestampDesc(Long productionStageId);

    /**
     * Update timestamp directly (bypasses @CreationTimestamp on INSERT)
     */
    @Modifying
    @Query("UPDATE StageTracking t SET t.timestamp = :timestamp WHERE t.id = :id")
    void updateTimestampById(@Param("id") Long id, @Param("timestamp") Instant timestamp);
}
