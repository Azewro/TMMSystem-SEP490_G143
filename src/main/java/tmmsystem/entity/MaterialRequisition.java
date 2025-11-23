package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity @Table(name = "material_requisition",
        uniqueConstraints = { @UniqueConstraint(columnNames = {"requisition_number"}) },
        indexes = {
                @Index(name = "idx_requisition_stage", columnList = "production_stage_id"),
                @Index(name = "idx_requisition_status_time", columnList = "status, requested_at"),
                @Index(name = "idx_requisition_issue", columnList = "source_issue_id")
        }
)
@Getter @Setter
public class MaterialRequisition {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requisition_number", length = 50, nullable = false)
    private String requisitionNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_stage_id")
    private ProductionStage productionStage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by")
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(length = 20)
    private String status = "PENDING"; // PENDING, APPROVED, ISSUED, REJECTED, CANCELED

    @CreationTimestamp
    @Column(name = "requested_at", updatable = false)
    private Instant requestedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(columnDefinition = "text")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_issue_id")
    private QualityIssue sourceIssue; // phát sinh từ lỗi QC mức nặng

    @Column(name = "requisition_type", length = 30)
    private String requisitionType; // YARN_SUPPLY, OTHER
}
