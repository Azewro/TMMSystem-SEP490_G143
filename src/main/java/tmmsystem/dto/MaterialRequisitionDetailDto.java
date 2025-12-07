package tmmsystem.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MaterialRequisitionDetailDto {
    private Long id;
    private String materialName; // Added for display
    private String materialCode; // Added for display
    private BigDecimal quantityRequested;
    private BigDecimal quantityApproved;
    private String unit;
    private String notes;
}
