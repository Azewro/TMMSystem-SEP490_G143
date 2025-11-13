package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "quotation",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"quotation_number"})
        },
        indexes = {
                @Index(name = "idx_quotation_customer_created", columnList = "customer_id, created_at"),
                @Index(name = "idx_quotation_status_canceled", columnList = "status, is_canceled"),
                // new indexes for assignments
                @Index(name = "idx_quotation_assigned_sales", columnList = "assigned_sales_id"),
                @Index(name = "idx_quotation_assigned_planning", columnList = "assigned_planning_id")
        }
)
@Getter @Setter
public class Quotation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "quotation_number", nullable = false, length = 50)
    private String quotationNumber; // QUO-YYYYMMDD-XXX

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id")
    private Rfq rfq; // nullable when direct quotation

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil; // expiry date

    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    // New: attached quotation file
    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(length = 20)
    private String status = "DRAFT"; // DRAFT, SENT, ACCEPTED, REJECTED, EXPIRED, CANCELED

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "is_accepted")
    private Boolean accepted = false;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "is_canceled")
    private Boolean canceled = false;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "reject_reason", columnDefinition = "text")
    private String rejectReason;

    // Capacity check (Planning Department)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "capacity_checked_by")
    private User capacityCheckedBy;

    @Column(name = "capacity_checked_at")
    private Instant capacityCheckedAt;

    @Column(name = "capacity_check_notes", columnDefinition = "text")
    private String capacityCheckNotes;

    // Assignees (mirror RFQ)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_sales_id")
    private User assignedSales; // Sales in charge of sending quotation

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_planning_id")
    private User assignedPlanning; // Planning who created/maintains quotation

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy; // Sales or Planning

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    // Contact snapshots copied from RFQ at time of quotation creation
    @Column(name = "contact_person_snapshot", length = 150)
    private String contactPersonSnapshot;

    @Column(name = "contact_email_snapshot", length = 150)
    private String contactEmailSnapshot;

    @Column(name = "contact_phone_snapshot", length = 30)
    private String contactPhoneSnapshot;

    @Column(name = "contact_address_snapshot", columnDefinition = "text")
    private String contactAddressSnapshot;

    @Column(name = "contact_method", length = 10)
    private String contactMethod; // EMAIL | PHONE

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuotationDetail> details = new ArrayList<>();
}
