package tmmsystem.dto.production;

import lombok.Getter; import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter
public class WorkOrderDto {
    private Long id;
    private Long productionOrderId;
    private String woNumber;
    private LocalDate deadline;
    private String status;
    private String sendStatus;
    private Boolean isProduction;
    private Long createdById;
    private Long approvedById;
    private Instant createdAt;
    private Instant updatedAt;
    private List<WorkOrderDetailDto> details;
}
