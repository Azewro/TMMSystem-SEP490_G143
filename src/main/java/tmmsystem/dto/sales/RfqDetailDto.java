package tmmsystem.dto.sales;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter; import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class RfqDetailDto {
    private Long id;
    
    @NotNull(message = "Vui lòng chọn sản phẩm.")
    private Long productId;

    @NotNull(message = "Số lượng là bắt buộc")
    @DecimalMin(value = "100", message = "Số lượng tối thiểu là 100.")
    private BigDecimal quantity;
    
    @NotBlank(message = "Đơn vị là bắt buộc")
    private String unit;
    private String noteColor;
    private String notes;
}


