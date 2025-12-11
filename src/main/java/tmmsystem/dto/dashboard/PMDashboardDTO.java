package tmmsystem.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PMDashboardDTO {

    // === ALERTS (Needs Attention) ===
    private int overdueStages;
    private int qcFailedStages;
    private int pendingMaterialRequests;
    private int pendingQualityIssues;

    // === STAGE PROGRESS (by stage type) ===
    private Map<String, StageStatusCountDTO> stageProgress;

    // === MACHINE STATUS ===
    private int machinesInUse;
    private int machinesAvailable;
    private int machinesMaintenance;
    private int machinesNeedMaintenanceSoon; // next 7 days

    // === STAFF ALLOCATION ===
    private int activeLeaders;
    private int unassignedStages;

    // === QC TODAY ===
    private double qcPassRate;
    private int newIssues;
    private int minorIssues;
    private int majorIssues;
    private int reworkStages;

    // === TODAY SCHEDULE ===
    private List<ScheduleItemDTO> todaySchedule;

    // === PRODUCTION ORDERS SUMMARY ===
    private int totalActiveOrders;
    private int ordersInProgress;
    private int ordersWaitingMaterial;
    private int ordersCompleted;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StageStatusCountDTO {
        private int inProgress;
        private int waitingQC;
        private int completed;
        private int failed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleItemDTO {
        private Long stageId;
        private String stageType;
        private String stageTypeName;
        private String poNumber;
        private String productName;
        private Instant plannedStartAt;
        private String status;
        private String leaderName;
        private String machineName;
    }
}
