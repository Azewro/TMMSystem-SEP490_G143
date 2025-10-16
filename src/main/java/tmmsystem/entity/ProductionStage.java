// entity/ProductionStage.java
package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tmmsystem.common.BaseEntity;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "production_stage", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"work_order_detail_id", "stage_sequence"})
})
@Getter
@Setter
public class ProductionStage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_detail_id", nullable = false)
    private WorkOrderDetail workOrderDetail;

    // FIXED: Stage types match actual workflow (CRITICAL)
    @Column(name = "stage_type", nullable = false, length = 20)
    private String stageType; // WARPING, WEAVING, DYEING, CUTTING, HEMMING, PACKAGING

    @Column(name = "stage_sequence", nullable = false)
    private Integer stageSequence; // 1, 2, 3, 4, 5, 6

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id")
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo; // Production worker

    // NEW: Leader assignment (MEDIUM FIX)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_leader_id")
    private User assignedLeader; // Warping leader, Weaving leader, etc.

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

    // Outsourcing (for DYEING stage typically)
    @Column(name = "is_outsourced")
    private Boolean outsourced = false;

    @Column(name = "outsource_vendor", length = 255)
    private String outsourceVendor;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
