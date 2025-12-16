package tmmsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.entity.*;
import tmmsystem.repository.*;
import tmmsystem.dto.production.ProductionOrderDto;
import tmmsystem.dto.production.ProductionStageDto;
import tmmsystem.mapper.ProductionMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Lazy;

@Service
public class ProductionService {

    private static final List<String> BLOCKING_STAGE_EXEC_STATUSES = List.of("WAITING", "READY_TO_PRODUCE",
            "IN_PROGRESS", "WAITING_QC", "QC_IN_PROGRESS", "QC_FAILED", "WAITING_REWORK",
            "REWORK_IN_PROGRESS", "PAUSED");
    private final ProductionOrderRepository poRepo;
    private final ProductionOrderDetailRepository podRepo;
    private final TechnicalSheetRepository techRepo;
    // REMOVED: WorkOrderRepository và WorkOrderDetailRepository - không còn dùng
    // nữa
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
    private final OutsourcingTaskRepository outsourcingTaskRepository;
    private final MachineAssignmentRepository machineAssignmentRepository;
    private final MachineRepository machineRepository;
    private final ProductionPlanRepository productionPlanRepository;
    private final ProductionMapper productionMapper;
    private final tmmsystem.repository.QualityIssueRepository issueRepo;
    private final tmmsystem.repository.MaterialRequisitionRepository reqRepo;
    private final tmmsystem.repository.MaterialRequisitionDetailRepository reqDetailRepo;
    private final tmmsystem.repository.QcInspectionRepository qcInspectionRepository;

    private static final java.util.Map<String, String> STAGE_TYPE_ALIASES = java.util.Map.ofEntries(
            java.util.Map.entry("WARPING", "CUONG_MAC"),
            java.util.Map.entry("CUONG_MAC", "WARPING"),
            java.util.Map.entry("WEAVING", "DET"),
            java.util.Map.entry("DET", "WEAVING"),
            java.util.Map.entry("DYEING", "NHUOM"),
            java.util.Map.entry("NHUOM", "DYEING"),
            java.util.Map.entry("CUTTING", "CAT"),
            java.util.Map.entry("CAT", "CUTTING"),
            java.util.Map.entry("HEMMING", "MAY"),
            java.util.Map.entry("MAY", "HEMMING"),
            java.util.Map.entry("PACKAGING", "DONG_GOI"),
            java.util.Map.entry("DONG_GOI", "PACKAGING"));

    public ProductionService(ProductionOrderRepository poRepo,
            ProductionOrderDetailRepository podRepo,
            TechnicalSheetRepository techRepo,
            // REMOVED: WorkOrderRepository và WorkOrderDetailRepository - không còn dùng
            // nữa
            ProductionStageRepository stageRepo,
            UserRepository userRepository,
            NotificationService notificationService,
            ContractRepository contractRepository,
            BomRepository bomRepository,
            ProductionOrderDetailRepository productionOrderDetailRepository,
            @Lazy ProductionPlanService productionPlanService,
            StageTrackingRepository stageTrackingRepository,
            StagePauseLogRepository stagePauseLogRepository,
            OutsourcingTaskRepository outsourcingTaskRepository,
            MachineAssignmentRepository machineAssignmentRepository,
            MachineRepository machineRepository,
            ProductionPlanRepository productionPlanRepository,
            ProductionMapper productionMapper,
            tmmsystem.repository.QualityIssueRepository issueRepo,
            tmmsystem.repository.MaterialRequisitionRepository reqRepo,
            tmmsystem.repository.MaterialRequisitionDetailRepository reqDetailRepo,
            tmmsystem.repository.QcInspectionRepository qcInspectionRepository) {
        this.poRepo = poRepo;
        this.podRepo = podRepo;
        this.techRepo = techRepo;
        // REMOVED: WorkOrderRepository và WorkOrderDetailRepository - không còn dùng
        // nữa
        this.stageRepo = stageRepo;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.contractRepository = contractRepository;
        this.bomRepository = bomRepository;
        this.productionOrderDetailRepository = productionOrderDetailRepository;
        this.productionPlanService = productionPlanService;
        this.stageTrackingRepository = stageTrackingRepository;
        this.stagePauseLogRepository = stagePauseLogRepository;
        this.outsourcingTaskRepository = outsourcingTaskRepository;
        this.machineAssignmentRepository = machineAssignmentRepository;
        this.machineRepository = machineRepository;
        this.productionPlanRepository = productionPlanRepository;
        this.productionMapper = productionMapper;
        this.issueRepo = issueRepo;
        this.reqRepo = reqRepo;
        this.reqDetailRepo = reqDetailRepo;
        this.qcInspectionRepository = qcInspectionRepository;
    }

    /**
     * Startup fixer: ensure only one active (progress < 100%) stage per type
     * (except
     * outsourced dyeing). Any extra active stages are reset to WAITING/PENDING.
     *
     * @return number of stages that were demoted to WAITING
     */
    @Transactional
    public int enforceExclusiveStageConflicts() {
        List<ProductionStage> activeStages = stageRepo.findByExecutionStatusIn(BLOCKING_STAGE_EXEC_STATUSES);
        if (activeStages.isEmpty()) {
            return 0;
        }

        Map<String, List<ProductionStage>> byType = activeStages.stream()
                .filter(s -> s.getProgressPercent() == null || s.getProgressPercent() < 100)
                .filter(s -> {
                    boolean isOutsourcedDyeing = "DYEING".equalsIgnoreCase(s.getStageType())
                            && Boolean.TRUE.equals(s.getOutsourced());
                    return !isOutsourcedDyeing; // allow parallel only for outsourced dyeing
                })
                .collect(Collectors.groupingBy(
                        s -> s.getStageType() == null ? "UNKNOWN" : s.getStageType().toUpperCase()));

        int demoted = 0;
        for (List<ProductionStage> list : byType.values()) {
            if (list.size() <= 1) {
                continue;
            }
            // Pick the one to keep: earliest startAt (non-null first), then smallest id
            list.sort((a, b) -> {
                Instant aStart = a.getStartAt();
                Instant bStart = b.getStartAt();
                if (aStart != null && bStart != null) {
                    int cmp = aStart.compareTo(bStart);
                    if (cmp != 0)
                        return cmp;
                } else if (aStart != null) {
                    return -1;
                } else if (bStart != null) {
                    return 1;
                }
                return a.getId().compareTo(b.getId());
            });

            ProductionStage keep = list.get(0);
            List<ProductionStage> demoteList = list.subList(1, list.size());
            for (ProductionStage s : demoteList) {
                s.setExecutionStatus("WAITING");
                s.setStatus("PENDING");
                s.setStartAt(null); // force to start again later
            }
            stageRepo.saveAll(demoteList);
            demoted += demoteList.size();
            // Log to console to trace which stages were demoted
            System.out.println("Exclusive stage fix: keep stage " + keep.getId() + " type " + keep.getStageType()
                    + ", demoted=" + demoteList.stream().map(ProductionStage::getId).toList());
        }

        return demoted;
    }

    /**
     * Promote the next pending stage of a given type when the slot becomes
     * available.
     * This enables cross-PO stage promotion (e.g., when PO1's WARPING completes,
     * PO2's WARPING can start).
     * 
     * Rules:
     * 1. DYEING is outsourced - allow parallel, promote all eligible
     * 2. Other stages: only one active at a time
     * 3. A lot can only start a stage if its previous stage is complete
     * (QC_PASSED/COMPLETED)
     * 4. Order: priority DESC, createdAt ASC (FIFO)
     */
    @Transactional
    public void promoteNextOrderForStageType(String stageType) {
        // DYEING: Allow parallel (outsourced), promote all eligible
        if ("DYEING".equalsIgnoreCase(stageType)) {
            promoteAllEligibleDyeingStages();
            return;
        }

        // "Truly active" statuses that block the slot (exclude WAITING/PENDING - those
        // need promotion)
        List<String> TRULY_ACTIVE_STATUSES = List.of("READY_TO_PRODUCE", "IN_PROGRESS",
                "WAITING_QC", "QC_IN_PROGRESS", "QC_FAILED", "WAITING_REWORK",
                "REWORK_IN_PROGRESS", "PAUSED");

        // Count stages that are truly occupying the production slot
        long activeCount = stageRepo.countByStageTypeAndExecutionStatusIn(stageType, TRULY_ACTIVE_STATUSES);

        if (activeCount == 0) {
            // Slot available - find stages in WAITING or PENDING status
            // WAITING stages have priority (they're the first stage of each PO)
            List<ProductionStage> waitingStages = stageRepo.findByStageTypeAndExecutionStatus(stageType, "WAITING");
            List<ProductionStage> pendingStages = stageRepo.findPendingByStageTypeOrderByPriority(stageType);

            // Combine: WAITING first, then PENDING
            List<ProductionStage> candidates = new java.util.ArrayList<>();
            candidates.addAll(waitingStages);
            candidates.addAll(pendingStages);

            for (ProductionStage next : candidates) {
                if (canEnterStage(next)) {
                    next.setExecutionStatus("READY_TO_PRODUCE");
                    syncStageStatus(next, "READY_TO_PRODUCE");
                    stageRepo.save(next);

                    // Notify assigned leader
                    if (next.getAssignedLeader() != null) {
                        String lotCode = "N/A";
                        if (next.getProductionOrder() != null) {
                            // Try to get lotCode from ProductionPlan
                            String planCode = extractPlanCodeFromNotes(next.getProductionOrder().getNotes());
                            if (planCode != null) {
                                ProductionPlan plan = productionPlanRepository.findByPlanCode(planCode).orElse(null);
                                if (plan != null && plan.getLot() != null) {
                                    lotCode = plan.getLot().getLotCode();
                                }
                            }
                            // Fallback to poNumber if no lotCode
                            if ("N/A".equals(lotCode)) {
                                lotCode = next.getProductionOrder().getPoNumber();
                            }
                        }
                        notificationService.notifyUser(next.getAssignedLeader(), "PRODUCTION", "INFO",
                                "Công đoạn sẵn sàng",
                                "Công đoạn " + stageType + " của lô " + lotCode + " sẵn sàng bắt đầu.",
                                "PRODUCTION_STAGE", next.getId());
                    }
                    System.out.println("Promoted stage " + next.getId() + " (" + stageType + ") to READY_TO_PRODUCE");
                    break; // Only promote one (slot now occupied)
                }
            }
        }
    }

    /**
     * Check if a stage can enter production (previous stage in same PO must be
     * completed).
     * Ensures each lot processes stages in order:
     * WARPING→WEAVING→DYEING→CUTTING→HEMMING→PACKAGING
     */
    private boolean canEnterStage(ProductionStage stage) {
        // First stage (sequence 1, usually WARPING) can always enter if slot available
        if (stage.getStageSequence() == null || stage.getStageSequence() == 1) {
            return true;
        }

        ProductionOrder po = stage.getProductionOrder();
        if (po == null) {
            return false;
        }

        List<ProductionStage> stages = stageRepo.findByProductionOrderIdOrderByStageSequenceAsc(po.getId());
        ProductionStage prev = stages.stream()
                .filter(s -> s.getStageSequence() != null
                        && s.getStageSequence() == stage.getStageSequence() - 1)
                .findFirst().orElse(null);

        if (prev == null) {
            return true; // No previous stage found, allow
        }

        String prevStatus = prev.getExecutionStatus();
        return "QC_PASSED".equals(prevStatus) || "COMPLETED".equals(prevStatus);
    }

    /**
     * For DYEING (outsourced): Promote all eligible pending stages since they can
     * run in parallel.
     */
    private void promoteAllEligibleDyeingStages() {
        List<ProductionStage> pending = stageRepo.findPendingByStageTypeOrderByPriority("DYEING");
        for (ProductionStage stage : pending) {
            if (canEnterStage(stage)) {
                stage.setExecutionStatus("READY_TO_PRODUCE");
                syncStageStatus(stage, "READY_TO_PRODUCE");
                stageRepo.save(stage);

                if (stage.getAssignedLeader() != null) {
                    String poNumber = stage.getProductionOrder() != null
                            ? stage.getProductionOrder().getPoNumber()
                            : "N/A";
                    notificationService.notifyUser(stage.getAssignedLeader(), "PRODUCTION", "INFO",
                            "Công đoạn nhuộm sẵn sàng",
                            "Công đoạn nhuộm của lô " + poNumber + " sẵn sàng bắt đầu.",
                            "PRODUCTION_STAGE", stage.getId());
                }
            }
        }
    }

    // Production Order
    public List<ProductionOrder> findAllPO() {
        return poRepo.findAll();
    }

    public List<ProductionOrder> findPOByQuotationId(Long quotationId) {
        return poRepo.findByContract_Quotation_Id(quotationId);
    }

    public ProductionOrder findPO(Long id) {
        return poRepo.findById(id).orElseThrow(() -> new RuntimeException("PO not found"));
    }

    @Transactional
    public ProductionOrder createPO(ProductionOrder po) {
        return poRepo.save(po);
    }

    @Transactional
    public ProductionOrder updatePO(Long id, ProductionOrder upd) {
        ProductionOrder e = poRepo.findById(id).orElseThrow();
        e.setPoNumber(upd.getPoNumber());
        e.setContract(upd.getContract());
        e.setTotalQuantity(upd.getTotalQuantity());
        e.setPlannedStartDate(upd.getPlannedStartDate());
        e.setPlannedEndDate(upd.getPlannedEndDate());
        e.setStatus(upd.getStatus());
        e.setPriority(upd.getPriority());
        e.setNotes(upd.getNotes());
        e.setCreatedBy(upd.getCreatedBy());
        e.setApprovedBy(upd.getApprovedBy());
        e.setApprovedAt(upd.getApprovedAt());
        return e;
    }

    public void deletePO(Long id) {
        poRepo.deleteById(id);
    }

    // PO Detail
    public List<ProductionOrderDetail> findPODetails(Long poId) {
        return podRepo.findByProductionOrderId(poId);
    }

    public ProductionOrderDetail findPODetail(Long id) {
        return podRepo.findById(id).orElseThrow();
    }

    @Transactional
    public ProductionOrderDetail createPODetail(ProductionOrderDetail d) {
        return podRepo.save(d);
    }

    @Transactional
    public ProductionOrderDetail updatePODetail(Long id, ProductionOrderDetail upd) {
        ProductionOrderDetail e = podRepo.findById(id).orElseThrow();
        e.setProductionOrder(upd.getProductionOrder());
        e.setProduct(upd.getProduct());
        e.setBom(upd.getBom());
        e.setBomVersion(upd.getBomVersion());
        e.setQuantity(upd.getQuantity());
        e.setUnit(upd.getUnit());
        e.setNoteColor(upd.getNoteColor());
        return e;
    }

    public void deletePODetail(Long id) {
        podRepo.deleteById(id);
    }

    // Technical Sheet
    public TechnicalSheet findTechSheet(Long id) {
        return techRepo.findById(id).orElseThrow();
    }

    @Transactional
    public TechnicalSheet createTechSheet(TechnicalSheet t) {
        return techRepo.save(t);
    }

    @Transactional
    public TechnicalSheet updateTechSheet(Long id, TechnicalSheet upd) {
        TechnicalSheet e = techRepo.findById(id).orElseThrow();
        e.setProductionOrder(upd.getProductionOrder());
        e.setSheetNumber(upd.getSheetNumber());
        e.setYarnSpecifications(upd.getYarnSpecifications());
        e.setMachineSettings(upd.getMachineSettings());
        e.setQualityStandards(upd.getQualityStandards());
        e.setSpecialInstructions(upd.getSpecialInstructions());
        e.setCreatedBy(upd.getCreatedBy());
        e.setApprovedBy(upd.getApprovedBy());
        return e;
    }

    public void deleteTechSheet(Long id) {
        techRepo.deleteById(id);
    }

    // REMOVED: Tất cả methods liên quan WorkOrder và WorkOrderDetail - không còn
    // dùng nữa
    // Stages giờ được query trực tiếp theo ProductionOrder

    // Stage - Query theo ProductionOrder
    public List<ProductionStage> findStagesByOrderId(Long orderId) {
        return stageRepo.findStagesByOrderId(orderId);
    }

    public ProductionStage findStage(Long id) {
        return stageRepo.findById(id).orElseThrow();
    }

    public ProductionStageDto getStageDto(Long id) {
        ProductionStage stage = findStage(id);
        ProductionStageDto dto = productionMapper.toDto(stage);

        // Always check QualityIssue for defect details to ensure source of truth
        // This covers cases where stage status changed (e.g. REWORK) or data updated in
        // QualityIssue
        List<QualityIssue> issues = issueRepo.findByProductionStageId(id);
        if (!issues.isEmpty()) {
            // Use the latest issue
            QualityIssue issue = issues.get(issues.size() - 1);
            dto.setDefectLevel(issue.getSeverity());
            dto.setDefectSeverity(issue.getSeverity());
            dto.setDefectDescription(issue.getDescription());
            dto.setDefectId(issue.getId());
        }

        return dto;
    }

    @Transactional
    public ProductionStage createStage(ProductionStage s) {
        return stageRepo.save(s);
    }

    @Transactional
    public ProductionStage updateStage(Long id, ProductionStage upd) {
        ProductionStage e = stageRepo.findById(id).orElseThrow();
        // REMOVED: e.setWorkOrderDetail() - field đã bị xóa
        e.setProductionOrder(upd.getProductionOrder()); // NEW: Set ProductionOrder
        e.setStageType(upd.getStageType());
        e.setStageSequence(upd.getStageSequence());
        e.setMachine(upd.getMachine());
        e.setAssignedTo(upd.getAssignedTo());
        e.setAssignedLeader(upd.getAssignedLeader());
        e.setBatchNumber(upd.getBatchNumber());
        e.setPlannedOutput(upd.getPlannedOutput());
        e.setActualOutput(upd.getActualOutput());
        e.setStartAt(upd.getStartAt());
        e.setCompleteAt(upd.getCompleteAt());
        e.setStatus(upd.getStatus());
        e.setOutsourced(upd.getOutsourced());
        e.setOutsourceVendor(upd.getOutsourceVendor());
        e.setNotes(upd.getNotes());
        return e;
    }

    public void deleteStage(Long id) {
        stageRepo.deleteById(id);
    }

    // ===== GIAI ĐOẠN 4: PRODUCTION ORDER CREATION & APPROVAL =====

    // ===== GIAI ĐOẠN 4: PRODUCTION ORDER CREATION & APPROVAL (NEW WORKFLOW) =====

