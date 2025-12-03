package tmmsystem.dto.sales;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class RfqPublicCreateDto {
    private String rfqNumber;

    @NotBlank(message = "contactPerson is required")
    private String contactPerson;

    @Email(message = "contactEmail must be a valid email")
    private String contactEmail;

    @Pattern(regexp = "^$|^[0-9+\\-() ]{6,20}$", message = "contactPhone must be a valid phone number")
    private String contactPhone;

    private String contactAddress;

    private String contactMethod; // EMAIL | PHONE (optional -> infer)

    private String sourceType;
    private LocalDate expectedDeliveryDate;
    private String status;
    private Boolean isSent;
    private String notes;
    private Instant approvalDate;
    private List<RfqDetailDto> details;

    // Optional: if a sale staff fills employeeCode, auto-assign if valid
    private String employeeCode;
}
