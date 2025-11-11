package tmmsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.entity.*;
import tmmsystem.repository.*;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;

@Service
public class ProductionService {
    private final ProductionOrderRepository poRepo;
    private final ProductionOrderDetailRepository podRepo;
    private final TechnicalSheetRepository techRepo;
    private final WorkOrderRepository woRepo;
    private final WorkOrderDetailRepository wodRepo;
    private final ProductionStageRepository stageRepo;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    @SuppressWarnings("unused")
    private final ContractRepository contractRepository;
    @SuppressWarnings("unused")
    private final BomRepository bomRepository;
    @SuppressWarnings("unused")
    private final ProductionOrderDetailRepository productionOrderDetailRepository;
    private final ProductionPlanService productionPlanService;
    private final StageTrackingRepository stageTrackingRepository;
    private final StagePauseLogRepository stagePauseLogRepository;

    public ProductionService(ProductionOrderRepository poRepo,
                             ProductionOrderDetailRepository podRepo,
                             TechnicalSheetRepository techRepo,
                             WorkOrderRepository woRepo,
                             WorkOrderDetailRepository wodRepo,
                             ProductionStageRepository stageRepo,
                             UserRepository userRepository,
                             NotificationService notificationService,
                             ContractRepository contractRepository,
                             BomRepository bomRepository,
                             ProductionOrderDetailRepository productionOrderDetailRepository,
                             ProductionPlanService productionPlanService,
                             StageTrackingRepository stageTrackingRepository,
                             StagePauseLogRepository stagePauseLogRepository) {
        this.poRepo = poRepo; this.podRepo = podRepo; this.techRepo = techRepo;
        this.woRepo = woRepo; this.wodRepo = wodRepo; this.stageRepo = stageRepo;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.contractRepository = contractRepository;
        this.bomRepository = bomRepository;
        this.productionOrderDetailRepository = productionOrderDetailRepository;
        this.productionPlanService = productionPlanService;
        this.stageTrackingRepository = stageTrackingRepository;
        this.stagePauseLogRepository = stagePauseLogRepository;
    }

    // Production Order
    public List<ProductionOrder> findAllPO() { return poRepo.findAll(); }
    public ProductionOrder findPO(Long id) { return poRepo.findById(id).orElseThrow(); }
    @Transactional public ProductionOrder createPO(ProductionOrder po) { return poRepo.save(po); }
    @Transactional public ProductionOrder updatePO(Long id, ProductionOrder upd) {
        ProductionOrder e = poRepo.findById(id).orElseThrow();
        e.setPoNumber(upd.getPoNumber()); e.setContract(upd.getContract()); e.setTotalQuantity(upd.getTotalQuantity());
        e.setPlannedStartDate(upd.getPlannedStartDate()); e.setPlannedEndDate(upd.getPlannedEndDate());
        e.setStatus(upd.getStatus()); e.setPriority(upd.getPriority()); e.setNotes(upd.getNotes());
        e.setCreatedBy(upd.getCreatedBy()); e.setApprovedBy(upd.getApprovedBy()); e.setApprovedAt(upd.getApprovedAt());
        return e;
    }
    public void deletePO(Long id) { poRepo.deleteById(id); }

    // PO Detail
    public List<ProductionOrderDetail> findPODetails(Long poId) { return podRepo.findByProductionOrderId(poId); }
    public ProductionOrderDetail findPODetail(Long id) { return podRepo.findById(id).orElseThrow(); }
    @Transactional public ProductionOrderDetail createPODetail(ProductionOrderDetail d) { return podRepo.save(d); }
    @Transactional public ProductionOrderDetail updatePODetail(Long id, ProductionOrderDetail upd) {
        ProductionOrderDetail e = podRepo.findById(id).orElseThrow();
        e.setProductionOrder(upd.getProductionOrder()); e.setProduct(upd.getProduct()); e.setBom(upd.getBom());
        e.setBomVersion(upd.getBomVersion()); e.setQuantity(upd.getQuantity()); e.setUnit(upd.getUnit()); e.setNoteColor(upd.getNoteColor());
        return e;
    }
    public void deletePODetail(Long id) { podRepo.deleteById(id); }

