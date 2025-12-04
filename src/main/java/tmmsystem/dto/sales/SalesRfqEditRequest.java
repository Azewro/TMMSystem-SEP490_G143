package tmmsystem.dto.sales;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter; import lombok.Setter;
import tmmsystem.validation.VietnamesePhoneNumber;

import java.time.LocalDate;
import java.util.List;

@Getter @Setter
public class SalesRfqEditRequest {
    private String contactPerson;
    
    @Email(message = "Email không hợp lệ. Vui lòng nhập đúng định dạng email.")
    private String contactEmail;
    
    @VietnamesePhoneNumber(message = "Số điện thoại không hợp lệ. Vui lòng kiểm tra lại.")
    private String contactPhone;
    private String contactAddress;

    private String contactMethod; // EMAIL | PHONE (optional -> infer)

    private LocalDate expectedDeliveryDate; // optional new target date
    private String notes; // rfq notes

    // optional replace detail list (simple approach: remove existing & recreate)
    // If provided, must not be empty and all items must have quantity >= 100
    @NotEmpty(message = "RFQ phải có ít nhất một sản phẩm.")
    @Valid
    private List<RfqDetailDto> details;
}
