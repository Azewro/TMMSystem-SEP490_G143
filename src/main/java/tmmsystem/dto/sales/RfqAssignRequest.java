package tmmsystem.dto.sales;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "RfqAssignRequest", description = "Request gán Sales cho RFQ ở trạng thái DRAFT")
public class RfqAssignRequest {
    @NotNull
    @Schema(description = "ID Sales chính phụ trách RFQ", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long assignedSalesId;

    @Schema(description = "ID Giám đốc phê duyệt (tuỳ chọn)", example = "1")
    private Long approvedById;

    public Long getAssignedSalesId() { return assignedSalesId; }
    public void setAssignedSalesId(Long assignedSalesId) { this.assignedSalesId = assignedSalesId; }

    public Long getApprovedById() { return approvedById; }
    public void setApprovedById(Long approvedById) { this.approvedById = approvedById; }
}