    // Technical Sheet
    public TechnicalSheet findTechSheet(Long id) { return techRepo.findById(id).orElseThrow(); }
    @Transactional public TechnicalSheet createTechSheet(TechnicalSheet t) { return techRepo.save(t); }
    @Transactional public TechnicalSheet updateTechSheet(Long id, TechnicalSheet upd) {
        TechnicalSheet e = techRepo.findById(id).orElseThrow();
        e.setProductionOrder(upd.getProductionOrder()); e.setSheetNumber(upd.getSheetNumber());
        e.setYarnSpecifications(upd.getYarnSpecifications()); e.setMachineSettings(upd.getMachineSettings());
        e.setQualityStandards(upd.getQualityStandards()); e.setSpecialInstructions(upd.getSpecialInstructions());
        e.setCreatedBy(upd.getCreatedBy()); e.setApprovedBy(upd.getApprovedBy());
        return e;
    }
    public void deleteTechSheet(Long id) { techRepo.deleteById(id); }

    // Work Order
    public List<WorkOrder> findWOs(Long poId) { return woRepo.findByProductionOrderId(poId); }
    public WorkOrder findWO(Long id) { return woRepo.findById(id).orElseThrow(); }
    @Transactional public WorkOrder createWO(WorkOrder w) { return woRepo.save(w); }
    @Transactional public WorkOrder updateWO(Long id, WorkOrder upd) {
        WorkOrder e = woRepo.findById(id).orElseThrow();
        e.setProductionOrder(upd.getProductionOrder()); e.setWoNumber(upd.getWoNumber()); e.setDeadline(upd.getDeadline());
        e.setStatus(upd.getStatus()); e.setSendStatus(upd.getSendStatus()); e.setProduction(upd.getProduction());
        e.setCreatedBy(upd.getCreatedBy()); e.setApprovedBy(upd.getApprovedBy());
        return e;
    }
    public void deleteWO(Long id) { woRepo.deleteById(id); }

    // Work Order Detail
    public List<WorkOrderDetail> findWODetails(Long woId) { return wodRepo.findByWorkOrderId(woId); }
    public WorkOrderDetail findWODetail(Long id) { return wodRepo.findById(id).orElseThrow(); }
    @Transactional public WorkOrderDetail createWODetail(WorkOrderDetail d) { return wodRepo.save(d); }
    @Transactional public WorkOrderDetail updateWODetail(Long id, WorkOrderDetail upd) {
        WorkOrderDetail e = wodRepo.findById(id).orElseThrow();
        e.setWorkOrder(upd.getWorkOrder()); e.setProductionOrderDetail(upd.getProductionOrderDetail()); e.setStageSequence(upd.getStageSequence());
        e.setPlannedStartAt(upd.getPlannedStartAt()); e.setPlannedEndAt(upd.getPlannedEndAt()); e.setStartAt(upd.getStartAt()); e.setCompleteAt(upd.getCompleteAt());
        e.setWorkStatus(upd.getWorkStatus()); e.setNotes(upd.getNotes());
        return e;
    }
    public void deleteWODetail(Long id) { wodRepo.deleteById(id); }

    // Stage
    public List<ProductionStage> findStages(Long woDetailId) { return stageRepo.findByWorkOrderDetailIdOrderByStageSequenceAsc(woDetailId); }
    public ProductionStage findStage(Long id) { return stageRepo.findById(id).orElseThrow(); }
    @Transactional public ProductionStage createStage(ProductionStage s) { return stageRepo.save(s); }
    @Transactional public ProductionStage updateStage(Long id, ProductionStage upd) {
        ProductionStage e = stageRepo.findById(id).orElseThrow();
        e.setWorkOrderDetail(upd.getWorkOrderDetail()); e.setStageType(upd.getStageType()); e.setStageSequence(upd.getStageSequence());
        e.setMachine(upd.getMachine()); e.setAssignedTo(upd.getAssignedTo()); e.setAssignedLeader(upd.getAssignedLeader());
        e.setBatchNumber(upd.getBatchNumber()); e.setPlannedOutput(upd.getPlannedOutput()); e.setActualOutput(upd.getActualOutput());
        e.setStartAt(upd.getStartAt()); e.setCompleteAt(upd.getCompleteAt()); e.setStatus(upd.getStatus());
        e.setOutsourced(upd.getOutsourced()); e.setOutsourceVendor(upd.getOutsourceVendor()); e.setNotes(upd.getNotes());
        return e;
    }
    public void deleteStage(Long id) { stageRepo.deleteById(id); }

    // ===== GIAI ĐOẠN 4: PRODUCTION ORDER CREATION & APPROVAL =====
    
    // ===== GIAI ĐOẠN 4: PRODUCTION ORDER CREATION & APPROVAL (NEW WORKFLOW) =====
    
