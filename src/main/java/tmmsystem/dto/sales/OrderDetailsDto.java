package tmmsystem.dto.sales;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class OrderDetailsDto {
    // Contract info
    private Long contractId;
    private String contractNumber;
    private String status;
    private LocalDate contractDate;
    private LocalDate deliveryDate;
    private BigDecimal totalAmount;
    private String filePath;

    // Customer info
    private CustomerInfo customerInfo;

    // Order items (from quotation details)
    private List<OrderItemDto> orderItems;

    @Data
    @Builder
    public static class CustomerInfo {
        private Long customerId;
        private String customerName;
        private String phoneNumber;
        private String email;
        private String companyName;
        private String taxCode;
        private String address;
    }

    @Data
    @Builder
    public static class OrderItemDto {
        private Long productId;
        private String productName;
        private String productSize;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private String noteColor;
    }
}
