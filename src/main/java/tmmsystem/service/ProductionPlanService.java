package tmmsystem.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.dto.production_plan.*;
import tmmsystem.entity.*;
import tmmsystem.mapper.ProductionPlanMapper;
import tmmsystem.repository.*;
import tmmsystem.service.timeline.SequentialCapacityCalculator;
import tmmsystem.service.timeline.SequentialCapacityResult;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProductionPlanService {

    private final ProductionPlanRepository planRepo;
    private final ProductionPlanStageRepository stageRepo;
    private final ContractRepository contractRepo;
    private final UserRepository userRepo;
    // private final MachineRepository machineRepo; // REMOVED: Unused
    private final ProductRepository productRepo;
    private final ProductionOrderRepository poRepo;
    private final ProductionOrderDetailRepository podRepo;
    private final BomRepository bomRepo;
    private final BomDetailRepository bomDetailRepo;
    private final MaterialRepository materialRepo;
    @SuppressWarnings("unused")
    private final NotificationService notificationService;
    public final ProductionPlanMapper mapper;
    private final MachineSelectionService machineSelectionService;
    private final ProductionLotRepository lotRepo;
    private final ProductionLotOrderRepository lotOrderRepo;
    private final PlanningTimelineCalculator timelineCalculator;
    private final SequentialCapacityCalculator sequentialCapacityCalculator;
    private final MachineAssignmentRepository machineAssignmentRepository;
    private final ProductionService productionService;

    @Value("${planning.autoInitStages:true}")
    private boolean autoInitStages;

    public ProductionPlanService(ProductionPlanRepository planRepo,
            ProductionPlanStageRepository stageRepo,
            ContractRepository contractRepo,
            UserRepository userRepo,
            MachineRepository machineRepo,
            ProductRepository productRepo,
            ProductionOrderRepository poRepo,
            ProductionOrderDetailRepository podRepo,
            BomRepository bomRepo,
            BomDetailRepository bomDetailRepo,
            MaterialRepository materialRepo,
            NotificationService notificationService,
            ProductionPlanMapper mapper,
            MachineSelectionService machineSelectionService,
            ProductionLotRepository lotRepo,
            ProductionLotOrderRepository lotOrderRepo,
            MachineAssignmentRepository machineAssignmentRepository,
            PlanningTimelineCalculator timelineCalculator,
            SequentialCapacityCalculator sequentialCapacityCalculator,
            @Lazy ProductionService productionService) {
        this.planRepo = planRepo;
        this.stageRepo = stageRepo;
        this.contractRepo = contractRepo;
        this.userRepo = userRepo;
        // this.machineRepo = machineRepo;
        this.productRepo = productRepo;
        this.poRepo = poRepo;
        this.podRepo = podRepo;
        this.bomRepo = bomRepo;
        this.bomDetailRepo = bomDetailRepo;
        this.materialRepo = materialRepo;
        this.notificationService = notificationService;
        this.mapper = mapper;
        this.machineSelectionService = machineSelectionService;
        this.lotRepo = lotRepo;
        this.lotOrderRepo = lotOrderRepo;
        this.machineAssignmentRepository = machineAssignmentRepository;
        this.timelineCalculator = timelineCalculator;
        this.sequentialCapacityCalculator = sequentialCapacityCalculator;
        this.productionService = productionService;
    }

    // ===== LOT & PLAN VERSIONING =====
    @Transactional
    public ProductionLot createOrMergeLotFromContract(Long contractId) {
        Contract contract = contractRepo.findById(contractId).orElseThrow();
        if (!"APPROVED".equals(contract.getStatus()))
            throw new RuntimeException("Contract must be approved");
        if (contract.getQuotation() == null || contract.getQuotation().getDetails() == null
                || contract.getQuotation().getDetails().isEmpty()) {
            throw new RuntimeException("Quotation details required to form lot");
        }
        // Group by productId to ensure 1 lot = 1 product
        Map<Long, List<QuotationDetail>> byProduct = contract.getQuotation().getDetails()
                .stream().collect(Collectors.groupingBy(d -> d.getProduct().getId()));
        ProductionLot lastLot = null;
        for (var e : byProduct.entrySet()) {
            lastLot = createOrMergeLotFromContractAndProduct(contract, e.getKey(), e.getValue());
        }
        return lastLot;
    }

    // NEW: explicit per-product merge variant
    @Transactional
    public ProductionLot createOrMergeLotFromContractAndProduct(Contract contract, Long productId,
            List<QuotationDetail> detailsOfProduct) {
        Product product = productRepo.findById(productId).orElseThrow();
        LocalDate delivery = contract.getDeliveryDate();
        LocalDate contractDate = contract.getContractDate();
        LocalDate deliveryMin = delivery.minusDays(1), deliveryMax = delivery.plusDays(1);
        LocalDate contractMin = contractDate.minusDays(1), contractMax = contractDate.plusDays(1);
        ProductionLot lot = lotRepo.findAll().stream()
                .filter(l -> l.getProduct() != null && l.getProduct().getId().equals(productId))
                .filter(l -> List.of("FORMING", "READY_FOR_PLANNING").contains(l.getStatus()))
                .filter(l -> l.getDeliveryDateTarget() != null && !l.getDeliveryDateTarget().isBefore(deliveryMin)
                        && !l.getDeliveryDateTarget().isAfter(deliveryMax))
                .filter(l -> l.getContractDateMin() != null && l.getContractDateMax() != null
                        && !(l.getContractDateMax().isBefore(contractMin)
                                || l.getContractDateMin().isAfter(contractMax)))
                .findFirst().orElse(null);
        if (lot == null) {
            lot = new ProductionLot();
            lot.setLotCode(generateLotCode());
            lot.setProduct(product);
            lot.setSizeSnapshot(product.getStandardDimensions());
            lot.setDeliveryDateTarget(delivery);
            lot.setContractDateMin(contractDate);
            lot.setContractDateMax(contractDate);
            lot.setStatus("FORMING");
            lot.setTotalQuantity(java.math.BigDecimal.ZERO);
            lot = lotRepo.save(lot);
        }
        for (QuotationDetail qd : detailsOfProduct) {
            if (!qd.getProduct().getId().equals(productId))
                continue; // safety
            ProductionLotOrder lo = new ProductionLotOrder();
            lo.setLot(lot);
            lo.setContract(contract);
            lo.setQuotationDetail(qd);
            lo.setAllocatedQuantity(qd.getQuantity());
            lotOrderRepo.save(lo);
            lot.setTotalQuantity(lot.getTotalQuantity().add(qd.getQuantity()));
        }
        if (contract.getContractDate().isBefore(lot.getContractDateMin()))
            lot.setContractDateMin(contract.getContractDate());
        if (contract.getContractDate().isAfter(lot.getContractDateMax()))
            lot.setContractDateMax(contract.getContractDate());
        lot.setStatus("READY_FOR_PLANNING");
        return lotRepo.save(lot);
    }

    // Helper for job
    @Transactional
    public ProductionLot createOrMergeLotFromContractAndProduct(Long contractId, Long productId) {
        Contract contract = contractRepo.findById(contractId).orElseThrow();
        if (contract.getQuotation() == null || contract.getQuotation().getDetails() == null)
            throw new RuntimeException("No quotation details");
        List<QuotationDetail> details = contract.getQuotation().getDetails().stream()
                .filter(d -> d.getProduct() != null && d.getProduct().getId().equals(productId))
                .collect(Collectors.toList());
        if (details.isEmpty())
            throw new RuntimeException("No details for product in contract");
        return createOrMergeLotFromContractAndProduct(contract, productId, details);
    }

    @Transactional
    public ProductionPlan createPlanVersion(Long lotId) {
        ProductionLot lot = lotRepo.findById(lotId).orElseThrow();
        // lock lot for planning so it can't accept more merges
        if (!"PLANNING".equals(lot.getStatus())) {
            lot.setStatus("PLANNING");
            lotRepo.save(lot);
        }
        planRepo.findByLotIdAndCurrentVersionTrue(lotId).forEach(p -> {
            p.setCurrentVersion(false);
            p.setStatus(ProductionPlan.PlanStatus.SUPERSEDED);
            planRepo.save(p);
            releasePlanReservations(p.getId());
        });
        ProductionPlan plan = new ProductionPlan();
        plan.setLot(lot);
        ProductionLotOrder any = lotOrderRepo.findByLotId(lotId).stream().findFirst().orElse(null);
        if (any != null)
            plan.setContract(any.getContract());
        plan.setPlanCode(generatePlanCode());
        plan.setCreatedBy(getCurrentUser());
        plan.setVersionNo(nextVersionNumber(lotId));
        plan.setCurrentVersion(true);
        return planRepo.save(plan);
    }

    private int nextVersionNumber(Long lotId) {
        return planRepo.findByLotId(lotId).stream().map(p -> p.getVersionNo() == null ? 1 : p.getVersionNo())
                .max(Integer::compareTo).orElse(0) + 1;
    }

    public List<ProductionLot> findLots(String status) {
        return status == null ? lotRepo.findAll() : lotRepo.findByStatus(status);
    }

    public ProductionLot findLot(Long id) {
        return lotRepo.findById(id).orElseThrow();
    }

    private String generateLotCode() {
        String date = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "LOT-" + date + "-" + String.format("%03d", lotRepo.count() + 1);
    }

    // Deprecated old createPlanFromContract -> now uses lot & version
    @Transactional
    public ProductionPlanDto createPlanFromContract(CreateProductionPlanRequest request) {
        ProductionLot lot = createOrMergeLotFromContract(request.getContractId());
        ProductionPlan plan = createPlanVersion(lot.getId());
        plan.setApprovalNotes(request.getNotes());
        ProductionPlan saved = planRepo.save(plan);
        if (autoInitStages) {
            initDefaultStages(saved);
        }
        applyTimelineToPlan(saved);
        notificationService.notifyProductionPlanCreated(saved);
        return mapper.toDto(saved);
    }

    // NEW: create plan directly from an existing lot
    @Transactional
    public ProductionPlanDto createPlanFromLot(Long lotId) {
        ProductionPlan plan = createPlanVersion(lotId);
        ProductionPlan saved = planRepo.save(plan);
        if (autoInitStages) {
            initDefaultStages(saved);
        }
        applyTimelineToPlan(saved);
        notificationService.notifyProductionPlanCreated(saved);
        return mapper.toDto(saved);
    }

    private void initDefaultStages(ProductionPlan plan) {
        // Nếu đã có stage thì bỏ qua
        if (!stageRepo.findByPlanIdOrderBySequenceNo(plan.getId()).isEmpty())
            return;
        String[] types = new String[] { "WARPING", "WEAVING", "DYEING", "CUTTING", "HEMMING", "PACKAGING" };
        java.time.LocalDateTime start = java.time.LocalDateTime.now().withHour(8).withMinute(0).withSecond(0)
                .withNano(0);
        for (int i = 0; i < types.length; i++) {
            ProductionPlanStage s = new ProductionPlanStage();
            s.setPlan(plan);
            s.setStageType(types[i]);
            s.setSequenceNo(i + 1);
            s.setPlannedStartTime(start.plusHours(i * 4L));
            s.setPlannedEndTime(start.plusHours((i + 1) * 4L));
            s.setStageStatus("PENDING");
            stageRepo.save(s);
        }
    }

    // ===== Approval Workflow (adapted) =====
    @Transactional
    public ProductionPlanDto submitForApproval(Long planId, SubmitForApprovalRequest request) {
        ProductionPlan plan = planRepo.findById(planId).orElseThrow();
        if (plan.getStatus() != ProductionPlan.PlanStatus.DRAFT &&
                plan.getStatus() != ProductionPlan.PlanStatus.REJECTED)
            throw new RuntimeException("Only draft or rejected plans can be submitted");
        plan.setStatus(ProductionPlan.PlanStatus.PENDING_APPROVAL);
        plan.setApprovalNotes(request.getNotes());
        ProductionPlan saved = planRepo.save(plan);
        notificationService.notifyProductionPlanSubmittedForApproval(saved);
        return mapper.toDto(saved);
    }

    @Transactional
    public ProductionPlanDto approvePlan(Long planId, ApproveProductionPlanRequest request) {
        ProductionPlan plan = planRepo.findById(planId).orElseThrow();
        if (plan.getStatus() != ProductionPlan.PlanStatus.PENDING_APPROVAL)
            throw new RuntimeException("Only pending approval plans can be approved");
        plan.setStatus(ProductionPlan.PlanStatus.APPROVED);
        plan.setApprovedBy(getCurrentUser());
        plan.setApprovedAt(Instant.now());
        plan.setApprovalNotes(request.getApprovalNotes());
        ProductionPlan saved = planRepo.save(plan);
        List<ProductionPlanStage> planStages = stageRepo.findByPlanIdOrderBySequenceNo(planId);
        if (saved.getLot() != null) {
            saved.getLot().setStatus("PLAN_APPROVED");
            lotRepo.save(saved.getLot());
        }
        ProductionOrder po = createProductionOrderFromPlan(saved);

        // Auto-create ProductionStages directly from planning stages (NEW: không qua
        // WorkOrder)
        // Auto-create ProductionStages directly from planning stages
        productionService.createStagesFromPlan(po.getId(), planStages);
        if (!planStages.isEmpty()) {
            planStages.forEach(stage -> stage.setStageStatus("RELEASED"));
            stageRepo.saveAll(planStages);
        }
        // reservePlanStages(saved, stageMapping); // REMOVED: No machine reservation in
        // planning
        notificationService.notifyRole("PRODUCTION_MANAGER", "PRODUCTION", "INFO",
                "Nhận kế hoạch sản xuất đã duyệt",
                "PO #" + po.getPoNumber() + " đã được kích hoạt từ kế hoạch " + saved.getPlanCode()
                        + ". Vui lòng điều phối thực hiện.",
                "PRODUCTION_ORDER", po.getId());
        notificationService.notifyRole("TECHNICAL_STAFF", "PRODUCTION", "INFO",
                "Chuẩn bị hướng dẫn kỹ thuật",
                "PO #" + po.getPoNumber()
                        + " đã chuyển sang sản xuất, cần rà soát sheet kỹ thuật và hỗ trợ tổ trưởng.",
                "PRODUCTION_ORDER", po.getId());
        notificationService.notifyRole("QC_STAFF", "QC", "INFO",
                "Lịch kiểm tra mới từ kế hoạch " + saved.getPlanCode(),
                "Các công đoạn sẽ lần lượt hoàn tất theo timeline vừa duyệt. Theo dõi bảng phân công để chuẩn bị.",
                "PRODUCTION_PLAN", saved.getId());

        notificationService.notifyProductionPlanApproved(saved);
        return mapper.toDto(saved);
    }

    @Transactional
    public ProductionPlanDto rejectPlan(Long planId, RejectProductionPlanRequest request) {
        ProductionPlan plan = planRepo.findById(planId).orElseThrow();
        if (plan.getStatus() != ProductionPlan.PlanStatus.PENDING_APPROVAL)
            throw new RuntimeException("Only pending approval plans can be rejected");
        plan.setStatus(ProductionPlan.PlanStatus.REJECTED);
        plan.setApprovedBy(getCurrentUser());
        plan.setApprovedAt(Instant.now());
        plan.setApprovalNotes(request.getRejectionReason());
        ProductionPlan saved = planRepo.save(plan);
        releasePlanReservations(saved.getId());
        notificationService.notifyProductionPlanRejected(saved);
        return mapper.toDto(saved);
    }

    public List<ProductionPlanDto> findAllPlans() {
        return planRepo.findAll().stream().map(mapper::toDto).toList();
    }

    // ===== Production Order Creation (quantity from lot) =====
    private ProductionOrder createProductionOrderFromPlan(ProductionPlan plan) {
        ProductionOrder po = new ProductionOrder();
        po.setPoNumber("PO-" + System.currentTimeMillis());
        po.setContract(plan.getContract());
        // Sau khi director approve plan, ProductionOrder có status "WAITING_PRODUCTION"
        // (chờ sản xuất)
        po.setStatus("WAITING_PRODUCTION");
        po.setExecutionStatus("WAITING_PRODUCTION");
        po.setNotes("Auto-generated from Production Plan: " + plan.getPlanCode());
        po.setCreatedBy(plan.getCreatedBy());
        java.math.BigDecimal totalQty = plan.getLot() != null ? plan.getLot().getTotalQuantity()
                : java.math.BigDecimal.ZERO;
        po.setTotalQuantity(totalQty);
        // planned dates placeholder (start today, end +14)
        po.setPlannedStartDate(LocalDate.now());
        po.setPlannedEndDate(LocalDate.now().plusDays(14));
        ProductionOrder savedPO = poRepo.save(po);
        // create order detail lines from lot contracts
        if (plan.getLot() != null) {
            java.util.Set<Long> contractIds = new java.util.HashSet<>();
            lotOrderRepo.findByLotId(plan.getLot().getId()).forEach(lo -> {
                if (lo.getContract() != null)
                    contractIds.add(lo.getContract().getId());
                // Validate required fields để tránh null pointer
                if (lo.getQuotationDetail() == null || lo.getQuotationDetail().getProduct() == null) {
                    throw new RuntimeException("QuotationDetail or Product is null for LotOrder: " + lo.getId());
                }
                if (lo.getAllocatedQuantity() == null) {
                    throw new RuntimeException("AllocatedQuantity is null for LotOrder: " + lo.getId());
                }
                ProductionOrderDetail pod = new ProductionOrderDetail();
                pod.setProductionOrder(savedPO);
                pod.setProduct(lo.getQuotationDetail().getProduct());
                pod.setQuantity(lo.getAllocatedQuantity());
                pod.setUnit("UNIT");
                pod.setNoteColor(lo.getQuotationDetail().getNoteColor());
                Bom bom = bomRepo.findActiveBomByProductId(lo.getQuotationDetail().getProduct().getId())
                        .orElseGet(() -> bomRepo
                                .findByProductIdOrderByCreatedAtDesc(lo.getQuotationDetail().getProduct().getId())
                                .stream().findFirst().orElse(null));
                if (bom != null) {
                    pod.setBom(bom);
                    pod.setBomVersion(bom.getVersion());
                }
                podRepo.save(pod);
            });
            savedPO.setContractIds(contractIds.toString()); // Simple list to string [1, 2]
            poRepo.save(savedPO);
        }
        return savedPO;
    }

    /**
     * Public API used by ProductionService: create ProductionOrder from an APPROVED
     * ProductionPlan ID
     */
    @Transactional
    public ProductionOrder createProductionOrderFromApprovedPlan(Long planId) {
        ProductionPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> new RuntimeException("Production plan not found"));
        if (plan.getStatus() != ProductionPlan.PlanStatus.APPROVED) {
            throw new RuntimeException("Production plan must be APPROVED to create Production Order");
        }
        return createProductionOrderFromPlan(plan);
    }

    // ===== Machine Selection (stage.plan) =====
    public List<MachineSelectionService.MachineSuggestionDto> getMachineSuggestionsForStage(String stageType,
            Long productId, BigDecimal requiredQuantity, LocalDateTime preferredStartTime,
            LocalDateTime preferredEndTime) {
        return machineSelectionService.getSuitableMachines(stageType, productId, requiredQuantity, preferredStartTime,
                preferredEndTime);
    }

    @Transactional
    public ProductionPlanStageDto autoAssignMachineToStage(Long stageId) {
        // DEPRECATED: No machine assignment in planning
        throw new UnsupportedOperationException("Machine assignment is no longer supported in planning phase");
    }

    /**
     * Kiểm tra xung đột lịch trình cho một stage (dựa trên plan.lot sản phẩm/khối
     * lượng)
     */
    public List<String> checkStageScheduleConflicts(Long stageId) {
        // DEPRECATED: No machine assignment in planning
        return List.of();
    }

    // Helpers
    private String generatePlanCode() {
        String base = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "PP-" + base + "-" + String.format("%03d", planRepo.count() + 1);
    }

    private User getCurrentUser() {
        return userRepo.findById(1L).orElseThrow();
    }

    public ProductionPlanDto findPlanById(Long id) {
        return mapper.toDto(planRepo.findById(id).orElseThrow(() -> new RuntimeException("Production plan not found")));
    }

    public List<ProductionPlanDto> findPlansByStatus(String status) {
        try {
            var st = ProductionPlan.PlanStatus.valueOf(status.toUpperCase());
            return planRepo.findByStatus(st).stream().map(mapper::toDto).toList();
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + status);
        }
    }

    public List<ProductionPlanDto> findPendingApprovalPlans() {
        return planRepo.findPendingApprovalPlans().stream().map(mapper::toDto).toList();
    }

    public List<ProductionPlanDto> findPlansByContract(Long contractId) {
        return planRepo.findByContractId(contractId).stream().map(mapper::toDto).toList();
    }

    public List<ProductionPlanDto> findPlansByCreator(Long userId) {
        return planRepo.findByCreatedById(userId).stream().map(mapper::toDto).toList();
    }

    public List<ProductionPlanDto> findApprovedPlansNotConverted() {
        return planRepo.findApprovedPlansNotConverted().stream().map(mapper::toDto).toList();
    }

    public ProductionPlanStage findStageById(Long stageId) {
        return stageRepo.findById(stageId).orElseThrow(() -> new RuntimeException("Production plan stage not found"));
    }

    @Transactional
    public ProductionPlanStageDto assignInChargeUser(Long stageId, Long userId) {
        var stage = stageRepo.findById(stageId)
                .orElseThrow(() -> new RuntimeException("Production plan stage not found"));
        var user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        stage.setInChargeUser(user);
        return mapper.toDto(stageRepo.save(stage));
    }

    @Transactional
    public ProductionPlanStageDto assignQcUser(Long stageId, Long userId) {
        var stage = stageRepo.findById(stageId)
                .orElseThrow(() -> new RuntimeException("Production plan stage not found"));
        var user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        stage.setQcUser(user);
        return mapper.toDto(stageRepo.save(stage));
    }

    @Transactional
    public ProductionPlanStageDto updateStage(Long stageId, ProductionPlanStageRequest req) {
        var stage = stageRepo.findById(stageId)
                .orElseThrow(() -> new RuntimeException("Production plan stage not found"));
        if (req.getAssignedMachineId() != null) {
            // IGNORE: Machine assignment not supported
            // stage.setAssignedMachine(machineRepo.findById(req.getAssignedMachineId())
            // .orElseThrow(() -> new RuntimeException("Machine not found")));
        }
        if (req.getInChargeUserId() != null) {
            stage.setInChargeUser(userRepo.findById(req.getInChargeUserId())
                    .orElseThrow(() -> new RuntimeException("User not found")));
        }
        if (req.getQcUserId() != null) {
            stage.setQcUser(
                    userRepo.findById(req.getQcUserId()).orElseThrow(() -> new RuntimeException("User not found")));
        }
        if (req.getPlannedStartTime() != null) {
            stage.setPlannedStartTime(req.getPlannedStartTime());
        }
        if (req.getPlannedEndTime() != null) {
            stage.setPlannedEndTime(req.getPlannedEndTime());
        }
        if (req.getMinRequiredDurationMinutes() != null) {
            stage.setMinRequiredDurationMinutes(req.getMinRequiredDurationMinutes());
        }
        if (req.getTransferBatchQuantity() != null) {
            stage.setTransferBatchQuantity(req.getTransferBatchQuantity());
        }
        if (req.getCapacityPerHour() != null) {
            stage.setCapacityPerHour(req.getCapacityPerHour());
        }
        if (req.getNotes() != null) {
            stage.setNotes(req.getNotes());
        }
        return mapper.toDto(stageRepo.save(stage));
    }

    public java.util.List<ProductionPlanStageDto> listStagesOfPlan(Long planId) {
        return stageRepo.findByPlanIdOrderBySequenceNo(planId).stream().map(mapper::toDto).toList();
    }

    @Transactional
    public ProductionPlan calculateAndSetPlanDates(Long planId) {
        ProductionPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> new RuntimeException("Production plan not found"));
        List<ProductionPlanStage> stages = stageRepo.findByPlanIdOrderBySequenceNo(planId);

        if (stages.isEmpty()) {
            // Cannot calculate dates if there are no stages
            return plan;
        }

        LocalDateTime overallStartTime = stages.stream()
                .map(ProductionPlanStage::getPlannedStartTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        LocalDateTime overallEndTime = stages.stream()
                .map(ProductionPlanStage::getPlannedEndTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        plan.setProposedStartDate(overallStartTime != null ? overallStartTime.toLocalDate() : null);
        plan.setProposedEndDate(overallEndTime != null ? overallEndTime.toLocalDate() : null);

        return planRepo.save(plan);
    }

    public ProductionPlanDto calculateScheduleAndReturnDto(Long planId) {
        ProductionPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> new RuntimeException("Production plan not found"));
        applyTimelineToPlan(plan);
        ProductionPlan updatedPlan = calculateAndSetPlanDates(plan.getId());
        return mapper.toDto(updatedPlan);
    }

    // private void reservePlanStages(ProductionPlan plan, Map<Long,
    // List<ProductionStage>> stageMapping) {
    // // REMOVED: No machine reservation in planning
    // }

    private void releasePlanReservations(Long planId) {
        if (planId == null)
            return;
        machineAssignmentRepository.deleteByPlanStagePlanId(planId);
    }

    // private Instant toInstant(LocalDateTime value) {
    // if (value == null)
    // return null;
    // return value.atZone(java.time.ZoneId.systemDefault()).toInstant();
    // }

    private void applyTimelineToPlan(ProductionPlan plan) {
        if (plan == null || plan.getLot() == null || plan.getLot().getProduct() == null) {
            return;
        }
        ProductionLot lot = plan.getLot();
        BigDecimal quantity = lot.getTotalQuantity() != null ? lot.getTotalQuantity() : BigDecimal.ZERO;
        BigDecimal unitWeight = lot.getProduct().getStandardWeight() != null
                ? lot.getProduct().getStandardWeight().divide(new BigDecimal("1000"), 4, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ONE;
        BigDecimal totalWeight = unitWeight.multiply(quantity);

        QuantityBreakdown breakdown = classifyQuantity(lot.getProduct().getName(), quantity);
        SequentialCapacityResult result = sequentialCapacityCalculator.calculate(totalWeight, breakdown.face(),
                breakdown.bath(), breakdown.sport());
        java.time.LocalDate startDate = lot.getContractDateMin() != null ? lot.getContractDateMin()
                : java.time.LocalDate.now();
        var timelines = timelineCalculator.buildTimeline(startDate, result);

        var stages = stageRepo.findByPlanIdOrderBySequenceNo(plan.getId());
        for (ProductionPlanStage stage : stages) {
            var timeline = timelines.stream()
                    .filter(t -> stage.getStageType().equalsIgnoreCase(t.stageType()))
                    .findFirst().orElse(null);
            if (timeline == null)
                continue;
            stage.setPlannedStartTime(timeline.start());
            stage.setPlannedEndTime(timeline.end());
            stageRepo.save(stage);
        }

        if (!timelines.isEmpty()) {
            plan.setProposedStartDate(timelines.get(0).start().toLocalDate());
            plan.setProposedEndDate(timelines.get(timelines.size() - 1).end().toLocalDate());
        }
        planRepo.save(plan);
    }

    private record QuantityBreakdown(BigDecimal face, BigDecimal bath, BigDecimal sport) {
    }

    private QuantityBreakdown classifyQuantity(String productName, BigDecimal quantity) {
        String name = productName != null ? productName.toLowerCase() : "";
        BigDecimal face = BigDecimal.ZERO;
        BigDecimal bath = BigDecimal.ZERO;
        BigDecimal sport = BigDecimal.ZERO;
        if (name.contains("khăn mặt")) {
            face = quantity;
        } else if (name.contains("khăn tắm")) {
            bath = quantity;
        } else {
            sport = quantity;
        }
        return new QuantityBreakdown(face, bath, sport);
    }
}
