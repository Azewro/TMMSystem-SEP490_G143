package tmmsystem.dto.execution;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class MaterialRequisitionApprovalDto {
    private Long directorId;
    private boolean force;
    private String notes;
    private List<MaterialRequisitionDetailDto> details;
}
