// entity/BomDetail.java
package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tmmsystem.common.BaseEntity;

import java.math.BigDecimal;

@Entity
@Table(name = "bom_detail")
@Getter
@Setter
public class BomDetail extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_id", nullable = false)
    private Bom bom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity; // Amount per 1 unit of product

    @Column(length = 20)
    private String unit = "KG";

    @Column(length = 20)
    private String stage; // Which stage uses this material: WEAVING, DYEING, etc.

    @Column(name = "is_optional")
    private Boolean optional = false;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
