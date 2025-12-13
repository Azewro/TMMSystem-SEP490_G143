package tmmsystem.dto.production;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class ProductionStageDto {
    private Long id;
    private Long productionOrderId; // NEW: Link trực tiếp với ProductionOrder
    private Long workOrderDetailId; // DEPRECATED: Giữ lại để backward compatibility, không dùng nữa
    private String stageType;
    private Integer stageSequence;
    private Long machineId;
    private Long assignedToId;
    private Long assignedLeaderId;
    private String batchNumber;
    private BigDecimal plannedOutput;
    private BigDecimal actualOutput;
    private Instant startAt;
    private Instant completeAt;
    private String status;
    private Boolean isOutsourced;
    private String outsourceVendor;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant plannedStartAt;
    private Instant plannedEndAt;
    private BigDecimal plannedDurationHours;
    private String qrToken;
    private String qcLastResult;
    private Instant qcLastCheckedAt;
    private Long qcAssigneeId;
    private String executionStatus;
    private Integer progressPercent;
    private Boolean isRework;

    // Enriched fields for frontend
    private String stageName; // Tên công đoạn để hiển thị (Cuồng mắc, Dệt, Nhuộm, ...)
    private String assigneeName; // Tên người phụ trách để hiển thị
    private String statusLabel; // Nhãn trạng thái để hiển thị
    private String stageCode; // Mã công đoạn (CUONG_MAC, DET, NHUOM, ...)

    // Additional fields for Leader progress tracking
    private java.math.BigDecimal totalHours; // Tổng thời gian đã làm (tính từ StageTracking)
    private String startTimeFormatted; // Thời gian bắt đầu định dạng (cho Leader)
    private String endTimeFormatted; // Thời gian kết thúc định dạng (cho Leader)

    // Defect Info for Rework
    private Long defectId;
    private String defectDescription;
    private String defectSeverity;
    private String defectLevel; // NEW: Field name matching Entity for consistency

    // NEW: Queue Status Display with Vietnamese stage name
    // Example: "Sẵn sàng sản xuất công đoạn cuồng mắc", "Chuẩn bị sản xuất công
    // đoạn dệt"
    private String statusDisplay;

    // NEW: Blocking status - for frontend to show "Chờ đến lượt" vs "Sẵn sàng"
    private Boolean isBlocked; // true if another lot is using the same stage type
    private String blockedBy; // lotCode of the blocking lot
}
