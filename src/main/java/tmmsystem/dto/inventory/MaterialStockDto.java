package tmmsystem.dto.inventory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter; import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter @Setter
public class MaterialStockDto {
    private Long id;
    
    @NotNull(message = "Vui lòng chọn nguyên liệu")
    private Long materialId;
    
    private String materialCode; // Material code for display
    private String materialName; // Material name for display
    
    @NotNull(message = "Số lượng là bắt buộc")
    @DecimalMin(value = "0.0001", inclusive = false, message = "Vui lòng nhập số lượng hợp lệ")
    private BigDecimal quantity;
    
    private String unit;
    
    @NotNull(message = "Đơn giá là bắt buộc")
    @DecimalMin(value = "0.0001", inclusive = false, message = "Vui lòng nhập đơn giá hợp lệ")
    private BigDecimal unitPrice; // Giá nhập của batch này
    
    private String location;
    private String batchNumber;
    
    @NotNull(message = "Vui lòng chọn ngày nhập hàng")
    private LocalDate receivedDate;
    
    private LocalDate expiryDate;
    private Instant lastUpdatedAt;
}


