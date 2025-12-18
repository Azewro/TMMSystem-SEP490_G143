package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "contract", uniqueConstraints = {
                @UniqueConstraint(columnNames = { "contract_number" })
}, indexes = {
                @Index(name = "idx_contract_customer_date", columnList = "customer_id, contract_date"),
                @Index(name = "idx_contract_status", columnList = "status"),
                @Index(name = "idx_contract_sales_approved_by", columnList = "sales_approved_by"),
                @Index(name = "idx_contract_planning_approved_by", columnList = "planning_approved_by")
})
@Getter
@Setter
public class Contract {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @CreationTimestamp
        @Column(name = "created_at", updatable = false)
        private Instant createdAt;

        @UpdateTimestamp
        @Column(name = "updated_at")
        private Instant updatedAt;

        @Column(name = "contract_number", nullable = false, length = 50)
        private String contractNumber; // CON-YYYYMMDD-XXX

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "quotation_id")
        private Quotation quotation;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "customer_id")
        private Customer customer;

        @Column(name = "contract_date", nullable = false)
        private LocalDate contractDate;

        @Column(name = "delivery_date", nullable = false)
        private LocalDate deliveryDate;

        @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
        private BigDecimal totalAmount;

        @Column(name = "file_path", length = 500)
        private String filePath; // S3/Firebase path

        @Column(length = 20)
        private String status = "DRAFT"; // DRAFT, PENDING_APPROVAL, APPROVED, SIGNED, IN_PRODUCTION,
                                         // PRODUCTION_COMPLETED, CANCELED

        // NEW: Assignees (mirror RFQ/Quotation)
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "assigned_sales_id")
        private User assignedSales; // Sales in charge

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "assigned_planning_id")
        private User assignedPlanning; // Planning in charge

        // Director approval workflow
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "director_approved_by")
        private User directorApprovedBy;

        @Column(name = "director_approved_at")
        private Instant directorApprovedAt;

        @Column(name = "director_approval_notes", columnDefinition = "text")
        private String directorApprovalNotes;

        // Sales approval (separate from director)
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "sales_approved_by")
        private User salesApprovedBy;

        @Column(name = "sales_approved_at")
        private Instant salesApprovedAt;

        // Planning approval (capacity/feasibility sign-off)
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "planning_approved_by")
        private User planningApprovedBy;

        @Column(name = "planning_approved_at")
        private Instant planningApprovedAt;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "created_by")
        private User createdBy; // Sales person

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "approved_by")
        private User approvedBy; // Final approver
}
