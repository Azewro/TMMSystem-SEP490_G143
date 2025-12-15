package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "rfq_detail")
@Getter
@Setter
public class RfqDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_id")
    private Rfq rfq;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal quantity;

    @Column(length = 20)
    private String unit = "UNIT";

    @Column(name = "note_color", length = 100)
    private String noteColor;

    @Column(columnDefinition = "text")
    private String notes;
}
