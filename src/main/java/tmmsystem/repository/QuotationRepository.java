// repository/QuotationRepository.java
package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tmmsystem.entity.Quotation;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    @Query("SELECT q FROM Quotation q LEFT JOIN FETCH q.customer LEFT JOIN FETCH q.createdBy WHERE q.quotationNumber = :quotationNumber")
    Optional<Quotation> findByQuotationNumber(@Param("quotationNumber") String quotationNumber);

    boolean existsByQuotationNumber(String quotationNumber);

    List<Quotation> findByCustomerId(Long customerId);

    List<Quotation> findByStatus(String status);

    List<Quotation> findByRfqId(Long rfqId);
}
