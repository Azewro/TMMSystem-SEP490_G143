package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tmmsystem.entity.QcCheckpoint;

import java.util.List;

public interface QcCheckpointRepository extends JpaRepository<QcCheckpoint, Long> {
    List<QcCheckpoint> findByStageTypeOrderByDisplayOrderAsc(String stageType);
    boolean existsByStageTypeAndCheckpointName(String stageType, String checkpointName);
}
