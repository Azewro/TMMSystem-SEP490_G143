package tmmsystem.dto.sales;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter; import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter @Setter
public class SalesRfqCreateRequest {
    @NotBlank
    private String contactPerson;
    @Email
    private String contactEmail;
    @Pattern(regexp = "^$|^[0-9+\\-() ]{6,20}$")
    private String contactPhone;
    private String contactAddress;

    private LocalDate expectedDeliveryDate;
    private String notes;
    private List<RfqDetailDto> details;
}
