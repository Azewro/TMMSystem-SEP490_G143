package tmmsystem.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tmmsystem.entity.Quotation;

public interface QuotationRepository extends JpaRepository<Quotation, Long>, JpaSpecificationExecutor<Quotation> {
    boolean existsByQuotationNumber(String quotationNumber);

    java.util.List<Quotation> findByCustomer_Id(Long customerId);

    Page<Quotation> findByCustomer_Id(Long customerId, Pageable pageable);

    Page<Quotation> findByAssignedSales_Id(Long salesId, Pageable pageable);

    Page<Quotation> findByAssignedPlanning_Id(Long planningId, Pageable pageable);

    java.util.List<Quotation> findByStatus(String status);

    Page<Quotation> findByStatus(String status, Pageable pageable);

    java.util.List<Quotation> findByStatusAndAssignedSales_Id(String status, Long salesId);

    Page<Quotation> findByStatusAndAssignedSales_Id(String status, Long salesId, Pageable pageable);

    java.util.List<Quotation> findByStatusAndSentAtBefore(String status, java.time.Instant sentAt);

    java.util.List<Quotation> findByStatusAndExpirationWarningSentFalseAndSentAtBefore(String status,
            java.time.Instant sentAt);

    // For capacity check: find active quotations (blocking capacity)
    java.util.List<Quotation> findByStatusIn(java.util.List<String> statuses);

    // With RFQ delivery date filter for capacity window check
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Quotation q JOIN q.rfq r WHERE q.status IN :statuses AND r.expectedDeliveryDate BETWEEN :start AND :end")
    java.util.List<Quotation> findByStatusInAndRfqDeliveryDateBetween(
            @org.springframework.data.repository.query.Param("statuses") java.util.List<String> statuses,
            @org.springframework.data.repository.query.Param("start") java.time.LocalDate start,
            @org.springframework.data.repository.query.Param("end") java.time.LocalDate end);
}