    /**
     * @deprecated Use ProductionPlanService.createPlanFromContract() instead
     *             This method is kept for backward compatibility but will redirect
     *             to new workflow
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
                        "3. Approve to automatically create Production Order");
    }

    /**
     * New method: Create Production Order from approved Production Plan
     * This method is called automatically by ProductionPlanService when plan is
     * approved
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

    // REMOVED: Work Order approval flow - không còn dùng nữa
    // ProductionOrder được tạo trực tiếp từ ProductionPlan khi director approve

    public ProductionStage findStageByQrToken(String token) {
        return stageRepo.findByQrToken(token).orElseThrow(() -> new RuntimeException("Invalid QR token"));
    }

    // Leader Defect Methods
    public List<tmmsystem.dto.qc.QualityIssueDto> getLeaderDefects(Long leaderUserId) {
        // Get defects assigned to this leader:
        // 1. MINOR defects: PROCESSED (after Technical processed) or IN_PROGRESS (being
        // worked on)
        // 2. MAJOR defects: PROCESSED (after material requisition approved by PM)
        // Exclude PENDING (waiting for Technical/PM) and RESOLVED (completed)
        return issueRepo.findAll().stream()
                .filter(i -> {
                    String severity = i.getSeverity();
                    String status = i.getStatus();

                    // For MINOR defects: show PROCESSED or IN_PROGRESS
                    if ("MINOR".equals(severity)) {
                        return "PROCESSED".equals(status) || "IN_PROGRESS".equals(status);
                    }

                    // For MAJOR defects: show PROCESSED (PM has approved material requisition)
                    if ("MAJOR".equals(severity)) {
                        return "PROCESSED".equals(status) || "IN_PROGRESS".equals(status);
                    }

                    return false;
                })
                .filter(i -> {
                    ProductionStage stage = i.getProductionStage();
                    return stage != null &&
                            stage.getAssignedLeader() != null &&
                            stage.getAssignedLeader().getId().equals(leaderUserId);
                })
                .map(this::mapQualityIssueToDto)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Sync QualityIssue status based on stage QC status.
     * Resolves issues for stages that have already passed QC.
     * 
     * @return number of issues fixed
     */
    @Transactional
    public int syncQualityIssueStatus() {
        List<QualityIssue> allIssues = issueRepo.findAll();
        int fixed = 0;
        for (QualityIssue issue : allIssues) {
            if ("RESOLVED".equals(issue.getStatus())) {
                continue; // Already resolved
            }
            ProductionStage stage = issue.getProductionStage();
            if (stage == null) {
                continue;
            }
            // Check if stage has passed QC (using qcLastResult which is more reliable)
            String qcResult = stage.getQcLastResult();
            String stageStatus = stage.getExecutionStatus();
            boolean hasPassedQc = "PASS".equals(qcResult)
                    || (stageStatus != null && (stageStatus.contains("QC_PASSED") || stageStatus.equals("COMPLETED")));

            if (hasPassedQc) {
                issue.setStatus("RESOLVED");
                issue.setResolvedAt(java.time.Instant.now());
                issueRepo.save(issue);
                fixed++;
            }
        }
        return fixed;
    }

    public List<tmmsystem.dto.qc.QualityIssueDto> getTechnicalDefects() {
        // Show ALL defects (including RESOLVED for history tracking)
        return issueRepo.findAll().stream()
                .map(this::mapQualityIssueToDto)
                .collect(java.util.stream.Collectors.toList());
    }

    public tmmsystem.dto.qc.QualityIssueDto getDefectDetail(Long defectId) {
        QualityIssue issue = issueRepo.findById(defectId)
                .orElseThrow(() -> new RuntimeException("Defect not found"));
        return mapQualityIssueToDto(issue);
    }

    public void startReworkFromDefect(Long defectId, Long userId) {
        QualityIssue issue = issueRepo.findById(defectId)
                .orElseThrow(() -> new RuntimeException("Defect not found"));
        ProductionStage stage = issue.getProductionStage();
        if (stage == null) {
            throw new RuntimeException("Stage not found for this defect");
        }
        // Reset stage to allow rework
        stage.setExecutionStatus("WAITING_REWORK");
        stage.setProgressPercent(0);
        stageRepo.save(stage);
        // Update issue status
        issue.setStatus("IN_PROGRESS");
        issueRepo.save(issue);
    }

    private tmmsystem.dto.qc.QualityIssueDto mapQualityIssueToDto(QualityIssue issue) {
        tmmsystem.dto.qc.QualityIssueDto dto = new tmmsystem.dto.qc.QualityIssueDto();
        dto.setId(issue.getId());
        dto.setSeverity(issue.getSeverity());
        dto.setIssueType(issue.getIssueType());
        dto.setDescription(issue.getDescription());
        dto.setStatus(issue.getStatus());
        dto.setCreatedAt(issue.getCreatedAt());

        // NEW: Calculate attemptNumber dynamically based on createdAt order within same
        // stage
        if (issue.getProductionStage() != null && issue.getCreatedAt() != null) {
            List<QualityIssue> stageIssues = issueRepo.findByProductionStageId(issue.getProductionStage().getId());
            // Sort by createdAt ascending
            stageIssues.sort((a, b) -> {
                if (a.getCreatedAt() == null)
                    return 1;
                if (b.getCreatedAt() == null)
                    return -1;
                return a.getCreatedAt().compareTo(b.getCreatedAt());
            });
            // Find position of current issue (1-indexed)
            int attemptNum = 1;
            for (QualityIssue i : stageIssues) {
                if (i.getId().equals(issue.getId())) {
                    break;
                }
                attemptNum++;
            }
            dto.setAttemptNumber(attemptNum);
            dto.setAttemptLabel("Lỗi lần " + attemptNum);
        } else {
            dto.setAttemptNumber(1);
            dto.setAttemptLabel("Lỗi lần 1");
        }

        if (issue.getProductionStage() != null) {
            dto.setStageId(issue.getProductionStage().getId());
            dto.setStageType(issue.getProductionStage().getStageType());
            dto.setStageName(issue.getProductionStage().getStageType()); // Use stageType as stageName for now
            dto.setStageStatus(issue.getProductionStage().getExecutionStatus());

            // Populate Batch Number (Lot Code)
            // Populate Batch Number (Lot Code)
            String batchNumber = issue.getProductionStage().getBatchNumber();
            // If batchNumber is missing or is actually a PO Number, try to find real Lot
            // Code
            if ((batchNumber == null || batchNumber.isEmpty() || batchNumber.startsWith("PO-"))
                    && issue.getProductionOrder() != null) {
                // FIX: Priority 1 - Try from PO.notes containing planCode (Plan: PLAN-XXXX)
                ProductionOrder po = issue.getProductionOrder();
                if (po.getNotes() != null && po.getNotes().contains("Plan: ")) {
                    try {
                        String notes = po.getNotes();
                        String planCode = notes.substring(notes.indexOf("Plan: ") + 6).trim();
                        tmmsystem.entity.ProductionPlan plan = productionPlanRepository.findByPlanCode(planCode)
                                .orElse(null);
                        if (plan != null && plan.getLot() != null) {
                            batchNumber = plan.getLot().getLotCode();
                        }
                    } catch (Exception e) {
                        // Fallback to contract-based lookup
                    }
                }

                // Fallback: Try from Contract -> ProductionPlan (may be inaccurate for
                // multi-plan contracts)
                if ((batchNumber == null || batchNumber.isEmpty() || batchNumber.startsWith("PO-"))
                        && po.getContract() != null) {
                    List<tmmsystem.entity.ProductionPlan> plans = productionPlanRepository
                            .findByContractId(po.getContract().getId());
                    tmmsystem.entity.ProductionPlan currentPlan = plans.stream()
                            .filter(p -> Boolean.TRUE.equals(p.getCurrentVersion()))
                            .findFirst()
                            .orElse(plans.isEmpty() ? null : plans.get(plans.size() - 1)); // Fallback to latest
                    if (currentPlan != null && currentPlan.getLot() != null) {
                        batchNumber = currentPlan.getLot().getLotCode();
                    }
                }
            }
            if (batchNumber == null) {
                batchNumber = issue.getProductionOrder() != null ? issue.getProductionOrder().getPoNumber() : "N/A";
            }
            dto.setBatchNumber(batchNumber);

            if (issue.getProductionStage().getAssignedLeader() != null) {
                dto.setReportedBy(issue.getProductionStage().getAssignedLeader().getName());
                dto.setLeaderName(issue.getProductionStage().getAssignedLeader().getName());
            }
        }

        dto.setIssueDescription(issue.getDescription());

        // Populate technical notes from stage
        if (issue.getProductionStage() != null) {
            dto.setTechnicalNotes(issue.getProductionStage().getDefectDescription());
        }

        // Fallback for evidence photo if missing in QualityIssue
        String photo = issue.getEvidencePhoto();

        // Fetch inspections
        if (issue.getProductionStage() != null) {
            List<tmmsystem.entity.QcInspection> inspections = qcInspectionRepository
                    .findByProductionStageId(issue.getProductionStage().getId());

            // Map to DTO
            List<tmmsystem.dto.qc.QcInspectionDto> inspectionDtos = inspections.stream().map(ins -> {
                tmmsystem.dto.qc.QcInspectionDto d = new tmmsystem.dto.qc.QcInspectionDto();
                d.setId(ins.getId());
                d.setResult(ins.getResult());
                d.setNotes(ins.getNotes());
                d.setPhotoUrl(ins.getPhotoUrl());
                d.setCheckpointName(ins.getQcCheckpoint() != null ? ins.getQcCheckpoint().getCheckpointName() : null);
                d.setInspectedAt(ins.getInspectedAt()); // NEW: For grouping by inspection round
                return d;
            }).collect(java.util.stream.Collectors.toList());
            dto.setInspections(inspectionDtos);

            // Fallback photo logic
            if (photo == null || photo.isEmpty()) {
                for (tmmsystem.entity.QcInspection ins : inspections) {
                    if ("FAIL".equals(ins.getResult()) && ins.getPhotoUrl() != null && !ins.getPhotoUrl().isEmpty()) {
                        photo = ins.getPhotoUrl();
                        break;
                    }
                }
            }
        }
        dto.setEvidencePhoto(photo);

        // Populate Rework Progress and History
        if (issue.getProductionStage() != null) {
            dto.setReworkProgress(issue.getProductionStage().getProgressPercent());

            List<StageTracking> trackings = stageTrackingRepository
                    .findByProductionStageIdOrderByTimestampDesc(issue.getProductionStage().getId());
            List<tmmsystem.dto.production.StageTrackingDto> history = trackings.stream()
                    .filter(t -> Boolean.TRUE.equals(t.getIsRework()))
                    .map(t -> {
                        tmmsystem.dto.production.StageTrackingDto d = new tmmsystem.dto.production.StageTrackingDto();
                        d.setId(t.getId());
                        d.setAction(t.getAction());
                        d.setQuantityCompleted(t.getQuantityCompleted());
                        d.setNotes(t.getNotes());
                        d.setTimestamp(t.getTimestamp());
                        d.setOperatorName(t.getOperator() != null ? t.getOperator().getName() : "Unknown");
                        d.setIsRework(t.getIsRework());
                        return d;
                    })
                    .collect(java.util.stream.Collectors.toList());
            dto.setReworkHistory(history);
        }

        if (issue.getProductionOrder() != null) {
            dto.setOrderId(issue.getProductionOrder().getId());
            dto.setPoNumber(issue.getProductionOrder().getPoNumber());

            // Populate Product Name and Size
            List<ProductionOrderDetail> details = podRepo.findByProductionOrderId(issue.getProductionOrder().getId());
            if (!details.isEmpty()) {
                ProductionOrderDetail firstDetail = details.get(0);
                if (firstDetail.getProduct() != null) {
                    dto.setProductName(firstDetail.getProduct().getName());
                    String size = firstDetail.getProduct().getStandardDimensions();
                    // Fallback to lot size if needed, but for now just use product size
                    dto.setSize(size != null ? size : "N/A");
                }
            }
        }

        // Populate Material Requisition Info
        if (issue.getProductionStage() != null) {
            List<tmmsystem.entity.MaterialRequisition> reqs = reqRepo
                    .findByProductionStageId(issue.getProductionStage().getId());
            // Get the latest one if multiple (though usually one per defect/stage cycle)
            if (!reqs.isEmpty()) {
                tmmsystem.entity.MaterialRequisition req = reqs.get(0);
                tmmsystem.dto.execution.MaterialRequisitionDto reqDto = new tmmsystem.dto.execution.MaterialRequisitionDto();
                reqDto.setId(req.getId());
                reqDto.setRequisitionNumber(req.getRequisitionNumber());
                reqDto.setStatus(req.getStatus());
                reqDto.setQuantityRequested(req.getQuantityRequested());
                reqDto.setQuantityApproved(req.getQuantityApproved());
                reqDto.setNotes(req.getNotes());
                reqDto.setRequestedAt(req.getRequestedAt());
                reqDto.setApprovedAt(req.getApprovedAt());
                if (req.getRequestedBy() != null) {
                    reqDto.setRequestedByName(req.getRequestedBy().getName());
                }
                if (req.getApprovedBy() != null) {
                    reqDto.setApprovedByName(req.getApprovedBy().getName());
                }

                // Map Details
                List<tmmsystem.entity.MaterialRequisitionDetail> details = reqDetailRepo
                        .findByRequisitionId(req.getId());
                List<tmmsystem.dto.execution.MaterialRequisitionDetailDto> detailDtos = details.stream().map(d -> {
                    tmmsystem.dto.execution.MaterialRequisitionDetailDto dd = new tmmsystem.dto.execution.MaterialRequisitionDetailDto();
                    dd.setId(d.getId());
                    dd.setMaterialId(d.getMaterial() != null ? d.getMaterial().getId() : null);
                    dd.setMaterialName(d.getMaterial() != null ? d.getMaterial().getName() : null);
                    dd.setQuantityRequested(d.getQuantityRequested());
                    dd.setQuantityApproved(d.getQuantityApproved());
                    return dd;
                }).collect(java.util.stream.Collectors.toList());
                reqDto.setDetails(detailDtos);

                dto.setMaterialRequisition(reqDto);
            }
        }

        return dto;
    }

    // Redo stage (fail nhẹ)
    @Transactional
    public ProductionStage redoStage(Long stageId) {
        ProductionStage s = stageRepo.findById(stageId).orElseThrow();
        syncStageStatus(s, "PENDING");
        s.setStartAt(null);
        s.setCompleteAt(null);
        s.setQcLastResult(null);
        s.setQcLastCheckedAt(null);
        return stageRepo.save(s);
    }

