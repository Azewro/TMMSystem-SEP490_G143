// entity/ProductionOrderDetail.java
package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tmmsystem.common.BaseEntity;

import java.math.BigDecimal;

@Entity
@Table(name = "production_order_detail")
@Getter
@Setter
public class ProductionOrderDetail extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_order_id", nullable = false)
    private ProductionOrder productionOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Lock BOM version (HIGH FIX)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_id", nullable = false)
    private Bom bom; // Lock BOM version at production time

    @Column(name = "bom_version", length = 20)
    private String bomVersion; // Snapshot: v1.2 for audit

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(length = 20)
    private String unit = "UNIT";

    @Column(name = "note_color", length = 100)
    private String noteColor;
}
