// repository/ProductionOrderRepository.java
package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tmmsystem.entity.ProductionOrder;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, Long> {

    @Query("SELECT po FROM ProductionOrder po LEFT JOIN FETCH po.createdBy LEFT JOIN FETCH po.contract WHERE po.poNumber = :poNumber")
    Optional<ProductionOrder> findByPoNumber(@Param("poNumber") String poNumber);

    boolean existsByPoNumber(String poNumber);

    List<ProductionOrder> findByStatus(String status);

    List<ProductionOrder> findByContractId(Long contractId);

    @Query("SELECT po FROM ProductionOrder po WHERE po.status = :status ORDER BY po.priority DESC, po.plannedStartDate ASC")
    List<ProductionOrder> findByStatusOrderByPriorityDesc(@Param("status") String status);
}
