package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "production_lot_order")
@Getter @Setter
public class ProductionLotOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private ProductionLot lot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_detail_id")
    private QuotationDetail quotationDetail; // snapshot line

    @Column(name = "allocated_quantity", precision = 12, scale = 2)
    private BigDecimal allocatedQuantity;
}