    // Filter lists for roles
    public List<ProductionStage> findStagesForLeader(Long leaderUserId) {
        return stageRepo.findByAssignedLeaderIdAndStatusIn(leaderUserId, java.util.List.of("PENDING", "IN_PROGRESS"));
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

        // FIX: If this was a rework, mark linked QualityIssues as RESOLVED
        if (Boolean.TRUE.equals(s.getIsRework())) {
            List<QualityIssue> linkedIssues = issueRepo.findByProductionStageId(stageId);
            for (QualityIssue issue : linkedIssues) {
                if ("PENDING".equals(issue.getStatus()) || "IN_PROGRESS".equals(issue.getStatus())) {
                    issue.setStatus("RESOLVED");
                    issue.setResolvedAt(Instant.now());
                    issueRepo.save(issue);
                }
            }
            // Clear rework flag now that it's resolved
            s.setIsRework(false);
        }

        syncStageStatus(s, "QC_PASSED");
        stageRepo.save(s);

        // NEW: Resume paused stages when rework stage completes
        // When a rework stage passes QC, resume all PAUSED stages of the same type
        boolean isReworkStage = Boolean.TRUE.equals(s.getIsRework()) ||
                (s.getProductionOrder() != null && s.getProductionOrder().getPoNumber() != null &&
                        s.getProductionOrder().getPoNumber().contains("-REWORK"));

        if (isReworkStage && !"DYEING".equalsIgnoreCase(s.getStageType())) {
            List<ProductionStage> pausedStages = stageRepo.findByStageTypeAndExecutionStatus(s.getStageType(),
                    "PAUSED");

            // Resume the oldest paused stage (by createdAt) to READY_TO_PRODUCE
            ProductionStage toResume = pausedStages.stream()
                    .filter(ps -> !ps.getId().equals(s.getId()))
                    .min((a, b) -> {
                        Instant aCreated = a.getCreatedAt() != null ? a.getCreatedAt() : Instant.MAX;
                        Instant bCreated = b.getCreatedAt() != null ? b.getCreatedAt() : Instant.MAX;
                        return aCreated.compareTo(bCreated);
                    })
                    .orElse(null);

            if (toResume != null) {
                toResume.setExecutionStatus("READY_TO_PRODUCE");
                toResume.setStatus("READY_TO_PRODUCE");
                stageRepo.save(toResume);

                // Notify leader that they can resume
                if (toResume.getAssignedLeader() != null) {
                    notificationService.notifyUser(toResume.getAssignedLeader(), "PRODUCTION", "SUCCESS",
                            "Có thể tiếp tục sản xuất",
                            "Lô bổ sung đã hoàn thành công đoạn " + s.getStageType() +
                                    ". Lô " + toResume.getProductionOrder().getPoNumber() +
                                    " có thể tiếp tục sản xuất.",
                            "PRODUCTION_STAGE", toResume.getId());
                }

                System.out.println("Resumed stage " + toResume.getId() + " for order " +
                        toResume.getProductionOrder().getPoNumber() + " after rework completion at "
                        + s.getStageType());
            }

            // Other paused stages remain PAUSED (will be promoted via
            // promoteNextOrderForStageType)
        }

        // Determine next stage in same ProductionOrder (NEW: không qua WorkOrderDetail)
        ProductionOrder po = s.getProductionOrder();
        if (po == null) {
            // Nếu stage chưa có ProductionOrder, không thể tìm next stage
            return;
        }

        List<ProductionStage> stages = stageRepo.findByProductionOrderIdOrderByStageSequenceAsc(po.getId());
        ProductionStage next = stages.stream().filter(x -> x.getStageSequence() != null && s.getStageSequence() != null
                && x.getStageSequence() == s.getStageSequence() + 1).findFirst().orElse(null);

        if (next != null) {
            // Set next stage to READY_TO_PRODUCE (Rolling Production)
            syncStageStatus(next, "READY_TO_PRODUCE");
            next.setExecutionStatus("READY_TO_PRODUCE");
            stageRepo.save(next);

            // AMBULANCE PRIORITY: If this is a rework order, pause ALL other stages at next
            // stage type
            if (isReworkStage && !"DYEING".equalsIgnoreCase(next.getStageType())) {
                List<String> statusesToPause = List.of("IN_PROGRESS", "WAITING", "READY_TO_PRODUCE");
                List<ProductionStage> stagesToPause = stageRepo.findByStageTypeAndExecutionStatusIn(
                        next.getStageType(), statusesToPause);

                for (ProductionStage other : stagesToPause) {
                    if (!other.getId().equals(next.getId())) {
                        // Skip if this is also a rework stage
                        boolean otherIsRework = Boolean.TRUE.equals(other.getIsRework()) ||
                                (other.getProductionOrder() != null && other.getProductionOrder().getPoNumber() != null
                                        &&
                                        other.getProductionOrder().getPoNumber().contains("-REWORK"));
                        if (otherIsRework) {
                            continue;
                        }

                        other.setExecutionStatus("PAUSED");
                        other.setStatus("PAUSED");
                        stageRepo.save(other);

                        // Notify leader
                        if (other.getAssignedLeader() != null) {
                            notificationService.notifyUser(other.getAssignedLeader(), "PRODUCTION", "WARNING",
                                    "Tạm dừng - Ưu tiên lô bổ sung",
                                    "Công đoạn " + other.getStageType() + " của lô " +
                                            other.getProductionOrder().getPoNumber() +
                                            " bị tạm dừng để ưu tiên lô bổ sung. Chờ lô bổ sung hoàn thành.",
                                    "PRODUCTION_STAGE", other.getId());
                        }
                    }
                }
                System.out.println("Ambulance priority: Paused stages at " + next.getStageType() +
                        " for rework order " + po.getPoNumber());
            }
        } else {
            // No next stage in this order - check if this is a rework order
            // If yes, we need to activate the next stage in the ORIGINAL order
            if (Boolean.TRUE.equals(s.getIsRework())
                    || (po.getPoNumber() != null && po.getPoNumber().contains("-REWORK"))) {
                // Find the original stage this rework stage is linked to
                ProductionStage originalStage = s.getOriginalStage();
                if (originalStage != null) {
                    ProductionOrder originalPO = originalStage.getProductionOrder();
                    if (originalPO != null) {
                        // Find the next stage in the original order (after the defective stage)
                        int defectiveSequence = originalStage.getStageSequence() != null
                                ? originalStage.getStageSequence()
                                : 0;
                        List<ProductionStage> originalStages = stageRepo
                                .findByProductionOrderIdOrderByStageSequenceAsc(originalPO.getId());
                        ProductionStage originalNext = originalStages.stream()
                                .filter(x -> x.getStageSequence() != null
                                        && x.getStageSequence() == defectiveSequence + 1)
                                .findFirst().orElse(null);

                        if (originalNext != null) {
                            // Activate the next stage in the original order
                            syncStageStatus(originalNext, "READY_TO_PRODUCE");
                            originalNext.setExecutionStatus("READY_TO_PRODUCE");
                            stageRepo.save(originalNext);

                            // Mark original order as back in production
                            originalPO.setExecutionStatus("IN_PROGRESS");
                            poRepo.save(originalPO);

                            // Notify the leader
                            if (originalNext.getAssignedLeader() != null) {
                                notificationService.notifyUser(originalNext.getAssignedLeader(), "PRODUCTION",
                                        "SUCCESS",
                                        "Lô bổ sung hoàn thành",
                                        "Lô bổ sung đã hoàn thành. Công đoạn " + originalNext.getStageType()
                                                + " của lô chính sẵn sàng tiếp tục.",
                                        "PRODUCTION_STAGE", originalNext.getId());
                            }
                        }

                        // Mark rework order as completed
                        po.setStatus("ORDER_COMPLETED");
                        po.setExecutionStatus("IN_SUPPLEMENTARY"); // Keep as supplementary completed
                        poRepo.save(po);
                    }
                }
            }
        }

        String currentStageType = s.getStageType();

        // Workflow logic based on current stage - only if there's a next stage
        if (next != null
                && ("WARPING".equalsIgnoreCase(currentStageType) || "CUONG_MAC".equalsIgnoreCase(currentStageType))) {
            // Cuồng mắc PASS → notify Tổ Trưởng dệt
            if (next.getAssignedLeader() != null) {
                notificationService.notifyUser(next.getAssignedLeader(), "PRODUCTION", "SUCCESS",
                        "Công đoạn cuồng mắc đạt",
                        "Công đoạn cuồng mắc đã đạt QC. Bạn có thể bắt đầu công đoạn " + next.getStageType(),
                        "PRODUCTION_STAGE", next.getId());
            }
        } else if (next != null
                && ("WEAVING".equalsIgnoreCase(currentStageType) || "DET".equalsIgnoreCase(currentStageType))) {
            // Dệt PASS → notify Production Manager (for nhuộm)
            if ("DYEING".equalsIgnoreCase(next.getStageType()) || "NHUOM".equalsIgnoreCase(next.getStageType())) {
                notificationService.notifyRole("PRODUCTION_MANAGER", "PRODUCTION", "SUCCESS",
                        "Công đoạn dệt đạt",
                        "Công đoạn dệt đã đạt QC. Công đoạn nhuộm sẵn sàng bắt đầu.",
                        "PRODUCTION_STAGE", next.getId());
            } else if (next.getAssignedLeader() != null) {
                notificationService.notifyUser(next.getAssignedLeader(), "PRODUCTION", "SUCCESS",
                        "Công đoạn dệt đạt",
                        "Công đoạn dệt đã đạt QC. Bạn có thể bắt đầu công đoạn " + next.getStageType(),
                        "PRODUCTION_STAGE", next.getId());
            }
        } else if (next != null
                && ("DYEING".equalsIgnoreCase(currentStageType) || "NHUOM".equalsIgnoreCase(currentStageType))) {
            // Nhuộm PASS → notify Tổ Trưởng cắt
            if (next.getAssignedLeader() != null) {
                notificationService.notifyUser(next.getAssignedLeader(), "PRODUCTION", "SUCCESS",
                        "Công đoạn nhuộm đạt",
                        "Công đoạn nhuộm đã đạt QC. Bạn có thể bắt đầu công đoạn " + next.getStageType(),
                        "PRODUCTION_STAGE", next.getId());
            }
        } else if (next != null
                && ("CUTTING".equalsIgnoreCase(currentStageType) || "CAT".equalsIgnoreCase(currentStageType))) {
            // Cắt PASS → notify Tổ Trưởng May
            if (next.getAssignedLeader() != null) {
                notificationService.notifyUser(next.getAssignedLeader(), "PRODUCTION", "SUCCESS",
                        "Công đoạn cắt đạt",
                        "Công đoạn cắt đã đạt QC. Bạn có thể bắt đầu công đoạn " + next.getStageType(),
                        "PRODUCTION_STAGE", next.getId());
            }
        } else if (next != null
                && ("HEMMING".equalsIgnoreCase(currentStageType) || "MAY".equalsIgnoreCase(currentStageType))) {
            // May PASS → notify Tổ Trưởng đóng gói
            if (next.getAssignedLeader() != null) {
                notificationService.notifyUser(next.getAssignedLeader(), "PRODUCTION", "SUCCESS",
                        "Công đoạn may đạt",
                        "Công đoạn may đã đạt QC. Bạn có thể bắt đầu công đoạn " + next.getStageType(),
                        "PRODUCTION_STAGE", next.getId());
            }
        } else if ("PACKAGING".equalsIgnoreCase(currentStageType)
                || "DONG_GOI".equalsIgnoreCase(currentStageType)) {
            // Đóng gói PASS → notify Kho
            notificationService.notifyRole("INVENTORY_STAFF", "PRODUCTION", "SUCCESS",
                    "Công đoạn đóng gói đạt",
                    "Công đoạn đóng gói đã đạt QC. Sản phẩm sẵn sàng nhập kho.",
                    "PRODUCTION_STAGE", s.getId());

            // Check if all stages completed
            // NEW: Lấy ProductionOrder trực tiếp từ ProductionStage
            ProductionOrder poForCheck = s.getProductionOrder();

            if (poForCheck != null) {
                final Long orderId = poForCheck.getId(); // Make effectively final for lambda
                // NEW: Check all stages directly by ProductionOrder
                List<ProductionStage> allStagesForOrder = stageRepo.findStagesByOrderId(orderId);
                boolean allOk = true;

                if (!allStagesForOrder.isEmpty()) {
                    // Check via ProductionOrder (new way)
                    for (ProductionStage stg : allStagesForOrder) {
                        if (!"COMPLETED".equals(stg.getExecutionStatus()) &&
                                !"QC_PASSED".equals(stg.getExecutionStatus())) {
                            allOk = false;
                            break;
                        }
                    }
                }
                // REMOVED: Fallback check via WorkOrderDetail - không còn dùng nữa

                if (allOk) {
                    poForCheck.setStatus("ORDER_COMPLETED");
                    poRepo.save(poForCheck);
                    notificationService.notifyOrderCompleted(poForCheck);
                }
            }
        }

        // NEW: Promote next order for this stage type (cross-PO promotion)
        // This enables the next lot in queue to start the same stage type
        promoteNextOrderForStageType(s.getStageType());
    }

    // Dyeing hook on start/complete
    @Transactional
    public ProductionStage startStage(Long stageId, Long leaderUserId) {
        return startStage(stageId, leaderUserId, null, BigDecimal.ZERO);
    }

    @Transactional
    public ProductionStage startStage(Long stageId, Long leaderUserId, String evidencePhotoUrl,
            BigDecimal qtyCompleted) {
        ensureLeader(stageId, leaderUserId);
        ProductionStage s = stageRepo.findById(stageId).orElseThrow();

        // Validation (Merged from duplicate method)
        // Allow start if READY_TO_PRODUCE or WAITING (fallback)
        if (!"READY_TO_PRODUCE".equals(s.getExecutionStatus()) && !"WAITING".equals(s.getExecutionStatus())) {
            // Allow re-start if it was PAUSED?
            if (!"PAUSED".equals(s.getStatus())) {
                throw new RuntimeException("Công đoạn chưa sẵn sàng hoặc đã bắt đầu.");
            }
        }

        // NEW: Block normal orders if a rework order is active at this stage type
        boolean thisIsRework = Boolean.TRUE.equals(s.getIsRework()) ||
                (s.getProductionOrder() != null && s.getProductionOrder().getPoNumber() != null &&
                        s.getProductionOrder().getPoNumber().contains("-REWORK"));

        if (!thisIsRework && !"DYEING".equalsIgnoreCase(s.getStageType())) {
            // Check if any rework order is currently IN_PROGRESS at this stage type
            List<ProductionStage> reworkStagesInProgress = stageRepo.findByStageTypeAndExecutionStatus(
                    s.getStageType(), "IN_PROGRESS");
            for (ProductionStage reworkStage : reworkStagesInProgress) {
                boolean isRework = Boolean.TRUE.equals(reworkStage.getIsRework()) ||
                        (reworkStage.getProductionOrder() != null &&
                                reworkStage.getProductionOrder().getPoNumber() != null &&
                                reworkStage.getProductionOrder().getPoNumber().contains("-REWORK"));
                if (isRework) {
                    String reworkPO = reworkStage.getProductionOrder() != null
                            ? reworkStage.getProductionOrder().getPoNumber()
                            : "lô bổ sung";
                    throw new RuntimeException("Công đoạn " + s.getStageType() +
                            " đang ưu tiên cho lô bổ sung " + reworkPO +
                            ". Vui lòng chờ lô bổ sung hoàn thành công đoạn này.");
                }
            }
        }

        // Enforce single-lot rule per stage type (except outsourced dyeing)
        // Only block if there's a stage truly IN production (not WAITING/PENDING)
        boolean isOutsourcedDyeing = "DYEING".equalsIgnoreCase(s.getStageType())
                && Boolean.TRUE.equals(s.getOutsourced());
        if (!isOutsourcedDyeing) {
            // Truly active = already started production, not just waiting in queue
            List<String> TRULY_ACTIVE_STATUSES = List.of("IN_PROGRESS",
                    "WAITING_QC", "QC_IN_PROGRESS", "QC_FAILED", "WAITING_REWORK",
                    "REWORK_IN_PROGRESS", "PAUSED");
            long activeSameType = stageRepo.countActiveByStageTypeExcludingStage(s.getStageType(), s.getId(),
                    TRULY_ACTIVE_STATUSES);
            if (activeSameType > 0) {
                throw new RuntimeException("Công đoạn " + s.getStageType()
                        + " đang bận với lô khác. Vui lòng chờ hoàn tất trước khi chạy lô mới.");
            }
        }

        if (s.getStartAt() == null)
            s.setStartAt(Instant.now());

        // FIX: Reset progress when starting/restarting a stage
        // This handles restart from PAUSED state or re-running a completed stage
        if (s.getProgressPercent() != null && s.getProgressPercent() >= 100) {
            s.setProgressPercent(0);
            s.setStartAt(Instant.now()); // Also reset start time for new run
            s.setCompleteAt(null); // Clear completion time
        }

        syncStageStatus(s, "IN_PROGRESS");
        stageRepo.save(s);

        // FIX: Demote other READY_TO_PRODUCE stages of same type to WAITING
        // This ensures only one lot can be "sẵn sàng", others show "chờ đến lượt"
        if (!"DYEING".equalsIgnoreCase(s.getStageType())) { // Skip for parallel DYEING stages
            List<ProductionStage> readyStages = stageRepo.findByStageTypeAndExecutionStatus(
                    s.getStageType(), "READY_TO_PRODUCE");
            for (ProductionStage other : readyStages) {
                if (!other.getId().equals(s.getId())) {
                    other.setExecutionStatus("WAITING");
                    syncStageStatus(other, "WAITING");
                    stageRepo.save(other);
                }
            }
        }

        StageTracking tr = new StageTracking();
        tr.setProductionStage(s);
        tr.setOperator(userRepository.findById(leaderUserId).orElseThrow());
        tr.setAction("START");
        tr.setEvidencePhotoUrl(evidencePhotoUrl);
        tr.setQuantityCompleted(qtyCompleted);
        stageTrackingRepository.save(tr);
        if ("DYEING".equalsIgnoreCase(s.getStageType()) && Boolean.TRUE.equals(s.getOutsourced())) {
            // create outsourcing task if not exists for this stage
            if (outsourcingTaskRepository.findByProductionStageId(s.getId()).isEmpty()) {
                OutsourcingTask ot = new OutsourcingTask();
                ot.setProductionStage(s);
                // Fallback if vendor not specified
                String vendor = s.getOutsourceVendor();
                ot.setVendorName(vendor != null && !vendor.isEmpty() ? vendor : "Đơn vị nhuộm ngoài");
                ot.setSentAt(Instant.now());
                ot.setStatus("SENT");
                outsourcingTaskRepository.save(ot);
            }
        }
        // NEW: Automate machine status - Set ALL machines of this type to IN_USE
        machineRepository.updateStatusByType(s.getStageType(), "IN_USE");

        // NEW: Log Machine Assignment for Traceability
        List<Machine> machines = machineRepository.findAll().stream()
                .filter(m -> s.getStageType().equals(m.getType()))
                .toList();

        for (Machine m : machines) {
            MachineAssignment ma = new MachineAssignment();
            ma.setMachine(m);
            ma.setProductionStage(s);
            ma.setAssignedAt(Instant.now());
            ma.setReservationStatus("ACTIVE");
            ma.setReservationType("LOG");
            machineAssignmentRepository.save(ma);
        }

        // NEW: Full Rework Preemption Logic
        // If this is a Rework Stage, PAUSE ALL other stages of the same type (except
        // DYEING)
        // This includes IN_PROGRESS, WAITING, and READY_TO_PRODUCE stages
        boolean isReworkStage = Boolean.TRUE.equals(s.getIsRework()) ||
                (s.getProductionOrder() != null && s.getProductionOrder().getPoNumber() != null &&
                        s.getProductionOrder().getPoNumber().contains("-REWORK"));

        if (isReworkStage && !"DYEING".equalsIgnoreCase(s.getStageType())) {
            // Pause ALL stages of same type that are not this rework stage
            List<String> statusesToPause = List.of("IN_PROGRESS", "WAITING", "READY_TO_PRODUCE");
            List<ProductionStage> stagesToPause = stageRepo.findByStageTypeAndExecutionStatusIn(s.getStageType(),
                    statusesToPause);

            for (ProductionStage other : stagesToPause) {
                if (!other.getId().equals(s.getId())) {
                    // Skip if this is also a rework stage (don't pause other rework orders)
                    boolean otherIsRework = Boolean.TRUE.equals(other.getIsRework()) ||
                            (other.getProductionOrder() != null && other.getProductionOrder().getPoNumber() != null &&
                                    other.getProductionOrder().getPoNumber().contains("-REWORK"));
                    if (otherIsRework) {
                        continue; // Don't pause other rework orders
                    }

                    other.setExecutionStatus("PAUSED");
                    other.setStatus("PAUSED");
                    stageRepo.save(other);

                    // Log tracking
                    StageTracking pauseTr = new StageTracking();
                    pauseTr.setProductionStage(other);
                    pauseTr.setOperator(userRepository.findById(leaderUserId).orElseThrow());
                    pauseTr.setAction("PAUSED_BY_REWORK_PRIORITY");
                    pauseTr.setNotes(
                            "Tạm dừng do ưu tiên lô sản xuất bổ sung: " + s.getProductionOrder().getPoNumber());
                    stageTrackingRepository.save(pauseTr);

                    // Notify Leader of paused stage
                    if (other.getAssignedLeader() != null) {
                        notificationService.notifyUser(other.getAssignedLeader(), "PRODUCTION", "WARNING",
                                "Tạm dừng sản xuất - Ưu tiên lô bổ sung",
                                "Công đoạn " + other.getStageType() + " của lô "
                                        + other.getProductionOrder().getPoNumber() +
                                        " bị tạm dừng để ưu tiên lô bổ sung " + s.getProductionOrder().getPoNumber() +
                                        ". Vui lòng chờ lô bổ sung hoàn thành công đoạn này.",
                                "PRODUCTION_STAGE", other.getId());
                    }
                }
            }

            System.out.println("Rework preemption: Paused " + (stagesToPause.size() - 1) + " stages for rework order "
                    + s.getProductionOrder().getPoNumber() + " at stage " + s.getStageType());
        }

        // NEW: Trigger event-driven stage promotion for other stage types
        // When a stage starts, check if other lots can be promoted for different stages
        // (e.g., when WARPING starts, check if any WEAVING/DYEING/etc. can be promoted)
        for (String type : List.of("WARPING", "WEAVING", "DYEING", "CUTTING", "HEMMING", "PACKAGING")) {
            if (!type.equalsIgnoreCase(s.getStageType())) {
                promoteNextOrderForStageType(type);
            }
        }

        return s;
    }

    // REMOVED: createStandardWorkOrder, createDefaultStage - không còn dùng nữa
    // Stages được tạo trực tiếp từ ProductionPlan khi director approve

    /**
     * Create ProductionStages directly from planning stages (NEW: không qua
     * WorkOrder)
     * Tạo stages trực tiếp với ProductionOrder
     * Trả về Map để tương thích với reservePlanStages()
     */
    @Transactional
    public Map<Long, List<ProductionStage>> createStagesFromPlan(Long poId, List<ProductionPlanStage> planStages) {
        ProductionOrder po = poRepo.findById(poId).orElseThrow(() -> new RuntimeException("PO not found"));
        Map<Long, List<ProductionStage>> stageMapping = new HashMap<>();

        // SAFEGUARD: Check if stages already exist to prevent duplication
        if (stageRepo.existsByProductionOrderId(poId)) {
            System.out.println("Stages already exist for PO " + poId + ". Skipping creation.");
            return stageMapping;
        }

        // Nếu không có planStages, tạo 6 stages mặc định
        if (planStages == null || planStages.isEmpty()) {
            List<ProductionStage> defaultStages = new ArrayList<>();
            String[] stageTypes = { "WARPING", "WEAVING", "DYEING", "CUTTING", "HEMMING", "PACKAGING" };
            boolean[] outsourced = { false, false, true, false, false, false };

            for (int i = 0; i < stageTypes.length; i++) {
                ProductionStage stage = new ProductionStage();
                stage.setProductionOrder(po);
                stage.setStageType(stageTypes[i]);
                stage.setStageSequence(i + 1);
                // All stages start as PENDING. PM's startWorkOrder will set first stage to
                // WAITING.
                syncStageStatus(stage, "PENDING");
                stage.setOutsourced(outsourced[i]);
                stage.setProgressPercent(0);

                // NEW: Generate QR Token for first stage
                if (i == 0) {
                    stage.setQrToken(generateQrToken());
                }

                defaultStages.add(stageRepo.save(stage));
            }
            // Return empty map for default stages (no planStage mapping)
            return stageMapping;
        }

        // Sắp xếp planStages theo sequence
        List<ProductionPlanStage> orderedStages = planStages.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(stage -> stage.getSequenceNo() == null ? Integer.MAX_VALUE : stage.getSequenceNo()))
                .toList();

