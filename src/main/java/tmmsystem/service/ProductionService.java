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
    private final OutsourcingTaskRepository outsourcingTaskRepository;
    private final MachineAssignmentRepository machineAssignmentRepository;
    private final MachineRepository machineRepository;
    private final ProductionPlanRepository productionPlanRepository;
    private final ProductionLotRepository productionLotRepository;
    private final ProductionMapper productionMapper;

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
            StagePauseLogRepository stagePauseLogRepository,
            OutsourcingTaskRepository outsourcingTaskRepository,
            MachineAssignmentRepository machineAssignmentRepository,
            MachineRepository machineRepository,
            ProductionPlanRepository productionPlanRepository,
            ProductionLotRepository productionLotRepository,
            ProductionMapper productionMapper) {
        this.poRepo = poRepo;
        this.podRepo = podRepo;
        this.techRepo = techRepo;
        this.woRepo = woRepo;
        this.wodRepo = wodRepo;
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

    // Work Order
    public List<WorkOrder> findWOs(Long poId) {
        return woRepo.findByProductionOrderId(poId);
    }

    public WorkOrder findWO(Long id) {
        return woRepo.findById(id).orElseThrow();
    }

    @Transactional
    public WorkOrder createWO(WorkOrder w) {
        return woRepo.save(w);
    }

    @Transactional
    public WorkOrder updateWO(Long id, WorkOrder upd) {
        WorkOrder e = woRepo.findById(id).orElseThrow();
        e.setProductionOrder(upd.getProductionOrder());
        e.setWoNumber(upd.getWoNumber());
        e.setDeadline(upd.getDeadline());
        e.setStatus(upd.getStatus());
        e.setSendStatus(upd.getSendStatus());
        e.setProduction(upd.getProduction());
        e.setCreatedBy(upd.getCreatedBy());
        e.setApprovedBy(upd.getApprovedBy());
        return e;
    }

    public void deleteWO(Long id) {
        woRepo.deleteById(id);
    }

    // Work Order Detail
    public List<WorkOrderDetail> findWODetails(Long woId) {
        return wodRepo.findByWorkOrderId(woId);
    }

    public WorkOrderDetail findWODetail(Long id) {
        return wodRepo.findById(id).orElseThrow();
    }

    @Transactional
    public WorkOrderDetail createWODetail(WorkOrderDetail d) {
        return wodRepo.save(d);
    }

    @Transactional
    public WorkOrderDetail updateWODetail(Long id, WorkOrderDetail upd) {
        WorkOrderDetail e = wodRepo.findById(id).orElseThrow();
        e.setWorkOrder(upd.getWorkOrder());
        e.setProductionOrderDetail(upd.getProductionOrderDetail());
        e.setStageSequence(upd.getStageSequence());
        e.setPlannedStartAt(upd.getPlannedStartAt());
        e.setPlannedEndAt(upd.getPlannedEndAt());
        e.setStartAt(upd.getStartAt());
        e.setCompleteAt(upd.getCompleteAt());
        e.setWorkStatus(upd.getWorkStatus());
        e.setNotes(upd.getNotes());
        return e;
    }

    public void deleteWODetail(Long id) {
        wodRepo.deleteById(id);
    }

    // Stage
    public List<ProductionStage> findStages(Long woDetailId) {
        return stageRepo.findByWorkOrderDetailIdOrderByStageSequenceAsc(woDetailId);
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
        e.setWorkOrderDetail(upd.getWorkOrderDetail());
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
        List<WorkOrderDetail> details = wodRepo.findByWorkOrderId(saved.getId());
        ProductionStage firstStageWithLeader = null;
        for (WorkOrderDetail d : details) {
            List<ProductionStage> stages = stageRepo.findByWorkOrderDetailIdOrderByStageSequenceAsc(d.getId());
            for (ProductionStage s : stages) {
                if (s.getQrToken() == null || s.getQrToken().isBlank()) {
                    s.setQrToken(generateQrToken());
                }
                if (firstStageWithLeader == null && s.getStageSequence() == 1 && s.getAssignedLeader() != null) {
                    firstStageWithLeader = s;
                    syncStageStatus(s, "WAITING"); // Set first stage to WAITING
                }
            }
            stageRepo.saveAll(stages);
        }
        notificationService.notifyWorkOrderApproved(saved);
        if (firstStageWithLeader != null) {
            notificationService.notifyUser(firstStageWithLeader.getAssignedLeader(), "WORK_ORDER", "SUCCESS",
                    "WO đã duyệt", "Công đoạn đầu (" + firstStageWithLeader.getStageType() + ") sẵn sàng bắt đầu",
                    "WORK_ORDER", saved.getId());
        }
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

        // Determine next stage in same WorkOrderDetail
        WorkOrderDetail wod = s.getWorkOrderDetail();
        List<ProductionStage> stages = stageRepo.findByWorkOrderDetailIdOrderByStageSequenceAsc(wod.getId());
        ProductionStage next = stages.stream().filter(x -> x.getStageSequence() != null && s.getStageSequence() != null
                && x.getStageSequence() == s.getStageSequence() + 1).findFirst().orElse(null);

        if (next != null) {
            // Set next stage to WAITING (chờ làm)
            syncStageStatus(next, "WAITING");
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
                WorkOrderDetail wodOfStage = s.getWorkOrderDetail();
                ProductionOrder po = wodOfStage.getProductionOrderDetail().getProductionOrder();
                List<WorkOrderDetail> allWod = wodRepo.findAll().stream()
                        .filter(w -> w.getProductionOrderDetail() != null
                                && w.getProductionOrderDetail().getProductionOrder() != null
                                && w.getProductionOrderDetail().getProductionOrder().getId().equals(po.getId()))
                        .toList();
                boolean allOk = true;
                for (WorkOrderDetail w : allWod) {
                    List<ProductionStage> stgs = stageRepo.findByWorkOrderDetailIdOrderByStageSequenceAsc(w.getId());
                    if (stgs.isEmpty()) {
                        allOk = false;
                        break;
                    }
                    for (ProductionStage st : stgs) {
                        if (!"PASS".equalsIgnoreCase(st.getQcLastResult())) {
                            allOk = false;
                            break;
                        }
                    }
                    if (!allOk)
                        break;
                    ProductionStage pkg = stgs.stream()
                            .filter(st -> "PACKAGING".equalsIgnoreCase(st.getStageType())
                                    || "DONG_GOI".equalsIgnoreCase(st.getStageType()))
                            .findFirst().orElse(null);
                    if (pkg == null || pkg.getCompleteAt() == null) {
                        allOk = false;
                        break;
                    }
                }
                if (allOk) {
                    po.setStatus("ORDER_COMPLETED");
                    poRepo.save(po);
                    notificationService.notifyOrderCompleted(po);
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
        return s;
    }

    private void createDefaultStage(WorkOrderDetail wod, String type, int sequence, boolean outsourced) {
        ProductionStage s = new ProductionStage();
        s.setWorkOrderDetail(wod);
        s.setStageType(type);
        s.setStageSequence(sequence);
        syncStageStatus(s, "PENDING");
        s.setOutsourced(outsourced);
        stageRepo.save(s);
    }

    /**
     * Create a standard Work Order with 6 default stages for each
     * ProductionOrderDetail under the PO
     */
    @Transactional
    public WorkOrder createStandardWorkOrder(Long poId, Long createdById) {
        ProductionOrder po = poRepo.findById(poId).orElseThrow(() -> new RuntimeException("PO not found"));
        WorkOrder wo = new WorkOrder();
        wo.setProductionOrder(po);
        wo.setWoNumber("WO-" + System.currentTimeMillis());
        wo.setStatus("DRAFT");
        wo.setSendStatus("NOT_SENT");
        wo.setProduction(true);
        if (createdById != null) {
            userRepository.findById(createdById).ifPresent(wo::setCreatedBy);
        }
        WorkOrder saved = woRepo.save(wo);
        List<ProductionOrderDetail> pods = podRepo.findByProductionOrderId(poId);
        int seq = 1;
        for (ProductionOrderDetail pod : pods) {
            WorkOrderDetail wod = new WorkOrderDetail();
            wod.setWorkOrder(saved);
            wod.setProductionOrderDetail(pod);
            wod.setStageSequence(seq++);
            wod.setWorkStatus("PENDING");
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

    public record WorkOrderStageResult(WorkOrder workOrder, Map<Long, List<ProductionStage>> stageMap) {
    }

    /**
     * Create WorkOrder + ProductionStages directly from planning stages (auto
     * release flow)
     */
    @Transactional
    public WorkOrderStageResult createWorkOrderFromPlanStages(Long poId, List<ProductionPlanStage> planStages,
            Long createdById) {
        if (planStages == null || planStages.isEmpty()) {
            WorkOrder wo = createStandardWorkOrder(poId, createdById);
            return new WorkOrderStageResult(wo, Map.of());
        }
        ProductionOrder po = poRepo.findById(poId).orElseThrow(() -> new RuntimeException("PO not found"));
        List<ProductionPlanStage> orderedStages = planStages.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(stage -> stage.getSequenceNo() == null ? Integer.MAX_VALUE : stage.getSequenceNo()))
                .toList();
        Map<Long, List<ProductionStage>> stageMapping = new HashMap<>();
        WorkOrder wo = new WorkOrder();
        wo.setProductionOrder(po);
        wo.setWoNumber("WO-" + System.currentTimeMillis());
        wo.setStatus("DRAFT");
        wo.setSendStatus("NOT_SENT");
        wo.setProduction(true);
        if (createdById != null) {
            userRepository.findById(createdById).ifPresent(wo::setCreatedBy);
        }
        WorkOrder saved = woRepo.save(wo);
        List<ProductionOrderDetail> pods = podRepo.findByProductionOrderId(poId);
        int seq = 1;
        Integer firstSequence = orderedStages.stream()
                .map(ProductionPlanStage::getSequenceNo)
                .filter(Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(null);
        for (ProductionOrderDetail pod : pods) {
            WorkOrderDetail wod = new WorkOrderDetail();
            wod.setWorkOrder(saved);
            wod.setProductionOrderDetail(pod);
            wod.setStageSequence(seq++);
            if (!orderedStages.isEmpty()) {
                wod.setPlannedStartAt(toInstant(orderedStages.get(0).getPlannedStartTime()));
                wod.setPlannedEndAt(toInstant(orderedStages.get(orderedStages.size() - 1).getPlannedEndTime()));
            }
            wod.setWorkStatus("PENDING");
            wod = wodRepo.save(wod);

            String productName = safeProductName(pod);
            for (ProductionPlanStage planStage : orderedStages) {
                ProductionStage stage = new ProductionStage();
                stage.setWorkOrderDetail(wod);
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
                ProductionStage savedStage = stageRepo.save(stage);
                stageMapping.computeIfAbsent(planStage.getId(), key -> new ArrayList<>()).add(savedStage);
                notifyStageAssignments(savedStage, po.getPoNumber(), productName);
            }
        }
        return new WorkOrderStageResult(saved, stageMapping);
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
            s.setCompleteAt(Instant.now());
            releaseMachineReservations(s);

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

        return stageRepo.save(s);
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

    @Transactional
    public java.util.List<ProductionStage> bulkAssignStageLeaders(Long woId, java.util.Map<String, Long> map) {
        WorkOrder wo = woRepo.findById(woId).orElseThrow();
        java.util.List<WorkOrderDetail> details = wodRepo.findByWorkOrderId(wo.getId());
        java.util.List<ProductionStage> affected = new java.util.ArrayList<>();
        for (WorkOrderDetail d : details) {
            java.util.List<ProductionStage> stages = stageRepo
                    .findByWorkOrderDetailIdOrderByStageSequenceAsc(d.getId());
            for (ProductionStage s : stages) {
                Long uid = map.get(s.getStageType());
                if (uid != null) {
                    userRepository.findById(uid).ifPresent(s::setAssignedLeader);
                }
            }
            affected.addAll(stageRepo.saveAll(stages));
        }
        return affected;
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
     * - Các stage khác: executionStatus = "PENDING" (đợi)
     */
    @Transactional
    public List<ProductionStage> startWorkOrder(Long orderId) {
        ProductionOrder po = poRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Production Order not found"));

        // Get all work orders for this production order
        List<WorkOrder> workOrders = woRepo.findByProductionOrderId(orderId);
        if (workOrders.isEmpty()) {
            throw new RuntimeException("No Work Order found for Production Order " + orderId);
        }

        List<ProductionStage> allStages = new ArrayList<>();
        java.util.Set<User> notifiedLeaders = new java.util.HashSet<>();
        java.util.Set<User> notifiedQcs = new java.util.HashSet<>();

        for (WorkOrder wo : workOrders) {
            List<WorkOrderDetail> wodList = wodRepo.findByWorkOrderId(wo.getId());
            for (WorkOrderDetail wod : wodList) {
                List<ProductionStage> stages = stageRepo.findByWorkOrderDetailIdOrderByStageSequenceAsc(wod.getId());

                // Find first stage (lowest sequence number)
                ProductionStage firstStage = stages.stream()
                        .filter(s -> s.getStageSequence() != null)
                        .min(Comparator.comparing(ProductionStage::getStageSequence))
                        .orElse(null);

                for (ProductionStage stage : stages) {
                    if (stage.equals(firstStage)) {
                        // First stage: WAITING (chờ làm)
                        syncStageStatus(stage, "WAITING");

                        // Notify assigned leader
                        if (stage.getAssignedLeader() != null && !notifiedLeaders.contains(stage.getAssignedLeader())) {
                            notificationService.notifyUser(stage.getAssignedLeader(), "PRODUCTION", "INFO",
                                    "Công đoạn sẵn sàng bắt đầu",
                                    "Công đoạn " + stage.getStageType() + " của lệnh sản xuất #" + po.getPoNumber()
                                            + " đã sẵn sàng. Bạn có thể bắt đầu làm việc.",
                                    "PRODUCTION_STAGE", stage.getId());
                            notifiedLeaders.add(stage.getAssignedLeader());
                        }
                    } else {
                        // Other stages: PENDING (đợi)
                        syncStageStatus(stage, "PENDING");
                    }

                    // Notify QC if assigned
                    if (stage.getQcAssignee() != null && !notifiedQcs.contains(stage.getQcAssignee())) {
                        notificationService.notifyUser(stage.getQcAssignee(), "QC", "INFO",
                                "Chuẩn bị kiểm tra",
                                "Công đoạn " + stage.getStageType() + " của lệnh sản xuất #" + po.getPoNumber()
                                        + " sẽ cần kiểm tra sau khi hoàn thành.",
                                "PRODUCTION_STAGE", stage.getId());
                        notifiedQcs.add(stage.getQcAssignee());
                    }

                    allStages.add(stage);
                }
            }
        }

        // Notify all QC staff if no specific QC assigned
        List<User> qcStaff = userRepository.findByRoleName("QC_STAFF");
        for (User qc : qcStaff) {
            if (!notifiedQcs.contains(qc)) {
                notificationService.notifyUser(qc, "QC", "INFO",
                        "Lệnh sản xuất mới",
                        "Lệnh sản xuất #" + po.getPoNumber()
                                + " đã bắt đầu. Các công đoạn sẽ cần kiểm tra sau khi hoàn thành.",
                        "PRODUCTION_ORDER", po.getId());
            }
        }

        stageRepo.saveAll(allStages);
        return allStages;
    }

    /**
     * Lấy danh sách stages của Production Order để hiển thị kế hoạch
     */
    public List<ProductionStage> getOrderStages(Long orderId) {
        return stageRepo.findStagesByOrderId(orderId);
    }

    /**
     * Lấy danh sách orders cho Production Manager
     */
    public List<ProductionOrder> getManagerOrders() {
        // Get all production orders that have work orders
        return poRepo.findAll().stream()
                .filter(po -> !woRepo.findByProductionOrderId(po.getId()).isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Lấy danh sách orders cho Team Leader
     */
    public List<ProductionOrder> getLeaderOrders(Long leaderUserId) {
        // Get orders that have stages assigned to this leader
        List<ProductionStage> leaderStages = stageRepo.findByAssignedLeaderIdAndExecutionStatusIn(
                leaderUserId,
                java.util.List.of("WAITING", "IN_PROGRESS", "WAITING_QC", "QC_IN_PROGRESS"));

        java.util.Set<Long> orderIds = leaderStages.stream()
                .map(s -> s.getWorkOrderDetail().getProductionOrderDetail().getProductionOrder().getId())
                .collect(java.util.stream.Collectors.toSet());

        return poRepo.findAllById(orderIds);
    }

    /**
     * Lấy danh sách orders cho KCS
     */
    public List<ProductionOrder> getQaOrders() {
        // Get orders that have stages waiting for QC or in QC
        List<ProductionStage> qcStages = stageRepo.findByExecutionStatusIn(
                java.util.List.of("WAITING_QC", "QC_IN_PROGRESS"));

        java.util.Set<Long> orderIds = qcStages.stream()
                .map(s -> s.getWorkOrderDetail().getProductionOrderDetail().getProductionOrder().getId())
                .collect(java.util.stream.Collectors.toSet());

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

        // Map status sang statusLabel
        dto.setStatusLabel(mapStatusToLabel(po.getStatus()));

        // Set aliases for frontend compatibility
        dto.setExpectedStartDate(po.getPlannedStartDate());
        dto.setExpectedFinishDate(po.getPlannedEndDate());
        dto.setExpectedDeliveryDate(po.getPlannedStartDate()); // Leader dùng expectedDeliveryDate

        // Lấy danh sách stages và enrich với totalHours
        List<ProductionStage> stages = getOrderStages(po.getId());
        List<ProductionStageDto> stageDtos = stages.stream().map(stage -> {
            ProductionStageDto stageDto = productionMapper.toDto(stage);
            // Tính totalHours từ StageTracking
            java.math.BigDecimal totalHours = calculateTotalHoursForStage(stage.getId());
            stageDto.setTotalHours(totalHours);
            return stageDto;
        }).collect(java.util.stream.Collectors.toList());
        dto.setStages(stageDtos);

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
