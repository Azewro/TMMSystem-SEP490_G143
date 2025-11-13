package tmmsystem.dto.sales;

import lombok.Getter; import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter @Setter
public class ContractDto {
    private Long id;
    private String contractNumber;
    private Long quotationId;
    private Long customerId;
    private LocalDate contractDate;
    private LocalDate deliveryDate;
    private BigDecimal totalAmount;
    private String filePath;
    private String status;
    private Long directorApprovedById;
    private Instant directorApprovedAt;
    private String directorApprovalNotes;
    private Long createdById;
    private Long approvedById;
    // NEW: assignments
    private Long assignedSalesId;
    private Long assignedPlanningId;
    private Instant createdAt;
    private Instant updatedAt;
    // NEW: approvals like RFQ/Quotation
    private Long salesApprovedById;
    private Instant salesApprovedAt;
    private Long planningApprovedById;
    private Instant planningApprovedAt;
}
