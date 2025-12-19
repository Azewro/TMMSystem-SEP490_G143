package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "production_stage", indexes = {
        // REMOVED: idx_stage_wodetail_sequence - cột work_order_detail_id đã bị xóa
        // khỏi database
        @Index(name = "idx_stage_po_sequence", columnList = "production_order_id, stage_sequence"), // Removed unique:
                                                                                                    // một PO có thể có
                                                                                                    // nhiều sets stages
        @Index(name = "idx_stage_status_type", columnList = "status, stage_type"),
        @Index(name = "idx_stage_leader_status", columnList = "assigned_leader_id, status"),
        @Index(name = "idx_stage_machine_status", columnList = "machine_id, status"),
        @Index(name = "idx_stage_exec_status", columnList = "execution_status")
})
@Getter
@Setter
public class ProductionStage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // REMOVED: workOrderDetail field - cột work_order_detail_id đã bị xóa khỏi
    // database
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "work_order_detail_id")
    // private WorkOrderDetail workOrderDetail;

    // NEW: Link trực tiếp với ProductionOrder
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_order_id")
    private ProductionOrder productionOrder;

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

    @Column(length = 30)
    private String status = "PENDING"; // PENDING, IN_PROGRESS, PAUSED, COMPLETED, FAILED, CANCELED, IN_SUPPLEMENTARY

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qc_assignee_id")
    private User qcAssignee; // Người QC phụ trách (tuỳ chọn)

    @Column(name = "execution_status", length = 30)
    private String executionStatus; // NEW: WAITING, IN_PROGRESS, WAITING_QC, QC_IN_PROGRESS, QC_PASSED, QC_FAILED,
                                    // WAITING_REWORK, REWORK_IN_PROGRESS, COMPLETED

    @Column(name = "progress_percent")
    private Integer progressPercent; // NEW: 0-100 lưu % tiến độ hiện tại

    @Column(name = "is_rework")
    private Boolean isRework = false; // NEW: đánh dấu giai đoạn đang ở phiên sửa lỗi

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_stage_id")
    private ProductionStage originalStage; // NEW: nếu là rework clone từ stage trước (tùy chọn sử dụng)

    @Column(name = "defect_level", length = 20)
    private String defectLevel; // MINOR, MAJOR, CRITICAL

    @Column(columnDefinition = "text")
    private String defectDescription;
}
