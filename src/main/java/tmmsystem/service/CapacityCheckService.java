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

    private final PlanningTimelineCalculator timelineCalculator;
    private final SequentialCapacityCalculator sequentialCapacityCalculator;

    private static final BigDecimal WORKING_HOURS_PER_DAY = new BigDecimal("8"); // giờ/ngày

    // Thời gian chờ giữa các công đoạn (ngày)
    private static final BigDecimal WARPING_WAIT_TIME = new BigDecimal("0.5");
    private static final BigDecimal WEAVING_WAIT_TIME = new BigDecimal("0.5");
    private static final BigDecimal DYEING_WAIT_TIME = new BigDecimal("1.0");
    private static final BigDecimal CUTTING_WAIT_TIME = new BigDecimal("0.2");
    private static final BigDecimal SEWING_WAIT_TIME = new BigDecimal("0.3");
    private static final BigDecimal PACKAGING_WAIT_TIME = new BigDecimal("0.2");
    private static final BigDecimal VENDOR_DYEING_TIME = new BigDecimal("2.0"); // Vendor nhuộm mất 2 ngày

    private final ContractRepository contractRepository;
    private final QuotationRepository quotationRepository;

    // Quotation statuses that block production capacity
    private static final java.util.List<String> CAPACITY_BLOCKING_STATUSES = java.util.List.of(
            "SENT", "ACCEPTED", "ORDER_CREATED");

    public CapacityCheckService(RfqRepository rfqRepository,
            RfqDetailRepository rfqDetailRepository,
            ProductRepository productRepository,
            MachineRepository machineRepository,
            WorkOrderRepository workOrderRepository,
            PlanningTimelineCalculator timelineCalculator,
            SequentialCapacityCalculator sequentialCapacityCalculator,
            ContractRepository contractRepository,
            QuotationRepository quotationRepository) {
        this.rfqRepository = rfqRepository;
        this.rfqDetailRepository = rfqDetailRepository;
        this.productRepository = productRepository;
        this.machineRepository = machineRepository;

        this.timelineCalculator = timelineCalculator;
        this.sequentialCapacityCalculator = sequentialCapacityCalculator;
        this.contractRepository = contractRepository;
        this.quotationRepository = quotationRepository;
    }

    public CapacityCheckResultDto checkMachineCapacity(Long rfqId) {
        Rfq rfq = rfqRepository.findById(rfqId).orElseThrow();

        // 1. Calculate New Order Weight (kg)
        List<RfqDetail> rfqDetails = rfqDetailRepository.findByRfqId(rfqId);
        BigDecimal newOrderWeightKg = calculateTotalWeight(rfqDetails);

        // Also calculate stage breakdown for display purposes
        SequentialCapacityResult newOrderCapacity = calculateCapacityForDetails(rfqDetails);

        // 2. Calculate Backlog Weight (kg) using QUEUE-BASED approach
        // 
        // REAL PRODUCTION LOGIC:
        // - All pending orders go through the same production line (bottleneck: Mắc cuồng)
        // - Orders are processed by PRIORITY (earliest delivery date first)
        // - To know when WE can deliver, we need to know:
        //   a) How much work is ahead of us in the queue (higher priority orders)
        //   b) Time to clear that backlog + produce our order
        //
        // IMPORTANT: We calculate backlog based on a FIXED reference point (today)
        // NOT based on our target delivery date. This prevents the snowball effect.
        
        LocalDate targetDate = rfq.getExpectedDeliveryDate();
        LocalDate productionStartDate = LocalDate.now().plusDays(7);
        LocalDate productionDeadline = targetDate.minusDays(7);

        // Get ALL active quotations
        List<Quotation> allActiveQuotations = quotationRepository.findByStatusIn(CAPACITY_BLOCKING_STATUSES);
        
        // Separate into: blocking backlog (higher priority) vs non-blocking (lower priority)
        BigDecimal blockingBacklogKg = BigDecimal.ZERO;  // Orders that MUST be done before us
        BigDecimal totalQueueKg = BigDecimal.ZERO;       // ALL pending orders (for proposed date calculation)
        List<CapacityCheckResultDto.BacklogOrderDto> backlogOrdersList = new ArrayList<>();

        for (Quotation q : allActiveQuotations) {
            // Skip if this is the same RFQ we're checking
            if (q.getRfq() != null && q.getRfq().getId().equals(rfqId)) {
                continue;
            }
            
            LocalDate qDeliveryDate = q.getRfq() != null ? q.getRfq().getExpectedDeliveryDate() : null;
            if (qDeliveryDate == null) {
                continue;
            }
            
            BigDecimal qWeight = calculateQuotationWeight(q);
            
            // Always count towards total queue (for proposed date calculation)
            totalQueueKg = totalQueueKg.add(qWeight);
            
            // PRIORITY-BASED BACKLOG:
            // Only orders with delivery date BEFORE OR EQUAL to our target date block us
            // These orders have HIGHER priority (earlier deadline = higher priority)
            // Orders with later deadlines will be produced AFTER us
            boolean isBlockingOrder = !qDeliveryDate.isAfter(targetDate);
            
            if (isBlockingOrder) {
                blockingBacklogKg = blockingBacklogKg.add(qWeight);
            }
            
            // Build backlog order entry for display (show all for transparency)
            CapacityCheckResultDto.BacklogOrderDto backlogOrder = new CapacityCheckResultDto.BacklogOrderDto();
            backlogOrder.setQuotationCode(q.getQuotationNumber());
            backlogOrder.setCustomerName(q.getRfq().getCustomer() != null
                    ? q.getRfq().getCustomer().getCompanyName()
                    : "RFQ-" + q.getRfq().getId());
            backlogOrder.setDeliveryDate(qDeliveryDate);
            backlogOrder.setWeightKg(qWeight.setScale(2, RoundingMode.HALF_UP));
            backlogOrder.setStatus(q.getStatus());
            // Mark if this order blocks us
            backlogOrder.setStatus(isBlockingOrder ? q.getStatus() + " (ưu tiên cao hơn)" : q.getStatus());
            backlogOrdersList.add(backlogOrder);
        }
        
        // Sort backlog by delivery date (earliest first = highest priority)
        backlogOrdersList.sort((a, b) -> a.getDeliveryDate().compareTo(b.getDeliveryDate()));
        
        // 3. Get Bottleneck Capacity (kg/day)
        BigDecimal bottleneckCapacity = sequentialCapacityCalculator.getBottleneckCapacityPerDay();
        
        // 4. Calculate EARLIEST POSSIBLE delivery date (independent of target date)
        // This prevents snowball effect by giving a FIXED answer
        // Logic: All pending work + our order must go through bottleneck
        BigDecimal totalQueueLoad = totalQueueKg.add(newOrderWeightKg);
        BigDecimal daysToCompleteAllWork;
        if (bottleneckCapacity.compareTo(BigDecimal.ZERO) > 0) {
            daysToCompleteAllWork = totalQueueLoad.divide(bottleneckCapacity, 2, RoundingMode.HALF_UP);
        } else {
            daysToCompleteAllWork = newOrderCapacity.getTotalDays();
        }
        
        LocalDate earliestPossibleDelivery = LocalDate.now()
                .plusDays(7)  // production start buffer
                .plusDays(daysToCompleteAllWork.setScale(0, RoundingMode.CEILING).longValue())
                .plusDays(7); // delivery buffer
        
        // 5. Calculate required days for BLOCKING backlog only (for display)
        BigDecimal backlogWeightKg = blockingBacklogKg;
        BigDecimal totalLoadKg = newOrderWeightKg.add(backlogWeightKg);
        BigDecimal requiredDays;
        if (bottleneckCapacity.compareTo(BigDecimal.ZERO) > 0) {
            requiredDays = totalLoadKg.divide(bottleneckCapacity, 2, RoundingMode.HALF_UP);
        } else {
            requiredDays = newOrderCapacity.getTotalDays();
        }

        // 6. Available Days (from target date)
        long availableDaysCount = ChronoUnit.DAYS.between(productionStartDate, productionDeadline);
        BigDecimal availableDays = BigDecimal.valueOf(Math.max(0, availableDaysCount));
        BigDecimal maxCapacityKg = availableDays.multiply(bottleneckCapacity);

        // 7. Result - Compare target date with earliest possible delivery
        // This is the KEY fix: use earliest possible delivery to avoid snowball
        boolean isSufficient = !targetDate.isBefore(earliestPossibleDelivery);

        // Construct Result DTO
        CapacityCheckResultDto result = new CapacityCheckResultDto();
        CapacityCheckResultDto.MachineCapacityDto machineCapacity = new CapacityCheckResultDto.MachineCapacityDto();

        machineCapacity.setSufficient(isSufficient);
        machineCapacity.setRequiredDays(requiredDays);
        machineCapacity.setAvailableDays(availableDays);
        machineCapacity.setProductionStartDate(productionStartDate);
        machineCapacity.setProductionEndDate(
                productionStartDate.plusDays(requiredDays.setScale(0, RoundingMode.CEILING).longValue()));
        machineCapacity.setConflicts(new ArrayList<>());
        machineCapacity.setBottleneck(getBottleneckVietnameseName(bottleneckCapacity));

        // Populate calculation details (NEW)
        machineCapacity.setNewOrderWeightKg(newOrderWeightKg.setScale(2, RoundingMode.HALF_UP));
        machineCapacity.setBacklogWeightKg(backlogWeightKg.setScale(2, RoundingMode.HALF_UP));
        machineCapacity.setTotalLoadKg(totalLoadKg.setScale(2, RoundingMode.HALF_UP));
        machineCapacity.setMaxCapacityKg(maxCapacityKg.setScale(2, RoundingMode.HALF_UP));
        machineCapacity.setBottleneckCapacityPerDay(bottleneckCapacity.setScale(2, RoundingMode.HALF_UP));
        machineCapacity.setBacklogOrders(backlogOrdersList);

        // Populate stage capacities to explain bottleneck
        machineCapacity.setStageCapacities(sequentialCapacityCalculator.getAllStageCapacities());

        // Populate stage details for the NEW order only (for visualization)
        populateSequentialStages(machineCapacity, newOrderCapacity, productionStartDate);

        if (isSufficient) {
            machineCapacity.setStatus("SUFFICIENT");
            machineCapacity.setMergeSuggestion(
                    "Đủ năng lực. Ngày giao sớm nhất có thể: " + earliestPossibleDelivery + ". "
                            + "Tổng hàng đợi: " + totalQueueLoad.setScale(2, RoundingMode.HALF_UP) + " kg ("
                            + daysToCompleteAllWork.setScale(0, RoundingMode.HALF_UP) + " ngày).");
        } else {
            machineCapacity.setStatus("INSUFFICIENT");
            machineCapacity
                    .setMergeSuggestion("Không đủ năng lực! Ngày giao yêu cầu: " + targetDate 
                            + " < Ngày giao sớm nhất có thể: " + earliestPossibleDelivery + ". "
                            + "Tổng hàng đợi: " + totalQueueLoad.setScale(2, RoundingMode.HALF_UP) + " kg ("
                            + daysToCompleteAllWork.setScale(0, RoundingMode.HALF_UP) + " ngày). "
                            + "Cần dời ngày giao hàng.");

            // Use the ALREADY CALCULATED earliest possible delivery date
            // This is based on total queue, so it's STABLE (no snowball effect)
            // If user updates to this date and checks again, result will be SUFFICIENT
            machineCapacity.setProposedNewDeliveryDate(earliestPossibleDelivery);
        }
        result.setMachineCapacity(machineCapacity);

        // Warehouse is always sufficient per requirement
        CapacityCheckResultDto.WarehouseCapacityDto warehouseCapacity = new CapacityCheckResultDto.WarehouseCapacityDto();
        warehouseCapacity.setSufficient(true);
        warehouseCapacity.setMessage("Kho luôn đủ nguyên liệu");
        result.setWarehouseCapacity(warehouseCapacity);

        return result;
    }

    private BigDecimal calculateTotalWeight(List<RfqDetail> details) {
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (RfqDetail d : details) {
            Product p = d.getProduct();
            if (p == null)
                p = productRepository.findById(d.getProduct().getId()).orElseThrow();
            BigDecimal qty = d.getQuantity();
            BigDecimal weightPerItem = p.getStandardWeight().divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP); // g
                                                                                                                      // to
                                                                                                                      // kg
            totalWeight = totalWeight.add(weightPerItem.multiply(qty));
        }
        return totalWeight;
    }

    private BigDecimal calculateQuotationWeight(Quotation q) {
        BigDecimal totalWeight = BigDecimal.ZERO;
        if (q.getDetails() == null)
            return totalWeight;
        for (QuotationDetail d : q.getDetails()) {
            Product p = d.getProduct();
            if (p == null)
                continue;
            BigDecimal qty = d.getQuantity();
            BigDecimal weightPerItem = p.getStandardWeight().divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
            totalWeight = totalWeight.add(weightPerItem.multiply(qty));
        }
        return totalWeight;
    }

    private SequentialCapacityResult calculateCapacityForDetails(List<RfqDetail> details) {
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalFace = BigDecimal.ZERO;
        BigDecimal totalBath = BigDecimal.ZERO;
        BigDecimal totalSports = BigDecimal.ZERO;

        for (RfqDetail d : details) {
            Product p = d.getProduct(); // Assuming fetched or eager
            // If lazy, might need productRepository.findById
            if (p == null)
                p = productRepository.findById(d.getProduct().getId()).orElseThrow();

            BigDecimal qty = d.getQuantity();
            BigDecimal weight = p.getStandardWeight().divide(new BigDecimal("1000")).multiply(qty);
            totalWeight = totalWeight.add(weight);

            String name = p.getName().toLowerCase();
            if (name.contains("khăn mặt"))
                totalFace = totalFace.add(qty);
            else if (name.contains("khăn tắm"))
                totalBath = totalBath.add(qty);
            else if (name.contains("khăn thể thao"))
                totalSports = totalSports.add(qty);
        }
        return sequentialCapacityCalculator.calculate(totalWeight, totalFace, totalBath, totalSports);
    }

    private SequentialCapacityResult calculateCapacityForQuotationDetails(List<QuotationDetail> details) {
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalFace = BigDecimal.ZERO;
        BigDecimal totalBath = BigDecimal.ZERO;
        BigDecimal totalSports = BigDecimal.ZERO;

        for (QuotationDetail d : details) {
            Product p = d.getProduct();
            BigDecimal qty = d.getQuantity();
            BigDecimal weight = p.getStandardWeight().divide(new BigDecimal("1000")).multiply(qty);
            totalWeight = totalWeight.add(weight);

            String name = p.getName().toLowerCase();
            if (name.contains("khăn mặt"))
                totalFace = totalFace.add(qty);
            else if (name.contains("khăn tắm"))
                totalBath = totalBath.add(qty);
            else if (name.contains("khăn thể thao"))
                totalSports = totalSports.add(qty);
        }
        return sequentialCapacityCalculator.calculate(totalWeight, totalFace, totalBath, totalSports);
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
        BigDecimal faceDays = faceTowels.compareTo(BigDecimal.ZERO) > 0
                ? faceTowels.divide(totalFaceCapacity, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal bathDays = bathTowels.compareTo(BigDecimal.ZERO) > 0
                ? bathTowels.divide(totalBathCapacity, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal sportsDays = sportsTowels.compareTo(BigDecimal.ZERO) > 0
                ? sportsTowels.divide(totalSportsCapacity, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

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
        var packaging = findTimeline(timelines, "PACKAGING");

        machineCapacity.setWarpingStage(createStageInfo("Mắc cuồng", "WARPING", capacityResult.getWarpingDays(),
                WARPING_WAIT_TIME, warping.start().toLocalDate(), warping.end().toLocalDate(),
                getMachineCapacity("WARPING"), "Mắc sợi thành cuồng"));

        machineCapacity.setWeavingStage(createStageInfo("Dệt vải", "WEAVING", capacityResult.getWeavingDays(),
                WEAVING_WAIT_TIME, weaving.start().toLocalDate(), weaving.end().toLocalDate(),
                getMachineCapacity("WEAVING"), "Dệt cuồng thành vải"));

        machineCapacity.setDyeingStage(createStageInfo("Nhuộm vải", "DYEING", capacityResult.getDyeingDays(),
                DYEING_WAIT_TIME, dyeing.start().toLocalDate(), dyeing.end().toLocalDate(),
                getMachineCapacity("DYEING"), "Nhuộm 5000 sp/ngày"));

        machineCapacity.setCuttingStage(createStageInfo("Cắt vải", "CUTTING", capacityResult.getCuttingDays(),
                CUTTING_WAIT_TIME, cutting.start().toLocalDate(), cutting.end().toLocalDate(),
                getMachineCapacity("CUTTING"), "Cắt vải theo kích thước"));

        machineCapacity.setSewingStage(createStageInfo("May thành phẩm", "SEWING", capacityResult.getSewingDays(),
                SEWING_WAIT_TIME, hemming.start().toLocalDate(), hemming.end().toLocalDate(),
                getMachineCapacity("SEWING"), "May vải thành sản phẩm hoàn chỉnh"));

        machineCapacity.setPackagingStage(createStageInfo("Đóng gói", "PACKAGING", capacityResult.getPackagingDays(),
                PACKAGING_WAIT_TIME, packaging.start().toLocalDate(), packaging.end().toLocalDate(),
                getMachineCapacity("PACKAGING"), "Đóng gói (2 người, 1000 sp/giờ)"));

        BigDecimal totalWaitTime = WARPING_WAIT_TIME
                .add(WEAVING_WAIT_TIME)
                .add(DYEING_WAIT_TIME)
                .add(CUTTING_WAIT_TIME)
                .add(SEWING_WAIT_TIME)
                .add(PACKAGING_WAIT_TIME);
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

    private PlanningTimelineCalculator.StageTimeline findTimeline(
            List<PlanningTimelineCalculator.StageTimeline> timelines, String type) {
        return timelines.stream()
                .filter(t -> type.equalsIgnoreCase(t.stageType()))
                .findFirst()
                .orElseThrow();
    }

    /**
     * Lấy tổng công suất của loại máy từ database
     */
    private BigDecimal getMachineCapacity(String machineType) {
        if ("DYEING".equals(machineType)) {
            // Dyeing: 5000 items/day
            return new BigDecimal("5000");
        } else if ("CUTTING".equals(machineType) || "SEWING".equals(machineType)) {
            // Đối với máy cắt và may, tính công suất theo số lượng sản phẩm
            return machineRepository.findAll().stream()
                    .filter(m -> machineType.equals(m.getType()))
                    // Không lọc theo status để tính tổng công suất nhà máy
                    .map(this::extractCapacityPerDayFromSpecifications)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else if ("PACKAGING".equals(machineType)) {
            return new BigDecimal("8000");
        } else {
            // Đối với máy mắc và dệt, tính công suất theo khối lượng
            return machineRepository.findAll().stream()
                    .filter(m -> machineType.equals(m.getType()))
                    // Không lọc theo status để tính tổng công suất nhà máy
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
            BigDecimal avgCapacity = faceCapacity.add(bathCapacity).add(sportsCapacity).divide(new BigDecimal("3"), 2,
                    RoundingMode.HALF_UP);
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
        BigDecimal faceDays = faceTowels.compareTo(BigDecimal.ZERO) > 0
                ? faceTowels.divide(totalFaceCapacity, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal bathDays = bathTowels.compareTo(BigDecimal.ZERO) > 0
                ? bathTowels.divide(totalBathCapacity, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal sportsDays = sportsTowels.compareTo(BigDecimal.ZERO) > 0
                ? sportsTowels.divide(totalSportsCapacity, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

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

    /**
     * Get bottleneck stage name in Vietnamese based on actual capacity comparison
     */
    private String getBottleneckVietnameseName(BigDecimal bottleneckCapacity) {
        BigDecimal warpingCapacity = sequentialCapacityCalculator.getTotalCapacityPerDay("WARPING");
        BigDecimal weavingCapacity = sequentialCapacityCalculator.getTotalCapacityPerDay("WEAVING");

        // Determine which one is the actual bottleneck
        if (warpingCapacity.compareTo(weavingCapacity) <= 0) {
            return "Mắc cuồng (" + bottleneckCapacity.setScale(0, RoundingMode.HALF_UP) + " kg/ngày)";
        } else {
            return "Dệt vải (" + bottleneckCapacity.setScale(0, RoundingMode.HALF_UP) + " kg/ngày)";
        }
    }
}
