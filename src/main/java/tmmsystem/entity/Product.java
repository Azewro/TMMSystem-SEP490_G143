// entity/Product.java
package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tmmsystem.common.BaseEntity;

import java.math.BigDecimal;

@Entity
@Table(name = "product")
@Getter
@Setter
public class Product extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code; // TWL-30x50-WHT

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ProductCategory category;

    @Column(length = 20)
    private String unit = "UNIT";

    @Column(name = "standard_weight", precision = 10, scale = 3)
    private BigDecimal standardWeight; // kg per unit

    @Column(name = "standard_dimensions", length = 100)
    private String standardDimensions; // 30cm x 50cm

    @Column(name = "base_price", precision = 12, scale = 2)
    private BigDecimal basePrice; // Base selling price

    @Column(name = "is_active")
    private Boolean active = true;
}
