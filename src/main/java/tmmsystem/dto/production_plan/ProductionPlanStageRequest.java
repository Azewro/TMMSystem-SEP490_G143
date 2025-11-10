package tmmsystem.dto.production_plan;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ProductionPlanStageRequest {
    private String stageType;
    private Integer sequenceNo;
    private Long assignedMachineId;
    private Long inChargeUserId;
    private Long qcUserId;
    private LocalDateTime plannedStartTime;
    private LocalDateTime plannedEndTime;
    private Integer minRequiredDurationMinutes;
    private BigDecimal transferBatchQuantity;
    private BigDecimal capacityPerHour;
    private String notes;
}