    /**
     * @deprecated Use ProductionPlanService.createPlanFromContract() instead
     * This method is kept for backward compatibility but will redirect to new workflow
     */
    @Deprecated
    @Transactional
    public ProductionOrder createFromContract(Long contractId, Long planningUserId, 
                                           LocalDate plannedStartDate, LocalDate plannedEndDate, String notes) {
        // Redirect to new Production Plan workflow
        throw new UnsupportedOperationException(
            "Direct Production Order creation from contract is deprecated. " +
            "Please use Production Plan workflow: " +
            "1. Create Production Plan via ProductionPlanService.createPlanFromContract() " +
            "2. Submit for approval " +
            "3. Approve to automatically create Production Order"
        );
    }
    
    /**
     * New method: Create Production Order from approved Production Plan
     * This method is called automatically by ProductionPlanService when plan is approved
     */
    @Transactional
    public ProductionOrder createFromApprovedPlan(Long planId) {
        // This method will be implemented to create PO from approved Production Plan
        // For now, delegate to ProductionPlanService
        return productionPlanService.createProductionOrderFromApprovedPlan(planId);
    }
    
    @Transactional
    public ProductionOrder approveProductionOrder(Long poId, Long directorId, String notes) {
        ProductionOrder po = poRepo.findById(poId)
            .orElseThrow(() -> new RuntimeException("Production Order not found"));
        
        User director = userRepository.findById(directorId)
            .orElseThrow(() -> new RuntimeException("Director not found"));
        
        po.setStatus("APPROVED");
        po.setApprovedBy(director);
        po.setApprovedAt(Instant.now());
        
        ProductionOrder savedPO = poRepo.save(po);
        
        // Send notification to Production Team
        notificationService.notifyProductionOrderApproved(savedPO);
        
        return savedPO;
    }
    
    @Transactional
    public ProductionOrder rejectProductionOrder(Long poId, Long directorId, String rejectionNotes) {
        ProductionOrder po = poRepo.findById(poId)
            .orElseThrow(() -> new RuntimeException("Production Order not found"));
        
        User director = userRepository.findById(directorId)
            .orElseThrow(() -> new RuntimeException("Director not found"));
        
        po.setStatus("REJECTED");
        po.setApprovedBy(director);
        po.setApprovedAt(Instant.now());
        po.setNotes(rejectionNotes);
        
        ProductionOrder savedPO = poRepo.save(po);
        
        // Send notification to Planning Department
        notificationService.notifyProductionOrderRejected(savedPO);
        
        return savedPO;
    }
    
    public List<ProductionOrder> getProductionOrdersPendingApproval() {
        return poRepo.findByStatus("PENDING_APPROVAL");
    }
    
    public List<ProductionOrder> getDirectorPendingProductionOrders() {
        return poRepo.findByStatus("PENDING_APPROVAL");
    }
    
    // ===== PRODUCTION PLAN INTEGRATION METHODS =====
    
    public List<tmmsystem.dto.production_plan.ProductionPlanDto> getProductionPlansForContract(Long contractId) {
        return productionPlanService.findPlansByContract(contractId);
    }
    
    public List<tmmsystem.dto.production_plan.ProductionPlanDto> getProductionPlansPendingApproval() {
        return productionPlanService.findPendingApprovalPlans();
    }

