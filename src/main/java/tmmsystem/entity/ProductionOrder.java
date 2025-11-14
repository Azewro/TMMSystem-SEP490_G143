package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity @Table(name = "production_order",
        uniqueConstraints = { @UniqueConstraint(columnNames = {"po_number"}) },
        indexes = {
                @Index(name = "idx_po_contract", columnList = "contract_id"),
                @Index(name = "idx_po_status_priority", columnList = "status, priority")
        }
)
@Getter @Setter
public class ProductionOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "po_number", length = 50, nullable = false)
    private String poNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract; // nullable for internal orders

    @Column(name = "total_quantity", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalQuantity;

    @Column(name = "planned_start_date")
    private LocalDate plannedStartDate;

    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;

    @Column(length = 30)
    private String status = "DRAFT";

    @Column
    private Integer priority = 0;

    @Column(columnDefinition = "text")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy; // Planning Department

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy; // Director approval

    @Column(name = "approved_at")
    private Instant approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_technician_id")
    private User assignedTechnician; // Kỹ thuật viên được PM phân công

    @Column(name = "assigned_at")
    private Instant assignedAt;
}
