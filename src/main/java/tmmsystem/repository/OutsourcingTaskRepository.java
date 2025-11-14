package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tmmsystem.entity.OutsourcingTask;

import java.util.List;

public interface OutsourcingTaskRepository extends JpaRepository<OutsourcingTask, Long> {
    List<OutsourcingTask> findByProductionStageId(Long productionStageId);
}
