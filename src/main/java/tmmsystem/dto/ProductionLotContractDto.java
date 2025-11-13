package tmmsystem.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO thể hiện 1 hợp đồng đã được merge vào Lot.
 * Giúp FE hiển thị bảng danh sách ORD-xxx với số lượng phân bổ.
 */
@Getter @Setter
public class ProductionLotContractDto {
    private Long contractId;
    private String contractNumber;
    private BigDecimal allocatedQuantity; // tổng SL của contract đó đóng góp vào lot (cộng các quotation detail nếu có nhiều)
    private LocalDate contractDate;       // ngày ký
    private LocalDate deliveryDate;       // ngày giao mục tiêu của contract
}

