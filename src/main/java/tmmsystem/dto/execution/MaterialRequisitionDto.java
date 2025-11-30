package tmmsystem.dto.execution;

import lombok.Data;
import java.time.Instant;
import java.math.BigDecimal;

@Data
public class MaterialRequisitionDto {
    private Long id;
    private String requisitionNumber;
    private Long productionStageId;
    private String stageType; // Useful for display
    private String requestedByName;
    private Long requestedById;
    private String approvedByName;
    private Long approvedById;
    private String status;
    private Instant requestedAt;
    private Instant approvedAt;
    private Instant issuedAt;
    private String notes;
    private BigDecimal quantityRequested;
    private BigDecimal quantityApproved;
    private String requisitionType;
}
