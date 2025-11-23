package tmmsystem.dto.production_plan;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ProductionPlanDto {
    private Long id;
    private Long contractId;
    private String contractNumber;
    private String planCode;
    private String status;
    private Long createdById;
    private String createdByName;
    private Long approvedById;
    private String approvedByName;
    private Instant approvedAt;
    private String approvalNotes;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ProductionPlanDetailDto> details;
    private LocalDate proposedStartDate;
    private LocalDate proposedEndDate;
    private tmmsystem.dto.ProductionLotDto lot;

    // Contract information
    private String customerName;
    private String customerCode;
    private Instant contractCreatedAt;
    private Instant contractApprovedAt;
}
