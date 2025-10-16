// entity/WorkOrderDetail.java
package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tmmsystem.common.BaseEntity;

import java.time.Instant;

@Entity
@Table(name = "work_order_detail")
@Getter
@Setter
public class WorkOrderDetail extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_order_detail_id", nullable = false)
    private ProductionOrderDetail productionOrderDetail;

    @Column(name = "stage_sequence")
    private Integer stageSequence; // Processing order: 1=WARPING, 2=WEAVING, etc.

    @Column(name = "planned_start_at")
    private Instant plannedStartAt;

    @Column(name = "planned_end_at")
    private Instant plannedEndAt;

    @Column(name = "start_at")
    private Instant startAt;

    @Column(name = "complete_at")
    private Instant completeAt;

    @Column(name = "work_status", length = 20)
    private String workStatus = "PENDING"; // PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELED

    @Column(columnDefinition = "TEXT")
    private String notes;
}
