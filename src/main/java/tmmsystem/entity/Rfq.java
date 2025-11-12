package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "rfq",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"rfq_number"})
        },
        indexes = {
                @Index(name = "idx_rfq_customer_created", columnList = "customer_id, created_at"),
                @Index(name = "idx_rfq_status", columnList = "status"),
                @Index(name = "idx_rfq_assigned_sales", columnList = "assigned_sales_id"),
                @Index(name = "idx_rfq_assigned_planning", columnList = "assigned_planning_id")
        }
)
@Getter @Setter
public class Rfq {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "rfq_number", nullable = false, length = 50)
    private String rfqNumber; // RFQ-YYYYMMDD-XXX

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "source_type", length = 30)
    private String sourceType = "CUSTOMER_PORTAL"; // CUSTOMER_PORTAL | PUBLIC_FORM | BY_SALES

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(length = 100)
    private String status = "DRAFT"; // DRAFT, SENT, PRELIMINARY_CHECKED, FORWARDED_TO_PLANNING, RECEIVED_BY_PLANNING, QUOTED, CANCELED

    @Column(name = "is_sent")
    private Boolean sent = false;

    @Column(columnDefinition = "text")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy; // Sales person

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_sales_id")
    private User assignedSales; // Sales chính phụ trách RFQ

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_planning_id")
    private User assignedPlanning; // Planning phụ trách kiểm tra năng lực

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy; // Director

    @Column(name = "approval_date")
    private Instant approvalDate;

    // Sales confirmation & locking
    @Column(name = "sales_confirmed_at")
    private Instant salesConfirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_confirmed_by")
    private User salesConfirmedBy;

    @Column(name = "is_locked")
    private Boolean locked = false;

    // Capacity evaluation result
    @Column(name = "capacity_status", length = 20)
    private String capacityStatus; // SUFFICIENT / INSUFFICIENT

    @Column(name = "capacity_reason", columnDefinition = "text")
    private String capacityReason;

    @Column(name = "proposed_new_delivery_date")
    private LocalDate proposedNewDeliveryDate;

    // Contact snapshots at time of RFQ creation (or latest sales edit before lock)
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

    @OneToMany(mappedBy = "rfq", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RfqDetail> details = new ArrayList<>();
}
