package tmmsystem.dto.production;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ProductionOrderDto {
    private Long id;
    private String poNumber;
    private Long contractId;
    private List<Long> contractIds;
    private BigDecimal totalQuantity;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private String status;
    private Integer priority;
    private String notes;
    private Long createdById;
    private Long approvedById;
    private Instant approvedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ProductionOrderDetailDto> details;
    private Long assignedTechnicianId;
    private Instant assignedAt;
}