        Integer firstSequence = orderedStages.stream()
                .map(ProductionPlanStage::getSequenceNo)
                .filter(Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(null);

        List<ProductionOrderDetail> pods = podRepo.findByProductionOrderId(poId);
        String productName = pods.isEmpty() ? ""
                : pods.stream()
                        .map(this::safeProductName)
                        .distinct()
                        .collect(Collectors.joining(", "));

        for (ProductionPlanStage planStage : orderedStages) {
            ProductionStage stage = new ProductionStage();
            stage.setProductionOrder(po); // NEW: Link trực tiếp với ProductionOrder
            stage.setStageType(planStage.getStageType());
            stage.setStageSequence(planStage.getSequenceNo());

            // Validate và resolve machine/user từ DB để tránh FK constraint violation
            stage.setMachine(resolveMachine(planStage.getAssignedMachine()));
            stage.setAssignedLeader(resolveUser(planStage.getInChargeUser()));
            stage.setQcAssignee(resolveUser(planStage.getQcUser()));
            stage.setPlannedStartAt(toInstant(planStage.getPlannedStartTime()));
            stage.setPlannedEndAt(toInstant(planStage.getPlannedEndTime()));
            stage.setPlannedDurationHours(calculateDurationHours(planStage.getPlannedStartTime(),
                    planStage.getPlannedEndTime()));
            stage.setOutsourced("DYEING".equalsIgnoreCase(planStage.getStageType()));

            boolean isFirstStage = firstSequence != null && firstSequence.equals(planStage.getSequenceNo());
            // All stages start as PENDING. PM's startWorkOrder will set first stage to
            // WAITING.
            syncStageStatus(stage, "PENDING");
            stage.setProgressPercent(0);

            // NEW: Generate QR Token for first stage
            if (isFirstStage) {
                stage.setQrToken(generateQrToken());
            }

            ProductionStage savedStage = stageRepo.save(stage);
            // Map theo planStage.id để tương thích với reservePlanStages()
            stageMapping.computeIfAbsent(planStage.getId(), key -> new ArrayList<>()).add(savedStage);
            notifyStageAssignments(savedStage, po.getPoNumber(), productName);
        }

        return stageMapping;
    }

    @Transactional
    public ProductionStage pauseStage(Long stageId, Long leaderUserId, String pauseReason, String pauseNotes) {
        ensureLeader(stageId, leaderUserId);
        ProductionStage s = stageRepo.findById(stageId).orElseThrow();

        // 1. Pause the requested stage
        pauseSingleStage(s, leaderUserId, pauseReason, pauseNotes);

        // 2. CASCADE PAUSE: Find other IN_PROGRESS stages on the same machine
        if (s.getMachine() != null) {
            List<ProductionStage> otherActiveStages = stageRepo.findByMachineIdAndExecutionStatus(
                    s.getMachine().getId(), "IN_PROGRESS");

            for (ProductionStage otherStage : otherActiveStages) {
                // Skip the current stage (already paused)
                if (otherStage.getId().equals(s.getId()))
                    continue;

                // Pause other stages with a system reason
                String systemReason = "Tạm dừng do đơn hàng khác (" + s.getProductionOrder().getPoNumber()
                        + ") gặp sự cố trên cùng máy.";
                pauseSingleStage(otherStage, leaderUserId, "CASCADE_PAUSE", systemReason);

                // Notify the leader of the other stage
                if (otherStage.getAssignedLeader() != null) {
                    notificationService.notifyUser(otherStage.getAssignedLeader(), "PRODUCTION", "WARNING",
                            "Công đoạn bị tạm dừng",
                            "Công đoạn của bạn bị tạm dừng do máy " + s.getMachine().getName()
                                    + " đang xử lý sự cố của đơn hàng khác.",
                            "PRODUCTION_STAGE", otherStage.getId());
                }
            }
        }

        return s;
    }

    private void pauseSingleStage(ProductionStage s, Long userId, String reason, String notes) {
        // PAUSED không có trong executionStatus, giữ status riêng
        s.setStatus("PAUSED");
        // executionStatus giữ nguyên để biết đang ở giai đoạn nào
        stageRepo.save(s);

        StagePauseLog pl = new StagePauseLog();
        pl.setProductionStage(s);
        pl.setPausedBy(userRepository.findById(userId).orElseThrow());
        pl.setPauseReason(reason);
        pl.setPauseNotes(notes);
        pl.setPausedAt(Instant.now());
        stagePauseLogRepository.save(pl);

        StageTracking tr = new StageTracking();
        tr.setProductionStage(s);
        tr.setOperator(pl.getPausedBy());
        tr.setAction("PAUSE");
        stageTrackingRepository.save(tr);
    }

    @Transactional
    public ProductionStage resumeStage(Long stageId, Long leaderUserId) {
        ensureLeader(stageId, leaderUserId);
        ProductionStage s = stageRepo.findById(stageId).orElseThrow();

        // Resume: executionStatus was preserved during pause, just clear PAUSED status
        // The executionStatus tells us what state the stage was in before pause
        String originalExecStatus = s.getExecutionStatus();

        // If executionStatus is one of the "working" statuses, restore it
        // Otherwise default to IN_PROGRESS (for safety)
        if (originalExecStatus == null || "PAUSED".equals(originalExecStatus)
                || "PENDING".equals(originalExecStatus) || "WAITING".equals(originalExecStatus)) {
            // Fallback: set to IN_PROGRESS if original status was not a valid working state
            syncStageStatus(s, "IN_PROGRESS");
        } else {
            // Restore: just clear PAUSED from status field, keep executionStatus as-is
            s.setStatus(originalExecStatus);
        }
        stageRepo.save(s);

        // close last open pause
        List<StagePauseLog> pauses = stagePauseLogRepository.findByProductionStageIdOrderByPausedAtDesc(s.getId());
        for (StagePauseLog p : pauses) {
            if (p.getResumedAt() == null) {
                p.setResumedAt(Instant.now());
                if (p.getPausedAt() != null) {
                    long mins = java.time.Duration.between(p.getPausedAt(), p.getResumedAt()).toMinutes();
                    p.setDurationMinutes((int) mins);
                }
                stagePauseLogRepository.save(p);
                break;
            }
        }
        StageTracking tr = new StageTracking();
        tr.setProductionStage(s);
        tr.setOperator(userRepository.findById(leaderUserId).orElseThrow());
        tr.setAction("RESUME");
        stageTrackingRepository.save(tr);
        return s;
    }

    @Transactional
    public ProductionStage leaderUpdateProgress(Long stageId, Long leaderUserId, BigDecimal progressPercent) {
        ensureLeader(stageId, leaderUserId);
        ProductionStage s = stageRepo.findById(stageId).orElseThrow();

        // NEW: Block update if status is QC_FAILED (must wait for Tech decision)
        if ("QC_FAILED".equals(s.getExecutionStatus()) || "QC_FAILED".equals(s.getStatus())) {
            throw new RuntimeException(
                    "BLOCKING: Công đoạn đang chờ kỹ thuật xử lý lỗi (QC Failed). Vui lòng đợi chỉ đạo.");
        }

        if (progressPercent.compareTo(BigDecimal.valueOf(100)) >= 0) {
            s.setProgressPercent(100);
            syncStageStatus(s, "WAITING_QC");
            s.setExecutionStatus("WAITING_QC"); // Explicitly set execution status
            s.setCompleteAt(Instant.now());
            releaseMachineReservations(s);

            // NEW: Automate machine status - Set ALL machines of this type to AVAILABLE
            machineRepository.updateStatusByType(s.getStageType(), "AVAILABLE");

            // NEW: Release Machine Assignment Log
            List<MachineAssignment> activeAssignments = machineAssignmentRepository
                    .findByProductionStageIdAndReleasedAtIsNull(s.getId());
            for (MachineAssignment ma : activeAssignments) {
                ma.setReleasedAt(Instant.now());
                ma.setReservationStatus("RELEASED");
                machineAssignmentRepository.save(ma);
            }

            // Notify QC
            if (s.getQcAssignee() != null) {
                notificationService.notifyUser(s.getQcAssignee(), "QC", "INFO", "Chờ kiểm tra QC",
                        "Công đoạn " + s.getStageType() + " đã hoàn thành 100%, chờ kiểm tra.", "PRODUCTION_STAGE",
                        s.getId());
            } else {
                notificationService.notifyRole("QC_STAFF", "QC", "INFO", "Chờ kiểm tra QC",
                        "Công đoạn " + s.getStageType() + " đã hoàn thành 100%, chờ kiểm tra.", "PRODUCTION_STAGE",
                        s.getId());
            }

            // NEW: Rework Preemption Logic - Resume Paused Stages
            // If this Rework Stage is completed, RESUME all PAUSED stages of the same type
            if (Boolean.TRUE.equals(s.getIsRework()) && !"DYEING".equalsIgnoreCase(s.getStageType())) {
                List<ProductionStage> pausedStages = stageRepo.findByStageTypeAndExecutionStatus(s.getStageType(),
                        "PAUSED");
                for (ProductionStage paused : pausedStages) {
                    paused.setExecutionStatus("IN_PROGRESS");
                    paused.setStatus("IN_PROGRESS");
                    stageRepo.save(paused);

                    // Log tracking
                    StageTracking resumeTr = new StageTracking();
                    resumeTr.setProductionStage(paused);
                    resumeTr.setOperator(userRepository.findById(leaderUserId).orElseThrow());
                    resumeTr.setAction("RESUMED_FROM_PRIORITY");
                    resumeTr.setNotes("Tiếp tục sản xuất sau khi đơn hàng bù hoàn thành.");
                    stageTrackingRepository.save(resumeTr);

                    // Notify Leader
                    if (paused.getAssignedLeader() != null) {
                        String poNumber = paused.getProductionOrder() != null
                                ? paused.getProductionOrder().getPoNumber()
                                : "N/A";
                        notificationService.notifyUser(paused.getAssignedLeader(), "PRODUCTION", "INFO",
                                "Tiếp tục sản xuất",
                                "Công đoạn " + paused.getStageType() + " của đơn " + poNumber +
                                        " đã được tiếp tục.",
                                "PRODUCTION_STAGE", paused.getId());
                    }
                }
            }

            // NEW: Trigger event-driven stage promotion when stage reaches 100%
            // This frees the slot for the next lot to start
            promoteNextOrderForStageType(s.getStageType());
        } else {
            s.setProgressPercent(progressPercent.intValue());
            syncStageStatus(s, "IN_PROGRESS");
        }

        ProductionStage saved = stageRepo.save(s);

        StageTracking tr = new StageTracking();
        tr.setProductionStage(saved);
        tr.setOperator(userRepository.findById(leaderUserId).orElseThrow());
        tr.setAction(progressPercent.compareTo(BigDecimal.valueOf(100)) >= 0 ? "COMPLETE" : "UPDATE_PROGRESS");
        tr.setQuantityCompleted(progressPercent);

        // Robustly determine isRework: Check flag OR status
        boolean isRework = Boolean.TRUE.equals(s.getIsRework()) ||
                "REWORK_IN_PROGRESS".equals(s.getExecutionStatus()) ||
                "WAITING_REWORK".equals(s.getExecutionStatus());
        tr.setIsRework(isRework);

        stageTrackingRepository.save(tr);

        return saved;
    }

    @Transactional
    public ProductionStage completeStage(Long stageId, Long leaderUserId, String evidencePhotoUrl,
            BigDecimal qtyCompleted) {
        // Deprecated or alias to updateProgress(100)
        return leaderUpdateProgress(stageId, leaderUserId, BigDecimal.valueOf(100));
    }

    // ==== ASSIGNMENT METHODS ====
    // Delegates (placed early to satisfy resolution order if tool incomplete
    // indexing)
    @Transactional
    public ProductionOrder assignTechnician(Long poId, Long technicianUserId) {
        return internalAssignTechnician(poId, technicianUserId);
    }

    // ==== ROLLING PRODUCTION WORKFLOW METHODS ====

    @Transactional
    public List<ProductionStage> startWorkOrder(Long orderId) {
        ProductionOrder po = poRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Production Order not found"));

        // 1. Capacity Check for WARPING (First stage - bottleneck)
        // Fixed limit: 1 WAITING + 1 READY_TO_PRODUCE + 1 IN_PROGRESS = 3 max
        // All machines work on ONE lot at a time, this is just queue management
        List<String> activeStatuses = List.of("WAITING", "READY_TO_PRODUCE", "IN_PROGRESS");
        long activeWarpingCount = stageRepo.countByStageTypeAndExecutionStatusIn("WARPING", activeStatuses);

        // Fixed max: 3 lots in WARPING pipeline at any time
        final long MAX_WARPING_QUEUE = 3;

        // REWORK ORDERS GET PRIORITY - bypass queue limit and preempt if needed
        boolean isReworkOrder = po.getPoNumber() != null && po.getPoNumber().contains("-REWORK");

        if (activeWarpingCount >= MAX_WARPING_QUEUE) {
            if (isReworkOrder) {
                // Rework order: preempt the oldest WAITING normal order to make room
                List<ProductionStage> waitingWarpingStages = stageRepo.findByStageTypeAndExecutionStatusIn("WARPING",
                        List.of("WAITING"));
                // Find oldest non-rework order to preempt
                ProductionStage toPreempt = waitingWarpingStages.stream()
                        .filter(s -> s.getProductionOrder() != null &&
                                (s.getProductionOrder().getPoNumber() == null ||
                                        !s.getProductionOrder().getPoNumber().contains("-REWORK")))
                        .min((a, b) -> {
                            Instant aCreated = a.getCreatedAt() != null ? a.getCreatedAt() : Instant.MAX;
                            Instant bCreated = b.getCreatedAt() != null ? b.getCreatedAt() : Instant.MAX;
                            return aCreated.compareTo(bCreated);
                        })
                        .orElse(null);

                if (toPreempt != null) {
                    // Pause this normal order to make room for rework
                    toPreempt.setExecutionStatus("PENDING");
                    toPreempt.setStatus("PENDING");
                    stageRepo.save(toPreempt);

                    // Also update the production order status
                    ProductionOrder preemptedPO = toPreempt.getProductionOrder();
                    if (preemptedPO != null) {
                        preemptedPO.setExecutionStatus("WAITING_PRODUCTION");
                        poRepo.save(preemptedPO);

                        // Notify the leader
                        if (toPreempt.getAssignedLeader() != null) {
                            notificationService.notifyUser(toPreempt.getAssignedLeader(), "PRODUCTION", "WARNING",
                                    "Lô bị tạm dừng",
                                    "Lô " + preemptedPO.getPoNumber()
                                            + " đã bị tạm dừng để ưu tiên lô bổ sung (sửa lỗi).",
                                    "PRODUCTION_ORDER", preemptedPO.getId());
                        }
                    }
                    System.out
                            .println("Preempted normal order " + (preemptedPO != null ? preemptedPO.getPoNumber() : "?")
                                    + " to make room for rework order " + po.getPoNumber());
                }
                // Continue with starting the rework order even if preemption failed
            } else {
                // Normal order: block if queue is full
                // Build detailed message with lot codes
                List<ProductionStage> activeWarpingStages = stageRepo.findByStageTypeAndExecutionStatusIn("WARPING",
                        activeStatuses);
                StringBuilder occupyingLots = new StringBuilder();
                StringBuilder waitingLots = new StringBuilder();

                for (ProductionStage stage : activeWarpingStages) {
                    String lotCode = "N/A";
                    if (stage.getProductionOrder() != null) {
                        lotCode = stage.getProductionOrder().getPoNumber();
                        // Try to get lotCode from plan
                        String planCode = extractPlanCodeFromNotes(stage.getProductionOrder().getNotes());
                        if (planCode != null) {
                            ProductionPlan plan = productionPlanRepository.findByPlanCode(planCode).orElse(null);
                            if (plan != null && plan.getLot() != null) {
                                lotCode = plan.getLot().getLotCode();
                            }
                        }
                    }

                    if ("IN_PROGRESS".equals(stage.getExecutionStatus())) {
                        if (occupyingLots.length() > 0)
                            occupyingLots.append(", ");
                        occupyingLots.append(lotCode);
                    } else {
                        if (waitingLots.length() > 0)
                            waitingLots.append(", ");
                        waitingLots.append(lotCode);
                    }
                }

                String message = "Không thể bắt đầu lệnh. Hàng chờ Cuồng mắc đã đầy ("
                        + activeWarpingCount + "/" + MAX_WARPING_QUEUE + ").";
                if (occupyingLots.length() > 0) {
                    message += " Đang chiếm dụng: " + occupyingLots + ".";
                }
                if (waitingLots.length() > 0) {
                    message += " Đang chờ: " + waitingLots + ".";
                }
                message += " Vui lòng đợi lô trước hoàn thành.";

                throw new RuntimeException(message);
            }
        }

        // 2. Update Order Status - mark as in production queue
        po.setExecutionStatus("IN_PROGRESS");
        po.setStatus("IN_PROGRESS");
        poRepo.save(po);

        // 2. Set First Stage (WARPING) to WAITING (queue for scheduler to promote)
        List<ProductionStage> stages = stageRepo.findByProductionOrderIdOrderByStageSequenceAsc(orderId);
        if (stages.isEmpty()) {
            throw new RuntimeException("Đơn hàng chưa có công đoạn nào.");
        }

        ProductionStage firstStage = stages.get(0);
        // Set to WAITING - scheduler will promote to READY_TO_PRODUCE when slot
        // available
        syncStageStatus(firstStage, "WAITING");
        firstStage.setExecutionStatus("WAITING");
        stageRepo.save(firstStage);

        // 3. Set other stages to PENDING
        for (int i = 1; i < stages.size(); i++) {
            ProductionStage s = stages.get(i);
            s.setExecutionStatus("PENDING");
            s.setStatus("PENDING");
            stageRepo.save(s);
        }

        // 4. Trigger scheduler to check if slot is available and promote immediately
        promoteNextOrderForStageType(firstStage.getStageType());

        // 5. Notify Leader (will be notified again when promoted to READY_TO_PRODUCE)
        if (firstStage.getAssignedLeader() != null) {
            String currentStatus = stageRepo.findById(firstStage.getId()).map(ProductionStage::getExecutionStatus)
                    .orElse("WAITING");
            // Get lotCode for notification
            String lotCode = po.getPoNumber(); // Fallback
            String planCode = extractPlanCodeFromNotes(po.getNotes());
            if (planCode != null) {
                ProductionPlan plan = productionPlanRepository.findByPlanCode(planCode).orElse(null);
                if (plan != null && plan.getLot() != null) {
                    lotCode = plan.getLot().getLotCode();
                }
            }
            String message = "READY_TO_PRODUCE".equals(currentStatus)
                    ? "Lô " + lotCode + " đã sẵn sàng sản xuất công đoạn " + firstStage.getStageType()
                    : "Lô " + lotCode + " đã vào hàng chờ công đoạn " + firstStage.getStageType();
            notificationService.notifyUser(firstStage.getAssignedLeader(), "PRODUCTION", "INFO",
                    "Đơn hàng mới",
                    message,
                    "PRODUCTION_STAGE", firstStage.getId());
        }

        return stages;
    }

