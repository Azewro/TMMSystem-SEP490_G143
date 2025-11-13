package tmmsystem.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ProductionLotDto {
    private Long id;
    private String lotCode;
    private Long productId;
    private String productCode;
    private String productName;
    private String sizeSnapshot;
    private BigDecimal totalQuantity;
    private LocalDate deliveryDateTarget;
    private LocalDate contractDateMin;
    private LocalDate contractDateMax;
    private String status;
    private List<String> orderNumbers; // e.g., ORD-001, ORD-002
    private Long currentPlanId; // current plan version id if any
    private String currentPlanStatus;
    // NEW: danh sách chi tiết các hợp đồng đã merge vào lot (để FE hiển thị badges ORD-xxx)
    private List<ProductionLotContractDto> mergedContracts;
    // NEW: tổng số lượng hợp đồng (số phần tử mergedContracts) để FE hiển thị nhanh, tránh size() null check
    private Integer totalContractsCount;
}
