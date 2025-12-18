package tmmsystem.dto.product;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class ProductDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Long categoryId;
    private String categoryName; // NEW: for frontend filtering
    private String unit;
    private BigDecimal standardWeight;
    private String standardDimensions;
    private BigDecimal basePrice;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
