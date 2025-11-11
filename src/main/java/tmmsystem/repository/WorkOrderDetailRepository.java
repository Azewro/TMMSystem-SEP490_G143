package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tmmsystem.entity.WorkOrderDetail;

import java.util.List;

public interface WorkOrderDetailRepository extends JpaRepository<WorkOrderDetail, Long> {
    List<WorkOrderDetail> findByWorkOrderId(Long workOrderId);
}
