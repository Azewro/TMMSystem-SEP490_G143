package tmmsystem.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectorDashboardDTO {

    // === PENDING APPROVALS ===
    private int pendingProductionPlans;
    private int pendingContracts;
    private int pendingQuotations;

    // === BUSINESS OVERVIEW ===
    private int activeContracts;
    private BigDecimal expectedRevenue;
    private int contractsNearDelivery; // delivery within 7 days
    private int activeProductionOrders;

    // === PRODUCTION OVERVIEW ===
    private double efficiencyRate; // % on-time completion
    private double defectRate; // % stages with QC fail
    private double onTimeDeliveryRate; // % orders delivered on time

    // === CHART DATA ===
    private List<ContractByMonthDTO> contractsByMonth;
    private List<StatusCountDTO> productionOrdersByStatus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractByMonthDTO {
        private String month; // e.g., "T10", "T11"
        private int count;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusCountDTO {
        private String status;
        private String label;
        private int count;
    }
}
