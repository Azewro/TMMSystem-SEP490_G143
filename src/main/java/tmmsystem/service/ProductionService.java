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

import org.springframework.context.annotation.Lazy;

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

    public List<tmmsystem.dto.qc.QualityIssueDto> getTechnicalDefects() {
        // Get all defects (Technical sees everything)
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

        if (issue.getProductionStage() != null) {
            dto.setStageId(issue.getProductionStage().getId());
            dto.setStageType(issue.getProductionStage().getStageType());
            dto.setStageName(issue.getProductionStage().getStageType()); // Use stageType as stageName for now

            // Populate Batch Number (Lot Code)
            String batchNumber = issue.getProductionStage().getBatchNumber();
            if (batchNumber == null && issue.getProductionOrder().getContract() != null) {
                // Fallback: Get from ProductionPlan -> ProductionLot
                List<tmmsystem.entity.ProductionPlan> plans = productionPlanRepository
                        .findByContractId(issue.getProductionOrder().getContract().getId());
                tmmsystem.entity.ProductionPlan currentPlan = plans.stream()
                        .filter(p -> Boolean.TRUE.equals(p.getCurrentVersion()))
                        .findFirst()
                        .orElse(null);
                if (currentPlan != null && currentPlan.getLot() != null) {
                    batchNumber = currentPlan.getLot().getLotCode();
                }
            }
            dto.setBatchNumber(batchNumber);

            if (issue.getProductionStage().getAssignedLeader() != null) {
                dto.setReportedBy(issue.getProductionStage().getAssignedLeader().getName());
            }
        }
        dto.setIssueDescription(issue.getDescription());

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

        // NEW: Rework Preemption Logic
        // If this is a Rework Stage, PAUSE all other IN_PROGRESS stages of the same
        // type (except DYEING)
        if (Boolean.TRUE.equals(s.getIsRework()) && !"DYEING".equalsIgnoreCase(s.getStageType())) {
            List<ProductionStage> activeStages = stageRepo.findByStageTypeAndExecutionStatus(s.getStageType(),
                    "IN_PROGRESS");
            for (ProductionStage active : activeStages) {
                if (!active.getId().equals(s.getId())) {
                    active.setExecutionStatus("PAUSED");
                    active.setStatus("PAUSED"); // Sync status
                    stageRepo.save(active);

                    // Log tracking
                    StageTracking pauseTr = new StageTracking();
                    pauseTr.setProductionStage(active);
                    pauseTr.setOperator(userRepository.findById(leaderUserId).orElseThrow());
                    pauseTr.setAction("PAUSED_BY_PRIORITY");
                    pauseTr.setNotes("Tạm dừng do ưu tiên đơn hàng bù: " + s.getProductionOrder().getPoNumber());
                    stageTrackingRepository.save(pauseTr);

                    // Notify Leader of paused stage
                    if (active.getAssignedLeader() != null) {
                        notificationService.notifyUser(active.getAssignedLeader(), "PRODUCTION", "WARNING",
                                "Tạm dừng sản xuất",
                                "Công đoạn " + active.getStageType() + " của đơn "
                                        + active.getProductionOrder().getPoNumber() +
                                        " bị tạm dừng để ưu tiên đơn hàng bù " + s.getProductionOrder().getPoNumber(),
                                "PRODUCTION_STAGE", active.getId());
                    }
                }
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
                        notificationService.notifyUser(paused.getAssignedLeader(), "PRODUCTION", "INFO",
                                "Tiếp tục sản xuất",
                                "Công đoạn " + paused.getStageType() + " của đơn "
                                        + paused.getProductionOrder().getPoNumber() +
                                        " đã được tiếp tục.",
                                "PRODUCTION_STAGE", paused.getId());
                    }
                }
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

        // Update Details
        if (dto.getDetails() != null) {
            for (tmmsystem.dto.execution.MaterialRequisitionDetailDto detailDto : dto.getDetails()) {
                if (detailDto.getId() != null) {
                    tmmsystem.entity.MaterialRequisitionDetail detail = reqDetailRepo.findById(detailDto.getId())
                            .orElse(null);
                    if (detail != null && detail.getRequisition().getId().equals(requestId)) {
                        detail.setQuantityApproved(detailDto.getQuantityApproved());
                        reqDetailRepo.save(detail);
                        if (detail.getQuantityApproved() != null) {
                            totalApproved = totalApproved.add(detail.getQuantityApproved());
                        }
                    }
                }
            }
        } else {
            // Fallback for legacy calls or empty details (should not happen with new UI)
            // If needed, we could set totalApproved from a field in DTO, but let's rely on
            // details sum
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

        // Calculate total approved quantity
        if (totalApproved.compareTo(BigDecimal.ZERO) == 0 && req.getQuantityApproved() != null) {
            totalApproved = req.getQuantityApproved();
        }

        // Convert kg (approvedQuantity) to pcs (totalQuantity)
        // Formula: Pcs = Kg / Weight_per_piece_in_kg
        BigDecimal reworkQuantityPcs = totalApproved;
        try {
            List<ProductionOrderDetail> details = podRepo.findByProductionOrderId(originalPO.getId());
            if (!details.isEmpty()) {
                Product product = details.get(0).getProduct();
                BigDecimal standardWeight = product.getStandardWeight();
                if (standardWeight != null && standardWeight.compareTo(BigDecimal.ZERO) > 0) {
                    // Assuming standardWeight is in GRAMS (common for towels).
                    BigDecimal weightInKg = standardWeight.divide(new BigDecimal(1000), 4,
                            java.math.RoundingMode.HALF_UP);
                    if (weightInKg.compareTo(BigDecimal.ZERO) > 0) {
                        reworkQuantityPcs = totalApproved.divide(weightInKg, 0, java.math.RoundingMode.HALF_UP);
                    }
                }
            }
        } catch (Exception e) {
            // Fallback or log error
            System.err.println("Error converting unit: " + e.getMessage());
        }

        if (reworkQuantityPcs.compareTo(BigDecimal.ZERO) <= 0) {
            reworkQuantityPcs = BigDecimal.ONE; // Default to 1 if calculation fails
        }
        reworkPO.setTotalQuantity(reworkQuantityPcs); // Quantity for rework in PCS

        reworkPO.setPlannedStartDate(java.time.LocalDate.now());
        reworkPO.setPlannedEndDate(java.time.LocalDate.now().plusDays((long) Math.ceil(estimatedDays)));
        reworkPO.setStatus("WAITING_PRODUCTION");
        reworkPO.setExecutionStatus("WAITING_PRODUCTION");
        reworkPO.setPriority(originalPO.getPriority() + 1); // Higher priority
        reworkPO.setNotes("Supplementary order for " + originalPO.getPoNumber() + ". Reason: " + req.getNotes());
        reworkPO.setCreatedBy(req.getRequestedBy());
        reworkPO.setApprovedBy(req.getApprovedBy());
        reworkPO.setApprovedAt(Instant.now());

        ProductionOrder savedReworkPO = poRepo.save(reworkPO);

        // 3.1. Clone ProductionOrderDetail
        try {
            List<ProductionOrderDetail> originalDetails = podRepo.findByProductionOrderId(originalPO.getId());
            if (!originalDetails.isEmpty()) {
                ProductionOrderDetail originalDetail = originalDetails.get(0);
                ProductionOrderDetail reworkDetail = new ProductionOrderDetail();
                reworkDetail.setProductionOrder(savedReworkPO);
                reworkDetail.setProduct(originalDetail.getProduct());
                reworkDetail.setQuantity(reworkQuantityPcs); // Set quantity in PCS (BigDecimal)
                reworkDetail.setUnit(originalDetail.getUnit());
                // reworkDetail.setNotes("Rework detail from " + originalPO.getPoNumber()); //
                // Removed as setNotes is undefined
                podRepo.save(reworkDetail);
            }
        } catch (Exception e) {
            System.err.println("Error cloning production order detail: " + e.getMessage());
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

        return savedReworkPO;
    }

    @Transactional
    public ProductionOrder startSupplementaryOrder(Long orderId) {
        ProductionOrder order = poRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getPoNumber().contains("-REWORK")) {
            throw new RuntimeException("Not a supplementary order");
        }

        if (!"WAITING_PRODUCTION".equals(order.getStatus())) {
            throw new RuntimeException("Order is not in WAITING_PRODUCTION status");
        }

        order.setStatus("IN_PROGRESS");
        order.setExecutionStatus("IN_PROGRESS");
        // order.setActualStartDate(java.time.LocalDate.now()); // Field removed/not
        // present

        return poRepo.save(order);
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

    @Transactional
    public void fixMissingReworkDetails() {
        List<ProductionOrder> allOrders = poRepo.findAll();
        int count = 0;
        for (ProductionOrder po : allOrders) {
            if (po.getPoNumber().contains("-REWORK")) {
                List<ProductionOrderDetail> details = podRepo.findByProductionOrderId(po.getId());
                // Fix if details missing OR quantity is 0
                boolean needsFix = details.isEmpty();
                ProductionOrderDetail detailToFix = null;
                if (!details.isEmpty()) {
                    detailToFix = details.get(0);
                    if (detailToFix.getQuantity() == null
                            || detailToFix.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                        needsFix = true;
                    } else {
                        // Already has quantity, skip
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
                                                // Found it!
                                                BigDecimal approvedKg = req.getQuantityApproved();

                                                // Fallback: If parent quantity is null/zero, sum from details
                                                if (approvedKg == null || approvedKg.compareTo(BigDecimal.ZERO) == 0) {
                                                    List<tmmsystem.entity.MaterialRequisitionDetail> reqDetails = reqDetailRepo
                                                            .findByRequisitionId(req.getId());
                                                    approvedKg = reqDetails.stream()
                                                            .map(d -> d.getQuantityApproved() != null
                                                                    ? d.getQuantityApproved()
                                                                    : BigDecimal.ZERO)
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
            // Skip the current rework order
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
                notificationService.notifyUser(stage.getAssignedLeader(), "PRODUCTION", "WARNING",
                        "Tạm dừng sản xuất",
                        "Công đoạn " + stageType + " của lệnh " + stage.getProductionOrder().getPoNumber()
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

            // Resume the stage
            stage.setStatus("IN_PROGRESS");
            stage.setNotes((stage.getNotes() != null ? stage.getNotes() + "\n" : "")
                    + "System: Resumed after Rework Order completion.");
            stageRepo.save(stage);

            // Notify Leader
            if (stage.getAssignedLeader() != null) {
                notificationService.notifyUser(stage.getAssignedLeader(), "PRODUCTION", "INFO",
                        "Tiếp tục sản xuất",
                        "Công đoạn " + stageType + " của lệnh " + stage.getProductionOrder().getPoNumber()
                                + " đã được tiếp tục.",
                        "PRODUCTION_ORDER", stage.getProductionOrder().getId());
            }
        }
    }
}
