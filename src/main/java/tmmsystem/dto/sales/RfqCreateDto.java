package tmmsystem.dto.sales;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter
public class RfqCreateDto {
    @NotNull
    private Long customerId;

    private String rfqNumber;
    private String sourceType;
    private LocalDate expectedDeliveryDate;
    private String status;
    private Boolean isSent;
    private String notes;
    private Long createdById;
    private Long assignedSalesId;
    private Long assignedPlanningId;
    private Long approvedById;
    private Instant approvalDate;
    private List<RfqDetailDto> details;

    // Optional: employee code of sale staff creating (auto-assign if valid)
    private String employeeCode;
}
