package tmmsystem.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import tmmsystem.entity.Rfq;

public interface RfqRepository extends JpaRepository<Rfq, Long>, JpaSpecificationExecutor<Rfq> {
    boolean existsByRfqNumber(String rfqNumber);

    @Query(value = "SELECT MAX(CAST(SUBSTRING_INDEX(rfq_number, '-', -1) AS UNSIGNED)) FROM rfq WHERE rfq_number LIKE CONCAT('RFQ-', DATE_FORMAT(UTC_TIMESTAMP(), '%Y%m%d'), '-%')", nativeQuery = true)
    Integer findMaxRfqSeqForToday();
    
    java.util.List<Rfq> findByAssignedSales_Id(Long salesId);
    Page<Rfq> findByAssignedSales_Id(Long salesId, Pageable pageable);
    java.util.List<Rfq> findByAssignedPlanning_Id(Long planningId);
    Page<Rfq> findByAssignedPlanning_Id(Long planningId, Pageable pageable);
    java.util.List<Rfq> findByStatusAndAssignedSalesIsNullOrAssignedPlanningIsNull(String status);
    Page<Rfq> findByStatusAndAssignedSalesIsNullOrAssignedPlanningIsNull(String status, Pageable pageable);
}
