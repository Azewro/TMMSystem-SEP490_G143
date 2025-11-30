package tmmsystem.dto.execution;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MaterialRequisitionDetailDto {
    private Long id;
    private Long requisitionId;
    private Long materialId;
    private BigDecimal quantityRequested;
    private BigDecimal quantityApproved;
    private BigDecimal quantityIssued;
    private String unit;
    private String notes;
    private String materialName;
}