    @Transactional
    public ProductionStage assignStageLeader(Long stageId, Long leaderUserId) {
        return internalAssignStageLeader(stageId, leaderUserId);
    }

    @Transactional
    public ProductionStage assignStageQc(Long stageId, Long qcUserId) {
        return internalAssignStageQc(stageId, qcUserId);
    }

    // Internal implementations (original ones retained below or we refactor). If
    // they exist below rename them accordingly.
    @Transactional
    public ProductionOrder internalAssignTechnician(Long poId, Long technicianUserId) {
        ProductionOrder po = poRepo.findById(poId).orElseThrow();
        User tech = userRepository.findById(technicianUserId).orElseThrow();
        po.setAssignedTechnician(tech);
        po.setAssignedAt(Instant.now());
        ProductionOrder saved = poRepo.save(po);
        notificationService.notifyUser(tech, "PRODUCTION", "INFO", "PO được phân công",
                "Lệnh sản xuất #" + po.getPoNumber() + " đã được phân công cho bạn", "PRODUCTION_ORDER", po.getId());
        return saved;
    }

    @Transactional
    public ProductionStage internalAssignStageLeader(Long stageId, Long leaderUserId) {
        ProductionStage s = stageRepo.findById(stageId).orElseThrow();
        User leader = userRepository.findById(leaderUserId).orElseThrow();
        s.setAssignedLeader(leader);
        ProductionStage saved = stageRepo.save(s);
        notificationService.notifyUser(leader, "PRODUCTION", "INFO", "Phân công công đoạn",
                "Bạn được phân công công đoạn " + s.getStageType(), "PRODUCTION_STAGE", s.getId());
        return saved;
    }

    @Transactional
    public ProductionStage internalAssignStageQc(Long stageId, Long qcUserId) {
        ProductionStage s = stageRepo.findById(stageId).orElseThrow();
        User qc = userRepository.findById(qcUserId).orElseThrow();
        s.setQcAssignee(qc);
        ProductionStage saved = stageRepo.save(s);
        notificationService.notifyUser(qc, "QC", "INFO", "Phân công QC",
                "Bạn được phân công kiểm tra công đoạn " + s.getStageType(), "PRODUCTION_STAGE", s.getId());
        return saved;
    }

    /**
     * Bulk assign leaders to stages of a ProductionOrder
     * NEW: Query stages trực tiếp theo ProductionOrder (không qua WorkOrder)
     */
    @Transactional
    public java.util.List<ProductionStage> bulkAssignStageLeaders(Long orderId, java.util.Map<String, Long> map) {
        // Verify ProductionOrder exists
        poRepo.findById(orderId).orElseThrow(() -> new RuntimeException("Production Order not found"));
        java.util.List<ProductionStage> stages = stageRepo.findStagesByOrderId(orderId);
        for (ProductionStage s : stages) {
            Long uid = map.get(s.getStageType());
            if (uid != null) {
                userRepository.findById(uid).ifPresent(s::setAssignedLeader);
            }
        }
        return stageRepo.saveAll(stages);
    }
    // ==== END ASSIGNMENT METHODS ====

    private void ensureLeader(Long stageId, Long leaderUserId) {
        ProductionStage s = stageRepo.findById(stageId).orElseThrow();
        if (s.getAssignedLeader() == null || !s.getAssignedLeader().getId().equals(leaderUserId)) {
            throw new RuntimeException("Access denied: not assigned leader");
        }
    }

    /**
     * Đồng bộ cả status và executionStatus
     * executionStatus là trường chính, status được map từ executionStatus để tương
     * thích
     * Method này có thể được gọi từ các service khác (như QcService)
     * 
     * Mapping theo yêu cầu từ workflow:
     * - "đợi" → PENDING
     * - "chờ làm" → WAITING
     * - "đang làm" → IN_PROGRESS
     * - "chờ kiểm tra" → WAITING_QC
     * - "đang kiểm tra" → QC_IN_PROGRESS (status riêng để phân biệt)
     * - "đạt" → QC_PASSED (status riêng để phân biệt với hoàn thành)
     * - "không đạt" → QC_FAILED
     * - "chờ sửa" → WAITING_REWORK (status riêng để phân biệt)
     * - "đang sửa" → REWORK_IN_PROGRESS (status riêng để phân biệt)
     * - "hoàn thành" → COMPLETED
     */
    public void syncStageStatus(ProductionStage stage, String executionStatus) {
        stage.setExecutionStatus(executionStatus);

        // Map executionStatus sang status để tương thích với code cũ
        // Giữ nguyên executionStatus values để frontend có thể phân biệt rõ ràng
        String mappedStatus;
        switch (executionStatus) {
            case "WAITING":
            case "READY_TO_PRODUCE": // NEW: Map READY_TO_PRODUCE to WAITING
                mappedStatus = "WAITING"; // "chờ làm"
                break;
            case "IN_PROGRESS":
                mappedStatus = "IN_PROGRESS"; // "đang làm"
                break;
            case "WAITING_QC":
                mappedStatus = "WAITING_QC"; // "chờ kiểm tra"
                break;
            case "QC_IN_PROGRESS":
                mappedStatus = "QC_IN_PROGRESS"; // "đang kiểm tra" - phân biệt rõ với WAITING_QC
                break;
            case "QC_PASSED":
                mappedStatus = "QC_PASSED"; // "đạt" - phân biệt với COMPLETED (hoàn thành)
                break;
            case "QC_FAILED":
                mappedStatus = "QC_FAILED"; // "không đạt"
                break;
            case "WAITING_REWORK":
                mappedStatus = "WAITING_REWORK"; // "chờ sửa" - phân biệt rõ với FAILED
                break;
            case "REWORK_IN_PROGRESS":
                mappedStatus = "REWORK_IN_PROGRESS"; // "đang sửa" - phân biệt rõ với IN_PROGRESS
                break;
            case "COMPLETED":
                mappedStatus = "COMPLETED"; // "hoàn thành"
                break;
            case "PENDING":
            default:
                mappedStatus = "PENDING"; // "đợi"
                break;
        }
        stage.setStatus(mappedStatus);
    }

