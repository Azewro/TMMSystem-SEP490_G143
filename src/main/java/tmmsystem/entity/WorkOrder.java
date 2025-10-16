// entity/WorkOrder.java
package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tmmsystem.common.BaseEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "work_order")
@Getter
@Setter
public class WorkOrder extends BaseEntity {

    @Column(name = "wo_number", nullable = false, unique = true, length = 50)
    private String woNumber; // WO-YYYYMMDD-XXX

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_order_id", nullable = false)
    private ProductionOrder productionOrder;

    @Column
    private LocalDate deadline;

    @Column(length = 30)
    private String status = "DRAFT"; // DRAFT, APPROVED, IN_PROGRESS, COMPLETED, CANCELED

    @Column(name = "send_status", length = 20)
    private String sendStatus = "NOT_SENT"; // NOT_SENT, SENT_TO_FLOOR

    @Column(name = "is_production")
    private Boolean production = true; // false for maintenance WOs

    // Audit fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    // One-to-Many with WorkOrderDetail
    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkOrderDetail> details = new ArrayList<>();

    // Helper method
    public void addDetail(WorkOrderDetail detail) {
        details.add(detail);
        detail.setWorkOrder(this);
    }
}
