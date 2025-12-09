package tmmsystem.dto.sales;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class CapacityCheckResultDto {
    private MachineCapacityDto machineCapacity;
    private WarehouseCapacityDto warehouseCapacity;

    @Getter
    @Setter
    public static class MachineCapacityDto {
        private boolean sufficient;
        private String bottleneck;
        private BigDecimal requiredDays;
        private BigDecimal availableDays;
        private LocalDate productionStartDate;
        private LocalDate productionEndDate;
        private List<DailyCapacityDto> dailyCapacities;
        private List<ConflictDto> conflicts;

        // Thông tin chi tiết các công đoạn tuần tự
        private SequentialStageDto warpingStage;
        private SequentialStageDto weavingStage;
        private SequentialStageDto dyeingStage;
        private SequentialStageDto cuttingStage;
        private SequentialStageDto sewingStage;
        private SequentialStageDto packagingStage;
        private BigDecimal totalWaitTime;

        // Status and suggestion
        private String status; // SUFFICIENT | INSUFFICIENT
        private String mergeSuggestion;

        // Chi tiết tính toán năng lực
        private BigDecimal newOrderWeightKg;
        private BigDecimal backlogWeightKg;
        private BigDecimal totalLoadKg;
        private BigDecimal maxCapacityKg;
        private BigDecimal bottleneckCapacityPerDay;
        private List<BacklogOrderDto> backlogOrders;

        // Bảng công suất các công đoạn (để giải thích bottleneck)
        private List<StageCapacityDto> stageCapacities;
    }

    @Getter
    @Setter
    public static class StageCapacityDto {
        private String stageName; // Tên công đoạn (VD: Mắc cuồng)
        private String stageType; // Loại (WARPING, WEAVING, ...)
        private int machineCount; // Số máy
        private BigDecimal capacityPerMachine; // Năng suất mỗi máy
        private BigDecimal totalCapacityPerDay; // Tổng năng suất/ngày
        private String unit; // Đơn vị (kg, sản phẩm)
        private boolean isBottleneck; // Có phải bottleneck không
    }

    @Getter
    @Setter
    public static class BacklogOrderDto {
        private String quotationCode; // Mã báo giá
        private String customerName; // Tên khách hàng
        private LocalDate deliveryDate; // Ngày giao dự kiến
        private BigDecimal weightKg; // Khối lượng (kg)
        private String status; // Trạng thái (SENT/ACCEPTED/ORDER_CREATED)
    }

    @Getter
    @Setter
    public static class WarehouseCapacityDto {
        private boolean sufficient;
        private String message;
    }

    @Getter
    @Setter
    public static class DailyCapacityDto {
        private LocalDate date;
        private BigDecimal warpingRequired;
        private BigDecimal warpingAvailable;
        private BigDecimal weavingRequired;
        private BigDecimal weavingAvailable;
        private BigDecimal sewingRequired;
        private BigDecimal sewingAvailable;
    }

    @Getter
    @Setter
    public static class ConflictDto {
        private LocalDate date;
        private String machineType;
        private BigDecimal required;
        private BigDecimal available;
        private BigDecimal used;
        private String conflictMessage;
    }

    @Getter
    @Setter
    public static class SequentialStageDto {
        private String stageName;
        private String stageType;
        private BigDecimal processingDays;
        private BigDecimal waitTime;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal capacity;
        private String description;
    }
}