    private void releaseMachineReservations(ProductionStage stage) {
        List<MachineAssignment> assignments = machineAssignmentRepository.findByProductionStageId(stage.getId());
        if (assignments == null || assignments.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        for (MachineAssignment assignment : assignments) {
            if (!"RELEASED".equalsIgnoreCase(assignment.getReservationStatus())) {
                assignment.setReservationStatus("RELEASED");
                assignment.setReleasedAt(now);
            }
        }
        machineAssignmentRepository.saveAll(assignments);
    }

    private BigDecimal calculateDurationHours(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return null;
        }
        long minutes = Duration.between(start, end).toMinutes();
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private Instant toInstant(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atZone(ZoneId.systemDefault()).toInstant();
    }

    private void notifyStageAssignments(ProductionStage stage, String poNumber, String productName) {
        if (stage.getAssignedLeader() != null) {
            notificationService.notifyUser(stage.getAssignedLeader(), "PRODUCTION", "INFO", "Công đoạn mới được giao",
                    "PO #" + poNumber + " - " + productName + ": công đoạn " + stage.getStageType()
                            + " đã sẵn sàng chuẩn bị.",
                    "PRODUCTION_STAGE", stage.getId());
        }
        if (stage.getQcAssignee() != null) {
            notificationService.notifyUser(stage.getQcAssignee(), "QC", "INFO", "Chuẩn bị kiểm tra KCS",
                    "PO #" + poNumber + " - " + productName + ": công đoạn " + stage.getStageType()
                            + " sẽ cần QC sau khi hoàn tất.",
                    "PRODUCTION_STAGE", stage.getId());
        }
    }

    private String safeProductName(ProductionOrderDetail pod) {
        if (pod == null || pod.getProduct() == null || pod.getProduct().getName() == null) {
            return "Sản phẩm";
        }
        return pod.getProduct().getName();
    }

    /**
     * Validate và resolve User từ DB để tránh FK constraint violation
     * Nếu user không tồn tại hoặc null, trả về null
     */
    private User resolveUser(User user) {
        if (user == null || user.getId() == null) {
            return null;
        }
        return userRepository.findById(user.getId()).orElse(null);
    }

    /**
     * Validate và resolve Machine từ DB để tránh FK constraint violation
     * Nếu machine không tồn tại hoặc null, trả về null
     */
    private Machine resolveMachine(Machine machine) {
        if (machine == null || machine.getId() == null) {
            return null;
        }
        return machineRepository.findById(machine.getId()).orElse(null);
    }

    // ===== NEW WORKFLOW ENDPOINTS =====

    /**
     * Production Manager: Bắt đầu lệnh làm việc
     * Sau khi PM ấn "Bắt đầu lệnh làm việc":
     * - Gửi thông báo đến tất cả Tổ Trưởng và KCS
     * - Stage đầu tiên: executionStatus = "WAITING" (chờ làm)
     * 
     * return stages;
     * }
     * 
     * /**
     * Lấy danh sách stages của Production Order để hiển thị kế hoạch
     */
    public List<ProductionStage> getOrderStages(Long orderId) {
        return stageRepo.findStagesByOrderId(orderId);
    }

    /**
     * Lấy danh sách orders cho Production Manager
     * NEW: Lấy tất cả ProductionOrder có stages (không qua WorkOrder)
     */
    public List<ProductionOrder> getManagerOrders() {
        // Get all production orders that have stages
        List<ProductionOrder> allOrders = poRepo.findAll();
        return allOrders.stream()
                .filter(po -> {
                    List<ProductionStage> stages = stageRepo.findStagesByOrderId(po.getId());
                    return !stages.isEmpty();
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Lấy danh sách orders cho Team Leader
     */
    /**
     * Lấy danh sách orders cho Team Leader
     * NEW: Query trực tiếp theo ProductionOrder (không qua WorkOrderDetail)
     * 
     * Bao gồm tất cả stages được assign cho leader này, không phân biệt status
     * (vì sau khi start work order, stage đầu tiên = WAITING, các stage khác =
     * PENDING)
     */
    public List<ProductionOrder> getLeaderOrders(Long leaderUserId) {
        // Query tất cả stages được assign cho leader này (không filter theo status)
        // Vì sau khi start work order, stage đầu tiên = WAITING, các stage khác =
        // PENDING
        // Nếu chỉ query WAITING/IN_PROGRESS thì sẽ miss các stage PENDING
        // Sử dụng JOIN FETCH trong repository để load productionOrder ngay lập tức
        List<ProductionStage> allLeaderStages = stageRepo.findByAssignedLeaderId(leaderUserId);

        // Extract unique ProductionOrder IDs từ stages (productionOrder đã được fetch
        // join)
        java.util.Set<Long> orderIds = allLeaderStages.stream()
                .map(s -> {
                    if (s.getProductionOrder() != null) {
                        return s.getProductionOrder().getId();
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        if (orderIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return poRepo.findAllById(orderIds);
    }

    /**
     * Lấy danh sách orders cho KCS
     * NEW: Query trực tiếp theo ProductionOrder (không qua WorkOrderDetail)
     * Query tất cả stages được assign cho QA (qcAssigneeId), không filter theo
     * status
     * QA sẽ thấy tất cả đơn hàng đã được assign, nhưng chỉ có thể kiểm tra khi
     * status = WAITING_QC hoặc QC_IN_PROGRESS
     */
    public List<ProductionOrder> getQaOrders(Long qcUserId) {
        // Get all stages assigned to this QA (không filter theo status)
        // Sử dụng JOIN FETCH để load productionOrder ngay lập tức, tránh
        // LazyLoadingException
        List<ProductionStage> qcStages = stageRepo.findByQcAssigneeId(qcUserId);

        // Extract unique ProductionOrder IDs từ stages (productionOrder đã được fetch
        // join)
        java.util.Set<Long> orderIds = qcStages.stream()
                .map(s -> {
                    if (s.getProductionOrder() != null) {
                        return s.getProductionOrder().getId();
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        if (orderIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<ProductionOrder> orders = poRepo.findAllById(orderIds);

        // Filter out WAITING_PRODUCTION and PENDING_APPROVAL
        return orders.stream()
                .filter(o -> !"WAITING_PRODUCTION".equals(o.getExecutionStatus()) &&
                        !"PENDING_APPROVAL".equals(o.getExecutionStatus()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Count active stages for a Leader (for workload balancing)
     */
    public long countActiveStagesForLeader(Long leaderId, List<String> activeStatuses) {
        return stageRepo.countByAssignedLeaderIdAndExecutionStatusIn(leaderId, activeStatuses);
    }

    // NEW: Strict active stage count (progress < 100)
    public long countActiveStagesForLeaderStrict(Long leaderId, List<String> executionStatuses) {
        return stageRepo.countByAssignedLeaderIdAndProgressPercentLessThan100(leaderId, executionStatuses);
    }

    /**
     * Count active stages for a QC (for workload balancing)
     */
    public long countActiveStagesForQc(Long qcId, List<String> activeStatuses) {
        return stageRepo.countByQcAssigneeIdAndExecutionStatusIn(qcId, activeStatuses);
    }

    /**
     * Count distinct Production Orders assigned to a leader (for workload balancing
     * by PO count)
     */
    public long countProductionOrdersForLeader(Long leaderId) {
        return stageRepo.countDistinctProductionOrdersByLeaderId(leaderId);
    }

    /**
     * Enrich ProductionOrderDto với các thông tin cần thiết cho frontend
     * - lotCode từ ProductionLot (thông qua ProductionPlan)
     * - productName từ ProductionOrderDetail
     * - size từ Product.standardDimensions hoặc ProductionLot.sizeSnapshot
     * - statusLabel từ status
     * - stages từ WorkOrder -> ProductionStage
     */
    public ProductionOrderDto enrichProductionOrderDto(ProductionOrder po) {
        ProductionOrderDto dto = productionMapper.toDto(po);

        // Lấy lotCode từ ProductionPlan -> ProductionLot
        // Extract planCode from notes (format: "Auto-generated from Production Plan:
        // PP-xxxxxx")
        String planCode = extractPlanCodeFromNotes(po.getNotes());
        if (planCode != null) {
            tmmsystem.entity.ProductionPlan plan = productionPlanRepository.findByPlanCode(planCode).orElse(null);
            if (plan != null && plan.getLot() != null) {
                dto.setLotCode(plan.getLot().getLotCode());
            }
        } else if (po.getContract() != null) {
            // Fallback: tìm qua contract (chỉ dùng nếu không có planCode trong notes)
            List<tmmsystem.entity.ProductionPlan> plans = productionPlanRepository
                    .findByContractId(po.getContract().getId());
            tmmsystem.entity.ProductionPlan currentPlan = plans.stream()
                    .filter(p -> Boolean.TRUE.equals(p.getCurrentVersion()))
                    .findFirst()
                    .orElse(null);
            if (currentPlan != null && currentPlan.getLot() != null) {
                dto.setLotCode(currentPlan.getLot().getLotCode());
            }
        }

        // Lấy productName và size từ ProductionOrderDetail đầu tiên
        List<ProductionOrderDetail> details = podRepo.findByProductionOrderId(po.getId());
        if (!details.isEmpty()) {
            ProductionOrderDetail firstDetail = details.get(0);
            if (firstDetail.getProduct() != null) {
                dto.setProductName(firstDetail.getProduct().getName());
                // Lấy size từ Product.standardDimensions hoặc ProductionLot.sizeSnapshot
                String size = firstDetail.getProduct().getStandardDimensions();
                if (size == null || size.isEmpty()) {
                    // Fallback: lấy từ ProductionLot nếu có
                    if (po.getContract() != null) {
                        List<tmmsystem.entity.ProductionPlan> plans = productionPlanRepository
                                .findByContractId(po.getContract().getId());
                        tmmsystem.entity.ProductionPlan currentPlan = plans.stream()
                                .filter(p -> Boolean.TRUE.equals(p.getCurrentVersion()))
                                .findFirst()
                                .orElse(null);
                        if (currentPlan != null && currentPlan.getLot() != null
                                && currentPlan.getLot().getSizeSnapshot() != null) {
                            size = currentPlan.getLot().getSizeSnapshot();
                        }
                    }
                }
                dto.setSize(size);
            }
        }

        // Lấy danh sách stages và enrich với totalHours
        List<ProductionStage> stages = getOrderStages(po.getId());

        // NEW: Set QR Token from first stage
        if (!stages.isEmpty()) {
            // Sort by sequence to ensure we get the first one
            stages.sort(java.util.Comparator.comparing(ProductionStage::getStageSequence,
                    java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));
            ProductionStage firstStage = stages.get(0);

            // Lazy generation: If token is missing, generate and save it
            if (firstStage.getQrToken() == null || firstStage.getQrToken().isEmpty()) {
                firstStage.setQrToken(generateQrToken());
                stageRepo.save(firstStage);
            }

            dto.setQrToken(firstStage.getQrToken());
        }

        List<ProductionStageDto> stageDtos = stages.stream().map(stage -> {
            ProductionStageDto stageDto = productionMapper.toDto(stage);
            // Tính totalHours từ StageTracking
            java.math.BigDecimal totalHours = calculateTotalHoursForStage(stage.getId());
            stageDto.setTotalHours(totalHours);

            // NEW: Check if blocked by another lot at same stage type
            String execStatus = stage.getExecutionStatus();
            if ("WAITING".equals(execStatus) || "READY".equals(execStatus) || "READY_TO_PRODUCE".equals(execStatus)) {
                BlockingInfo blockInfo = checkStageBlocked(stage);
                stageDto.setIsBlocked(blockInfo.isBlocked);
                stageDto.setBlockedBy(blockInfo.blockedByLotCode);
            } else {
                stageDto.setIsBlocked(false);
            }

            return stageDto;
        }).collect(java.util.stream.Collectors.toList());
        dto.setStages(stageDtos);

        // Map status sang statusLabel - ưu tiên tính toán từ stages nếu có
        // Theo yêu cầu: ProductionOrder status nên phản ánh trạng thái của các stages
        String calculatedStatusLabel = calculateOrderStatusLabelFromStages(stages, po.getStatus());
        dto.setStatusLabel(calculatedStatusLabel != null ? calculatedStatusLabel : mapStatusToLabel(po.getStatus()));

        // Set aliases for frontend compatibility
        dto.setExpectedStartDate(po.getPlannedStartDate());
        dto.setExpectedFinishDate(po.getPlannedEndDate());
        dto.setExpectedDeliveryDate(po.getPlannedStartDate()); // Leader dùng expectedDeliveryDate

        // Check for pending material requests
        Long pendingReqId = null;
        if (stages != null && !stages.isEmpty()) {
            for (ProductionStage s : stages) {
                List<MaterialRequisition> reqs = reqRepo.findByProductionStageIdAndStatus(s.getId(), "PENDING");
                if (!reqs.isEmpty()) {
                    pendingReqId = reqs.get(0).getId();
                    break;
                }
            }
        }
        dto.setPendingMaterialRequestId(pendingReqId);

        return dto;
    }

    /**
     * Lấy stages của một leader cho một order cụ thể
     */
    public List<ProductionStage> getLeaderStagesForOrder(Long orderId, Long leaderUserId) {
        List<ProductionStage> allStages = getOrderStages(orderId);
        return allStages.stream()
                .filter(s -> s.getAssignedLeader() != null && s.getAssignedLeader().getId().equals(leaderUserId))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Tính tổng thời gian làm việc cho một stage từ StageTracking
     */
    public java.math.BigDecimal calculateTotalHoursForStage(Long stageId) {
        List<tmmsystem.entity.StageTracking> trackings = stageTrackingRepository
                .findByProductionStageIdOrderByTimestampAsc(stageId);
        if (trackings.isEmpty()) {
            return java.math.BigDecimal.ZERO;
        }

        java.time.Duration totalDuration = java.time.Duration.ZERO;
        java.time.Instant lastStartTime = null;

        for (tmmsystem.entity.StageTracking tracking : trackings) {
            String action = tracking.getAction();
            if ("START".equals(action) || "RESUME".equals(action)) {
                lastStartTime = tracking.getTimestamp();
            } else if (("PAUSE".equals(action) || "COMPLETE".equals(action)) && lastStartTime != null) {
                java.time.Duration duration = java.time.Duration.between(lastStartTime, tracking.getTimestamp());
                totalDuration = totalDuration.plus(duration);
                if ("COMPLETE".equals(action)) {
                    lastStartTime = null;
                }
            }
        }

        // Nếu stage đang làm việc (chưa pause hoặc complete), tính đến hiện tại
        if (lastStartTime != null) {
            java.time.Duration duration = java.time.Duration.between(lastStartTime, java.time.Instant.now());
            totalDuration = totalDuration.plus(duration);
        }

        // Convert Duration to hours (BigDecimal)
        double hours = totalDuration.toMillis() / (1000.0 * 60.0 * 60.0);
        return java.math.BigDecimal.valueOf(hours).setScale(1, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Map status code sang status label để hiển thị
     */
    /**
     * Tính toán statusLabel cho ProductionOrder dựa trên trạng thái của các stages
     * Theo yêu cầu: PM cần thấy trạng thái chi tiết như "chờ <công đoạn> làm",
     * "<công đoạn> đang làm", etc.
     */
    private String calculateOrderStatusLabelFromStages(List<ProductionStage> stages, String defaultStatus) {
        if (stages == null || stages.isEmpty()) {
            return mapStatusToLabel(defaultStatus);
        }

        // Sắp xếp stages theo sequence
        List<ProductionStage> sortedStages = stages.stream()
                .filter(s -> s.getStageSequence() != null)
                .sorted(java.util.Comparator.comparing(ProductionStage::getStageSequence))
                .collect(java.util.stream.Collectors.toList());

        if (sortedStages.isEmpty()) {
            return mapStatusToLabel(defaultStatus);
        }

        // Tìm stage đang active (không phải COMPLETED, QC_PASSED với stage tiếp theo đã
        // COMPLETED)
        ProductionStage activeStage = null;
        for (int i = 0; i < sortedStages.size(); i++) {
            ProductionStage stage = sortedStages.get(i);
            String execStatus = stage.getExecutionStatus();
            if (execStatus == null)
                continue;

            // Nếu stage này chưa hoàn thành hoặc đang trong quá trình
            if (!execStatus.equals("COMPLETED") &&
                    !(execStatus.equals("QC_PASSED") && i < sortedStages.size() - 1 &&
                            sortedStages.get(i + 1).getExecutionStatus() != null &&
                            !sortedStages.get(i + 1).getExecutionStatus().equals("PENDING"))) {
                activeStage = stage;
                break;
            }
        }

        // Nếu không tìm thấy active stage, kiểm tra xem tất cả đã hoàn thành chưa
        if (activeStage == null) {
            boolean allCompleted = sortedStages.stream()
                    .allMatch(s -> {
                        String execStatus = s.getExecutionStatus();
                        return execStatus != null &&
                                (execStatus.equals("COMPLETED") ||
                                        execStatus.equals("QC_PASSED"));
                    });
            if (allCompleted) {
                return "Hoàn thành";
            }
            // Nếu không phải tất cả đã hoàn thành, dùng stage đầu tiên
            activeStage = sortedStages.get(0);
        }

        String stageTypeName = getStageTypeName(activeStage.getStageType());
        String execStatus = activeStage.getExecutionStatus();

        // Map executionStatus sang label theo yêu cầu
        switch (execStatus) {
            case "WAITING":
            case "READY":
            case "READY_TO_PRODUCE": // NEW
                return "Chờ " + stageTypeName + " làm";
            case "IN_PROGRESS":
                return stageTypeName + " đang làm";
            case "WAITING_QC":
                return stageTypeName + " chờ kiểm tra";
            case "QC_IN_PROGRESS":
                return stageTypeName + " đang kiểm tra";
            case "WAITING_REWORK":
                return "Chờ " + stageTypeName + " sửa";
            case "REWORK_IN_PROGRESS":
                return stageTypeName + " đang sửa";
            case "QC_PASSED":
                // Nếu là stage cuối cùng và đạt QC thì "Hoàn thành"
                int currentIndex = sortedStages.indexOf(activeStage);
                if (currentIndex == sortedStages.size() - 1) {
                    return "Hoàn thành";
                }
                // Nếu không phải stage cuối, tìm stage tiếp theo
                ProductionStage nextStage = sortedStages.get(currentIndex + 1);
                if (nextStage != null) {
                    String nextStageName = getStageTypeName(nextStage.getStageType());
                    String nextStatus = nextStage.getExecutionStatus();
                    if (nextStatus != null && (nextStatus.equals("WAITING") || nextStatus.equals("READY")
                            || nextStatus.equals("PENDING") || nextStatus.equals("READY_TO_PRODUCE"))) { // NEW
                        return "Chờ " + nextStageName + " làm";
                    }
                }
                return "Hoàn thành";
            case "QC_FAILED":
                return stageTypeName + " không đạt";
            case "PENDING":
                return "Chờ sản xuất"; // Chưa bắt đầu
            case "COMPLETED":
                // Nếu là stage cuối cùng thì "Hoàn thành", nếu không thì tìm stage tiếp theo
                int completedIndex = sortedStages.indexOf(activeStage);
                if (completedIndex == sortedStages.size() - 1) {
                    return "Hoàn thành";
                }
                // Tìm stage tiếp theo chưa hoàn thành
                for (int i = completedIndex + 1; i < sortedStages.size(); i++) {
                    ProductionStage next = sortedStages.get(i);
                    String nextStatus = next.getExecutionStatus();
                    if (nextStatus != null && !nextStatus.equals("COMPLETED") && !nextStatus.equals("QC_PASSED")) {
                        String nextStageName = getStageTypeName(next.getStageType());
                        if (nextStatus.equals("WAITING") || nextStatus.equals("READY")
                                || nextStatus.equals("READY_TO_PRODUCE")) { // NEW
                            return "Chờ " + nextStageName + " làm";
                        }
                        return getStageTypeName(next.getStageType()) + " "
                                + getStatusLabelFromExecutionStatus(nextStatus);
                    }
                }
                return "Hoàn thành";
            default:
                return mapStatusToLabel(defaultStatus);
        }
    }

    /**
     * Helper method để map executionStatus sang label ngắn gọn
     */
    private String getStatusLabelFromExecutionStatus(String execStatus) {
        if (execStatus == null)
            return "";
        switch (execStatus) {
            case "WAITING":
            case "READY":
                return "chờ làm";
            case "IN_PROGRESS":
                return "đang làm";
            case "WAITING_QC":
                return "chờ kiểm tra";
            case "QC_IN_PROGRESS":
                return "đang kiểm tra";
            default:
                return execStatus;
        }
    }

    /**
     * Map stage type code sang tên tiếng Việt
     */
    private String getStageTypeName(String stageType) {
        if (stageType == null)
            return "";
        switch (stageType.toUpperCase()) {
            case "WARPING":
            case "CUONG_MAC":
                return "Cuồng mắc";
            case "WEAVING":
            case "DET":
                return "Dệt";
            case "DYEING":
            case "NHUOM":
                return "Nhuộm";
            case "CUTTING":
            case "CAT":
                return "Cắt";
            case "HEMMING":
            case "MAY":
                return "May";
            case "PACKAGING":
            case "DONG_GOI":
                return "Đóng gói";
            default:
                return stageType;
        }
    }

    private String mapStatusToLabel(String status) {
        if (status == null)
            return "Không xác định";
        switch (status) {
            case "DRAFT":
                return "Nháp";
            case "PENDING_APPROVAL":
                return "Chờ phê duyệt";
            case "APPROVED":
                return "Đã phê duyệt";
            case "REJECTED":
                return "Từ chối";
            case "CHO_SAN_XUAT":
            case "WAITING_PRODUCTION":
                return "Chờ sản xuất";
            case "DANG_SAN_XUAT":
            case "IN_PROGRESS":
                return "Đang sản xuất";
            case "HOAN_THANH":
            case "COMPLETED":
            case "ORDER_COMPLETED":
                return "Hoàn thành";
            default:
                return status;
        }
    }

    @Transactional
    public void fixDataConsistency() {
        // 0. Cleanup Duplicate Orders (New Fix)
        cleanupDuplicateOrders();

        // 1. Fix missing QR tokens
        List<ProductionStage> stages = stageRepo.findAll();
        for (ProductionStage s : stages) {
            if (s.getQrToken() == null) {
                s.setQrToken(generateQrToken());
                stageRepo.save(s);
            }
        }

        // 2. Fix POs without stages
        List<ProductionOrder> orders = poRepo.findAll();
        for (ProductionOrder po : orders) {
            List<ProductionStage> existingStages = stageRepo.findByProductionOrderIdOrderByStageSequenceAsc(po.getId());
            if (existingStages.isEmpty()) {
                // Create default stages
                createStagesFromPlan(po.getId(), null);
                System.out.println("Auto-generated stages for PO: " + po.getPoNumber());
            } else {
                // 3. Fix Stuck Stages (e.g. Previous is QC_PASSED but Next is PENDING)
                for (int i = 0; i < existingStages.size() - 1; i++) {
                    ProductionStage current = existingStages.get(i);
                    ProductionStage next = existingStages.get(i + 1);

                    if ("QC_PASSED".equals(current.getExecutionStatus())
                            && "PENDING".equals(next.getExecutionStatus())) {
                        // Fix stuck stage
                        syncStageStatus(next, "READY_TO_PRODUCE"); // Or WAITING
                        next.setExecutionStatus("READY_TO_PRODUCE");
                        stageRepo.save(next);
                        System.out.println("Fixed stuck stage " + next.getId() + " (" + next.getStageType()
                                + ") for PO " + po.getPoNumber());
                    }
                }
            }
        }

        // 4. Fix Inconsistent QC Statuses (Self-Healing)
        // If stage is WAITING/READY but has a PENDING/PROCESSED issue, revert it.
        List<tmmsystem.entity.QualityIssue> issues = issueRepo.findAll();
        for (tmmsystem.entity.QualityIssue issue : issues) {
            ProductionStage stage = issue.getProductionStage();
            if (stage == null)
                continue;

            String stageStatus = stage.getExecutionStatus();
            if ("WAITING".equals(stageStatus) || "READY_TO_PRODUCE".equals(stageStatus)
                    || "PENDING".equals(stageStatus)) {
                if ("PENDING".equals(issue.getStatus())) {
                    // Issue is pending (Tech hasn't processed), so stage should be QC_FAILED
                    System.out.println("Self-healing: Reverting stage " + stage.getId()
                            + " to QC_FAILED due to PENDING issue " + issue.getId());
                    stage.setExecutionStatus("QC_FAILED");
                    syncStageStatus(stage, "QC_FAILED");
                    stageRepo.save(stage);
                } else if ("PROCESSED".equals(issue.getStatus()) && "REWORK".equals(issue.getIssueType())) {
                    // Issue is processed for REWORK, so stage should be WAITING_REWORK
                    System.out.println("Self-healing: Setting stage " + stage.getId()
                            + " to WAITING_REWORK due to PROCESSED REWORK issue " + issue.getId());
                    stage.setExecutionStatus("WAITING_REWORK");
                    syncStageStatus(stage, "WAITING_REWORK");
                    stageRepo.save(stage);
                }
            }
        }
    }

    @Transactional
    public tmmsystem.entity.MaterialRequisition getMaterialRequest(Long id) {
        return reqRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Material requisition not found"));
    }

    @Transactional
    public tmmsystem.dto.MaterialRequisitionResponseDto getMaterialRequestDetails(Long id) {
        tmmsystem.entity.MaterialRequisition req = getMaterialRequest(id);
        List<tmmsystem.entity.MaterialRequisitionDetail> details = reqDetailRepo.findByRequisitionId(id);

        tmmsystem.dto.MaterialRequisitionResponseDto dto = new tmmsystem.dto.MaterialRequisitionResponseDto();
        dto.setId(req.getId());
        dto.setRequisitionNumber(req.getRequisitionNumber());
        dto.setStatus(req.getStatus());
        dto.setRequestedAt(req.getRequestedAt());
        dto.setApprovedAt(req.getApprovedAt());
        dto.setNotes(req.getNotes());
        dto.setQuantityRequested(req.getQuantityRequested());
        dto.setQuantityApproved(req.getQuantityApproved());

        // Enhanced fields
        if (req.getProductionStage() != null) {
            dto.setStageName(req.getProductionStage().getStageType()); // Or a more descriptive name if available

            // Extract lotCode and poNumber from production order
            ProductionOrder po = req.getProductionStage().getProductionOrder();
            if (po != null) {
                dto.setPoNumber(po.getPoNumber());
                // Get lotCode from enriched DTO
                ProductionOrderDto enrichedPo = enrichProductionOrderDto(po);
                if (enrichedPo != null && enrichedPo.getLotCode() != null) {
                    dto.setLotCode(enrichedPo.getLotCode());
                }
            }
        }
        if (req.getRequestedBy() != null) {
            dto.setRequesterName(req.getRequestedBy().getName());
        }
        if (req.getApprovedBy() != null) {
            dto.setApproverName(req.getApprovedBy().getName());
        }

        if (req.getSourceIssue() != null) {
            tmmsystem.dto.MaterialRequisitionResponseDto.SourceIssueDto issueDto = new tmmsystem.dto.MaterialRequisitionResponseDto.SourceIssueDto();
            issueDto.setDescription(req.getSourceIssue().getDescription());
            issueDto.setSeverity(req.getSourceIssue().getSeverity());
            issueDto.setEvidencePhoto(req.getSourceIssue().getEvidencePhoto());
            dto.setSourceIssue(issueDto);

            // Populate Check Details (Failed Inspections)
            if (req.getProductionStage() != null) {
                // Find latest Failed inspections for this stage
                // Note: Logic allows seeing ALL failed inspections for this stage.
                // Alternatively we could filter by date close to issue creation.
                List<tmmsystem.entity.QcInspection> inspections = qcInspectionRepository
                        .findByProductionStageId(req.getProductionStage().getId());

                List<tmmsystem.dto.MaterialRequisitionResponseDto.DefectDetailDto> defects = inspections.stream()
                        .filter(i -> "FAIL".equalsIgnoreCase(i.getResult()))
                        .map(i -> {
                            tmmsystem.dto.MaterialRequisitionResponseDto.DefectDetailDto d = new tmmsystem.dto.MaterialRequisitionResponseDto.DefectDetailDto();
                            d.setCriteriaName(i.getQcCheckpoint() != null ? i.getQcCheckpoint().getInspectionCriteria()
                                    : "Kiểm tra chung");
                            d.setDescription(i.getNotes());
                            d.setPhotoUrl(i.getPhotoUrl());
                            return d;
                        })
                        .collect(java.util.stream.Collectors.toList());
                dto.setDefectDetails(defects);
            }
        }

        List<tmmsystem.dto.MaterialRequisitionDetailDto> detailDtos = details.stream().map(d -> {
            tmmsystem.dto.MaterialRequisitionDetailDto dDto = new tmmsystem.dto.MaterialRequisitionDetailDto();
            dDto.setId(d.getId());
            if (d.getMaterial() != null) {
                dDto.setMaterialName(d.getMaterial().getName());
                dDto.setMaterialCode(d.getMaterial().getCode());
            }
            dDto.setQuantityRequested(d.getQuantityRequested());
            dDto.setQuantityApproved(d.getQuantityApproved());
            dDto.setUnit(d.getUnit());
            dDto.setNotes(d.getNotes());
            return dDto;
        }).collect(java.util.stream.Collectors.toList());

        dto.setDetails(detailDtos);

        return dto;
    }

    // Legacy overload for ExecutionOrchestrationService
    @Transactional
    public ProductionOrder approveMaterialRequest(Long requestId, Long directorId, boolean force) {
        tmmsystem.entity.MaterialRequisition req = reqRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Requisition not found"));

        tmmsystem.dto.execution.MaterialRequisitionApprovalDto dto = new tmmsystem.dto.execution.MaterialRequisitionApprovalDto();
        dto.setDirectorId(directorId);
        dto.setForce(force);

        // Fetch details and approve full amount
        List<tmmsystem.entity.MaterialRequisitionDetail> details = reqDetailRepo.findByRequisitionId(requestId);
        List<tmmsystem.dto.execution.MaterialRequisitionDetailDto> detailDtos = new java.util.ArrayList<>();

        for (tmmsystem.entity.MaterialRequisitionDetail d : details) {
            tmmsystem.dto.execution.MaterialRequisitionDetailDto dd = new tmmsystem.dto.execution.MaterialRequisitionDetailDto();
            dd.setId(d.getId());
            dd.setQuantityApproved(d.getQuantityRequested()); // Auto-approve requested amount
            detailDtos.add(dd);
        }
        dto.setDetails(detailDtos);

        return approveMaterialRequest(requestId, dto);
    }

    @Transactional
    public ProductionOrder approveMaterialRequest(Long requestId,
            tmmsystem.dto.execution.MaterialRequisitionApprovalDto dto) {
        tmmsystem.entity.MaterialRequisition req = reqRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Requisition not found"));

        java.math.BigDecimal totalApproved = java.math.BigDecimal.ZERO;

        // Use quantityRequested from details (not quantityApproved since it may not be
        // set)
        if (dto.getDetails() != null) {
            for (tmmsystem.dto.execution.MaterialRequisitionDetailDto detailDto : dto.getDetails()) {
                if (detailDto.getId() != null) {
                    tmmsystem.entity.MaterialRequisitionDetail detail = reqDetailRepo.findById(detailDto.getId())
                            .orElse(null);
                    if (detail != null && detail.getRequisition().getId().equals(requestId)) {
                        // Set approved = requested if approved is not provided
                        BigDecimal approvedQty = detailDto.getQuantityApproved();
                        if (approvedQty == null || approvedQty.compareTo(BigDecimal.ZERO) == 0) {
                            approvedQty = detail.getQuantityRequested();
                        }
                        detail.setQuantityApproved(approvedQty);
                        reqDetailRepo.save(detail);
                        if (approvedQty != null) {
                            totalApproved = totalApproved.add(approvedQty);
                        }
                    }
                }
            }
        }

        // FALLBACK: If still 0, read from DB details using quantityRequested
        if (totalApproved.compareTo(BigDecimal.ZERO) == 0) {
            List<tmmsystem.entity.MaterialRequisitionDetail> dbDetails = reqDetailRepo.findByRequisitionId(requestId);
            for (tmmsystem.entity.MaterialRequisitionDetail dbDetail : dbDetails) {
                // Use quantityRequested if quantityApproved is not set
                BigDecimal qty = dbDetail.getQuantityApproved();
                if (qty == null || qty.compareTo(BigDecimal.ZERO) == 0) {
                    qty = dbDetail.getQuantityRequested();
                }
                if (qty != null) {
                    totalApproved = totalApproved.add(qty);
                }
            }
        }

        // ULTIMATE FALLBACK: Use parent requisition's quantityRequested
        if (totalApproved.compareTo(BigDecimal.ZERO) == 0 && req.getQuantityRequested() != null) {
            totalApproved = req.getQuantityRequested();
        }

        // 1. Time Validation (Standard Capacity Check)
        // Define standard daily capacities (can be moved to DB/Config later)
        Map<String, Double> standardDailyCapacity = new HashMap<>();
        standardDailyCapacity.put("WARPING", 2000.0); // kg
        standardDailyCapacity.put("WEAVING", 500.0); // meters
        standardDailyCapacity.put("DYEING", 1000.0); // kg
        standardDailyCapacity.put("CUTTING", 2000.0); // pcs
        standardDailyCapacity.put("HEMMING", 1500.0); // pcs
        standardDailyCapacity.put("PACKAGING", 3000.0); // pcs
        // Vietnamese aliases
        standardDailyCapacity.put("CUONG_MAC", 2000.0);
        standardDailyCapacity.put("DET", 500.0);
        standardDailyCapacity.put("NHUOM", 1000.0);
        standardDailyCapacity.put("CAT", 2000.0);
        standardDailyCapacity.put("MAY", 1500.0);
        standardDailyCapacity.put("DONG_GOI", 3000.0);

        ProductionStage originalStage = req.getProductionStage();
        String stageType = originalStage != null ? originalStage.getStageType() : "UNKNOWN";

        // Default to 1000 if unknown
        double dailyCapacity = standardDailyCapacity.getOrDefault(stageType.toUpperCase(), 1000.0);

        // Calculate estimated days
        double estimatedDays = totalApproved.doubleValue() / dailyCapacity;

        // Round up to nearest half day
        estimatedDays = Math.ceil(estimatedDays * 2) / 2.0;

        if (estimatedDays > 7.0 && !dto.isForce()) {
            throw new RuntimeException("TIME_EXCEEDED_WARNING: Thời gian làm quá lâu (" + estimatedDays
                    + " ngày) làm cho các đơn hàng sau sẽ bị quá ngày giao hàng. Bạn có chắc chắn muốn phê duyệt không?");
        }

        // 2. Update Requisition
        req.setStatus("APPROVED");
        Long directorId = dto.getDirectorId() != null ? dto.getDirectorId() : 1L;
        req.setApprovedBy(
                userRepository.findById(directorId).orElseThrow(() -> new RuntimeException("Director not found")));
        req.setApprovedAt(Instant.now());

        // Fallback: If totalApproved is still 0, try to read from existing database
        // details
        if (totalApproved.compareTo(BigDecimal.ZERO) == 0) {
            List<tmmsystem.entity.MaterialRequisitionDetail> dbDetails = reqDetailRepo.findByRequisitionId(requestId);
            System.out.println("[REWORK DEBUG] Fallback: Reading from DB. Found " + dbDetails.size() + " details");
            for (tmmsystem.entity.MaterialRequisitionDetail dbDetail : dbDetails) {
                if (dbDetail.getQuantityApproved() != null) {
                    totalApproved = totalApproved.add(dbDetail.getQuantityApproved());
                }
            }
        }

        req.setQuantityApproved(totalApproved);
        req.setNotes(dto.getNotes()); // Update notes if provided
        reqRepo.save(req);

        // 3. Create Supplementary Order (Rework Order)
        if (originalStage == null) {
            throw new RuntimeException("Cannot create rework order: Original stage is missing");
        }
        ProductionOrder originalPO = originalStage.getProductionOrder();

        ProductionOrder reworkPO = new ProductionOrder();
        reworkPO.setPoNumber(originalPO.getPoNumber() + "-REWORK-" + System.currentTimeMillis() % 1000);
        reworkPO.setContract(originalPO.getContract());

        System.out.println("[REWORK DEBUG] totalApproved (kg): " + totalApproved);

        // Convert kg (approvedQuantity) to pcs (totalQuantity)
        // Formula: Pcs = Kg * 1000 / Weight_per_piece_in_grams
        BigDecimal reworkQuantityPcs = totalApproved;
        try {
            List<ProductionOrderDetail> details = podRepo.findByProductionOrderId(originalPO.getId());
            if (!details.isEmpty()) {
                Product product = details.get(0).getProduct();
                BigDecimal standardWeight = product != null ? product.getStandardWeight() : null;

                if (standardWeight != null && standardWeight.compareTo(BigDecimal.ZERO) > 0) {
                    // standardWeight is in GRAMS. Convert to kg for division
                    BigDecimal weightInKg = standardWeight.divide(new BigDecimal(1000), 4,
                            java.math.RoundingMode.HALF_UP);
                    if (weightInKg.compareTo(BigDecimal.ZERO) > 0) {
                        reworkQuantityPcs = totalApproved.divide(weightInKg, 0, java.math.RoundingMode.HALF_UP);
                    }
                }
            }
        } catch (Exception e) {
            // Log error silently - calculation will fallback
        }

        if (reworkQuantityPcs.compareTo(BigDecimal.ZERO) <= 0) {
            reworkQuantityPcs = BigDecimal.ONE; // Default to 1 if calculation fails
        }

        reworkPO.setTotalQuantity(reworkQuantityPcs); // Quantity for rework in PCS

        reworkPO.setPlannedStartDate(java.time.LocalDate.now());
        reworkPO.setPlannedEndDate(java.time.LocalDate.now().plusDays((long) Math.ceil(estimatedDays)));

        // Determine rework order status based on WARPING availability
        // Check if any rework order is currently at WARPING stage with IN_PROGRESS
        // status
        boolean warpingBusy = isWarpingBusyForRework();
        if (warpingBusy) {
            reworkPO.setStatus("WAITING_SUPPLEMENTARY");
            reworkPO.setExecutionStatus("WAITING_SUPPLEMENTARY");
        } else {
            reworkPO.setStatus("READY_SUPPLEMENTARY");
            reworkPO.setExecutionStatus("READY_SUPPLEMENTARY");
        }

        reworkPO.setPriority(originalPO.getPriority() + 1); // Higher priority
        // Preserve planCode from original order notes so lotCode can be extracted
        String originalPlanCode = extractPlanCodeFromNotes(originalPO.getNotes());
        String planCodeSuffix = originalPlanCode != null ? " Auto-generated from Production Plan: " + originalPlanCode
                : "";
        reworkPO.setNotes(
                "Supplementary order for " + originalPO.getPoNumber() + ". Reason: " + req.getNotes() + planCodeSuffix);
        reworkPO.setCreatedBy(req.getRequestedBy());
        reworkPO.setApprovedBy(req.getApprovedBy());
        reworkPO.setApprovedAt(Instant.now());

        ProductionOrder savedReworkPO = poRepo.save(reworkPO);

        // Update original order status - no longer waiting for material approval
        originalPO.setExecutionStatus("SUPPLEMENTARY_CREATED");
        poRepo.save(originalPO);

        // 3.1. Clone ProductionOrderDetail
        try {
            List<ProductionOrderDetail> originalDetails = podRepo.findByProductionOrderId(originalPO.getId());
            if (!originalDetails.isEmpty()) {
                ProductionOrderDetail originalDetail = originalDetails.get(0);
                ProductionOrderDetail reworkDetail = new ProductionOrderDetail();
                reworkDetail.setProductionOrder(savedReworkPO);
                reworkDetail.setProduct(originalDetail.getProduct());
                reworkDetail.setBom(originalDetail.getBom()); // Required field
                reworkDetail.setBomVersion(originalDetail.getBomVersion()); // Copy BOM version
                reworkDetail.setQuantity(reworkQuantityPcs); // Set quantity in PCS (BigDecimal)
                reworkDetail.setUnit(originalDetail.getUnit());
                reworkDetail.setNoteColor(originalDetail.getNoteColor()); // Copy color note
                podRepo.save(reworkDetail);
            }
        } catch (Exception e) {
            System.err.println("Error cloning production order detail: " + e.getMessage());
            e.printStackTrace(); // Print full stack trace for debugging
        }

        // 4. Clone Stages (Only up to the defective stage)
        List<ProductionStage> originalStages = stageRepo
                .findByProductionOrderIdOrderByStageSequenceAsc(originalPO.getId());

        Integer defectiveStageSequence = originalStage.getStageSequence();

        for (ProductionStage oldStage : originalStages) {
            // Only clone stages up to and including the defective stage
            if (oldStage.getStageSequence() > defectiveStageSequence) {
                continue;
            }

            ProductionStage newStage = new ProductionStage();
            newStage.setProductionOrder(savedReworkPO);
            newStage.setStageType(oldStage.getStageType());
            newStage.setStageSequence(oldStage.getStageSequence());
            newStage.setMachine(oldStage.getMachine()); // Retain Machine
            newStage.setAssignedLeader(oldStage.getAssignedLeader()); // Retain Leader
            newStage.setAssignedTo(oldStage.getAssignedTo());
            newStage.setQcAssignee(oldStage.getQcAssignee());
            newStage.setOutsourced(oldStage.getOutsourced());
            newStage.setOutsourceVendor(oldStage.getOutsourceVendor());

            // Set Rework Flags
            newStage.setIsRework(true);
            newStage.setOriginalStage(oldStage);

            // Status: First stage READY, others PENDING
            if (newStage.getStageSequence() == 1) {
                syncStageStatus(newStage, "WAITING"); // Ready to start
            } else {
                syncStageStatus(newStage, "PENDING");
            }
            newStage.setProgressPercent(0);

            stageRepo.save(newStage);

            // Notify Leader
            if (newStage.getAssignedLeader() != null) {
                notificationService.notifyUser(newStage.getAssignedLeader(), "PRODUCTION", "INFO",
                        "Lệnh sản xuất bổ sung",
                        "Bạn được phân công lại công đoạn " + newStage.getStageType() + " cho lệnh bổ sung "
                                + savedReworkPO.getPoNumber(),
                        "PRODUCTION_ORDER", savedReworkPO.getId());
            }
        }

        // FIX: Update original order's executionStatus to match rework order's status
        // This ensures frontend shows correct status: "Sẵn sàng SX bổ sung" or "Chờ SX
        // bổ sung"
        if (originalPO != null && "WAITING_MATERIAL_APPROVAL".equals(originalPO.getExecutionStatus())) {
            // Use same status as the rework order (READY_SUPPLEMENTARY or
            // WAITING_SUPPLEMENTARY)
            originalPO.setExecutionStatus(savedReworkPO.getExecutionStatus());
            poRepo.save(originalPO);
        }

        return savedReworkPO;
    }

    @Transactional
    public ProductionOrder startSupplementaryOrder(Long orderId) {
        ProductionOrder order = poRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getPoNumber().contains("-REWORK")) {
            throw new RuntimeException("Not a supplementary order");
        }

        // Allow starting from READY_SUPPLEMENTARY or WAITING_SUPPLEMENTARY (legacy:
        // WAITING_PRODUCTION)
        if (!"READY_SUPPLEMENTARY".equals(order.getExecutionStatus())
                && !"WAITING_SUPPLEMENTARY".equals(order.getExecutionStatus())
                && !"WAITING_PRODUCTION".equals(order.getStatus())) {
            throw new RuntimeException("Order is not ready to start");
        }

        // Set this order to IN_SUPPLEMENTARY (Đang sản xuất bổ sung)
        order.setStatus("IN_PROGRESS");
        order.setExecutionStatus("IN_SUPPLEMENTARY");
        poRepo.save(order);

        // Demote all other READY_SUPPLEMENTARY orders to WAITING_SUPPLEMENTARY
        List<ProductionOrder> allReworkOrders = poRepo.findAll().stream()
                .filter(o -> o.getPoNumber() != null && o.getPoNumber().contains("-REWORK"))
                .filter(o -> !o.getId().equals(order.getId()))
                .filter(o -> "READY_SUPPLEMENTARY".equals(o.getExecutionStatus()))
                .collect(java.util.stream.Collectors.toList());

        for (ProductionOrder other : allReworkOrders) {
            other.setExecutionStatus("WAITING_SUPPLEMENTARY");
            poRepo.save(other);
        }

        // Start the first stage (WARPING)
        List<ProductionStage> stages = stageRepo.findByProductionOrderIdOrderByStageSequenceAsc(order.getId());
        if (!stages.isEmpty()) {
            ProductionStage firstStage = stages.get(0);
            syncStageStatus(firstStage, "WAITING");
            stageRepo.save(firstStage);
        }

        return order;
    }

    /**
     * Check if any rework order is currently doing WARPING stage (IN_PROGRESS)
     */
    private boolean isWarpingBusyForRework() {
        // Find all rework orders that are IN_SUPPLEMENTARY (currently being produced)
        List<ProductionOrder> activeReworkOrders = poRepo.findAll().stream()
                .filter(o -> o.getPoNumber() != null && o.getPoNumber().contains("-REWORK"))
                .filter(o -> "IN_SUPPLEMENTARY".equals(o.getExecutionStatus())
                        || "IN_PROGRESS".equals(o.getExecutionStatus()))
                .collect(java.util.stream.Collectors.toList());

        for (ProductionOrder reworkOrder : activeReworkOrders) {
            // Check if first stage (WARPING) is IN_PROGRESS
            List<ProductionStage> stages = stageRepo
                    .findByProductionOrderIdOrderByStageSequenceAsc(reworkOrder.getId());
            if (!stages.isEmpty()) {
                ProductionStage firstStage = stages.get(0);
                if ("IN_PROGRESS".equals(firstStage.getExecutionStatus())
                        && ("WARPING".equals(firstStage.getStageType())
                                || "CUONG_MAC".equals(firstStage.getStageType()))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Transactional
    public void migrateStageTrackingData() {
        List<ProductionStage> stages = stageRepo.findAll();
        int count = 0;
        for (ProductionStage stage : stages) {
            List<StageTracking> trackings = stageTrackingRepository
                    .findByProductionStageIdOrderByTimestampAsc(stage.getId());
            boolean isReworkMode = false;
            BigDecimal lastProgress = BigDecimal.ZERO;

            for (StageTracking t : trackings) {
                // Initialize if null
                if (t.getIsRework() == null) {
                    t.setIsRework(false);
                }

                // Detect reset: if progress drops significantly (e.g. from >=90 to <=20)
                // This usually indicates a rework start after completion
                if (t.getQuantityCompleted() != null) {
                    if (lastProgress.compareTo(BigDecimal.valueOf(90)) >= 0
                            && t.getQuantityCompleted().compareTo(BigDecimal.valueOf(20)) <= 0) {
                        isReworkMode = true;
                    }
                    lastProgress = t.getQuantityCompleted();
                }

                // Also check if the stage itself is currently in rework,
                // and this is a recent tracking (heuristic)
                // But relying on progress drop is safer for historical data.

                if (isReworkMode) {
                    t.setIsRework(true);
                    stageTrackingRepository.save(t);
                    count++;
                }
            }
        }
        if (count > 0) {
            System.out.println("Migrated " + count + " stage tracking records to Rework mode.");
        }
    }

    /**
     * Backfill initial 0% START entries for existing rework stages that don't have
     * them.
     * This ensures "Lịch sử cập nhật tiến độ lỗi" shows "Bắt đầu 0%" from the
     * beginning.
     * 
     * NOTE: Uses native query to update timestamp because @CreationTimestamp
     * overrides
     * setter values.
     */
    @Transactional
    public int backfillReworkInitialTracking() {
        int count = 0;
        // Find all stages that have rework history but no initial START entry
        List<ProductionStage> allStages = stageRepo.findAll();

        for (ProductionStage stage : allStages) {
            List<StageTracking> reworkTrackings = stageTrackingRepository
                    .findByProductionStageIdOrderByTimestampAsc(stage.getId())
                    .stream()
                    .filter(t -> Boolean.TRUE.equals(t.getIsRework()))
                    .collect(java.util.stream.Collectors.toList());

            // If there are rework trackings but no START action, add one
            if (!reworkTrackings.isEmpty()) {
                boolean hasStart = reworkTrackings.stream()
                        .anyMatch(t -> "START".equals(t.getAction()));

                if (!hasStart) {
                    // Get the earliest rework tracking to use its timestamp as reference
                    StageTracking earliest = reworkTrackings.get(0);
                    java.time.Instant targetTimestamp = earliest.getTimestamp() != null
                            ? earliest.getTimestamp().minusSeconds(1)
                            : java.time.Instant.now();

                    // Create initial START entry
                    StageTracking startEntry = new StageTracking();
                    startEntry.setProductionStage(stage);
                    startEntry.setOperator(
                            stage.getAssignedLeader() != null ? stage.getAssignedLeader() : earliest.getOperator());
                    startEntry.setAction("START");
                    startEntry.setQuantityCompleted(java.math.BigDecimal.ZERO);
                    startEntry.setIsRework(true);
                    startEntry.setNotes("Bắt đầu làm lại lỗi");

                    // Save first (timestamp will be set to NOW by @CreationTimestamp)
                    StageTracking saved = stageTrackingRepository.save(startEntry);

                    // Then update timestamp using native query to bypass @CreationTimestamp
                    stageTrackingRepository.updateTimestampById(saved.getId(), targetTimestamp);
                    count++;
                }
            }
        }

        return count;
    }

    @Transactional
    public void fixMissingReworkDetails() {
        List<ProductionOrder> allOrders = poRepo.findAll();
        int count = 0;
        for (ProductionOrder po : allOrders) {
            if (po.getPoNumber().contains("-REWORK")) {
                List<ProductionOrderDetail> details = podRepo.findByProductionOrderId(po.getId());
                // Fix if details missing OR quantity is 0 OR quantity is 1 (default fallback)
                boolean needsFix = details.isEmpty();
                ProductionOrderDetail detailToFix = null;
                if (!details.isEmpty()) {
                    detailToFix = details.get(0);
                    // Recalculate if quantity is null, 0, or 1 (1 is the default fallback when
                    // calculation failed)
                    if (detailToFix.getQuantity() == null
                            || detailToFix.getQuantity().compareTo(BigDecimal.ZERO) == 0
                            || detailToFix.getQuantity().compareTo(BigDecimal.ONE) == 0) {
                        needsFix = true;
                    } else {
                        // Already has valid quantity > 1, skip
                        needsFix = false;
                    }
                }

                if (needsFix) {
                    // Found a rework order needing fix
                    try {
                        // Find original PO via stages
                        List<ProductionStage> stages = stageRepo
                                .findByProductionOrderIdOrderByStageSequenceAsc(po.getId());
                        ProductionOrder originalPO = null;
                        for (ProductionStage stage : stages) {
                            if (stage.getOriginalStage() != null) {
                                originalPO = stage.getOriginalStage().getProductionOrder();
                                break;
                            }
                        }

                        if (originalPO != null) {
                            List<ProductionOrderDetail> originalDetails = podRepo
                                    .findByProductionOrderId(originalPO.getId());
                            if (!originalDetails.isEmpty()) {
                                ProductionOrderDetail originalDetail = originalDetails.get(0);
                                ProductionOrderDetail reworkDetail = (detailToFix != null) ? detailToFix
                                        : new ProductionOrderDetail();
                                if (reworkDetail.getId() == null) {
                                    reworkDetail.setProductionOrder(po);
                                }
                                reworkDetail.setProduct(originalDetail.getProduct());
                                reworkDetail.setUnit(originalDetail.getUnit());
                                reworkDetail.setBom(originalDetail.getBom()); // Fix: Copy BOM
                                reworkDetail.setBomVersion(originalDetail.getBomVersion()); // Fix: Copy BOM Version
                                reworkDetail.setNoteColor(originalDetail.getNoteColor()); // Fix: Copy Note Color too

                                // Calculate Quantity
                                BigDecimal reworkQty = BigDecimal.ZERO;
                                for (ProductionStage stage : stages) {
                                    if (stage.getOriginalStage() != null) {
                                        List<tmmsystem.entity.MaterialRequisition> reqs = reqRepo
                                                .findByProductionStageId(stage.getOriginalStage().getId());
                                        for (tmmsystem.entity.MaterialRequisition req : reqs) {
                                            if ("APPROVED".equals(req.getStatus())
                                                    && "YARN_SUPPLY".equals(req.getRequisitionType())) {
                                                // Found it! Use quantityRequested as fallback for quantityApproved
                                                BigDecimal approvedKg = req.getQuantityApproved();
                                                if (approvedKg == null || approvedKg.compareTo(BigDecimal.ZERO) == 0) {
                                                    approvedKg = req.getQuantityRequested();
                                                }

                                                // Fallback: If parent quantity is still null/zero, sum from details
                                                if (approvedKg == null || approvedKg.compareTo(BigDecimal.ZERO) == 0) {
                                                    List<tmmsystem.entity.MaterialRequisitionDetail> reqDetails = reqDetailRepo
                                                            .findByRequisitionId(req.getId());
                                                    approvedKg = reqDetails.stream()
                                                            .map(d -> {
                                                                // Use approved if available, else requested
                                                                BigDecimal qty = d.getQuantityApproved();
                                                                if (qty == null
                                                                        || qty.compareTo(BigDecimal.ZERO) == 0) {
                                                                    qty = d.getQuantityRequested();
                                                                }
                                                                return qty != null ? qty : BigDecimal.ZERO;
                                                            })
                                                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                                                }

                                                if (approvedKg != null && approvedKg.compareTo(BigDecimal.ZERO) > 0) {
                                                    Product product = originalDetail.getProduct();
                                                    BigDecimal standardWeight = product.getStandardWeight();
                                                    if (standardWeight != null
                                                            && standardWeight.compareTo(BigDecimal.ZERO) > 0) {
                                                        BigDecimal weightInKg = standardWeight.divide(
                                                                new BigDecimal(1000), 4,
                                                                java.math.RoundingMode.HALF_UP);
                                                        reworkQty = approvedKg.divide(weightInKg, 0,
                                                                java.math.RoundingMode.HALF_UP);
                                                    }
                                                }
                                                break;
                                            }
                                        }
                                    }
                                    if (reworkQty.compareTo(BigDecimal.ZERO) > 0)
                                        break;
                                }

                                reworkDetail.setQuantity(reworkQty);
                                podRepo.save(reworkDetail);

                                // Also update ProductionOrder.totalQuantity
                                po.setTotalQuantity(reworkQty);
                                poRepo.save(po);

                                System.out.println(
                                        "[FIX] Rework order " + po.getPoNumber() + " quantity updated to " + reworkQty);
                                count++;
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error fixing rework order " + po.getPoNumber() + ": " + e.getMessage());
                    }
                }
            }
        }
        if (count > 0) {
            System.out.println("Fixed " + count + " rework orders with missing details.");
        }
    }

    /**
     * Fix existing rework order statuses to use new supplementary status values
     * - WAITING_PRODUCTION -> READY_SUPPLEMENTARY (if no warping busy) or
     * WAITING_SUPPLEMENTARY
     * - Also updates original orders from WAITING_MATERIAL_APPROVAL to
     * SUPPLEMENTARY_CREATED if they have rework order
     */
    @Transactional
    public int fixReworkOrderStatuses() {
        int count = 0;
        List<ProductionOrder> allOrders = poRepo.findAll();

        for (ProductionOrder po : allOrders) {
            // Fix rework orders with old WAITING_PRODUCTION status
            if (po.getPoNumber() != null && po.getPoNumber().contains("-REWORK")) {
                if ("WAITING_PRODUCTION".equals(po.getExecutionStatus())) {
                    // Check if WARPING is busy
                    boolean warpingBusy = isWarpingBusyForRework();
                    if (warpingBusy) {
                        po.setExecutionStatus("WAITING_SUPPLEMENTARY");
                        po.setStatus("WAITING_SUPPLEMENTARY");
                    } else {
                        po.setExecutionStatus("READY_SUPPLEMENTARY");
                        po.setStatus("READY_SUPPLEMENTARY");
                    }
                    poRepo.save(po);
                    System.out.println(
                            "[FIX] Rework order " + po.getPoNumber() + " status updated to " + po.getExecutionStatus());
                    count++;
                }
            }

            // Fix original orders stuck at WAITING_MATERIAL_APPROVAL when rework exists
            // Also check orders that have approved requests but status not yet updated
            String currentStatus = po.getExecutionStatus();
            boolean needsStatusFix = "WAITING_MATERIAL_APPROVAL".equals(currentStatus)
                    || (currentStatus != null && !currentStatus.equals("SUPPLEMENTARY_CREATED")
                            && !currentStatus.equals("COMPLETED") && !currentStatus.equals("ORDER_COMPLETED")
                            && !po.getPoNumber().contains("-REWORK"));

            if (needsStatusFix) {
                // Check if there's an APPROVED material request for this order's stages
                List<ProductionStage> stages = stageRepo.findByProductionOrderIdOrderByStageSequenceAsc(po.getId());
                boolean hasApprovedRequest = false;
                for (ProductionStage stage : stages) {
                    List<MaterialRequisition> reqs = reqRepo.findByProductionStageId(stage.getId());
                    for (MaterialRequisition req : reqs) {
                        if ("APPROVED".equals(req.getStatus())) {
                            hasApprovedRequest = true;
                            break;
                        }
                    }
                    if (hasApprovedRequest)
                        break;
                }

                if (hasApprovedRequest && !"SUPPLEMENTARY_CREATED".equals(po.getExecutionStatus())) {
                    po.setExecutionStatus("SUPPLEMENTARY_CREATED");
                    poRepo.save(po);
                    System.out.println(
                            "[FIX] Original order " + po.getPoNumber()
                                    + " status updated to SUPPLEMENTARY_CREATED (was: " + currentStatus + ")");
                    count++;
                }
            }
        }

        if (count > 0) {
            System.out.println("Fixed " + count + " rework/original order statuses.");
        }
        return count;
    }

    public QualityIssue getDefectForStage(Long stageId) {
        return issueRepo.findByProductionStageId(stageId).stream().findFirst().orElse(null);
    }

    @Transactional
    public void pauseOtherOrdersAtStage(String stageType, Long excludeOrderId) {
        // Allow pausing Dyeing (NHUOM) as well if needed for Rework priority
        // if ("NHUOM".equalsIgnoreCase(stageType) ||
        // "DYEING".equalsIgnoreCase(stageType)) {
        // return;
        // }

        List<ProductionStage> activeStages = stageRepo.findByStageTypeAndStatus(stageType, "IN_PROGRESS");
        for (ProductionStage stage : activeStages) {
            // Skip if no production order or if it's the current rework order
            if (stage.getProductionOrder() == null) {
                continue;
            }
            if (stage.getProductionOrder().getId().equals(excludeOrderId)) {
                continue;
            }

            // Pause the stage
            stage.setStatus("PAUSED");
            stage.setNotes((stage.getNotes() != null ? stage.getNotes() + "\n" : "")
                    + "System: Paused due to priority Rework Order.");
            stageRepo.save(stage);

            // Release Machine
            try {
                // Update machine status to AVAILABLE
                machineRepository.updateStatusByType(stage.getStageType(), "AVAILABLE");

                // Release assignments
                List<MachineAssignment> assignments = machineAssignmentRepository
                        .findByProductionStageAndReservationStatus(stage, "ACTIVE");
                for (MachineAssignment ma : assignments) {
                    ma.setReservationStatus("RELEASED");
                    ma.setReleasedAt(Instant.now());
                    machineAssignmentRepository.save(ma);
                }

                // Also update for alias if exists
                if (STAGE_TYPE_ALIASES.containsKey(stage.getStageType())) {
                    machineRepository.updateStatusByType(STAGE_TYPE_ALIASES.get(stage.getStageType()), "AVAILABLE");
                }
            } catch (Exception e) {
                System.err.println("Error releasing machine for paused stage " + stage.getId() + ": " + e.getMessage());
            }

            // Notify Leader
            if (stage.getAssignedLeader() != null) {
                String poNumber = stage.getProductionOrder().getPoNumber() != null
                        ? stage.getProductionOrder().getPoNumber()
                        : "N/A";
                notificationService.notifyUser(stage.getAssignedLeader(), "PRODUCTION", "WARNING",
                        "Tạm dừng sản xuất",
                        "Công đoạn " + stageType + " của lệnh " + poNumber
                                + " bị tạm dừng để ưu tiên lệnh sửa lỗi.",
                        "PRODUCTION_ORDER", stage.getProductionOrder().getId());
            }
        }
    }

    @Transactional
    public void resumePausedOrdersAtStage(String stageType) {
        // Dyeing (NHUOM) is outsourced/parallel, so we don't pause it.
        if ("NHUOM".equalsIgnoreCase(stageType) || "DYEING".equalsIgnoreCase(stageType)) {
            return;
        }

        List<ProductionStage> pausedStages = stageRepo.findByStageTypeAndStatus(stageType, "PAUSED");
        for (ProductionStage stage : pausedStages) {
            // Check if there are any OTHER rework orders still running?
            // For simplicity, we assume strict serialization means if one finishes, we can
            // resume.
            // But ideally we should check if ANY rework is active.
            // Let's check if any Rework is IN_PROGRESS at this stage.
            boolean hasActiveRework = stageRepo.findByStageTypeAndStatus(stageType, "IN_PROGRESS").stream()
                    .anyMatch(s -> Boolean.TRUE.equals(s.getIsRework()));

            if (hasActiveRework) {
                continue; // Still blocked by another rework
            }

            // Resume the stage - restore original executionStatus (preserved during pause)
            String originalExecStatus = stage.getExecutionStatus();
            if (originalExecStatus == null || "PAUSED".equals(originalExecStatus)
                    || "PENDING".equals(originalExecStatus) || "WAITING".equals(originalExecStatus)) {
                stage.setStatus("IN_PROGRESS");
            } else {
                stage.setStatus(originalExecStatus);
            }
            stage.setNotes((stage.getNotes() != null ? stage.getNotes() + "\n" : "")
                    + "System: Resumed after Rework Order completion.");
            stageRepo.save(stage);

            // Notify Leader
            if (stage.getAssignedLeader() != null && stage.getProductionOrder() != null) {
                String poNumber = stage.getProductionOrder().getPoNumber() != null
                        ? stage.getProductionOrder().getPoNumber()
                        : "N/A";
                notificationService.notifyUser(stage.getAssignedLeader(), "PRODUCTION", "INFO",
                        "Tiếp tục sản xuất",
                        "Công đoạn " + stageType + " của lệnh " + poNumber
                                + " đã được tiếp tục.",
                        "PRODUCTION_ORDER", stage.getProductionOrder().getId());
            }
        }
    }

    // Cleanup Duplicate Orders Helper
    private void cleanupDuplicateOrders() {
        List<ProductionOrder> allOrders = poRepo.findAll();
        // Group by Notes (Plan Code)
        Map<String, List<ProductionOrder>> ordersByPlan = new HashMap<>();

        for (ProductionOrder po : allOrders) {
            String notes = po.getNotes();
            if (notes != null && notes.startsWith("Auto-generated from Production Plan:")) {
                ordersByPlan.computeIfAbsent(notes, k -> new ArrayList<>()).add(po);
            }
        }

        // Find duplicates
        int removedCount = 0;
        for (Map.Entry<String, List<ProductionOrder>> entry : ordersByPlan.entrySet()) {
            List<ProductionOrder> duplicates = entry.getValue();
            if (duplicates.size() > 1) {
                // Sort by ID ascending (keep the first created)
                duplicates.sort(Comparator.comparing(ProductionOrder::getId));

                // Keep the first one, delete the rest
                ProductionOrder toKeep = duplicates.get(0);

                for (int i = 1; i < duplicates.size(); i++) {
                    ProductionOrder toDelete = duplicates.get(i);
                    // Check if safe to delete (e.g. status is WAITING)
                    // But duplicates are bugs, so force delete to clean up
                    System.out.println("Cleaning up duplicate PO: " + toDelete.getPoNumber() + " (ID: "
                            + toDelete.getId() + ") for Plan: " + entry.getKey());

                    // Delete details first
                    List<ProductionOrderDetail> details = podRepo.findByProductionOrderId(toDelete.getId());
                    podRepo.deleteAll(details);

                    // Delete stages
                    List<ProductionStage> stages = stageRepo
                            .findByProductionOrderIdOrderByStageSequenceAsc(toDelete.getId());
                    // Delete tracking/assignments/issues associated with stages if cascade is not
                    // enough
                    for (ProductionStage s : stages) {
                        issueRepo.deleteAll(issueRepo.findByProductionStageId(s.getId()));
                        stageTrackingRepository.deleteAll(
                                stageTrackingRepository.findByProductionStageIdOrderByTimestampDesc(s.getId()));
                        machineAssignmentRepository.deleteByProductionStageId(s.getId());
                        reqRepo.deleteAll(reqRepo.findByProductionStageId(s.getId()));
                    }
                    stageRepo.deleteAll(stages);

                    poRepo.delete(toDelete);
                    removedCount++;
                }
            }
        }
        if (removedCount > 0) {
            System.out.println("Cleaned up " + removedCount + " duplicate Production Orders.");
        }
    }

    /**
     * Extract planCode from ProductionOrder notes field.
     * Format: "Auto-generated from Production Plan: PP-xxxxxx"
     */
    private String extractPlanCodeFromNotes(String notes) {
        if (notes == null || notes.isEmpty()) {
            return null;
        }
        String prefix = "Auto-generated from Production Plan: ";
        int startIndex = notes.indexOf(prefix);
        if (startIndex == -1) {
            return null;
        }
        String planCode = notes.substring(startIndex + prefix.length()).trim();
        // Handle if there's more text after planCode
        int spaceIndex = planCode.indexOf(' ');
        if (spaceIndex > 0) {
            planCode = planCode.substring(0, spaceIndex);
        }
        return planCode.isEmpty() ? null : planCode;
    }

    // ==================== BLOCKING CHECK ====================

    /**
     * Helper record to hold blocking information
     */
    private record BlockingInfo(boolean isBlocked, String blockedByLotCode) {
    }

    /**
     * Check if a stage is blocked by another lot doing the same stage type.
     * A stage is blocked if:
     * 1. It's a non-parallel stage (not DYEING)
     * 2. Another lot has IN_PROGRESS or REWORK_IN_PROGRESS at the same stage type
     */
    private BlockingInfo checkStageBlocked(ProductionStage stage) {
        // DYEING is outsourced/parallel - never blocked
        String stageType = stage.getStageType();
        if (stageType == null) {
            return new BlockingInfo(false, null);
        }
        boolean isParallelStage = "DYEING".equalsIgnoreCase(stageType) || "NHUOM".equalsIgnoreCase(stageType);
        if (isParallelStage) {
            return new BlockingInfo(false, null);
        }

        // Find all stages of same type that are IN_PROGRESS, REWORK_IN_PROGRESS,
        // WAITING_QC, or QC_IN_PROGRESS
        // BUG FIX: Include WAITING_QC and QC_IN_PROGRESS because stage is still
        // "occupying" the machine until QC completes
        List<ProductionStage> activeStages = stageRepo.findByExecutionStatusIn(
                List.of("IN_PROGRESS", "REWORK_IN_PROGRESS", "WAITING_QC", "QC_IN_PROGRESS"));

        for (ProductionStage activeStage : activeStages) {
            if (activeStage.getId().equals(stage.getId()))
                continue; // Skip self

            // Check if same stage type (considering aliases)
            String activeType = activeStage.getStageType() != null ? activeStage.getStageType().toUpperCase() : "";
            String thisType = stageType.toUpperCase();

            boolean sameType = activeType.equals(thisType);
            if (!sameType && STAGE_TYPE_ALIASES.containsKey(thisType)) {
                sameType = activeType.equals(STAGE_TYPE_ALIASES.get(thisType));
            }
            if (!sameType && STAGE_TYPE_ALIASES.containsKey(activeType)) {
                sameType = thisType.equals(STAGE_TYPE_ALIASES.get(activeType));
            }

            if (sameType) {
                // This stage is blocked by activeStage
                String blockedByLotCode = getLotCodeForStage(activeStage);
                return new BlockingInfo(true, blockedByLotCode);
            }
        }

        return new BlockingInfo(false, null);
    }

    /**
     * Get lotCode for a stage's production order
     */
    private String getLotCodeForStage(ProductionStage stage) {
        if (stage.getProductionOrder() == null)
            return "N/A";

        ProductionOrder po = stage.getProductionOrder();
        String planCode = extractPlanCodeFromNotes(po.getNotes());
        if (planCode != null) {
            tmmsystem.entity.ProductionPlan plan = productionPlanRepository.findByPlanCode(planCode).orElse(null);
            if (plan != null && plan.getLot() != null) {
                return plan.getLot().getLotCode();
            }
        }
        return po.getPoNumber() != null ? po.getPoNumber() : "N/A";
    }
}
