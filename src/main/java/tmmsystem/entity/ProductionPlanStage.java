package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "production_plan_stage",
        indexes = {
                @Index(name = "idx_plan_stage_plan", columnList = "plan_id"),
                @Index(name = "idx_plan_stage_type", columnList = "stage_type"),
                @Index(name = "idx_plan_stage_machine", columnList = "assigned_machine_id"),
                @Index(name = "idx_plan_stage_user", columnList = "in_charge_user_id"),
                @Index(name = "idx_plan_stage_sequence", columnList = "plan_id, sequence_no")
        }
)
@Getter
@Setter
public class ProductionPlanStage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ProductionPlan plan; // CHANGED: tham chiếu trực tiếp plan, disable auto FK creation

    @Column(name = "stage_type", length = 20, nullable = false)
    private String stageType; // Công đoạn sản xuất (WARPING, WEAVING, DYEING, CUTTING, HEMMING, PACKAGING)

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo; // Thứ tự công đoạn trong quy trình

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_machine_id")
    private Machine assignedMachine; // Máy được gợi ý/đề xuất

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "in_charge_user_id")
    private User inChargeUser; // Người phụ trách công đoạn

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qc_user_id")
    private User qcUser; // Người kiểm tra chất lượng

    @Column(name = "planned_start_time", nullable = false)
    private LocalDateTime plannedStartTime;

    @Column(name = "planned_end_time", nullable = false)
    private LocalDateTime plannedEndTime;

    @Column(name = "min_required_duration_minutes")
    private Integer minRequiredDurationMinutes; // Thời lượng tối thiểu hệ thống tính

    @Column(name = "transfer_batch_quantity", precision = 10, scale = 2)
    private BigDecimal transferBatchQuantity; // Số lượng chuyển batch giữa công đoạn

    @Column(name = "capacity_per_hour", precision = 10, scale = 2)
    private BigDecimal capacityPerHour; // Năng suất/giờ của máy tại công đoạn

    @Column(name = "stage_status", length = 20)
    private String stageStatus = "PENDING"; // PENDING, READY, IN_PROGRESS, PAUSED, COMPLETED, CANCELED

    @Column(name = "setup_time_minutes")
    private Integer setupTimeMinutes;

    @Column(name = "teardown_time_minutes")
    private Integer teardownTimeMinutes;

    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;

    @Column(name = "downtime_minutes")
    private Integer downtimeMinutes;

    @Column(name = "downtime_reason", length = 200)
    private String downtimeReason;

    @Column(name = "quantity_input", precision = 12, scale = 2)
    private BigDecimal quantityInput;

    @Column(name = "quantity_output", precision = 12, scale = 2)
    private BigDecimal quantityOutput;

    @Column(columnDefinition = "text")
    private String notes;
}
