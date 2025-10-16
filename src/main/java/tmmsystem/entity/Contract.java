// entity/Contract.java
package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tmmsystem.common.BaseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "contract")
@Getter
@Setter
public class Contract extends BaseEntity {

    @Column(name = "contract_number", nullable = false, unique = true, length = 50)
    private String contractNumber; // CON-YYYYMMDD-XXX

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id")
    private Quotation quotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "contract_date", nullable = false)
    private LocalDate contractDate;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "file_path", length = 500)
    private String filePath; // S3/Firebase path to signed PDF

    @Column(length = 20)
    private String status = "DRAFT"; // DRAFT, PENDING_APPROVAL, APPROVED, SIGNED, CANCELED

    // Director approval workflow
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "director_approved_by")
    private User directorApprovedBy; // Director who approved

    @Column(name = "director_approved_at")
    private Instant directorApprovedAt;

    @Column(name = "director_approval_notes", columnDefinition = "TEXT")
    private String directorApprovalNotes;

    // Audit fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy; // Sales person

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy; // Final approver
}
