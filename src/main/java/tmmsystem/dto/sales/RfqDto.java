package tmmsystem.dto.sales;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class RfqDto {
    private Long id;
    private String rfqNumber;
    private Long customerId;
    private String sourceType; // CUSTOMER_PORTAL | PUBLIC_FORM | BY_SALES
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate expectedDeliveryDate;
    private String status;
    private Boolean isSent;
    private String notes;
    private Long createdById;
    private Long assignedSalesId;
    private String assignedSalesName; // Name of sales person for display
    private String employeeCode; // Added to display Sales Staff Code on frontend
    private Long assignedPlanningId;
    private Long approvedById;
    private Instant approvalDate;
    private Instant createdAt;
    private Instant updatedAt;
    private List<RfqDetailDto> details;

    // Thông tin dành cho Public Form khi khách không đăng nhập (from snapshot or
    // fallback customer)
    private String contactPerson;
    private String contactEmail;
    private String contactPhone;
    private String contactAddress;
    private String contactMethod; // EMAIL | PHONE

    // Capacity check results
    private String capacityStatus;
    private String capacityReason;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate proposedNewDeliveryDate;
}
