package tmmsystem.dto.sales;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import tmmsystem.validation.ExpectedDeliveryDate;
import tmmsystem.validation.ValidName;
import tmmsystem.validation.VietnamesePhoneNumber;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter
public class RfqCreateDto {
    @NotNull
    private Long customerId;

    private String rfqNumber;
    private String sourceType;
    
    @NotNull(message = "Ngày giao hàng mong muốn là bắt buộc.")
    @ExpectedDeliveryDate
    private LocalDate expectedDeliveryDate;
    private String status;
    private Boolean isSent;
    private String notes;
    private Long createdById;
    private Long assignedSalesId;
    private Long assignedPlanningId;
    private Long approvedById;
    private Instant approvalDate;
    
    @NotNull(message = "Danh sách sản phẩm là bắt buộc")
    @NotEmpty(message = "RFQ phải có ít nhất một sản phẩm.")
    @Valid
    private List<RfqDetailDto> details;

    // Optional: employee code of sale staff creating (auto-assign if valid)
    private String employeeCode;

    // Contact fields for this specific RFQ, overriding customer's default info
    // Optional override fields - if provided, must be valid
    @ValidName
    private String contactPerson;
    
    @Email(message = "Email không hợp lệ.")
    private String contactEmail;
    
    @VietnamesePhoneNumber
    private String contactPhone;
    
    private String contactAddress;
    private String contactMethod;
}
