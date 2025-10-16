// entity/Material.java
package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tmmsystem.common.BaseEntity;

import java.math.BigDecimal;

@Entity
@Table(name = "material")
@Getter
@Setter
public class Material extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code; // YARN-20S-WHT, DYE-BLUE-001

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 20)
    private String type; // RAW_COTTON, YARN, DYE, CHEMICAL, PACKAGING

    @Column(length = 20)
    private String unit = "KG";

    @Column(name = "reorder_point", precision = 10, scale = 3)
    private BigDecimal reorderPoint; // Trigger reorder when stock below this

    @Column(name = "standard_cost", precision = 12, scale = 2)
    private BigDecimal standardCost; // Average cost per unit

    @Column(name = "is_active")
    private Boolean active = true;
}
