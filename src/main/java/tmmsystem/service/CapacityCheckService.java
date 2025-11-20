package tmmsystem.service;

import org.springframework.stereotype.Service;
import tmmsystem.dto.sales.CapacityCheckResultDto;
import tmmsystem.entity.*;
import tmmsystem.repository.*;
import tmmsystem.service.timeline.SequentialCapacityCalculator;
import tmmsystem.service.timeline.SequentialCapacityResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CapacityCheckService {
    
    private final RfqRepository rfqRepository;
    private final RfqDetailRepository rfqDetailRepository;
    private final ProductRepository productRepository;
    private final MachineRepository machineRepository;
    private final WorkOrderRepository workOrderRepository;
    private final ProductionStageRepository productionStageRepository;
    private final MachineAssignmentRepository machineAssignmentRepository;
    private final MachineMaintenanceRepository machineMaintenanceRepository;
    private final PlanningTimelineCalculator timelineCalculator;
    private final SequentialCapacityCalculator sequentialCapacityCalculator;
    
    private static final BigDecimal WORKING_HOURS_PER_DAY = new BigDecimal("8"); // giờ/ngày
    
    // Thời gian chờ giữa các công đoạn (ngày)
    private static final BigDecimal WARPING_WAIT_TIME = new BigDecimal("0.5");
    private static final BigDecimal WEAVING_WAIT_TIME = new BigDecimal("0.5");
    private static final BigDecimal DYEING_WAIT_TIME = new BigDecimal("1.0");
    private static final BigDecimal CUTTING_WAIT_TIME = new BigDecimal("0.2");
    private static final BigDecimal SEWING_WAIT_TIME = new BigDecimal("0.3");
    private static final BigDecimal VENDOR_DYEING_TIME = new BigDecimal("2.0"); // Vendor nhuộm mất 2 ngày
    
    public CapacityCheckService(RfqRepository rfqRepository,
                               RfqDetailRepository rfqDetailRepository,
                               ProductRepository productRepository,
                               MachineRepository machineRepository,
                               WorkOrderRepository workOrderRepository,
                               ProductionStageRepository productionStageRepository,
                               MachineAssignmentRepository machineAssignmentRepository,
                               MachineMaintenanceRepository machineMaintenanceRepository,
                               PlanningTimelineCalculator timelineCalculator,
                               SequentialCapacityCalculator sequentialCapacityCalculator) {
        this.rfqRepository = rfqRepository;
        this.rfqDetailRepository = rfqDetailRepository;
        this.productRepository = productRepository;
        this.machineRepository = machineRepository;
        this.workOrderRepository = workOrderRepository;
        this.productionStageRepository = productionStageRepository;
        this.machineAssignmentRepository = machineAssignmentRepository;
        this.machineMaintenanceRepository = machineMaintenanceRepository;
        this.timelineCalculator = timelineCalculator;
        this.sequentialCapacityCalculator = sequentialCapacityCalculator;
    }
    
    public CapacityCheckResultDto checkMachineCapacity(Long rfqId) {
        Rfq rfq = rfqRepository.findById(rfqId).orElseThrow();
        
        // Tính thời gian sản xuất
        LocalDate productionStartDate = rfq.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate().plusDays(7);
        LocalDate productionEndDate = rfq.getExpectedDeliveryDate().minusDays(7);
        BigDecimal availableDays = BigDecimal.valueOf(ChronoUnit.DAYS.between(productionStartDate, productionEndDate));
        
        // Lấy chi tiết RFQ
        List<RfqDetail> rfqDetails = rfqDetailRepository.findByRfqId(rfqId);
        
        // Tính tổng khối lượng và số lượng sản phẩm
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalFaceTowels = BigDecimal.ZERO;
        BigDecimal totalBathTowels = BigDecimal.ZERO;
        BigDecimal totalSportsTowels = BigDecimal.ZERO;
        
        for (RfqDetail detail : rfqDetails) {
            Product product = productRepository.findById(detail.getProduct().getId()).orElseThrow();
            BigDecimal quantity = detail.getQuantity();
            BigDecimal weight = product.getStandardWeight().divide(new BigDecimal("1000")); // gram to kg
            
            totalWeight = totalWeight.add(weight.multiply(quantity));
            
            // Phân loại sản phẩm theo tên
            String productName = product.getName().toLowerCase();
            if (productName.contains("khăn mặt")) {
                totalFaceTowels = totalFaceTowels.add(quantity);
            } else if (productName.contains("khăn tắm")) {
                totalBathTowels = totalBathTowels.add(quantity);
            } else if (productName.contains("khăn thể thao")) {
                totalSportsTowels = totalSportsTowels.add(quantity);
            }
        }
        
        // Tính toán theo mô hình tuần tự: Mắc → Dệt → Nhuộm → Cắt → May
        SequentialCapacityResult capacityResult = sequentialCapacityCalculator.calculate(totalWeight, totalFaceTowels, totalBathTowels, totalSportsTowels);
        
        // Kiểm tra xung đột với đơn hàng khác
        List<CapacityCheckResultDto.ConflictDto> conflicts = checkSequentialConflicts(productionStartDate, productionEndDate, capacityResult);
        
        // Tạo kết quả
        CapacityCheckResultDto result = new CapacityCheckResultDto();
        
        CapacityCheckResultDto.MachineCapacityDto machineCapacity = new CapacityCheckResultDto.MachineCapacityDto();
        machineCapacity.setSufficient(capacityResult.getTotalDays().compareTo(availableDays) <= 0 && conflicts.isEmpty());
        machineCapacity.setBottleneck(capacityResult.getBottleneck());
        machineCapacity.setRequiredDays(capacityResult.getTotalDays());
        machineCapacity.setAvailableDays(availableDays);
        machineCapacity.setProductionStartDate(productionStartDate);
        machineCapacity.setProductionEndDate(productionEndDate);
        machineCapacity.setConflicts(conflicts);
        
        // Populate thông tin chi tiết các công đoạn tuần tự
        populateSequentialStages(machineCapacity, capacityResult, productionStartDate);
        
        // After machineCapacity populated:
        if (machineCapacity.isSufficient()) {
            machineCapacity.setStatus("SUFFICIENT");
            machineCapacity.setMergeSuggestion("Có thể tiến hành lập báo giá ngay");
        } else {
            machineCapacity.setStatus("INSUFFICIENT");
            // Gợi ý: nếu có ngày conflict rải rác, tìm ngày bắt đầu mới khả thi = productionEndDate + 2
            java.time.LocalDate suggestionDate = machineCapacity.getProductionEndDate().plusDays(2);
            machineCapacity.setMergeSuggestion("Đề xuất dời lịch sản xuất sang ngày " + suggestionDate + " hoặc gộp với đơn cùng sản phẩm nếu còn slot.");
        }
        result.setMachineCapacity(machineCapacity);
        
        // Kho luôn đủ
        CapacityCheckResultDto.WarehouseCapacityDto warehouseCapacity = new CapacityCheckResultDto.WarehouseCapacityDto();
        warehouseCapacity.setSufficient(true);
        warehouseCapacity.setMessage("Kho luôn đủ nguyên liệu");
        result.setWarehouseCapacity(warehouseCapacity);
        
        return result;
    }
    
    /**
     * Tính toán năng lực theo mô hình tuần tự: Mắc → Dệt → Nhuộm → Cắt → May
     */
    @SuppressWarnings("unused")
    private SequentialCapacityResult calculateSequentialCapacity(BigDecimal totalWeight, 
                                                                 BigDecimal totalFaceTowels, 
                                                                 BigDecimal totalBathTowels, 
                                                                 BigDecimal totalSportsTowels) {
        SequentialCapacityResult result = new SequentialCapacityResult();
        
        // Lấy công suất máy từ database (chỉ cần cho mắc và dệt)
        BigDecimal warpingCapacity = getMachineCapacity("WARPING");
        BigDecimal weavingCapacity = getMachineCapacity("WEAVING");
        
        // 1. MẮC: Tính thời gian mắc cuồng
        BigDecimal warpingDays = totalWeight.divide(warpingCapacity, 2, RoundingMode.HALF_UP);
        result.setWarpingDays(warpingDays);
        
        // 2. DỆT: Tính thời gian dệt (sau khi mắc xong)
        BigDecimal weavingDays = totalWeight.divide(weavingCapacity, 2, RoundingMode.HALF_UP);
        result.setWeavingDays(weavingDays);
        
        // 3. NHUỘM: Vendor nhuộm mất 2 ngày cố định
        BigDecimal dyeingDays = VENDOR_DYEING_TIME;
        result.setDyeingDays(dyeingDays);
        
        // 4. CẮT: Tính thời gian cắt (dựa trên số lượng sản phẩm)
        BigDecimal cuttingDays = calculateCuttingDays(totalFaceTowels, totalBathTowels, totalSportsTowels);
        result.setCuttingDays(cuttingDays);
        
        // 5. MAY: Tính thời gian may (dựa trên số lượng sản phẩm)
        BigDecimal sewingDays = calculateSewingDays(totalFaceTowels, totalBathTowels, totalSportsTowels);
        result.setSewingDays(sewingDays);
        
        // Tính tổng thời gian tuần tự + thời gian chờ
        BigDecimal totalSequentialDays = warpingDays
                .add(weavingDays)
                .add(dyeingDays)
                .add(cuttingDays)
                .add(sewingDays);
        
        // Thêm thời gian chờ giữa các công đoạn
        BigDecimal totalWaitTime = WARPING_WAIT_TIME
                .add(WEAVING_WAIT_TIME)
                .add(DYEING_WAIT_TIME)
                .add(CUTTING_WAIT_TIME)
                .add(SEWING_WAIT_TIME);
        
        BigDecimal totalDays = totalSequentialDays.add(totalWaitTime);
        result.setTotalDays(totalDays);
        
        // Xác định bottleneck (công đoạn mất nhiều thời gian nhất)
        BigDecimal maxProcessTime = warpingDays.max(weavingDays).max(dyeingDays).max(cuttingDays).max(sewingDays);
        String bottleneck = "WARPING";
        if (weavingDays.compareTo(maxProcessTime) == 0) {
            bottleneck = "WEAVING";
        } else if (dyeingDays.compareTo(maxProcessTime) == 0) {
            bottleneck = "DYEING";
        } else if (cuttingDays.compareTo(maxProcessTime) == 0) {
            bottleneck = "CUTTING";
        } else if (sewingDays.compareTo(maxProcessTime) == 0) {
            bottleneck = "SEWING";
        }
        result.setBottleneck(bottleneck);
        
        return result;
    }
    
    /**
     * Tính thời gian cắt dựa trên loại sản phẩm
     */
    private BigDecimal calculateCuttingDays(BigDecimal faceTowels, BigDecimal bathTowels, BigDecimal sportsTowels) {
        // Lấy tất cả máy cắt
        List<Machine> cuttingMachines = machineRepository.findAll().stream()
                .filter(m -> "CUTTING".equals(m.getType()))
                .filter(m -> "AVAILABLE".equals(m.getStatus()))
                .collect(Collectors.toList());
        
        if (cuttingMachines.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Tính công suất theo từng loại sản phẩm
        BigDecimal totalFaceCapacity = BigDecimal.ZERO;
        BigDecimal totalBathCapacity = BigDecimal.ZERO;
        BigDecimal totalSportsCapacity = BigDecimal.ZERO;
        
        for (Machine machine : cuttingMachines) {
            String specs = machine.getSpecifications();
            if (specs != null && specs.contains("capacityPerHour")) {
                // Parse capacityPerHour JSON
                BigDecimal faceCapacity = extractCapacityFromJson(specs, "faceTowels");
                BigDecimal bathCapacity = extractCapacityFromJson(specs, "bathTowels");
                BigDecimal sportsCapacity = extractCapacityFromJson(specs, "sportsTowels");
                
                totalFaceCapacity = totalFaceCapacity.add(faceCapacity.multiply(WORKING_HOURS_PER_DAY));
                totalBathCapacity = totalBathCapacity.add(bathCapacity.multiply(WORKING_HOURS_PER_DAY));
                totalSportsCapacity = totalSportsCapacity.add(sportsCapacity.multiply(WORKING_HOURS_PER_DAY));
            }
        }
        
        // Tính thời gian cần thiết cho từng loại
        BigDecimal faceDays = faceTowels.compareTo(BigDecimal.ZERO) > 0 ? 
            faceTowels.divide(totalFaceCapacity, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal bathDays = bathTowels.compareTo(BigDecimal.ZERO) > 0 ? 
            bathTowels.divide(totalBathCapacity, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal sportsDays = sportsTowels.compareTo(BigDecimal.ZERO) > 0 ? 
            sportsTowels.divide(totalSportsCapacity, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        
        // Trả về thời gian lớn nhất (bottleneck)
        return faceDays.max(bathDays).max(sportsDays);
    }
    
    /**
     * Populate thông tin chi tiết các công đoạn tuần tự
     */
    private void populateSequentialStages(CapacityCheckResultDto.MachineCapacityDto machineCapacity,
                                          SequentialCapacityResult capacityResult,
                                          LocalDate productionStartDate) {
        var timelines = timelineCalculator.buildTimeline(productionStartDate, capacityResult);
        var warping = findTimeline(timelines, "WARPING");
        var weaving = findTimeline(timelines, "WEAVING");
        var dyeing = findTimeline(timelines, "DYEING");
        var cutting = findTimeline(timelines, "CUTTING");
        var hemming = findTimeline(timelines, "HEMMING");

        machineCapacity.setWarpingStage(createStageInfo("Mắc cuồng", "WARPING", capacityResult.getWarpingDays(),
                WARPING_WAIT_TIME, warping.start().toLocalDate(), warping.end().toLocalDate(),
                getMachineCapacity("WARPING"), "Mắc sợi thành cuồng"));

        machineCapacity.setWeavingStage(createStageInfo("Dệt vải", "WEAVING", capacityResult.getWeavingDays(),
                WEAVING_WAIT_TIME, weaving.start().toLocalDate(), weaving.end().toLocalDate(),
                getMachineCapacity("WEAVING"), "Dệt cuồng thành vải"));

        machineCapacity.setDyeingStage(createStageInfo("Nhuộm vải", "DYEING", capacityResult.getDyeingDays(),
                DYEING_WAIT_TIME, dyeing.start().toLocalDate(), dyeing.end().toLocalDate(),
                BigDecimal.ZERO, "Gửi vendor nhuộm (2 ngày cố định)"));

        machineCapacity.setCuttingStage(createStageInfo("Cắt vải", "CUTTING", capacityResult.getCuttingDays(),
                CUTTING_WAIT_TIME, cutting.start().toLocalDate(), cutting.end().toLocalDate(),
                getMachineCapacity("CUTTING"), "Cắt vải theo kích thước"));

        machineCapacity.setSewingStage(createStageInfo("May thành phẩm", "SEWING", capacityResult.getSewingDays(),
                SEWING_WAIT_TIME, hemming.start().toLocalDate(), hemming.end().toLocalDate(),
                getMachineCapacity("SEWING"), "May vải thành sản phẩm hoàn chỉnh"));

        BigDecimal totalWaitTime = WARPING_WAIT_TIME
                .add(WEAVING_WAIT_TIME)
                .add(DYEING_WAIT_TIME)
                .add(CUTTING_WAIT_TIME)
                .add(SEWING_WAIT_TIME);
        machineCapacity.setTotalWaitTime(totalWaitTime);
    }
    
    /**
     * Tạo thông tin cho một công đoạn
     */
    private CapacityCheckResultDto.SequentialStageDto createStageInfo(String stageName, String stageType, 
                                                                     BigDecimal processingDays, BigDecimal waitTime,
                                                                     LocalDate startDate, LocalDate endDate, 
                                                                     BigDecimal capacity, String description) {
        CapacityCheckResultDto.SequentialStageDto stage = new CapacityCheckResultDto.SequentialStageDto();
        stage.setStageName(stageName);
        stage.setStageType(stageType);
        stage.setProcessingDays(processingDays);
        stage.setWaitTime(waitTime);
        stage.setStartDate(startDate);
        stage.setEndDate(endDate);
        stage.setCapacity(capacity);
        stage.setDescription(description);
        return stage;
    }

    private PlanningTimelineCalculator.StageTimeline findTimeline(List<PlanningTimelineCalculator.StageTimeline> timelines, String type) {
        return timelines.stream()
                .filter(t -> type.equalsIgnoreCase(t.stageType()))
                .findFirst()
                .orElseThrow();
    }
    
    /**
     * Kiểm tra xung đột theo mô hình tuần tự
     */
    private List<CapacityCheckResultDto.ConflictDto> checkSequentialConflicts(LocalDate startDate, LocalDate endDate, 
                                                                              SequentialCapacityResult capacityResult) {
        List<CapacityCheckResultDto.ConflictDto> conflicts = new ArrayList<>();
        
        // Lấy tất cả WorkOrder đang chạy
        List<WorkOrder> activeWorkOrders = workOrderRepository.findAll().stream()
                .filter(wo -> "APPROVED".equals(wo.getStatus()) || "IN_PROGRESS".equals(wo.getStatus()))
                .collect(Collectors.toList());
        
        // Tính thời gian bắt đầu và kết thúc cho từng công đoạn
        LocalDate warpingStart = startDate;
        LocalDate warpingEnd = warpingStart.plusDays(capacityResult.getWarpingDays().longValue());
        
        LocalDate weavingStart = warpingEnd.plusDays(WARPING_WAIT_TIME.longValue());
        LocalDate weavingEnd = weavingStart.plusDays(capacityResult.getWeavingDays().longValue());
        
        LocalDate dyeingStart = weavingEnd.plusDays(WEAVING_WAIT_TIME.longValue());
        LocalDate dyeingEnd = dyeingStart.plusDays(capacityResult.getDyeingDays().longValue());
        
        LocalDate cuttingStart = dyeingEnd.plusDays(DYEING_WAIT_TIME.longValue());
        LocalDate cuttingEnd = cuttingStart.plusDays(capacityResult.getCuttingDays().longValue());
        
        LocalDate sewingStart = cuttingEnd.plusDays(CUTTING_WAIT_TIME.longValue());
        LocalDate sewingEnd = sewingStart.plusDays(capacityResult.getSewingDays().longValue());
        
        // Kiểm tra xung đột cho từng công đoạn
        conflicts.addAll(checkStageConflicts("WARPING", warpingStart, warpingEnd, activeWorkOrders));
        conflicts.addAll(checkStageConflicts("WEAVING", weavingStart, weavingEnd, activeWorkOrders));
        conflicts.addAll(checkStageConflicts("DYEING", dyeingStart, dyeingEnd, activeWorkOrders));
        conflicts.addAll(checkStageConflicts("CUTTING", cuttingStart, cuttingEnd, activeWorkOrders));
        conflicts.addAll(checkStageConflicts("SEWING", sewingStart, sewingEnd, activeWorkOrders));
        
        return conflicts;
    }
    
    /**
     * Kiểm tra xung đột cho một công đoạn cụ thể
     */
    private List<CapacityCheckResultDto.ConflictDto> checkStageConflicts(String stageType, LocalDate stageStart, LocalDate stageEnd, 
                                                                        List<WorkOrder> activeWorkOrders) {
        List<CapacityCheckResultDto.ConflictDto> conflicts = new ArrayList<>();
        
        // Lấy công suất tổng cho loại máy này
        BigDecimal totalCapacity = getMachineCapacity(stageType);
        
        // Kiểm tra từng ngày trong giai đoạn
        LocalDate currentDate = stageStart;
        while (!currentDate.isAfter(stageEnd)) {
            // Tính công suất đã sử dụng trong ngày này dựa trên lịch chạy thực tế
            BigDecimal usedCapacity = calculateActualUsedCapacityFromSchedule(stageType, currentDate, activeWorkOrders, totalCapacity);
            
            // Tính công suất còn lại
            BigDecimal availableCapacity = totalCapacity.subtract(usedCapacity);
            
            // Kiểm tra xung đột (giả định cần 100% công suất cho đơn hàng mới)
            if (totalCapacity.compareTo(availableCapacity) > 0) {
                CapacityCheckResultDto.ConflictDto conflict = new CapacityCheckResultDto.ConflictDto();
                conflict.setDate(currentDate);
                conflict.setMachineType(stageType);
                conflict.setRequired(totalCapacity);
                conflict.setAvailable(availableCapacity);
                conflict.setUsed(usedCapacity);
                conflict.setConflictMessage("Không đủ công suất " + getStageName(stageType) + " vào ngày " + currentDate);
                conflicts.add(conflict);
            }
            
            currentDate = currentDate.plusDays(1);
        }
        
        return conflicts;
    }
    
    /**
     * Tính công suất đã sử dụng thực tế dựa trên lịch chạy thực tế
     */
    private BigDecimal calculateActualUsedCapacityFromSchedule(String stageType, LocalDate currentDate, 
                                                              List<WorkOrder> activeWorkOrders, BigDecimal totalCapacity) {
        BigDecimal totalUsedCapacity = BigDecimal.ZERO;
        
        // Lấy tất cả ProductionStage đang chạy trong ngày này
        List<ProductionStage> activeStages = getActiveProductionStages(stageType, currentDate);
        
        // Tính công suất đã sử dụng từ MachineAssignment
        for (ProductionStage stage : activeStages) {
            BigDecimal stageUsedCapacity = calculateStageCapacityUsage(stage, stageType);
            totalUsedCapacity = totalUsedCapacity.add(stageUsedCapacity);
        }
        
        // Trừ đi công suất bị mất do bảo trì máy
        BigDecimal maintenanceLoss = calculateMaintenanceCapacityLoss(stageType, currentDate);
        totalUsedCapacity = totalUsedCapacity.add(maintenanceLoss);
        
        return totalUsedCapacity;
    }
    
    /**
     * Lấy tất cả ProductionStage đang chạy trong ngày này
     */
    private List<ProductionStage> getActiveProductionStages(String stageType, LocalDate currentDate) {
        return productionStageRepository.findAll().stream()
                .filter(stage -> stageType.equals(stage.getStageType()))
                .filter(stage -> "IN_PROGRESS".equals(stage.getStatus()))
                .filter(stage -> isStageActiveOnDate(stage, currentDate))
                .collect(Collectors.toList());
    }
    
    /**
     * Kiểm tra xem ProductionStage có đang chạy trong ngày này không
     */
    private boolean isStageActiveOnDate(ProductionStage stage, LocalDate date) {
        if (stage.getStartAt() == null || stage.getCompleteAt() == null) {
            return false;
        }
        
        LocalDate stageStart = stage.getStartAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        LocalDate stageEnd = stage.getCompleteAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        
        return !date.isBefore(stageStart) && !date.isAfter(stageEnd);
    }
    
    /**
     * Tính công suất sử dụng của một ProductionStage
     */
    private BigDecimal calculateStageCapacityUsage(ProductionStage stage, String stageType) {
        // Lấy MachineAssignment cho stage này
        List<MachineAssignment> assignments = machineAssignmentRepository.findAll().stream()
                .filter(assignment -> assignment.getProductionStage().getId().equals(stage.getId()))
                .filter(assignment -> assignment.getReleasedAt() == null) // Chưa được giải phóng
                .collect(Collectors.toList());
        
        if (assignments.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Tính công suất dựa trên số máy được gán
        BigDecimal totalMachineCapacity = BigDecimal.ZERO;
        for (MachineAssignment assignment : assignments) {
            Machine machine = assignment.getMachine();
            BigDecimal machineCapacity = getMachineCapacityByType(machine, stageType);
            totalMachineCapacity = totalMachineCapacity.add(machineCapacity);
        }
        
        return totalMachineCapacity;
    }
    
    /**
     * Tính công suất bị mất do bảo trì máy
     */
    private BigDecimal calculateMaintenanceCapacityLoss(String stageType, LocalDate currentDate) {
        List<MachineMaintenance> maintenanceList = machineMaintenanceRepository.findAll().stream()
                .filter(maintenance -> isMaintenanceActiveOnDate(maintenance, currentDate))
                .filter(maintenance -> isMachineTypeMatch(maintenance.getMachine(), stageType))
                .collect(Collectors.toList());
        
        BigDecimal totalLoss = BigDecimal.ZERO;
        for (MachineMaintenance maintenance : maintenanceList) {
            Machine machine = maintenance.getMachine();
            BigDecimal machineCapacity = getMachineCapacityByType(machine, stageType);
            totalLoss = totalLoss.add(machineCapacity);
        }
        
        return totalLoss;
    }
    
    /**
     * Kiểm tra xem bảo trì có đang diễn ra trong ngày này không
     */
    private boolean isMaintenanceActiveOnDate(MachineMaintenance maintenance, LocalDate date) {
        if (maintenance.getStartedAt() == null || maintenance.getCompletedAt() == null) {
            return false;
        }
        
        LocalDate maintenanceStart = maintenance.getStartedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        LocalDate maintenanceEnd = maintenance.getCompletedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        
        return !date.isBefore(maintenanceStart) && !date.isAfter(maintenanceEnd);
    }
    
    /**
     * Kiểm tra xem máy có phù hợp với loại công đoạn không
     */
    private boolean isMachineTypeMatch(Machine machine, String stageType) {
        return stageType.equals(machine.getType());
    }
    
    /**
     * Lấy công suất của một máy cụ thể theo loại công đoạn
     */
    private BigDecimal getMachineCapacityByType(Machine machine, String stageType) {
        if ("DYEING".equals(stageType)) {
            // Vendor nhuộm có công suất vô hạn
            return new BigDecimal("999999");
        } else if ("CUTTING".equals(stageType) || "SEWING".equals(stageType)) {
            // Máy cắt và may: tính theo số lượng sản phẩm
            return extractCapacityPerDayFromSpecifications(machine);
        } else {
            // Máy mắc và dệt: tính theo khối lượng
            return extractCapacityFromSpecifications(machine);
        }
    }
    
    
    
    /**
     * Lấy tên tiếng Việt của công đoạn
     */
    private String getStageName(String stageType) {
        switch (stageType) {
            case "WARPING": return "máy cuồng mắc";
            case "WEAVING": return "máy dệt";
            case "DYEING": return "vendor nhuộm";
            case "CUTTING": return "máy cắt";
            case "SEWING": return "máy may";
            default: return stageType;
        }
    }
    
    
    /**
     * Lấy tổng công suất của loại máy từ database
     */
    private BigDecimal getMachineCapacity(String machineType) {
        if ("DYEING".equals(machineType)) {
            // Vendor nhuộm có công suất vô hạn (chỉ bị giới hạn bởi thời gian)
            return new BigDecimal("999999");
        } else if ("CUTTING".equals(machineType) || "SEWING".equals(machineType)) {
            // Đối với máy cắt và may, tính công suất theo số lượng sản phẩm
            return machineRepository.findAll().stream()
                    .filter(m -> machineType.equals(m.getType()))
                    .filter(m -> "AVAILABLE".equals(m.getStatus()))
                    .map(this::extractCapacityPerDayFromSpecifications)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            // Đối với máy mắc và dệt, tính công suất theo khối lượng
            return machineRepository.findAll().stream()
                    .filter(m -> machineType.equals(m.getType()))
                    .filter(m -> "AVAILABLE".equals(m.getStatus()))
                    .map(this::extractCapacityFromSpecifications)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }
    
    /**
     * Trích xuất capacity per day từ JSON specifications (cho máy cắt và may)
     */
    private BigDecimal extractCapacityPerDayFromSpecifications(Machine machine) {
        try {
            String specs = machine.getSpecifications();
            if (specs == null || specs.isEmpty()) {
                return BigDecimal.ZERO;
            }
            
            // Parse JSON để lấy capacityPerHour và tính capacityPerDay
            BigDecimal faceCapacity = extractCapacityFromJson(specs, "faceTowels");
            BigDecimal bathCapacity = extractCapacityFromJson(specs, "bathTowels");
            BigDecimal sportsCapacity = extractCapacityFromJson(specs, "sportsTowels");
            
            // Tính tổng công suất trung bình (giả định phân bố đều)
            BigDecimal avgCapacity = faceCapacity.add(bathCapacity).add(sportsCapacity).divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP);
            return avgCapacity.multiply(WORKING_HOURS_PER_DAY);
            
        } catch (Exception e) {
            System.err.println("Error parsing capacity from machine " + machine.getCode() + ": " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Trích xuất capacity từ JSON specifications
     */
    private BigDecimal extractCapacityFromSpecifications(Machine machine) {
        try {
            String specs = machine.getSpecifications();
            if (specs == null || specs.isEmpty()) {
                return BigDecimal.ZERO;
            }
            
            // Parse JSON để lấy capacityPerDay
            // Đơn giản: tìm "capacityPerDay":value
            String capacityPattern = "\"capacityPerDay\":";
            int startIndex = specs.indexOf(capacityPattern);
            if (startIndex == -1) {
                return BigDecimal.ZERO;
            }
            
            startIndex += capacityPattern.length();
            int endIndex = specs.indexOf(",", startIndex);
            if (endIndex == -1) {
                endIndex = specs.indexOf("}", startIndex);
            }
            
            if (endIndex == -1) {
                return BigDecimal.ZERO;
            }
            
            String capacityStr = specs.substring(startIndex, endIndex).trim();
            return new BigDecimal(capacityStr);
            
        } catch (Exception e) {
            System.err.println("Error parsing capacity from machine " + machine.getCode() + ": " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Tính thời gian may dựa trên loại sản phẩm
     */
    private BigDecimal calculateSewingDays(BigDecimal faceTowels, BigDecimal bathTowels, BigDecimal sportsTowels) {
        // Lấy tất cả máy may
        List<Machine> sewingMachines = machineRepository.findAll().stream()
                .filter(m -> "SEWING".equals(m.getType()))
                .filter(m -> "AVAILABLE".equals(m.getStatus()))
                .collect(Collectors.toList());
        
        if (sewingMachines.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Tính công suất theo từng loại sản phẩm
        BigDecimal totalFaceCapacity = BigDecimal.ZERO;
        BigDecimal totalBathCapacity = BigDecimal.ZERO;
        BigDecimal totalSportsCapacity = BigDecimal.ZERO;
        
        for (Machine machine : sewingMachines) {
            String specs = machine.getSpecifications();
            if (specs != null && specs.contains("capacityPerHour")) {
                // Parse capacityPerHour JSON
                BigDecimal faceCapacity = extractCapacityFromJson(specs, "faceTowels");
                BigDecimal bathCapacity = extractCapacityFromJson(specs, "bathTowels");
                BigDecimal sportsCapacity = extractCapacityFromJson(specs, "sportsTowels");
                
                totalFaceCapacity = totalFaceCapacity.add(faceCapacity.multiply(WORKING_HOURS_PER_DAY));
                totalBathCapacity = totalBathCapacity.add(bathCapacity.multiply(WORKING_HOURS_PER_DAY));
                totalSportsCapacity = totalSportsCapacity.add(sportsCapacity.multiply(WORKING_HOURS_PER_DAY));
            }
        }
        
        // Tính thời gian cần thiết cho từng loại
        BigDecimal faceDays = faceTowels.compareTo(BigDecimal.ZERO) > 0 ? 
            faceTowels.divide(totalFaceCapacity, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal bathDays = bathTowels.compareTo(BigDecimal.ZERO) > 0 ? 
            bathTowels.divide(totalBathCapacity, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal sportsDays = sportsTowels.compareTo(BigDecimal.ZERO) > 0 ? 
            sportsTowels.divide(totalSportsCapacity, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        
        // Trả về thời gian lớn nhất (bottleneck)
        return faceDays.max(bathDays).max(sportsDays);
    }
    
    /**
     * Trích xuất giá trị từ JSON specifications
     */
    private BigDecimal extractCapacityFromJson(String specs, String key) {
        try {
            String pattern = "\"" + key + "\":";
            int startIndex = specs.indexOf(pattern);
            if (startIndex == -1) {
                return BigDecimal.ZERO;
            }
            
            startIndex += pattern.length();
            int endIndex = specs.indexOf(",", startIndex);
            if (endIndex == -1) {
                endIndex = specs.indexOf("}", startIndex);
            }
            
            if (endIndex == -1) {
                return BigDecimal.ZERO;
            }
            
            String valueStr = specs.substring(startIndex, endIndex).trim();
            return new BigDecimal(valueStr);
            
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
