package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tmmsystem.entity.QcSession;

import java.util.List;
import java.util.Optional;

@Repository
public interface QcSessionRepository extends JpaRepository<QcSession, Long> {
    List<QcSession> findByProductionStageId(Long stageId);
    Optional<QcSession> findByProductionStageIdAndStatus(Long stageId, String status);
}
