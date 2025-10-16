// repository/RfqRepository.java
package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tmmsystem.entity.Rfq;

import java.util.List;
import java.util.Optional;

@Repository
public interface RfqRepository extends JpaRepository<Rfq, Long> {

    @Query("SELECT r FROM Rfq r LEFT JOIN FETCH r.customer LEFT JOIN FETCH r.createdBy WHERE r.rfqNumber = :rfqNumber")
    Optional<Rfq> findByRfqNumber(@Param("rfqNumber") String rfqNumber);

    boolean existsByRfqNumber(String rfqNumber);

    List<Rfq> findByCustomerId(Long customerId);

    List<Rfq> findByStatus(String status);
}
