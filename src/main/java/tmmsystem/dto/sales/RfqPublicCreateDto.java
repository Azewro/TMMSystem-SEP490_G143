package tmmsystem.dto.sales;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import tmmsystem.validation.ExpectedDeliveryDate;
import tmmsystem.validation.VietnamesePhoneNumber;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class RfqPublicCreateDto {
    private String rfqNumber;

    @NotBlank(message = "Họ và tên là bắt buộc.")
    private String contactPerson;

    @NotBlank(message = "Email là bắt buộc.")
    @Email(message = "Email không hợp lệ.")
    private String contactEmail;

    @NotBlank(message = "Số điện thoại là bắt buộc.")
    @VietnamesePhoneNumber
    private String contactPhone;

    private String contactAddress;

    private String contactMethod; // EMAIL | PHONE (optional -> infer)

    private String sourceType;
    
    @NotNull(message = "Ngày giao hàng mong muốn là bắt buộc.")
    @ExpectedDeliveryDate
    private LocalDate expectedDeliveryDate;
    private String status;
    private Boolean isSent;
    private String notes;
    private Instant approvalDate;
    
    @NotNull(message = "Danh sách sản phẩm là bắt buộc")
    @NotEmpty(message = "RFQ phải có ít nhất một sản phẩm.")
    @Valid
    private List<RfqDetailDto> details;

    // Optional: if a sale staff fills employeeCode, auto-assign if valid
    private String employeeCode;
}
