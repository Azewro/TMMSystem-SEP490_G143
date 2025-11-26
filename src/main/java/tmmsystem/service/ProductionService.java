package tmmsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.entity.*;
import tmmsystem.entity.ProductionPlanStage;
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

@Service
public class ProductionService {
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
    private final ProductionLotRepository productionLotRepository;
    private final ProductionMapper productionMapper;
    private final tmmsystem.repository.QualityIssueRepository issueRepo;

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
            ProductionPlanService productionPlanService,
            StageTrackingRepository stageTrackingRepository,
            StagePauseLogRepository stagePauseLogRepository,
            OutsourcingTaskRepository outsourcingTaskRepository,
            MachineAssignmentRepository machineAssignmentRepository,
            MachineRepository machineRepository,
            ProductionPlanRepository productionPlanRepository,
            ProductionLotRepository productionLotRepository,
            ProductionMapper productionMapper,
            tmmsystem.repository.QualityIssueRepository issueRepo) {
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
        this.productionLotRepository = productionLotRepository;
        this.productionMapper = productionMapper;
        this.issueRepo = issueRepo;
    }

    // Production Order
    public List<ProductionOrder> findAllPO() {
        return poRepo.findAll();
    }

    public ProductionOrder findPO(Long id) {
        return poRepo.findById(id).orElseThrow();
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
        // Get all MINOR defects assigned to this leader
        return issueRepo.findAll().stream()
                .filter(i -> "MINOR".equals(i.getSeverity()))
                .filter(i -> {
                    ProductionStage stage = i.getProductionStage();
                    return stage != null &&
                            stage.getAssignedLeader() != null &&
                            stage.getAssignedLeader().getId().equals(leaderUserId);
                })
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
        if (issue.getProductionStage() != null) {
            dto.setStageId(issue.getProductionStage().getId());
            dto.setStageType(issue.getProductionStage().getStageType());
        }
        if (issue.getProductionOrder() != null) {
            dto.setOrderId(issue.getProductionOrder().getId());
            dto.setPoNumber(issue.getProductionOrder().getPoNumber());
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
        syncStageStatus(s, "QC_PASSED");
        stageRepo.save(s);

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

            String currentStageType = s.getStageType();

            // Workflow logic based on current stage
            if ("WARPING".equalsIgnoreCase(currentStageType) || "CUONG_MAC".equalsIgnoreCase(currentStageType)) {
                // Cuồng mắc PASS → notify Tổ Trưởng dệt
                if (next.getAssignedLeader() != null) {
                    notificationService.notifyUser(next.getAssignedLeader(), "PRODUCTION", "SUCCESS",
                            "Công đoạn cuồng mắc đạt",
                            "Công đoạn cuồng mắc đã đạt QC. Bạn có thể bắt đầu công đoạn " + next.getStageType(),
                            "PRODUCTION_STAGE", next.getId());
                }
            } else if ("WEAVING".equalsIgnoreCase(currentStageType) || "DET".equalsIgnoreCase(currentStageType)) {
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
            } else if ("DYEING".equalsIgnoreCase(currentStageType) || "NHUOM".equalsIgnoreCase(currentStageType)) {
                // Nhuộm PASS → notify Tổ Trưởng cắt
                if (next.getAssignedLeader() != null) {
                    notificationService.notifyUser(next.getAssignedLeader(), "PRODUCTION", "SUCCESS",
                            "Công đoạn nhuộm đạt",
                            "Công đoạn nhuộm đã đạt QC. Bạn có thể bắt đầu công đoạn " + next.getStageType(),
                            "PRODUCTION_STAGE", next.getId());
                }
            } else if ("CUTTING".equalsIgnoreCase(currentStageType) || "CAT".equalsIgnoreCase(currentStageType)) {
                // Cắt PASS → notify Tổ Trưởng May
                if (next.getAssignedLeader() != null) {
                    notificationService.notifyUser(next.getAssignedLeader(), "PRODUCTION", "SUCCESS",
                            "Công đoạn cắt đạt",
                            "Công đoạn cắt đã đạt QC. Bạn có thể bắt đầu công đoạn " + next.getStageType(),
                            "PRODUCTION_STAGE", next.getId());
                }
            } else if ("HEMMING".equalsIgnoreCase(currentStageType) || "MAY".equalsIgnoreCase(currentStageType)) {
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
        }
    }

    // Dyeing hook on start/complete
    @Transactional
    public ProductionStage startStage(Long stageId, Long leaderUserId, String evidencePhotoUrl,
            BigDecimal qtyCompleted) {
        ensureLeader(stageId, leaderUserId);
        ProductionStage s = stageRepo.findById(stageId).orElseThrow();
        if (s.getStartAt() == null)
            s.setStartAt(Instant.now());
        syncStageStatus(s, "IN_PROGRESS");
        stageRepo.save(s);
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
                ot.setVendorName(s.getOutsourceVendor());
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
                syncStageStatus(stage, i == 0 ? "WAITING" : "PENDING");
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

        for (ProductionOrderDetail pod : pods) {
            String productName = safeProductName(pod);

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
                syncStageStatus(stage, isFirstStage ? "WAITING" : "PENDING");
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
        }

        return stageMapping;
    }

    @Transactional
    public ProductionStage pauseStage(Long stageId, Long leaderUserId, String pauseReason, String pauseNotes) {
        ensureLeader(stageId, leaderUserId);
        ProductionStage s = stageRepo.findById(stageId).orElseThrow();
        // PAUSED không có trong executionStatus, giữ status riêng
        s.setStatus("PAUSED");
        // executionStatus giữ nguyên để biết đang ở giai đoạn nào
        stageRepo.save(s);
        StagePauseLog pl = new StagePauseLog();
        pl.setProductionStage(s);
        pl.setPausedBy(userRepository.findById(leaderUserId).orElseThrow());
        pl.setPauseReason(pauseReason);
        pl.setPauseNotes(pauseNotes);
        pl.setPausedAt(Instant.now());
        stagePauseLogRepository.save(pl);
        StageTracking tr = new StageTracking();
        tr.setProductionStage(s);
        tr.setOperator(pl.getPausedBy());
        tr.setAction("PAUSE");
        stageTrackingRepository.save(tr);
        return s;
    }

    @Transactional
    public ProductionStage resumeStage(Long stageId, Long leaderUserId) {
        ensureLeader(stageId, leaderUserId);
        ProductionStage s = stageRepo.findById(stageId).orElseThrow();
        // Resume về trạng thái đang làm
        syncStageStatus(s, "IN_PROGRESS");
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

        // 1. Capacity Check for WARPING (Công đoạn đầu tiên)
        // Count active WARPING stages (IN_PROGRESS or READY_TO_PRODUCE)
        long activeWarpingCount = stageRepo.countByStageTypeAndExecutionStatusIn(
                "WARPING", List.of("IN_PROGRESS", "READY_TO_PRODUCE"));

        // Get total WARPING machines
        long totalWarpingMachines = machineRepository.findAll().stream()
                .filter(m -> "WARPING".equals(m.getType()))
                .count();

        // If total machines is 0, we can't start.
        if (totalWarpingMachines == 0) {
            throw new RuntimeException("Không tìm thấy máy Cuồng mắc nào trong hệ thống.");
        }

        if (activeWarpingCount >= totalWarpingMachines) {
            throw new RuntimeException("Không thể bắt đầu. Máy cuồng mắc đang đầy (" + activeWarpingCount + "/"
                    + totalWarpingMachines + "). Vui lòng đợi đơn hàng trước hoàn thành.");
        }

        // 2. Update Order Status
        po.setExecutionStatus("IN_PROGRESS");
        po.setStatus("IN_PROGRESS");
        poRepo.save(po);

        // 3. Activate First Stage (WARPING)
        List<ProductionStage> stages = stageRepo.findByProductionOrderIdOrderByStageSequenceAsc(orderId);
        if (stages.isEmpty()) {
            throw new RuntimeException("Đơn hàng chưa có công đoạn nào.");
        }

        ProductionStage firstStage = stages.get(0);
        // Ensure it is WARPING (or whatever the first stage is)
        syncStageStatus(firstStage, "READY_TO_PRODUCE"); // Custom status mapping
        firstStage.setExecutionStatus("READY_TO_PRODUCE");
        stageRepo.save(firstStage);

        // 4. Set other stages to PENDING (if not already)
        for (int i = 1; i < stages.size(); i++) {
            ProductionStage s = stages.get(i);
            s.setExecutionStatus("PENDING"); // Changed from WAITING to PENDING to avoid confusion with legacy "Ready"
                                             // status
            s.setStatus("PENDING");
            stageRepo.save(s);
        }

        // 5. Notify Warping Leader
        if (firstStage.getAssignedLeader() != null) {
            notificationService.notifyUser(firstStage.getAssignedLeader(), "PRODUCTION", "INFO",
                    "Đơn hàng sẵn sàng",
                    "Đơn hàng " + po.getPoNumber() + " đã sẵn sàng sản xuất công đoạn " + firstStage.getStageType(),
                    "PRODUCTION_STAGE", firstStage.getId());
        }

        return stages;
    }

    @Transactional
    public ProductionStage startStage(Long stageId, Long userId) {
        ProductionStage s = stageRepo.findById(stageId)
                .orElseThrow(() -> new RuntimeException("Stage not found"));

        // Allow start if READY_TO_PRODUCE or WAITING (fallback)
        if (!"READY_TO_PRODUCE".equals(s.getExecutionStatus()) && !"WAITING".equals(s.getExecutionStatus())) {
            // Allow re-start if it was PAUSED?
            if (!"PAUSED".equals(s.getStatus())) {
                throw new RuntimeException("Công đoạn chưa sẵn sàng hoặc đã bắt đầu.");
            }
        }

        s.setStartAt(Instant.now());
        s.setExecutionStatus("IN_PROGRESS");
        syncStageStatus(s, "IN_PROGRESS");

        // Log tracking
        StageTracking tr = new StageTracking();
        tr.setProductionStage(s);
        tr.setOperator(userRepository.findById(userId).orElse(null));
        tr.setAction("START");
        // Removed setCreatedAt as it is handled by @CreationTimestamp
        stageTrackingRepository.save(tr);

        // Machine Status Update & Assignment Log (Reused from existing logic)
        machineRepository.updateStatusByType(s.getStageType(), "IN_USE");

        // Create MachineAssignment
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

        return stageRepo.save(s);
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

        return poRepo.findAllById(orderIds);
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
        if (po.getContract() != null) {
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
                            || nextStatus.equals("PENDING"))) {
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
                        if (nextStatus.equals("WAITING") || nextStatus.equals("READY")) {
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
}
