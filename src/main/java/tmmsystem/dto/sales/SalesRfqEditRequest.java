package tmmsystem.dto.sales;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Getter; import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter @Setter
public class SalesRfqEditRequest {
    private String contactPerson;
    @Email
    private String contactEmail;
    @Pattern(regexp = "^$|^[0-9+\\-() ]{6,20}$")
    private String contactPhone;
    private String contactAddress;

    private String contactMethod; // EMAIL | PHONE (optional -> infer)

    private LocalDate expectedDeliveryDate; // optional new target date
    private String notes; // rfq notes

    // optional replace detail list (simple approach: remove existing & recreate)
    private List<RfqDetailDto> details;
}
