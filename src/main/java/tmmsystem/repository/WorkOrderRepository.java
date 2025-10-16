// repository/WorkOrderRepository.java
package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tmmsystem.entity.WorkOrder;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    Optional<WorkOrder> findByWoNumber(String woNumber);

    boolean existsByWoNumber(String woNumber);

    List<WorkOrder> findByProductionOrderId(Long productionOrderId);

    List<WorkOrder> findByStatus(String status);

    List<WorkOrder> findBySendStatus(String sendStatus);
}
