package tmmsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.dto.production_plan.*;
import tmmsystem.entity.*;
import tmmsystem.mapper.ProductionPlanMapper;
import tmmsystem.repository.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductionPlanService {
    
    private final ProductionPlanRepository planRepo;
    private final ProductionPlanStageRepository stageRepo;
    private final ContractRepository contractRepo;
    private final UserRepository userRepo;
    private final MachineRepository machineRepo;
    private final ProductRepository productRepo;
    private final ProductionOrderRepository poRepo;
    private final ProductionOrderDetailRepository podRepo;
    private final BomRepository bomRepo;
    private final BomDetailRepository bomDetailRepo;
    private final MaterialRepository materialRepo;
    @SuppressWarnings("unused")
    private final NotificationService notificationService;
    private final ProductionPlanMapper mapper;
    private final MachineSelectionService machineSelectionService;
    private final ProductionLotRepository lotRepo;
    private final ProductionLotOrderRepository lotOrderRepo;

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
                                ProductionLotOrderRepository lotOrderRepo) {
        this.planRepo = planRepo;
        this.stageRepo = stageRepo;
        this.contractRepo = contractRepo;
        this.userRepo = userRepo;
        this.machineRepo = machineRepo;
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
    }
    
    // ===== LOT & PLAN VERSIONING =====
    @Transactional
    public ProductionLot createOrMergeLotFromContract(Long contractId) {
        Contract contract = contractRepo.findById(contractId).orElseThrow();
        if (!"APPROVED".equals(contract.getStatus())) throw new RuntimeException("Contract must be approved");
        if (contract.getQuotation()==null || contract.getQuotation().getDetails()==null || contract.getQuotation().getDetails().isEmpty()) {
            throw new RuntimeException("Quotation details required to form lot");
        }
        QuotationDetail first = contract.getQuotation().getDetails().get(0);
        Product product = first.getProduct();
        LocalDate delivery = contract.getDeliveryDate();
        LocalDate min = delivery.minusDays(1), max = delivery.plusDays(1);
        ProductionLot lot = lotRepo.findAll().stream()
                .filter(l -> l.getProduct()!=null && l.getProduct().getId().equals(product.getId()))
                .filter(l -> l.getDeliveryDateTarget()!=null && !l.getDeliveryDateTarget().isBefore(min) && !l.getDeliveryDateTarget().isAfter(max))
                .filter(l -> List.of("FORMING","READY_FOR_PLANNING").contains(l.getStatus()))
                .findFirst().orElse(null);
        if (lot==null){
            lot = new ProductionLot();
            lot.setLotCode(generateLotCode());
            lot.setProduct(product);
            lot.setSizeSnapshot(product.getStandardDimensions());
            lot.setDeliveryDateTarget(delivery);
            lot.setContractDateMin(contract.getContractDate());
            lot.setContractDateMax(contract.getContractDate());
            lot.setStatus("FORMING");
            lot.setTotalQuantity(java.math.BigDecimal.ZERO);
            lot = lotRepo.save(lot);
        }
        for (QuotationDetail qd : contract.getQuotation().getDetails()) {
            ProductionLotOrder lo = new ProductionLotOrder();
            lo.setLot(lot); lo.setContract(contract); lo.setQuotationDetail(qd); lo.setAllocatedQuantity(qd.getQuantity());
            lotOrderRepo.save(lo);
            lot.setTotalQuantity(lot.getTotalQuantity().add(qd.getQuantity()));
        }
        lot.setStatus("READY_FOR_PLANNING");
        return lotRepo.save(lot);
    }

    @Transactional
    public ProductionPlan createPlanVersion(Long lotId) {
        ProductionLot lot = lotRepo.findById(lotId).orElseThrow();
        // lock lot for planning so it can't accept more merges
        if (!"PLANNING".equals(lot.getStatus())) {
            lot.setStatus("PLANNING");
            lotRepo.save(lot);
        }
        planRepo.findByLotIdAndCurrentVersionTrue(lotId).forEach(p->{ p.setCurrentVersion(false); p.setStatus(ProductionPlan.PlanStatus.SUPERSEDED); planRepo.save(p); });
        ProductionPlan plan = new ProductionPlan();
        plan.setLot(lot);
        ProductionLotOrder any = lotOrderRepo.findByLotId(lotId).stream().findFirst().orElse(null);
        if (any!=null) plan.setContract(any.getContract());
        plan.setPlanCode(generatePlanCode());
        plan.setCreatedBy(getCurrentUser());
        plan.setVersionNo(nextVersionNumber(lotId));
        plan.setCurrentVersion(true);
        return planRepo.save(plan);
    }

    private int nextVersionNumber(Long lotId){
        return planRepo.findByLotId(lotId).stream().map(p -> p.getVersionNo()==null?1:p.getVersionNo()).max(Integer::compareTo).orElse(0)+1;
    }

    public List<ProductionLot> findLots(String status){ return status==null? lotRepo.findAll(): lotRepo.findByStatus(status); }
    public ProductionLot findLot(Long id){ return lotRepo.findById(id).orElseThrow(); }

    private String generateLotCode(){
        String date = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "LOT-"+date+"-"+String.format("%03d", lotRepo.count()+1);
    }

    // Deprecated old createPlanFromContract -> now uses lot & version
    @Transactional
    public ProductionPlanDto createPlanFromContract(CreateProductionPlanRequest request){
        ProductionLot lot = createOrMergeLotFromContract(request.getContractId());
        ProductionPlan plan = createPlanVersion(lot.getId());
        plan.setApprovalNotes(request.getNotes());
        ProductionPlan saved = planRepo.save(plan);
        notificationService.notifyProductionPlanCreated(saved);
        return mapper.toDto(saved);
    }

    // NEW: create plan directly from an existing lot
    @Transactional
    public ProductionPlanDto createPlanFromLot(Long lotId){
        ProductionPlan plan = createPlanVersion(lotId);
        ProductionPlan saved = planRepo.save(plan);
        notificationService.notifyProductionPlanCreated(saved);
        return mapper.toDto(saved);
    }

    // ===== Approval Workflow (adapted) =====
    @Transactional
    public ProductionPlanDto submitForApproval(Long planId, SubmitForApprovalRequest request){
        ProductionPlan plan = planRepo.findById(planId).orElseThrow();
        if (plan.getStatus()!=ProductionPlan.PlanStatus.DRAFT) throw new RuntimeException("Only draft plans can be submitted");
        plan.setStatus(ProductionPlan.PlanStatus.PENDING_APPROVAL); plan.setApprovalNotes(request.getNotes());
        ProductionPlan saved = planRepo.save(plan); notificationService.notifyProductionPlanSubmittedForApproval(saved); return mapper.toDto(saved);
    }

    @Transactional
    public ProductionPlanDto approvePlan(Long planId, ApproveProductionPlanRequest request){
        ProductionPlan plan = planRepo.findById(planId).orElseThrow();
        if (plan.getStatus()!=ProductionPlan.PlanStatus.PENDING_APPROVAL) throw new RuntimeException("Only pending approval plans can be approved");
        plan.setStatus(ProductionPlan.PlanStatus.APPROVED); plan.setApprovedBy(getCurrentUser()); plan.setApprovedAt(Instant.now()); plan.setApprovalNotes(request.getApprovalNotes());
        ProductionPlan saved = planRepo.save(plan);
        if (saved.getLot()!=null){ saved.getLot().setStatus("PLAN_APPROVED"); lotRepo.save(saved.getLot()); }
        createProductionOrderFromPlan(saved); notificationService.notifyProductionPlanApproved(saved); return mapper.toDto(saved);
    }

    @Transactional
    public ProductionPlanDto rejectPlan(Long planId, RejectProductionPlanRequest request){
        ProductionPlan plan = planRepo.findById(planId).orElseThrow();
        if (plan.getStatus()!=ProductionPlan.PlanStatus.PENDING_APPROVAL) throw new RuntimeException("Only pending approval plans can be rejected");
        plan.setStatus(ProductionPlan.PlanStatus.REJECTED); plan.setApprovedBy(getCurrentUser()); plan.setApprovedAt(Instant.now()); plan.setApprovalNotes(request.getRejectionReason());
        ProductionPlan saved = planRepo.save(plan); notificationService.notifyProductionPlanRejected(saved); return mapper.toDto(saved);
    }
    
    public List<ProductionPlanDto> findAllPlans(){ return planRepo.findAll().stream().map(mapper::toDto).toList(); }

    // ===== Production Order Creation (quantity from lot) =====
    private ProductionOrder createProductionOrderFromPlan(ProductionPlan plan){
        ProductionOrder po = new ProductionOrder();
        po.setPoNumber("PO-"+System.currentTimeMillis());
        po.setContract(plan.getContract());
        po.setStatus("PENDING_APPROVAL");
        po.setNotes("Auto-generated from Production Plan: "+plan.getPlanCode());
        po.setCreatedBy(plan.getCreatedBy());
        java.math.BigDecimal totalQty = plan.getLot()!=null? plan.getLot().getTotalQuantity(): java.math.BigDecimal.ZERO;
        po.setTotalQuantity(totalQty);
        // planned dates placeholder (start today, end +14)
        po.setPlannedStartDate(LocalDate.now());
        po.setPlannedEndDate(LocalDate.now().plusDays(14));
        ProductionOrder savedPO = poRepo.save(po);
        // create order detail lines from lot contracts
        if (plan.getLot()!=null){
            lotOrderRepo.findByLotId(plan.getLot().getId()).forEach(lo -> {
                ProductionOrderDetail pod = new ProductionOrderDetail();
                pod.setProductionOrder(savedPO);
                pod.setProduct(lo.getQuotationDetail().getProduct());
                pod.setQuantity(lo.getAllocatedQuantity());
                pod.setUnit("UNIT");
                pod.setNoteColor(lo.getQuotationDetail().getNoteColor());
                Bom bom = bomRepo.findActiveBomByProductId(lo.getQuotationDetail().getProduct().getId())
                        .orElseGet(() -> bomRepo.findByProductIdOrderByCreatedAtDesc(lo.getQuotationDetail().getProduct().getId()).stream().findFirst().orElse(null));
                if (bom!=null){ pod.setBom(bom); pod.setBomVersion(bom.getVersion()); }
                podRepo.save(pod);
            });
        }
        return savedPO;
    }
    
    /**
     * Public API used by ProductionService: create ProductionOrder from an APPROVED ProductionPlan ID
     */
    @Transactional
    public ProductionOrder createProductionOrderFromApprovedPlan(Long planId) {
        ProductionPlan plan = planRepo.findById(planId).orElseThrow(() -> new RuntimeException("Production plan not found"));
        if (plan.getStatus() != ProductionPlan.PlanStatus.APPROVED) {
            throw new RuntimeException("Production plan must be APPROVED to create Production Order");
        }
        return createProductionOrderFromPlan(plan);
    }

    // ===== Machine Selection (stage.plan) =====
    public List<MachineSelectionService.MachineSuggestionDto> getMachineSuggestionsForStage(String stageType, Long productId, BigDecimal requiredQuantity, LocalDateTime preferredStartTime, LocalDateTime preferredEndTime){
        return machineSelectionService.getSuitableMachines(stageType, productId, requiredQuantity, preferredStartTime, preferredEndTime);
    }

    @Transactional
    public ProductionPlanStageDto autoAssignMachineToStage(Long stageId){
        ProductionPlanStage stage = stageRepo.findById(stageId).orElseThrow();
        Product product = stage.getPlan().getLot()!=null? stage.getPlan().getLot().getProduct(): null;
        java.math.BigDecimal qty = stage.getPlan().getLot()!=null? stage.getPlan().getLot().getTotalQuantity(): java.math.BigDecimal.ZERO;
        List<MachineSelectionService.MachineSuggestionDto> suggestions = machineSelectionService.getSuitableMachines(stage.getStageType(), product!=null?product.getId():null, qty, stage.getPlannedStartTime(), stage.getPlannedEndTime());
        if (suggestions.isEmpty()) throw new RuntimeException("No suitable machines found for stage: "+stage.getStageType());
        MachineSelectionService.MachineSuggestionDto best = suggestions.get(0);
        Machine machine = machineRepo.findById(best.getMachineId()).orElseThrow();
        stage.setAssignedMachine(machine);
        if (best.getSuggestedStartTime()!=null) stage.setPlannedStartTime(best.getSuggestedStartTime());
        if (best.getSuggestedEndTime()!=null) stage.setPlannedEndTime(best.getSuggestedEndTime());
        if (best.getEstimatedDurationHours()!=null) stage.setMinRequiredDurationMinutes(best.getEstimatedDurationHours().multiply(BigDecimal.valueOf(60)).intValue());
        return mapper.toDto(stageRepo.save(stage));
    }

    /**
     * Kiểm tra xung đột lịch trình cho một stage (dựa trên plan.lot sản phẩm/khối lượng)
     */
    public List<String> checkStageScheduleConflicts(Long stageId) {
        ProductionPlanStage stage = stageRepo.findById(stageId)
            .orElseThrow(() -> new RuntimeException("Production plan stage not found"));
        if (stage.getAssignedMachine() == null) { return List.of("No machine assigned to this stage"); }
        Product product = stage.getPlan().getLot()!=null? stage.getPlan().getLot().getProduct(): null;
        java.math.BigDecimal qty = stage.getPlan().getLot()!=null? stage.getPlan().getLot().getTotalQuantity(): java.math.BigDecimal.ZERO;
        List<MachineSelectionService.MachineSuggestionDto> suggestions = machineSelectionService.getSuitableMachines(
                stage.getStageType(), product!=null?product.getId():null, qty, stage.getPlannedStartTime(), stage.getPlannedEndTime());
        MachineSelectionService.MachineSuggestionDto current = suggestions.stream()
                .filter(s -> s.getMachineId()!=null && s.getMachineId().equals(stage.getAssignedMachine().getId()))
                .findFirst().orElse(null);
        if (current != null) return current.getConflicts();
        return List.of("Unable to check conflicts for assigned machine");
    }

    // Helpers
    private String generatePlanCode(){
        String base = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "PP-"+base+"-"+String.format("%03d", planRepo.count()+1);
    }
    private User getCurrentUser(){ return userRepo.findById(1L).orElseThrow(); }

    public ProductionPlanDto findPlanById(Long id){ return mapper.toDto(planRepo.findById(id).orElseThrow(() -> new RuntimeException("Production plan not found"))); }
    public List<ProductionPlanDto> findPlansByStatus(String status){
        try { var st = ProductionPlan.PlanStatus.valueOf(status.toUpperCase()); return planRepo.findByStatus(st).stream().map(mapper::toDto).toList(); }
        catch (IllegalArgumentException e){ throw new RuntimeException("Invalid status: "+status); }
    }
    public List<ProductionPlanDto> findPendingApprovalPlans(){ return planRepo.findPendingApprovalPlans().stream().map(mapper::toDto).toList(); }
    public List<ProductionPlanDto> findPlansByContract(Long contractId){ return planRepo.findByContractId(contractId).stream().map(mapper::toDto).toList(); }
    public List<ProductionPlanDto> findPlansByCreator(Long userId){ return planRepo.findByCreatedById(userId).stream().map(mapper::toDto).toList(); }
    public List<ProductionPlanDto> findApprovedPlansNotConverted(){ return planRepo.findApprovedPlansNotConverted().stream().map(mapper::toDto).toList(); }
    public ProductionPlanStage findStageById(Long stageId){ return stageRepo.findById(stageId).orElseThrow(() -> new RuntimeException("Production plan stage not found")); }
    @Transactional public ProductionPlanStageDto assignInChargeUser(Long stageId, Long userId){ var stage = stageRepo.findById(stageId).orElseThrow(() -> new RuntimeException("Production plan stage not found")); var user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found")); stage.setInChargeUser(user); return mapper.toDto(stageRepo.save(stage)); }
    @Transactional public ProductionPlanStageDto assignQcUser(Long stageId, Long userId){ var stage = stageRepo.findById(stageId).orElseThrow(() -> new RuntimeException("Production plan stage not found")); var user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found")); stage.setQcUser(user); return mapper.toDto(stageRepo.save(stage)); }
    @Transactional public ProductionPlanStageDto updateStage(Long stageId, ProductionPlanStageRequest req){ var stage = stageRepo.findById(stageId).orElseThrow(() -> new RuntimeException("Production plan stage not found"));
        if (req.getAssignedMachineId()!=null){ stage.setAssignedMachine(machineRepo.findById(req.getAssignedMachineId()).orElseThrow(() -> new RuntimeException("Machine not found"))); }
        if (req.getInChargeUserId()!=null){ stage.setInChargeUser(userRepo.findById(req.getInChargeUserId()).orElseThrow(() -> new RuntimeException("User not found"))); }
        if (req.getQcUserId()!=null){ stage.setQcUser(userRepo.findById(req.getQcUserId()).orElseThrow(() -> new RuntimeException("User not found"))); }
        if (req.getPlannedStartTime()!=null){ stage.setPlannedStartTime(req.getPlannedStartTime()); }
        if (req.getPlannedEndTime()!=null){ stage.setPlannedEndTime(req.getPlannedEndTime()); }
        if (req.getMinRequiredDurationMinutes()!=null){ stage.setMinRequiredDurationMinutes(req.getMinRequiredDurationMinutes()); }
        if (req.getTransferBatchQuantity()!=null){ stage.setTransferBatchQuantity(req.getTransferBatchQuantity()); }
        if (req.getCapacityPerHour()!=null){ stage.setCapacityPerHour(req.getCapacityPerHour()); }
        if (req.getNotes()!=null){ stage.setNotes(req.getNotes()); }
        return mapper.toDto(stageRepo.save(stage)); }

    public java.util.List<ProductionPlanStageDto> listStagesOfPlan(Long planId){
        return stageRepo.findByPlanIdOrderBySequenceNo(planId).stream().map(mapper::toDto).toList();
    }
}
