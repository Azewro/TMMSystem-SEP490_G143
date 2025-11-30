package tmmsystem.dto.technical;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import tmmsystem.dto.execution.MaterialRequisitionDetailDto;

@Data
public class TechnicalDefectActionDto {
    private Long stageId;
    private String decision; // REWORK, MATERIAL_REQUEST, ACCEPT
    private String notes;
    private Long technicalUserId;
    private BigDecimal quantity;
    private List<MaterialRequisitionDetailDto> details;
}
