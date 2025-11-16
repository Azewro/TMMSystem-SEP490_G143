package tmmsystem.dto.inventory;

import lombok.Getter; import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter @Setter
public class MaterialStockDto {
    private Long id;
    private Long materialId;
    private String materialCode; // Material code for display
    private String materialName; // Material name for display
    private BigDecimal quantity;
    private String unit;
    private BigDecimal unitPrice; // Giá nhập của batch này
    private String location;
    private String batchNumber;
    private LocalDate receivedDate;
    private LocalDate expiryDate;
    private Instant lastUpdatedAt;
}


