package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tmmsystem.entity.Rfq;

public interface RfqRepository extends JpaRepository<Rfq, Long> {
    boolean existsByRfqNumber(String rfqNumber);

    @Query(value = "SELECT MAX(CAST(SUBSTRING_INDEX(rfq_number, '-', -1) AS UNSIGNED)) FROM rfq WHERE rfq_number LIKE CONCAT('RFQ-', DATE_FORMAT(UTC_TIMESTAMP(), '%Y%m%d'), '-%')", nativeQuery = true)
    Integer findMaxRfqSeqForToday();
}
