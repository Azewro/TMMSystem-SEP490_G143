package tmmsystem.dto.production;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ProductionOrderDto {
    private Long id;
    private String poNumber;
    private Long contractId;
    private List<Long> contractIds;
    private BigDecimal totalQuantity;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private String status;
    private String executionStatus; // NEW: Trạng thái thực thi chi tiết
    private Integer priority;
    private String notes;
    private Long createdById;
    private Long approvedById;
    private Instant approvedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ProductionOrderDetailDto> details;
    private Long assignedTechnicianId;
    private Instant assignedAt;

    // Enriched fields for frontend
    private String lotCode; // Mã lô từ ProductionLot
    private String productName; // Tên sản phẩm từ ProductionOrderDetail
    private String size; // Kích thước từ Product.standardDimensions hoặc ProductionLot.sizeSnapshot
    private String statusLabel; // Nhãn trạng thái để hiển thị
    private List<ProductionStageDto> stages; // Danh sách các công đoạn

    // Aliases for frontend compatibility
    private LocalDate expectedStartDate; // Alias cho plannedStartDate
    private LocalDate expectedFinishDate; // Alias cho plannedEndDate
    private LocalDate expectedDeliveryDate; // Alias cho plannedStartDate (dùng cho Leader)

    private String qrToken; // Token QR của lô (lấy từ stage đầu tiên)

    private Long pendingMaterialRequestId; // NEW: ID của yêu cầu cấp sợi đang chờ duyệt (nếu có)
}