    // Helper: generate QR token
    private String generateQrToken() {
        byte[] bytes = new byte[24]; // 32 chars in hex
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    // Work Order approval flow
    @Transactional
    public WorkOrder submitWorkOrderApproval(Long woId) {
        WorkOrder wo = woRepo.findById(woId).orElseThrow();
        wo.setStatus("PENDING_APPROVAL");
        return woRepo.save(wo);
    }

    @Transactional
    public WorkOrder approveWorkOrder(Long woId, Long pmUserId) {
        WorkOrder wo = woRepo.findById(woId).orElseThrow();
        User pm = userRepository.findById(pmUserId).orElseThrow();
        wo.setStatus("APPROVED");
        wo.setApprovedBy(pm);
        WorkOrder saved = woRepo.save(wo);
        // generate QR token for all stages under this WO
        List<WorkOrderDetail> details = wodRepo.findByWorkOrderId(saved.getId());
        for (WorkOrderDetail d : details) {
            List<ProductionStage> stages = stageRepo.findByWorkOrderDetailIdOrderByStageSequenceAsc(d.getId());
            for (ProductionStage s : stages) {
                if (s.getQrToken() == null || s.getQrToken().isBlank()) {
                    s.setQrToken(generateQrToken());
                }
            }
            stageRepo.saveAll(stages);
        }
        notificationService.notifyWorkOrderApproved(saved);
        return saved;
    }

    @Transactional
    public WorkOrder rejectWorkOrder(Long woId, Long pmUserId, String reason) {
        WorkOrder wo = woRepo.findById(woId).orElseThrow();
        User pm = userRepository.findById(pmUserId).orElseThrow();
        wo.setStatus("REJECTED");
        wo.setApprovedBy(pm);
        wo.setSendStatus(reason);
        WorkOrder saved = woRepo.save(wo);
        notificationService.notifyWorkOrderRejected(saved);
        return saved;
    }

    public ProductionStage findStageByQrToken(String token) {
        return stageRepo.findByQrToken(token).orElseThrow(() -> new RuntimeException("Invalid QR token"));
    }

    // Redo stage (fail nhẹ)
    @Transactional
    public ProductionStage redoStage(Long stageId) {
        ProductionStage s = stageRepo.findById(stageId).orElseThrow();
        s.setStatus("PENDING");
        s.setStartAt(null); s.setCompleteAt(null);
        s.setQcLastResult(null); s.setQcLastCheckedAt(null);
        return stageRepo.save(s);
    }

    // Filter lists for roles
    public List<ProductionStage> findStagesForLeader(Long leaderUserId) {
        return stageRepo.findByAssignedLeaderIdAndStatusIn(leaderUserId, java.util.List.of("PENDING","IN_PROGRESS"));
    }

    public List<ProductionStage> findStagesForKcs(String status) {
        // example rule: stages completed and waiting QC
        if (status == null || status.isBlank()) {
            return stageRepo.findByStatus("COMPLETED");
        }
        return stageRepo.findByStatus(status);
    }

    // QC hook: called when inspection PASS
    @Transactional
    public void markStageQcPass(Long stageId) {
        ProductionStage s = stageRepo.findById(stageId).orElseThrow();
        s.setQcLastResult("PASS");
        s.setQcLastCheckedAt(Instant.now());
        stageRepo.save(s);
        if ("PACKAGING".equalsIgnoreCase(s.getStageType())) {
            // Check all WorkOrderDetails under the same PO have all stages PASS
            WorkOrderDetail wodOfStage = s.getWorkOrderDetail();
            ProductionOrder po = wodOfStage.getProductionOrderDetail().getProductionOrder();
            // Collect all WOD of this PO
            List<WorkOrderDetail> allWod = wodRepo.findAll().stream()
                    .filter(w -> w.getProductionOrderDetail()!=null && w.getProductionOrderDetail().getProductionOrder()!=null
                            && w.getProductionOrderDetail().getProductionOrder().getId().equals(po.getId()))
                    .toList();
            boolean allOk = true;
            for (WorkOrderDetail w : allWod) {
                List<ProductionStage> stages = stageRepo.findByWorkOrderDetailIdOrderByStageSequenceAsc(w.getId());
                if (stages.isEmpty()) { allOk=false; break; }
                // Require every stage QC PASS
                for (ProductionStage st : stages) {
                    if (!"PASS".equalsIgnoreCase(st.getQcLastResult())) { allOk=false; break; }
                }
                if (!allOk) break;
                // And packaging stage must exist and be completed
                ProductionStage pkg = stages.stream().filter(st->"PACKAGING".equalsIgnoreCase(st.getStageType())).findFirst().orElse(null);
                if (pkg==null || pkg.getCompleteAt()==null) { allOk=false; break; }
            }
            if (allOk) {
                po.setStatus("ORDER_COMPLETED");
                poRepo.save(po);
                notificationService.notifyOrderCompleted(po);
            }
        }
    }

    /** Create a standard Work Order with 6 default stages for each ProductionOrderDetail under the PO */
    @Transactional
    public WorkOrder createStandardWorkOrder(Long poId, Long createdById){
        ProductionOrder po = poRepo.findById(poId).orElseThrow(() -> new RuntimeException("PO not found"));
        WorkOrder wo = new WorkOrder();
        wo.setProductionOrder(po);
        wo.setWoNumber("WO-"+System.currentTimeMillis());
        wo.setStatus("DRAFT");
        wo.setSendStatus("NOT_SENT");
        wo.setProduction(true);
        if (createdById!=null){ userRepository.findById(createdById).ifPresent(wo::setCreatedBy); }
        WorkOrder saved = woRepo.save(wo);
        List<ProductionOrderDetail> pods = podRepo.findByProductionOrderId(poId);
        int seq=1;
        for (ProductionOrderDetail pod : pods){
            WorkOrderDetail wod = new WorkOrderDetail();
            wod.setWorkOrder(saved); wod.setProductionOrderDetail(pod); wod.setStageSequence(seq++); wod.setWorkStatus("PENDING");
            wod = wodRepo.save(wod);
            createDefaultStage(wod, "WARPING", 1, false);
            createDefaultStage(wod, "WEAVING", 2, false);
            createDefaultStage(wod, "DYEING", 3, true);
            createDefaultStage(wod, "CUTTING", 4, false);
            createDefaultStage(wod, "HEMMING", 5, false);
            createDefaultStage(wod, "PACKAGING", 6, false);
        }
        return saved;
    }
    private void createDefaultStage(WorkOrderDetail wod, String type, int sequence, boolean outsourced){
        ProductionStage s = new ProductionStage();
        s.setWorkOrderDetail(wod); s.setStageType(type); s.setStageSequence(sequence); s.setStatus("PENDING"); s.setOutsourced(outsourced);
        stageRepo.save(s);
    }
    private void ensureLeader(Long stageId, Long leaderUserId){
        ProductionStage s = stageRepo.findById(stageId).orElseThrow();
        if (s.getAssignedLeader()==null || s.getAssignedLeader().getId()==null || !s.getAssignedLeader().getId().equals(leaderUserId)){
            throw new RuntimeException("Access denied: not assigned leader");
        }
    }
    @Transactional
    public ProductionStage startStage(Long stageId, Long leaderUserId, String evidencePhotoUrl, BigDecimal qtyCompleted){
        ensureLeader(stageId, leaderUserId);
        ProductionStage s = stageRepo.findById(stageId).orElseThrow();
        if (s.getStartAt()==null) s.setStartAt(Instant.now());
        s.setStatus("IN_PROGRESS");
        stageRepo.save(s);
        StageTracking tr = new StageTracking(); tr.setProductionStage(s); tr.setOperator(userRepository.findById(leaderUserId).orElseThrow());
        tr.setAction("START"); tr.setEvidencePhotoUrl(evidencePhotoUrl); tr.setQuantityCompleted(qtyCompleted); stageTrackingRepository.save(tr);
        return s;
    }
    @Transactional
    public ProductionStage pauseStage(Long stageId, Long leaderUserId, String pauseReason, String pauseNotes){
        ensureLeader(stageId, leaderUserId);
        ProductionStage s = stageRepo.findById(stageId).orElseThrow(); s.setStatus("PAUSED"); stageRepo.save(s);
        StagePauseLog pl = new StagePauseLog(); pl.setProductionStage(s); pl.setPausedBy(userRepository.findById(leaderUserId).orElseThrow());
        pl.setPauseReason(pauseReason); pl.setPauseNotes(pauseNotes); pl.setPausedAt(Instant.now()); stagePauseLogRepository.save(pl);
        StageTracking tr = new StageTracking(); tr.setProductionStage(s); tr.setOperator(pl.getPausedBy()); tr.setAction("PAUSE"); stageTrackingRepository.save(tr);
        return s;
    }
    @Transactional
    public ProductionStage resumeStage(Long stageId, Long leaderUserId){
        ensureLeader(stageId, leaderUserId);
        ProductionStage s = stageRepo.findById(stageId).orElseThrow(); s.setStatus("IN_PROGRESS"); stageRepo.save(s);
        // close last open pause
        List<StagePauseLog> pauses = stagePauseLogRepository.findByProductionStageIdOrderByPausedAtDesc(s.getId());
        for (StagePauseLog p : pauses){ if (p.getResumedAt()==null){ p.setResumedAt(Instant.now()); if (p.getPausedAt()!=null){ long mins = java.time.Duration.between(p.getPausedAt(), p.getResumedAt()).toMinutes(); p.setDurationMinutes((int)mins);} stagePauseLogRepository.save(p); break; } }
        StageTracking tr = new StageTracking(); tr.setProductionStage(s); tr.setOperator(userRepository.findById(leaderUserId).orElseThrow()); tr.setAction("RESUME"); stageTrackingRepository.save(tr);
        return s;
    }
    @Transactional
    public ProductionStage completeStage(Long stageId, Long leaderUserId, String evidencePhotoUrl, BigDecimal qtyCompleted){
        ensureLeader(stageId, leaderUserId);
        ProductionStage s = stageRepo.findById(stageId).orElseThrow(); s.setStatus("COMPLETED"); s.setCompleteAt(Instant.now()); stageRepo.save(s);
        StageTracking tr = new StageTracking(); tr.setProductionStage(s); tr.setOperator(userRepository.findById(leaderUserId).orElseThrow()); tr.setAction("COMPLETE"); tr.setEvidencePhotoUrl(evidencePhotoUrl); tr.setQuantityCompleted(qtyCompleted); stageTrackingRepository.save(tr);
        return s;
    }
}
