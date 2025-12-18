package tmmsystem.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.dto.dashboard.DirectorDashboardDTO;
import tmmsystem.dto.dashboard.PMDashboardDTO;
import tmmsystem.entity.*;
import tmmsystem.repository.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

        private final ProductionPlanRepository productionPlanRepository;
        private final ContractRepository contractRepository;
        private final QuotationRepository quotationRepository;
        private final ProductionOrderRepository productionOrderRepository;
        private final ProductionStageRepository productionStageRepository;
        private final MachineRepository machineRepository;
        private final QualityIssueRepository qualityIssueRepository;
        private final MaterialRequisitionRepository materialRequisitionRepository;

        // Stage types constant
        private static final List<String> STAGE_TYPES = Arrays.asList(
                        "WARPING", "WEAVING", "DYEING", "CUTTING", "HEMMING", "PACKAGING");

        private static final Map<String, String> STAGE_TYPE_NAMES = Map.of(
                        "WARPING", "Cuồng mắc",
                        "WEAVING", "Dệt",
                        "DYEING", "Nhuộm",
                        "CUTTING", "Cắt",
                        "HEMMING", "May",
                        "PACKAGING", "Đóng gói");

        // Active statuses for stages
        private static final List<String> ACTIVE_STATUSES = Arrays.asList(
                        "WAITING", "IN_PROGRESS", "WAITING_QC", "QC_IN_PROGRESS");

        /**
         * Get dashboard data for Director role
         */
        public DirectorDashboardDTO getDirectorDashboard() {
                log.info("Fetching Director dashboard data");

                // 1. Pending Approvals
                int pendingPlans = productionPlanRepository.findByStatus(ProductionPlan.PlanStatus.PENDING_APPROVAL)
                                .size();
                int pendingContracts = contractRepository.findByStatus("PENDING_APPROVAL").size();
                int pendingQuotations = quotationRepository.findByStatus("PENDING_APPROVAL").size();

                // 2. Business Overview
                List<Contract> activeContracts = contractRepository.findByStatus("APPROVED");
                BigDecimal expectedRevenue = activeContracts.stream()
                                .map(Contract::getTotalAmount)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Contracts near delivery (within 7 days)
                LocalDate now = LocalDate.now();
                LocalDate weekLater = now.plusDays(7);
                int contractsNearDelivery = (int) activeContracts.stream()
                                .filter(c -> c.getDeliveryDate() != null)
                                .filter(c -> !c.getDeliveryDate().isBefore(now)
                                                && !c.getDeliveryDate().isAfter(weekLater))
                                .count();

                // Active production orders
                List<ProductionOrder> allOrders = productionOrderRepository.findAll();
                int activeProductionOrders = (int) allOrders.stream()
                                .filter(o -> !"COMPLETED".equals(o.getExecutionStatus())
                                                && !"CANCELLED".equals(o.getStatus()))
                                .count();

                // 3. Production Overview
                List<ProductionStage> allStages = productionStageRepository.findAll();
                long completedStages = allStages.stream()
                                .filter(s -> "COMPLETED".equals(s.getExecutionStatus()))
                                .count();
                long totalStages = allStages.size();
                double efficiencyRate = totalStages > 0 ? (completedStages * 100.0 / totalStages) : 0;

                long failedStages = allStages.stream()
                                .filter(s -> "QC_FAILED".equals(s.getExecutionStatus())
                                                || "FAIL".equals(s.getQcLastResult()))
                                .count();
                double defectRate = totalStages > 0 ? (failedStages * 100.0 / totalStages) : 0;

                // On-time delivery rate (simplified: orders completed before planned end)
                long completedOrders = allOrders.stream()
                                .filter(o -> "COMPLETED".equals(o.getExecutionStatus()))
                                .count();
                long onTimeOrders = allOrders.stream()
                                .filter(o -> "COMPLETED".equals(o.getExecutionStatus()))
                                .filter(o -> o.getPlannedEndDate() != null)
                                .count(); // Simplified - assume all completed are on-time for now
                double onTimeDeliveryRate = completedOrders > 0 ? (onTimeOrders * 100.0 / completedOrders) : 100;

                // 4. Chart Data - Contracts by month (last 6 months)
                List<DirectorDashboardDTO.ContractByMonthDTO> contractsByMonth = getContractsByMonth();

                // 5. Production Orders by Status
                List<DirectorDashboardDTO.StatusCountDTO> ordersByStatus = getProductionOrdersByStatus(allOrders);

                return DirectorDashboardDTO.builder()
                                .pendingProductionPlans(pendingPlans)
                                .pendingContracts(pendingContracts)
                                .pendingQuotations(pendingQuotations)
                                .activeContracts(activeContracts.size())
                                .expectedRevenue(expectedRevenue)
                                .contractsNearDelivery(contractsNearDelivery)
                                .activeProductionOrders(activeProductionOrders)
                                .efficiencyRate(Math.round(efficiencyRate * 10.0) / 10.0)
                                .defectRate(Math.round(defectRate * 10.0) / 10.0)
                                .onTimeDeliveryRate(Math.round(onTimeDeliveryRate * 10.0) / 10.0)
                                .contractsByMonth(contractsByMonth)
                                .productionOrdersByStatus(ordersByStatus)
                                .build();
        }

        /**
         * Get dashboard data for Production Manager role
         */
        public PMDashboardDTO getPMDashboard() {
                log.info("Fetching Production Manager dashboard data");

                Instant now = Instant.now();
                Instant todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
                Instant todayEnd = todayStart.plus(1, ChronoUnit.DAYS);

                // 1. Alerts
                List<ProductionStage> allStages = productionStageRepository.findAll();

                // Overdue stages (planned end < now and not completed)
                int overdueStages = (int) allStages.stream()
                                .filter(s -> s.getPlannedEndAt() != null)
                                .filter(s -> s.getPlannedEndAt().isBefore(now))
                                .filter(s -> !"COMPLETED".equals(s.getExecutionStatus()))
                                .count();

                // QC Failed stages
                int qcFailedStages = (int) allStages.stream()
                                .filter(s -> "QC_FAILED".equals(s.getExecutionStatus())
                                                || "FAIL".equals(s.getQcLastResult()))
                                .count();

                // Pending material requests
                int pendingMaterialRequests = materialRequisitionRepository.findByStatus("PENDING").size();

                // Pending quality issues
                int pendingQualityIssues = qualityIssueRepository.findByStatus("PENDING").size();

                // 2. Stage Progress by type
                Map<String, PMDashboardDTO.StageStatusCountDTO> stageProgress = new LinkedHashMap<>();
                for (String stageType : STAGE_TYPES) {
                        // Check both executionStatus and status fields (fallback)
                        long inProgress = allStages.stream()
                                        .filter(s -> stageType.equals(s.getStageType()))
                                        .filter(s -> "IN_PROGRESS".equals(s.getExecutionStatus())
                                                        || (s.getExecutionStatus() == null
                                                                        && "IN_PROGRESS".equals(s.getStatus())))
                                        .count();
                        long waitingQC = allStages.stream()
                                        .filter(s -> stageType.equals(s.getStageType()))
                                        .filter(s -> "WAITING_QC".equals(s.getExecutionStatus())
                                                        || "QC_IN_PROGRESS".equals(s.getExecutionStatus()))
                                        .count();
                        long completed = allStages.stream()
                                        .filter(s -> stageType.equals(s.getStageType()))
                                        .filter(s -> "COMPLETED".equals(s.getExecutionStatus())
                                                        || (s.getExecutionStatus() == null
                                                                        && "COMPLETED".equals(s.getStatus())))
                                        .count();
                        long failed = allStages.stream()
                                        .filter(s -> stageType.equals(s.getStageType()))
                                        .filter(s -> "QC_FAILED".equals(s.getExecutionStatus())
                                                        || (s.getExecutionStatus() == null
                                                                        && "FAILED".equals(s.getStatus())))
                                        .count();

                        stageProgress.put(stageType, PMDashboardDTO.StageStatusCountDTO.builder()
                                        .inProgress((int) inProgress)
                                        .waitingQC((int) waitingQC)
                                        .completed((int) completed)
                                        .failed((int) failed)
                                        .build());
                }

                // 3. Machine Status
                List<Machine> allMachines = machineRepository.findAll();
                int machinesInUse = (int) allMachines.stream()
                                .filter(m -> "IN_USE".equals(m.getStatus()))
                                .count();
                int machinesAvailable = (int) allMachines.stream()
                                .filter(m -> "AVAILABLE".equals(m.getStatus()))
                                .count();
                int machinesMaintenance = (int) allMachines.stream()
                                .filter(m -> "MAINTENANCE".equals(m.getStatus()) || "BROKEN".equals(m.getStatus()))
                                .count();
                // Machines needing maintenance soon (within 7 days)
                Instant weekLater = now.plus(7, ChronoUnit.DAYS);
                int machinesNeedMaintenance = (int) allMachines.stream()
                                .filter(m -> m.getNextMaintenanceAt() != null)
                                .filter(m -> m.getNextMaintenanceAt().isBefore(weekLater))
                                .count();

                // 4. Staff Allocation
                long activeLeaders = allStages.stream()
                                .filter(s -> s.getAssignedLeader() != null)
                                .filter(s -> ACTIVE_STATUSES.contains(s.getExecutionStatus()))
                                .map(s -> s.getAssignedLeader().getId())
                                .distinct()
                                .count();

                int unassignedStages = (int) allStages.stream()
                                .filter(s -> s.getAssignedLeader() == null)
                                .filter(s -> !"COMPLETED".equals(s.getExecutionStatus())
                                                && !"PENDING".equals(s.getExecutionStatus()))
                                .count();

                // 5. QC Today
                List<ProductionStage> todayStages = allStages.stream()
                                .filter(s -> s.getQcLastCheckedAt() != null)
                                .filter(s -> !s.getQcLastCheckedAt().isBefore(todayStart)
                                                && s.getQcLastCheckedAt().isBefore(todayEnd))
                                .collect(Collectors.toList());

                long qcPassToday = todayStages.stream()
                                .filter(s -> "PASS".equals(s.getQcLastResult()))
                                .count();
                double qcPassRate = todayStages.isEmpty() ? 100 : (qcPassToday * 100.0 / todayStages.size());

                List<QualityIssue> todayIssues = qualityIssueRepository.findAll().stream()
                                .filter(i -> i.getCreatedAt() != null)
                                .filter(i -> !i.getCreatedAt().isBefore(todayStart)
                                                && i.getCreatedAt().isBefore(todayEnd))
                                .collect(Collectors.toList());

                int newIssues = todayIssues.size();
                int minorIssues = (int) todayIssues.stream()
                                .filter(i -> "MINOR".equals(i.getSeverity()))
                                .count();
                int majorIssues = (int) todayIssues.stream()
                                .filter(i -> "MAJOR".equals(i.getSeverity()))
                                .count();

                int reworkStages = (int) allStages.stream()
                                .filter(s -> Boolean.TRUE.equals(s.getIsRework()))
                                .filter(s -> !"COMPLETED".equals(s.getExecutionStatus()))
                                .count();

                // 6. Today Schedule
                List<PMDashboardDTO.ScheduleItemDTO> todaySchedule = allStages.stream()
                                .filter(s -> s.getPlannedStartAt() != null)
                                .filter(s -> !s.getPlannedStartAt().isBefore(todayStart)
                                                && s.getPlannedStartAt().isBefore(todayEnd))
                                .sorted(Comparator.comparing(ProductionStage::getPlannedStartAt))
                                .limit(10)
                                .map(this::mapToScheduleItem)
                                .collect(Collectors.toList());

                // 7. Production Orders Summary
                List<ProductionOrder> allOrders = productionOrderRepository.findAll();
                int totalActiveOrders = (int) allOrders.stream()
                                .filter(o -> !"COMPLETED".equals(o.getExecutionStatus())
                                                && !"CANCELLED".equals(o.getStatus()))
                                .count();
                int ordersInProgress = (int) allOrders.stream()
                                .filter(o -> "IN_PROGRESS".equals(o.getExecutionStatus()))
                                .count();
                int ordersWaitingMaterial = (int) allOrders.stream()
                                .filter(o -> "WAITING_MATERIAL_APPROVAL".equals(o.getExecutionStatus()))
                                .count();
                int ordersCompleted = (int) allOrders.stream()
                                .filter(o -> "COMPLETED".equals(o.getExecutionStatus()))
                                .count();

                return PMDashboardDTO.builder()
                                .overdueStages(overdueStages)
                                .qcFailedStages(qcFailedStages)
                                .pendingMaterialRequests(pendingMaterialRequests)
                                .pendingQualityIssues(pendingQualityIssues)
                                .stageProgress(stageProgress)
                                .machinesInUse(machinesInUse)
                                .machinesAvailable(machinesAvailable)
                                .machinesMaintenance(machinesMaintenance)
                                .machinesNeedMaintenanceSoon(machinesNeedMaintenance)
                                .activeLeaders((int) activeLeaders)
                                .unassignedStages(unassignedStages)
                                .qcPassRate(Math.round(qcPassRate * 10.0) / 10.0)
                                .newIssues(newIssues)
                                .minorIssues(minorIssues)
                                .majorIssues(majorIssues)
                                .reworkStages(reworkStages)
                                .todaySchedule(todaySchedule)
                                .totalActiveOrders(totalActiveOrders)
                                .ordersInProgress(ordersInProgress)
                                .ordersWaitingMaterial(ordersWaitingMaterial)
                                .ordersCompleted(ordersCompleted)
                                .build();
        }

        // =============== Helper Methods ===============

        private List<DirectorDashboardDTO.ContractByMonthDTO> getContractsByMonth() {
                List<Contract> allContracts = contractRepository.findAll();
                LocalDate now = LocalDate.now();

                List<DirectorDashboardDTO.ContractByMonthDTO> result = new ArrayList<>();
                for (int i = 5; i >= 0; i--) {
                        LocalDate month = now.minusMonths(i);
                        int monthValue = month.getMonthValue();
                        int year = month.getYear();

                        List<Contract> monthContracts = allContracts.stream()
                                        .filter(c -> c.getContractDate() != null)
                                        .filter(c -> c.getContractDate().getMonthValue() == monthValue
                                                        && c.getContractDate().getYear() == year)
                                        .collect(Collectors.toList());

                        BigDecimal revenue = monthContracts.stream()
                                        .map(Contract::getTotalAmount)
                                        .filter(Objects::nonNull)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                        result.add(DirectorDashboardDTO.ContractByMonthDTO.builder()
                                        .month("T" + monthValue)
                                        .count(monthContracts.size())
                                        .revenue(revenue)
                                        .build());
                }
                return result;
        }

        private List<DirectorDashboardDTO.StatusCountDTO> getProductionOrdersByStatus(List<ProductionOrder> orders) {
                Map<String, Long> statusCounts = orders.stream()
                                .collect(Collectors.groupingBy(
                                                o -> o.getExecutionStatus() != null ? o.getExecutionStatus()
                                                                : o.getStatus(),
                                                Collectors.counting()));

                Map<String, String> statusLabels = Map.of(
                                "IN_PROGRESS", "Đang sản xuất",
                                "COMPLETED", "Hoàn thành",
                                "WAITING_PRODUCTION", "Chờ sản xuất",
                                "WAITING_MATERIAL_APPROVAL", "Chờ duyệt NVL",
                                "WAITING_REWORK", "Chờ làm lại",
                                "PENDING_APPROVAL", "Chờ duyệt",
                                "APPROVED", "Đã duyệt");

                return statusCounts.entrySet().stream()
                                .map(e -> DirectorDashboardDTO.StatusCountDTO.builder()
                                                .status(e.getKey())
                                                .label(statusLabels.getOrDefault(e.getKey(), e.getKey()))
                                                .count(e.getValue().intValue())
                                                .build())
                                .collect(Collectors.toList());
        }

        private PMDashboardDTO.ScheduleItemDTO mapToScheduleItem(ProductionStage stage) {
                ProductionOrder order = stage.getProductionOrder();
                return PMDashboardDTO.ScheduleItemDTO.builder()
                                .stageId(stage.getId())
                                .stageType(stage.getStageType())
                                .stageTypeName(STAGE_TYPE_NAMES.getOrDefault(stage.getStageType(),
                                                stage.getStageType()))
                                .poNumber(order != null ? order.getPoNumber() : null)
                                .productName(
                                                order != null && order.getContract() != null
                                                                ? order.getContract().getContractNumber()
                                                                : null)
                                .plannedStartAt(stage.getPlannedStartAt())
                                .status(stage.getExecutionStatus())
                                .leaderName(stage.getAssignedLeader() != null ? stage.getAssignedLeader().getName()
                                                : null)
                                .machineName(stage.getMachine() != null ? stage.getMachine().getName() : null)
                                .build();
        }
}
