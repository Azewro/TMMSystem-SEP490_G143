package tmmsystem.dto.sales;

import jakarta.validation.constraints.DecimalMin;
import lombok.Getter; import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class RfqDetailDto {
    private Long id;
    private Long productId;

    @DecimalMin(value = "100", message = "Số lượng sản phẩm yêu cầu tối thiểu là 100")
    private BigDecimal quantity;
    
    private String unit;
    private String noteColor;
    private String notes;
}


