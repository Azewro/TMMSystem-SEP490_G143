package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "production_stage",
        indexes = {
                @Index(name = "idx_stage_wodetail_sequence", columnList = "work_order_detail_id, stage_sequence", unique = true),
                @Index(name = "idx_stage_status_type", columnList = "status, stage_type"),
                @Index(name = "idx_stage_leader_status", columnList = "assigned_leader_id, status"),
                @Index(name = "idx_stage_machine_status", columnList = "machine_id, status")
        }
)
@Getter @Setter
public class ProductionStage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_detail_id")
    private WorkOrderDetail workOrderDetail;

    @Column(name = "stage_type", length = 20, nullable = false)
    private String stageType; // WARPING, WEAVING, DYEING, CUTTING, HEMMING, PACKAGING

    @Column(name = "stage_sequence", nullable = false)
    private Integer stageSequence; // 1..6

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id")
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_leader_id")
    private User assignedLeader;

    @Column(name = "batch_number", length = 50)
    private String batchNumber;

    @Column(name = "planned_output", precision = 10, scale = 2)
    private BigDecimal plannedOutput;

    @Column(name = "actual_output", precision = 10, scale = 2)
    private BigDecimal actualOutput;

    @Column(name = "start_at")
    private Instant startAt;

    @Column(name = "complete_at")
    private Instant completeAt;

    @Column(length = 20)
    private String status = "PENDING"; // PENDING, IN_PROGRESS, PAUSED, COMPLETED, FAILED, CANCELED

    @Column(name = "is_outsourced")
    private Boolean outsourced = false;

    @Column(name = "outsource_vendor", length = 255)
    private String outsourceVendor;

    @Column(columnDefinition = "text")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "planned_start_at")
    private Instant plannedStartAt;

    @Column(name = "planned_end_at")
    private Instant plannedEndAt;

    @Column(name = "planned_duration_hours", precision = 10, scale = 2)
    private BigDecimal plannedDurationHours;

    @Column(name = "qr_token", length = 64, unique = true)
    private String qrToken;

    @Column(name = "qc_last_result", length = 20)
    private String qcLastResult; // PASS / FAIL / CONDITIONAL

    @Column(name = "qc_last_checked_at")
    private Instant qcLastCheckedAt;
}
