package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "production_lot", indexes = {
        @Index(name = "idx_lot_code", columnList = "lot_code", unique = true),
        @Index(name = "idx_lot_product", columnList = "product_id"),
        @Index(name = "idx_lot_delivery", columnList = "delivery_date_target")
})
@Getter @Setter
public class ProductionLot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "lot_code", length = 50, nullable = false)
    private String lotCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "size_snapshot", length = 200)
    private String sizeSnapshot;

    @Column(name = "total_quantity", precision = 12, scale = 2)
    private BigDecimal totalQuantity;

    @Column(name = "delivery_date_target")
    private LocalDate deliveryDateTarget;

    @Column(name = "contract_date_min")
    private LocalDate contractDateMin;

    @Column(name = "contract_date_max")
    private LocalDate contractDateMax;

    @Column(name = "status", length = 30)
    private String status = "FORMING"; // FORMING, READY_FOR_PLANNING, PLANNING, PLAN_APPROVED, IN_PRODUCTION, COMPLETED, CANCELED

    @Column(name = "material_requirements_json", columnDefinition = "json")
    private String materialRequirementsJson;

    @OneToMany(mappedBy = "lot", fetch = FetchType.LAZY)
    private List<ProductionLotOrder> lotOrders;
}

