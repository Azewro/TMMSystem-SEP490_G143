// entity/RfqDetail.java
package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tmmsystem.common.BaseEntity;

import java.math.BigDecimal;

@Entity
@Table(name = "rfq_detail")
@Getter
@Setter
public class RfqDetail extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id", nullable = false)
    private Rfq rfq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(length = 20)
    private String unit = "UNIT";

    @Column(name = "note_color", length = 100)
    private String noteColor; // Requested color: White, Blue, Custom

    @Column(columnDefinition = "TEXT")
    private String notes;
}
