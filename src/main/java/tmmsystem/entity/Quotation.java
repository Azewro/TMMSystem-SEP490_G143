// entity/Quotation.java
package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tmmsystem.common.BaseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quotation")
@Getter
@Setter
public class Quotation extends BaseEntity {

    @Column(name = "quotation_number", nullable = false, unique = true, length = 50)
    private String quotationNumber; // QUO-YYYYMMDD-XXX

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id")
    private Rfq rfq; // Can be null if direct quotation

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil; // Quotation expiry date

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 20)
    private String status = "DRAFT"; // DRAFT, SENT, ACCEPTED, REJECTED, EXPIRED, CANCELED

    @Column(name = "is_accepted")
    private Boolean accepted = false;

    @Column(name = "is_canceled")
    private Boolean canceled = false;

    // Capacity check (Planning Department workflow)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "capacity_checked_by")
    private User capacityCheckedBy; // Planning Department staff

    @Column(name = "capacity_checked_at")
    private Instant capacityCheckedAt;

    @Column(name = "capacity_check_notes", columnDefinition = "TEXT")
    private String capacityCheckNotes; // Can produce: Yes/No, Lead time: X weeks

    // Audit fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy; // Sales or Planning Department

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    // One-to-Many with QuotationDetail
    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuotationDetail> details = new ArrayList<>();

    // Helper method
    public void addDetail(QuotationDetail detail) {
        details.add(detail);
        detail.setQuotation(this);
    }
}
