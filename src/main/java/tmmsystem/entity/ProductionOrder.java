// entity/ProductionOrder.java
package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tmmsystem.common.BaseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "production_order")
@Getter
@Setter
public class ProductionOrder extends BaseEntity {

    @Column(name = "po_number", nullable = false, unique = true, length = 50)
    private String poNumber; // PO-YYYYMMDD-XXX

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract; // Can be null for internal orders

    @Column(name = "total_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalQuantity;

    @Column(name = "planned_start_date")
    private LocalDate plannedStartDate;

    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;

    @Column(length = 30)
    private String status = "DRAFT"; // DRAFT, APPROVED, IN_PROGRESS, COMPLETED, CANCELED

    @Column
    private Integer priority = 0; // Higher number = higher priority

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Audit fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy; // Planning Department

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy; // Director approval

    @Column(name = "approved_at")
    private Instant approvedAt;

    // One-to-Many with ProductionOrderDetail
    @OneToMany(mappedBy = "productionOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductionOrderDetail> details = new ArrayList<>();

    // Helper method
    public void addDetail(ProductionOrderDetail detail) {
        details.add(detail);
        detail.setProductionOrder(this);
    }
}
