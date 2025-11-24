package tmmsystem.dto.production_plan;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateProductionPlanRequest {
    private String planCode;
    private String notes;
    private LocalDate proposedStartDate;
    private LocalDate proposedEndDate;
}
