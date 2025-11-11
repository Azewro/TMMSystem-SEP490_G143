package tmmsystem.dto.sales;

import lombok.Getter; import lombok.Setter;

@Getter @Setter
public class AssignSalesRequest {
    // Either provide assignedSalesId or employeeCode
    private Long assignedSalesId;
    private String employeeCode;
}

