package tmmsystem.dto.production_plan;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ProductionPlanDetailDto {
    private Long id;
    private Long planId;
    private String lotCode;
    private Long productId;
    private String productCode;
    private String productName;
    private String productDescription;
    private String sizeSnapshot;
    private BigDecimal plannedQuantity;
    private LocalDate requiredDeliveryDate;
    private LocalDate proposedStartDate;
    private LocalDate proposedEndDate;
    private Long workCenterId;
    private String workCenterName;
    private String workCenterCode;
    private BigDecimal expectedCapacityPerDay;
    private Integer leadTimeDays;
    private String notes;
    private List<ProductionPlanStageDto> stages;

    // Calculated fields
    private Integer totalStages;
    private BigDecimal totalDurationDays;
}
