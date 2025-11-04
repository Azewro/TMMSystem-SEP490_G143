package tmmsystem.dto.sales;

import lombok.Getter; import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter
public class RfqDto {
    private Long id;
    private String rfqNumber;
    private Long customerId;
    private String sourceType; // CUSTOMER_PORTAL | PUBLIC_FORM | BY_SALES
    private LocalDate expectedDeliveryDate;
    private String status;
    private Boolean isSent;
    private String notes;
    private Long createdById;
    private Long assignedSalesId;
    private Long assignedPlanningId;
    private Long approvedById;
    private Instant approvalDate;
    private Instant createdAt;
    private Instant updatedAt;
    private List<RfqDetailDto> details;
}
