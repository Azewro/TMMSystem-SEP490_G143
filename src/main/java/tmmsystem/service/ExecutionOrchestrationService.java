package tmmsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.entity.MaterialRequisition;
import tmmsystem.entity.ProductionOrder;
import tmmsystem.entity.ProductionStage;
import tmmsystem.entity.QcSession;
import tmmsystem.entity.QualityIssue;
import tmmsystem.entity.StageTracking;
import tmmsystem.entity.User;
import tmmsystem.repository.MaterialRequisitionRepository;
import tmmsystem.repository.ProductionOrderRepository;
import tmmsystem.repository.ProductionStageRepository;
import tmmsystem.repository.QualityIssueRepository;
import tmmsystem.repository.QcSessionRepository;
import tmmsystem.repository.UserRepository;
import tmmsystem.repository.StageTrackingRepository;
import tmmsystem.repository.ProductionPlanRepository;
import tmmsystem.repository.MachineRepository;
import tmmsystem.entity.ProductionPlan;
import tmmsystem.repository.MachineAssignmentRepository;
import tmmsystem.entity.MachineAssignment;
import tmmsystem.entity.Machine;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import tmmsystem.dto.qc.QcInspectionDto;
import tmmsystem.entity.QcCheckpoint;
import tmmsystem.entity.QcInspection;
import tmmsystem.repository.QcCheckpointRepository;
import tmmsystem.repository.QcInspectionRepository;

@Service
public class ExecutionOrchestrationService {
    private final ProductionOrderRepository orderRepo;
    private final ProductionStageRepository stageRepo;
    private final QualityIssueRepository issueRepo;
    private final QcSessionRepository sessionRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;
    private final MaterialRequisitionRepository materialReqRepo;
    private final ProductionService productionService;
    private final StageTrackingRepository stageTrackingRepository;
    private final QcCheckpointRepository qcCheckpointRepository;
    private final QcInspectionRepository qcInspectionRepository;
    private final MachineRepository machineRepository;
    private final MachineAssignmentRepository machineAssignmentRepository;
    private final ProductionPlanRepository productionPlanRepository;
    private static final Map<String, String> STAGE_TYPE_ALIASES = Map.ofEntries(
            Map.entry("WARPING", "CUONG_MAC"),
            Map.entry("CUONG_MAC", "WARPING"),
            Map.entry("WEAVING", "DET"),
            Map.entry("DET", "WEAVING"),
            Map.entry("DYEING", "NHUOM"),
            Map.entry("NHUOM", "DYEING"),
            Map.entry("CUTTING", "CAT"),
            Map.entry("CAT", "CUTTING"),
            Map.entry("HEMMING", "MAY"),
            Map.entry("MAY", "HEMMING"),
            Map.entry("PACKAGING", "DONG_GOI"),
            Map.entry("DONG_GOI", "PACKAGING"));

    public ExecutionOrchestrationService(ProductionOrderRepository orderRepo,
            ProductionStageRepository stageRepo,
            QualityIssueRepository issueRepo,
            QcSessionRepository sessionRepo,
            UserRepository userRepo,
            NotificationService notificationService,
            MaterialRequisitionRepository materialReqRepo,
            ProductionService productionService,
            StageTrackingRepository stageTrackingRepository,
            QcCheckpointRepository qcCheckpointRepository,
            QcInspectionRepository qcInspectionRepository,
            MachineRepository machineRepository,
            MachineAssignmentRepository machineAssignmentRepository,
            ProductionPlanRepository productionPlanRepository) {
        this.orderRepo = orderRepo;
        this.stageRepo = stageRepo;
        this.issueRepo = issueRepo;
        this.sessionRepo = sessionRepo;
        this.userRepo = userRepo;
        this.notificationService = notificationService;
        this.materialReqRepo = materialReqRepo;
        this.productionService = productionService;
        this.stageTrackingRepository = stageTrackingRepository;
        this.qcCheckpointRepository = qcCheckpointRepository;
        this.qcInspectionRepository = qcInspectionRepository;
        this.machineRepository = machineRepository;
        this.machineAssignmentRepository = machineAssignmentRepository;
        this.productionPlanRepository = productionPlanRepository;
    }

    private record StageContext(String lotCode, String poNumber, String contractNumber, String stageType) {
        String summary() {
            // Prioritize Lot Code as per user request
            return "Lô " + (lotCode != null ? lotCode : "N/A") + " | PO " + (poNumber != null ? poNumber : "N/A")
                    + " | Hợp đồng " + (contractNumber != null ? contractNumber : "N/A");
        }
    }

    private StageContext buildContext(ProductionStage stage) {
        String stageType = stage.getStageType();
        String poNumber = null;
        String lotCode = null;
        String contractNumber = null;
        ProductionOrder order = stage.getProductionOrder();
        if (order != null) {
            poNumber = order.getPoNumber();
            if (order.getContract() != null) {
                contractNumber = order.getContract().getContractNumber();
            }
            // Fetch Lot Code from Production Plan
            try {
                String note = order.getNotes();
                if (note != null && note.contains("Plan: ")) {
                    String planCode = note.substring(note.indexOf("Plan: ") + 6).trim();
                    ProductionPlan plan = productionPlanRepository.findByPlanCode(planCode).orElse(null);
                    if (plan != null && plan.getLot() != null) {
                        lotCode = plan.getLot().getLotCode();
                    }
                }
            } catch (Exception e) {
                // Ignore errors fetching lot code
            }
        }
        return new StageContext(lotCode, poNumber, contractNumber, stageType);
    }

    // Helper lấy tất cả stage thuộc orderId (dựa trên quan hệ WorkOrderDetail ->
    // ProductionOrderDetail -> ProductionOrder)
    private List<ProductionStage> findStagesByOrderId(Long orderId) {
        return stageRepo.findStagesByOrderId(orderId);
    }

    @Transactional
    public ProductionOrder startProductionOrder(Long orderId, Long pmUserId) {
        ProductionOrder order = orderRepo.findById(orderId).orElseThrow();
        if (order.getExecutionStatus() == null || order.getExecutionStatus().isBlank()) {
            order.setExecutionStatus("WAITING_PRODUCTION");
        }
        if (!"WAITING_PRODUCTION".equals(order.getExecutionStatus())) {
            return order; // không đúng trạng thái để start
        }
        order.setExecutionStatus("IN_PROGRESS");
        List<ProductionStage> stages = findStagesByOrderId(orderId);
        for (ProductionStage s : stages) {
            if (s.getStageSequence() != null && s.getStageSequence() == 1) {
                s.setExecutionStatus("READY");
                stageRepo.save(s);
            } else {
                if (s.getExecutionStatus() == null)
                    s.setExecutionStatus("PENDING");
            }
        }
        return orderRepo.save(order);
    }

    @Transactional
    public ProductionStage startStage(Long stageId, Long userId) {
        ProductionStage stage = stageRepo.findById(stageId).orElseThrow();
        ensureOrderStarted(stage);
        String currentStatus = stage.getExecutionStatus();
        // Nếu đã ở trạng thái IN_PROGRESS thì coi như đã bắt đầu - trả về stage mà
        // không báo lỗi
        if ("IN_PROGRESS".equals(currentStatus)) {
            return stage;
        }

        // Chấp nhận cả WAITING (chờ làm), READY (sẵn sàng) và WAITING_REWORK (chờ sửa)
        if (!"READY".equals(currentStatus)
                && !"WAITING".equals(currentStatus)
                && !"READY_TO_PRODUCE".equals(currentStatus)
                && !"WAITING_REWORK".equals(currentStatus)) {
            throw new RuntimeException(
                    "Công đoạn không ở trạng thái sẵn sàng bắt đầu. Trạng thái hiện tại: " + currentStatus);
        }

        // NEW: Blocking Logic (Single Lot per Stage)
        // Exception: Only outsourced DYEING stages are allowed to run in parallel
        boolean isParallelStage = "DYEING".equalsIgnoreCase(stage.getStageType()) ||
                "NHUOM".equalsIgnoreCase(stage.getStageType());

        if (!isParallelStage) {
            // 1. Check if any Rework Order is IN_PROGRESS at this stage
            boolean hasActiveRework = stageRepo
                    .findByStageTypeAndExecutionStatus(stage.getStageType(), "REWORK_IN_PROGRESS").stream()
                    .anyMatch(s -> !s.getId().equals(stage.getId())); // Exclude self if we are the rework

            // Also check standard IN_PROGRESS if it happens to be a rework order (using
            // isRework flag)
            if (!hasActiveRework) {
                hasActiveRework = stageRepo.findByStageTypeAndExecutionStatus(stage.getStageType(), "IN_PROGRESS")
                        .stream()
                        .anyMatch(s -> Boolean.TRUE.equals(s.getIsRework()) && !s.getId().equals(stage.getId()));
            }

            if (hasActiveRework) {
                // If WE are also a rework order, we might be allowed if we are the ONE active
                // rework?
                // But for now, assume strict serialization.
                // If we are a rework order, we should have used startRework?
                // Or if we are using startStage for rework, we should preempt others.
                if (Boolean.TRUE.equals(stage.getIsRework())) {
                    // We are rework, so we preempt others!
                    productionService.pauseOtherOrdersAtStage(stage.getStageType(), stage.getProductionOrder().getId());
                } else {
                    // We are normal order, so we are blocked.
                    throw new RuntimeException("BLOCKING: Hệ thống đang ưu tiên xử lý lệnh sửa lỗi. Vui lòng chờ.");
                }
            } else {
                // No rework active. Check for ANY active stage (Strict Serialization for Normal
                // Orders)
                long activeCount = stageRepo.countByStageTypeAndExecutionStatusIn(
                        stage.getStageType(),
                        java.util.List.of("IN_PROGRESS", "WAITING_REWORK", "REWORK_IN_PROGRESS"));

                if (activeCount > 0) {
                    // Check if the active stage is THIS stage (re-starting a paused stage)
                    java.util.List<ProductionStage> activeStages = stageRepo.findByExecutionStatusIn(
                            java.util.List.of("IN_PROGRESS", "WAITING_REWORK", "REWORK_IN_PROGRESS"));

                    for (ProductionStage s : activeStages) {
                        if (s.getStageType().equals(stage.getStageType()) && !s.getId().equals(stage.getId())) {
                            // If we are Rework, we preempt.
                            if (Boolean.TRUE.equals(stage.getIsRework())) {
                                productionService.pauseOtherOrdersAtStage(stage.getStageType(),
                                        stage.getProductionOrder().getId());
                                break; // Proceed
                            } else {
                                throw new RuntimeException("BLOCKING: Công đoạn " + stage.getStageType()
                                        + " đang được sử dụng bởi đơn hàng " + s.getProductionOrder().getPoNumber()
                                        + ". Vui lòng chờ hoàn thành.");
                            }
                        }
                    }
                }
            }
        }

        // Check machine availability
        // Exception: Parallel // Check machine availability (SKIP for Parallel Stages)
        if (!isParallelStage && stage.getStageType() != null) {
            java.util.List<tmmsystem.entity.Machine> availableMachines = machineRepository
                    .findByTypeAndStatus(stage.getStageType(), "AVAILABLE");
            if (availableMachines.isEmpty()) {
                throw new RuntimeException("Không có máy " + stage.getStageType()
                        + " nào sẵn sàng (AVAILABLE). Vui lòng kiểm tra lại trạng thái máy móc.");
            }
        }

        User operator = validateStageStartPermission(stage, userId);
        stage.setStartAt(Instant.now());
        stage.setExecutionStatus("IN_PROGRESS");
        productionService.syncStageStatus(stage, "IN_PROGRESS"); // NEW: Sync status

        // Update machine status to IN_USE and create MachineAssignment (SKIP for
        // Parallel Stages)
        if (!isParallelStage && stage.getStageType() != null) {
            // Update status
            machineRepository.updateStatusByType(stage.getStageType(), "IN_USE");

            // Create assignment for main type
            List<Machine> machines = machineRepository.findByTypeAndStatus(stage.getStageType(), "IN_USE");
            for (Machine m : machines) {
                // Check if assignment already exists (to avoid Duplicate Entry)
                MachineAssignment ma = machineAssignmentRepository.findByMachineAndProductionStage(m, stage)
                        .orElse(new MachineAssignment());

                ma.setMachine(m);
                ma.setProductionStage(stage);
                ma.setAssignedAt(Instant.now());
                ma.setReservationStatus("ACTIVE");
                ma.setReservationType("PRODUCTION");
                ma.setReleasedAt(null); // Clear released date if re-using
                machineAssignmentRepository.save(ma);
            }

            // Also update for alias if exists
            if (STAGE_TYPE_ALIASES.containsKey(stage.getStageType())) {
                String aliasType = STAGE_TYPE_ALIASES.get(stage.getStageType());
                machineRepository.updateStatusByType(aliasType, "IN_USE");

                // Create assignment for alias type
                List<Machine> aliasMachines = machineRepository.findByTypeAndStatus(aliasType, "IN_USE");
                for (Machine m : aliasMachines) {
                    // Check if assignment already exists
                    MachineAssignment ma = machineAssignmentRepository.findByMachineAndProductionStage(m, stage)
                            .orElse(new MachineAssignment());

                    ma.setMachine(m);
                    ma.setProductionStage(stage);
                    ma.setAssignedAt(Instant.now());
                    ma.setReservationStatus("ACTIVE");
                    ma.setReservationType("PRODUCTION");
                    ma.setReleasedAt(null);
                    machineAssignmentRepository.save(ma);
                }
            }
            // NOTE: Removed demotion of other READY_TO_PRODUCE stages
            // Other lots stay READY_TO_PRODUCE until Leader tries to start them
            // If Leader tries to start but this stage is IN_PROGRESS, they'll be blocked
        }

        if (stage.getProgressPercent() == null)
            stage.setProgressPercent(0);
        ProductionStage saved = stageRepo.save(stage);

        StageTracking tracking = new StageTracking();
        tracking.setProductionStage(saved);
        tracking.setOperator(operator);
        tracking.setAction("START");
        tracking.setQuantityCompleted(java.math.BigDecimal.valueOf(saved.getProgressPercent()));
        stageTrackingRepository.save(tracking);

        return saved;
    }

    private User validateStageStartPermission(ProductionStage stage, Long userId) {
        User u = userRepo.findById(userId).orElseThrow();
        boolean isPm = u.getRole() != null && "PRODUCTION_MANAGER".equalsIgnoreCase(u.getRole().getName());
        if ("DYEING".equalsIgnoreCase(stage.getStageType())) {
            if (!isPm && (stage.getAssignedLeader() == null || !stage.getAssignedLeader().getId().equals(userId))) {
                throw new RuntimeException("Không có quyền bắt đầu công đoạn nhuộm");
            }
        } else {
            if (stage.getAssignedLeader() == null || !stage.getAssignedLeader().getId().equals(userId)) {
                throw new RuntimeException("Không có quyền: không phải leader được phân công");
            }
        }
        return u;
    }

    @Transactional
    public ProductionStage updateProgress(Long stageId, Long userId, Integer percent) {
        ProductionStage stage = stageRepo.findById(stageId).orElseThrow();
        ensureOrderStarted(stage);
        if (!"IN_PROGRESS".equals(stage.getExecutionStatus())
                && !"REWORK_IN_PROGRESS".equals(stage.getExecutionStatus())) {
            throw new RuntimeException("Công đoạn không ở trạng thái đang làm");
        }
        if (percent == null || percent < 0 || percent > 100)
            throw new RuntimeException("% không hợp lệ");
        if (stage.getAssignedLeader() == null || !stage.getAssignedLeader().getId().equals(userId)) {
            throw new RuntimeException("Không có quyền cập nhật tiến độ");
        }
        stage.setProgressPercent(percent);
        if (percent == 100) {
            stage.setExecutionStatus("WAITING_QC");
            if (stage.getCompleteAt() == null) {
                stage.setCompleteAt(Instant.now());
            }
            StageContext ctx = buildContext(stage);
            if (stage.getQcAssignee() != null) {
                notificationService.notifyUser(stage.getQcAssignee(), "QC", "INFO", "Chờ kiểm tra",
                        "Công đoạn " + stage.getStageType() + " đã đạt 100%. " + ctx.summary(),
                        "PRODUCTION_STAGE", stage.getId());
            } else {
                notificationService.notifyRole("QC_STAFF", "QC", "INFO", "Chờ kiểm tra",
                        "Công đoạn " + stage.getStageType() + " đã đạt 100%. " + ctx.summary(), "PRODUCTION_STAGE",
                        stage.getId());
            }

            // Update machine status to AVAILABLE and release MachineAssignment (SKIP for
            // Parallel Stages - only DYEING is parallel)
            boolean isParallelStage = "DYEING".equalsIgnoreCase(stage.getStageType()) ||
                    "NHUOM".equalsIgnoreCase(stage.getStageType());

            if (!isParallelStage && stage.getStageType() != null) {
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
            }

            // NEW: Resume Paused Orders if Rework Completes
            if (Boolean.TRUE.equals(stage.getIsRework())) {
                productionService.resumePausedOrdersAtStage(stage.getStageType());
            }

            // Notify PM about available slot for serialized stages (non-outsource)
            if (!isParallelStage) {
                notificationService.notifyRole("PRODUCTION_MANAGER", "PRODUCTION", "INFO", "Trống công đoạn",
                        "Công đoạn " + stage.getStageType()
                                + " vừa giải phóng slot. Lô tiếp theo có thể bắt đầu. " + ctx.summary(),
                        "PRODUCTION_STAGE", stage.getId());
            }
        }
        ProductionStage saved = stageRepo.save(stage);

        User operator = userRepo.findById(userId).orElseThrow();
        StageTracking tracking = new StageTracking();
        tracking.setProductionStage(saved);
        tracking.setOperator(operator);
        tracking.setAction(percent == 100 ? "COMPLETE" : "UPDATE_PROGRESS");
        tracking.setQuantityCompleted(java.math.BigDecimal.valueOf(percent));

        // Fix: Determine isRework based on stage status or flag
        boolean isRework = Boolean.TRUE.equals(stage.getIsRework()) ||
                "REWORK_IN_PROGRESS".equals(stage.getExecutionStatus()) ||
                "WAITING_REWORK".equals(stage.getExecutionStatus());
        tracking.setIsRework(isRework);

        stageTrackingRepository.save(tracking);

        return saved;
    }

    @Transactional
    public QcSession startQcSession(Long stageId, Long qcUserId) {
        ProductionStage stage = stageRepo.findById(stageId).orElseThrow();
        ensureOrderStarted(stage);
        String execStatus = stage.getExecutionStatus();
        if (!"WAITING_QC".equals(execStatus) && !"QC_IN_PROGRESS".equals(execStatus))
            throw new RuntimeException("Không ở trạng thái chờ QC");
        User qc = userRepo.findById(qcUserId).orElseThrow();

        if (!"QC_IN_PROGRESS".equals(execStatus)) {
            productionService.syncStageStatus(stage, "QC_IN_PROGRESS");
            stageRepo.save(stage);
        }

        QcSession existing = sessionRepo.findFirstByProductionStageIdAndStatusOrderByIdDesc(stageId, "IN_PROGRESS")
                .orElse(null);
        if (existing != null)
            return existing;
        QcSession session = new QcSession();
        session.setProductionStage(stage);
        session.setStartedBy(qc);
        session.setStatus("IN_PROGRESS");
        return sessionRepo.save(session);
    }

    @Transactional(readOnly = true)
    public List<QcCheckpoint> getCheckpointsForStage(Long stageId) {
        ProductionStage stage = stageRepo.findById(stageId).orElseThrow();
        List<QcCheckpoint> checkpoints = new java.util.ArrayList<>();

        // 1. Get checkpoints for the exact stage type
        List<QcCheckpoint> primary = qcCheckpointRepository
                .findByStageTypeOrderByDisplayOrderAsc(stage.getStageType());
        if (primary != null) {
            checkpoints.addAll(primary);
        }

        // 2. Get checkpoints for the alias type (if any)
        if (stage.getStageType() != null && STAGE_TYPE_ALIASES.containsKey(stage.getStageType())) {
            String alias = STAGE_TYPE_ALIASES.get(stage.getStageType());
            List<QcCheckpoint> secondary = qcCheckpointRepository.findByStageTypeOrderByDisplayOrderAsc(alias);
            if (secondary != null) {
                checkpoints.addAll(secondary);
            }
        }

        return checkpoints;
    }

    @Transactional
    public QcSession submitQcSession(Long sessionId, String overallResult, String notes, Long qcUserId,
            String defectLevel, String defectDescription, List<QcInspectionDto> criteriaResults) {
        QcSession session = sessionRepo.findById(sessionId).orElseThrow();
        if (!"IN_PROGRESS".equals(session.getStatus()))
            throw new RuntimeException("Session không ở trạng thái IN_PROGRESS");
        if (!"PASS".equalsIgnoreCase(overallResult) && !"FAIL".equalsIgnoreCase(overallResult))
            throw new RuntimeException("Kết quả không hợp lệ");

        // Save detailed inspections
        ProductionStage stage = session.getProductionStage();
        User inspector = userRepo.findById(qcUserId).orElseThrow();

        if (criteriaResults != null) {
            for (QcInspectionDto dto : criteriaResults) {
                QcInspection inspection = new QcInspection();
                inspection.setProductionStage(stage);
                if (dto.getQcCheckpointId() != null) {
                    QcCheckpoint cp = qcCheckpointRepository.findById(dto.getQcCheckpointId()).orElse(null);
                    inspection.setQcCheckpoint(cp);
                }
                inspection.setInspector(inspector);
                inspection.setResult(dto.getResult());
                inspection.setNotes(dto.getNotes());
                inspection.setPhotoUrl(dto.getPhotoUrl());
                qcInspectionRepository.save(inspection);
            }
        }

        session.setOverallResult(overallResult.toUpperCase());
        session.setNotes(notes);
        session.setStatus("SUBMITTED");
        session.setSubmittedAt(Instant.now());
        sessionRepo.save(session);
        ProductionStage stageRef = session.getProductionStage(); // Re-use stage reference
        StageContext ctx = buildContext(stageRef);
        if ("PASS".equalsIgnoreCase(overallResult)) {
            stageRef.setExecutionStatus("QC_PASSED");
            stageRepo.save(stageRef);

            // Resolve ALL linked QualityIssues when QC PASS (regardless of rework flag)
            List<tmmsystem.entity.QualityIssue> linkedIssues = issueRepo.findByProductionStageId(stageRef.getId());
            for (tmmsystem.entity.QualityIssue issue : linkedIssues) {
                if (!"RESOLVED".equals(issue.getStatus())) {
                    issue.setStatus("RESOLVED");
                    issue.setResolvedAt(Instant.now());
                    issueRepo.save(issue);
                }
            }

            // Store isRework flag before clearing (for merge-back logic below)
            boolean wasRework = Boolean.TRUE.equals(stageRef.getIsRework());
            if (wasRework) {
                // Clear rework flag now that it's resolved
                stageRef.setIsRework(false);
                stageRepo.save(stageRef);
            }

            // NEW: Merge Back Logic for Supplementary Orders
            if (wasRework) {
                ProductionOrder currentPO = stageRef.getProductionOrder();
                if (currentPO.getPoNumber().contains("-REWORK")) {
                    // This is a supplementary order
                    // 1. Complete Supplementary Order
                    currentPO.setExecutionStatus("COMPLETED");
                    currentPO.setStatus("COMPLETED");
                    // currentPO.setActualEndDate(java.time.LocalDate.now()); // Field might not
                    // exist
                    orderRepo.save(currentPO);

                    // 2. Resume Original Order
                    if (stageRef.getOriginalStage() != null) {
                        ProductionStage originalStage = stageRef.getOriginalStage();
                        originalStage.setExecutionStatus("QC_PASSED");
                        stageRepo.save(originalStage);

                        // Trigger next stage of ORIGINAL order
                        openNextStage(originalStage);

                        // Notify
                        ProductionOrder originalPO = originalStage.getProductionOrder();
                        originalPO.setExecutionStatus("IN_PROGRESS"); // Ensure it's back to IN_PROGRESS if it was
                                                                      // waiting
                        orderRepo.save(originalPO);
                    }
                } else {
                    // Minor defect rework (same order) -> Just open next stage
                    openNextStage(stageRef);
                }
            } else {
                // Normal flow
                openNextStage(stageRef);
            }

            // Notify PASS
            notificationService.notifyRole("PRODUCTION_MANAGER", "PRODUCTION", "SUCCESS", "QC đạt",
                    "Công đoạn " + stageRef.getStageType() + " QC PASS. " + ctx.summary(), "PRODUCTION_STAGE",
                    stageRef.getId());
            if (stageRef.getAssignedLeader() != null) {
                notificationService.notifyUser(stageRef.getAssignedLeader(), "PRODUCTION", "SUCCESS", "QC đạt",
                        "Công đoạn " + stageRef.getStageType() + " QC PASS. " + ctx.summary(), "PRODUCTION_STAGE",
                        stageRef.getId());
            }
            if (inspector != null) {
                notificationService.notifyUser(inspector, "QC", "SUCCESS", "Đã ghi nhận QC PASS",
                        "Bạn đã QC PASS cho công đoạn " + stageRef.getStageType() + ". " + ctx.summary(),
                        "PRODUCTION_STAGE", stageRef.getId());
            }

        } else {
            // Check for mandatory photo BEFORE saving anything
            boolean photoFound = false;
            String firstPhotoUrl = null;
            if (criteriaResults != null) {
                for (QcInspectionDto dto : criteriaResults) {
                    if (dto.getPhotoUrl() != null && !dto.getPhotoUrl().isEmpty()) {
                        firstPhotoUrl = dto.getPhotoUrl();
                        photoFound = true;
                        break; // Use the first photo found
                    }
                }
            }

            // Enforce mandatory photo for FAIL result
            if (!photoFound) {
                throw new RuntimeException(
                        "QC_FAIL_NO_PHOTO: Bắt buộc phải có hình ảnh minh chứng khi đánh giá không đạt (FAIL).");
            }

            // Now safe to save stage and issue together
            stageRef.setExecutionStatus("QC_FAILED");
            stageRef.setDefectLevel(defectLevel); // Persist defect level
            stageRef.setDefectDescription(defectDescription); // Persist defect description
            stageRepo.save(stageRef);

            QualityIssue issue = new QualityIssue();
            issue.setProductionStage(stageRef);
            issue.setProductionOrder(resolveOrder(stageRef));
            issue.setSeverity(defectLevel != null ? defectLevel : "MINOR");
            issue.setIssueType("REWORK");
            issue.setDescription(defectDescription != null ? defectDescription : notes);
            issue.setEvidencePhoto(firstPhotoUrl);
            // FIX: Set status so Leader Defects can show the issue
            // MINOR → PROCESSED (Leader sees immediately)
            // MAJOR → PENDING (waits for Technical to process)
            issue.setStatus("MINOR".equals(defectLevel) ? "PROCESSED" : "PENDING");

            issueRepo.save(issue);

            // Smart notification based on severity
            if ("MINOR".equals(defectLevel)) {
                // Notify assigned leader for minor defects
                User leader = stageRef.getAssignedLeader();
                if (leader != null) {
                    notificationService.notifyUser(leader, "PRODUCTION", "WARNING", "Lỗi nhẹ cần xử lý",
                            "Công đoạn " + stageRef.getStageType()
                                    + " có lỗi nhẹ cần làm lại. Vui lòng kiểm tra và xử lý.",
                            "QUALITY_ISSUE", issue.getId());
                }
            } else {
                // Notify technical staff for major defects
                notificationService.notifyRole("TECHNICAL_STAFF", "PRODUCTION", "WARNING", "Lỗi QC",
                        "Công đoạn " + stageRef.getStageType() + " QC FAIL. " + ctx.summary(), "QUALITY_ISSUE",
                        issue.getId());
            }

            // Notify PM and current leader about FAIL
            notificationService.notifyRole("PRODUCTION_MANAGER", "PRODUCTION", "ERROR", "QC FAIL",
                    "Công đoạn " + stageRef.getStageType() + " QC FAIL. " + ctx.summary(), "QUALITY_ISSUE",
                    issue.getId());
            if (stageRef.getAssignedLeader() != null) {
                notificationService.notifyUser(stageRef.getAssignedLeader(), "PRODUCTION", "ERROR", "QC FAIL",
                        "Công đoạn " + stageRef.getStageType() + " QC FAIL. " + ctx.summary(), "QUALITY_ISSUE",
                        issue.getId());
            }
        }
        return session;
    }

    private ProductionOrder resolveOrder(ProductionStage stage) {
        // NEW: Lấy ProductionOrder trực tiếp (không qua WorkOrderDetail)
        return stage.getProductionOrder();
    }

    @Transactional
    public ProductionStage requestRework(Long issueId, Long techUserId) {
        QualityIssue issue = issueRepo.findById(issueId).orElseThrow();
        ProductionStage stage = issue.getProductionStage();
        User techUser = userRepo.findById(techUserId).orElseThrow();
        issue.setStatus("PROCESSED");
        issue.setProcessedAt(Instant.now());
        issue.setProcessedBy(techUser);
        issueRepo.save(issue);
        stage.setExecutionStatus("WAITING_REWORK");
        stage.setIsRework(true);
        stage.setProgressPercent(0);
        ProductionStage saved = stageRepo.save(stage);

        // NEW: Create initial tracking entry at 0% for rework history (similar to
        // production start)
        StageTracking tracking = new StageTracking();
        tracking.setProductionStage(saved);
        tracking.setOperator(stage.getAssignedLeader() != null ? stage.getAssignedLeader() : techUser);
        tracking.setAction("START");
        tracking.setQuantityCompleted(java.math.BigDecimal.ZERO);
        tracking.setIsRework(true); // Mark as rework entry
        tracking.setNotes("Bắt đầu làm lại lỗi");
        stageTrackingRepository.save(tracking);

        if (stage.getAssignedLeader() != null) {
            notificationService.notifyUser(stage.getAssignedLeader(), "PRODUCTION", "INFO", "Chờ sửa",
                    "Công đoạn " + stage.getStageType() + " cần sửa lại", "PRODUCTION_STAGE", stage.getId());
        }
        return saved;
    }

    @Transactional
    public ProductionStage startRework(Long stageId, Long leaderUserId) {
        return startRework(stageId, leaderUserId, true); // Default: force stop active stages
    }

    @Transactional
    public ProductionStage startRework(Long stageId, Long leaderUserId, boolean forceStopActive) {
        ProductionStage stage = stageRepo.findById(stageId).orElseThrow();
        if (!"WAITING_REWORK".equals(stage.getExecutionStatus())
                && !"WAITING".equals(stage.getExecutionStatus())
                && !"READY".equals(stage.getExecutionStatus())
                && !"READY_TO_PRODUCE".equals(stage.getExecutionStatus())
                && !"PENDING".equals(stage.getExecutionStatus())
                && !"QC_FAILED".equals(stage.getExecutionStatus())) // FIX: Allow rework from QC_FAILED status
            throw new RuntimeException(
                    "Không ở trạng thái chờ sửa hoặc chờ làm (Current: " + stage.getExecutionStatus() + ")");
        if (stage.getAssignedLeader() == null || !stage.getAssignedLeader().getId().equals(leaderUserId))
            throw new RuntimeException("Không có quyền sửa");

        // NEW: Pre-emption Logic (Auto-Pause other lots)
        // Exception: Only DYEING (outsourced) does not pre-empt
        boolean isParallelStage = "DYEING".equalsIgnoreCase(stage.getStageType()) ||
                "NHUOM".equalsIgnoreCase(stage.getStageType());

        // Only pause if forceStopActive is true
        if (!isParallelStage && forceStopActive) {
            productionService.pauseOtherOrdersAtStage(stage.getStageType(), stage.getProductionOrder().getId());
        }

        // FIX: Set startAt timestamp for rework
        stage.setStartAt(Instant.now());
        stage.setProgressPercent(0); // Reset progress for rework
        stage.setCompleteAt(null); // Clear previous complete time
        stage.setIsRework(true); // FIX: Mark stage as rework so resume logic works when complete
        stage.setExecutionStatus("REWORK_IN_PROGRESS");
        ProductionStage saved = stageRepo.save(stage);

        // FIX: Update QualityIssue status to IN_PROGRESS
        // This ensures the defect list shows correct status "Đang xử lý"
        List<QualityIssue> issues = issueRepo.findByProductionStageId(stageId);
        for (QualityIssue issue : issues) {
            if ("PROCESSED".equals(issue.getStatus()) || "PENDING".equals(issue.getStatus())) {
                issue.setStatus("IN_PROGRESS");
                issueRepo.save(issue);
            }
        }

        // FIX: Create StageTracking record for rework start
        User leader = userRepo.findById(leaderUserId).orElseThrow();
        StageTracking tracking = new StageTracking();
        tracking.setProductionStage(saved);
        tracking.setOperator(leader);
        tracking.setAction("START_REWORK");
        tracking.setQuantityCompleted(java.math.BigDecimal.ZERO);
        tracking.setIsRework(true);
        stageTrackingRepository.save(tracking);

        return saved;
    }

    /**
     * Kiểm tra có stage nào đang hoạt động cùng loại trước khi bắt đầu rework
     * Returns: { hasActiveStages, stageType, activeStages: [{stageId, orderId,
     * lotCode}] }
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> checkActiveStagesBeforeRework(Long stageId) {
        ProductionStage stage = stageRepo.findById(stageId).orElseThrow();
        java.util.Map<String, Object> result = new java.util.HashMap<>();

        // Tìm tất cả stages cùng loại đang IN_PROGRESS
        java.util.List<ProductionStage> activeStages = stageRepo
                .findByStageTypeAndExecutionStatus(stage.getStageType(), "IN_PROGRESS")
                .stream()
                .filter(s -> !s.getId().equals(stageId))
                .toList();

        result.put("hasActiveStages", !activeStages.isEmpty());
        result.put("stageType", stage.getStageType());
        result.put("stageTypeName", getVietnameseStageType(stage.getStageType()));
        result.put("activeStages", activeStages.stream().map(s -> {
            java.util.Map<String, Object> dto = new java.util.HashMap<>();
            dto.put("stageId", s.getId());
            dto.put("orderId", s.getProductionOrder() != null ? s.getProductionOrder().getId() : null);

            // Get lotCode
            String lotCode = null;
            if (s.getProductionOrder() != null) {
                ProductionOrder po = s.getProductionOrder();
                try {
                    String note = po.getNotes();
                    if (note != null && note.contains("Plan: ")) {
                        String planCode = note.substring(note.indexOf("Plan: ") + 6).trim();
                        ProductionPlan plan = productionPlanRepository.findByPlanCode(planCode).orElse(null);
                        if (plan != null && plan.getLot() != null) {
                            lotCode = plan.getLot().getLotCode();
                        }
                    }
                } catch (Exception e) {
                    // Ignore
                }
                if (lotCode == null) {
                    lotCode = po.getPoNumber();
                }
            }
            dto.put("lotCode", lotCode != null ? lotCode : "N/A");
            return dto;
        }).toList());

        return result;
    }

    private void openNextStage(ProductionStage current) {
        // NEW: Query trực tiếp theo ProductionOrder (không qua WorkOrderDetail)
        ProductionOrder po = current.getProductionOrder();
        if (po == null) {
            // Nếu stage chưa có ProductionOrder, không thể tìm next stage
            return;
        }

        List<ProductionStage> stages = stageRepo
                .findByProductionOrderIdOrderByStageSequenceAsc(po.getId());
        ProductionStage next = stages.stream()
                .filter(s -> s.getStageSequence() != null && current.getStageSequence() != null
                        && s.getStageSequence() == current.getStageSequence() + 1)
                .findFirst().orElse(null);
        if (next == null) {
            ProductionOrder order = resolveOrder(current);
            if (order != null) {
                order.setExecutionStatus("COMPLETED");
                orderRepo.save(order);
                notificationService.notifyRole("INVENTORY_STAFF", "PRODUCTION", "SUCCESS", "Hoàn thành",
                        "Đơn hàng " + order.getPoNumber() + " đã hoàn thành", "PRODUCTION_ORDER", order.getId());
            }
            return;
        }
        // Use ProductionService to sync stage status
        productionService.syncStageStatus(next, "WAITING");
        stageRepo.save(next);
        StageContext ctx = buildContext(next);
        if ("DYEING".equalsIgnoreCase(next.getStageType())) {
            notificationService.notifyRole("PRODUCTION_MANAGER", "PRODUCTION", "INFO", "Chuẩn bị nhuộm",
                    "Công đoạn Nhuộm đã sẵn sàng. " + ctx.summary(), "PRODUCTION_STAGE", next.getId());
        } else if (next.getAssignedLeader() != null) {
            notificationService.notifyUser(next.getAssignedLeader(), "PRODUCTION", "SUCCESS", "Sẵn sàng",
                    "Công đoạn " + next.getStageType() + " đã sẵn sàng. " + ctx.summary(), "PRODUCTION_STAGE",
                    next.getId());
        } else {
            notificationService.notifyRole("PRODUCTION_STAFF", "PRODUCTION", "INFO", "Công đoạn tiếp theo",
                    "Công đoạn " + next.getStageType() + " sẵn sàng. " + ctx.summary(), "PRODUCTION_STAGE",
                    next.getId());
        }
        // Notify PM as well for coordination
        notificationService.notifyRole("PRODUCTION_MANAGER", "PRODUCTION", "INFO",
                "Công đoạn " + next.getStageType() + " đã sẵn sàng",
                (ctx.poNumber() != null ? "PO " + ctx.poNumber() + " | " : "")
                        + (ctx.lotCode() != null ? "Lô " + ctx.lotCode() + " | " : "")
                        + (ctx.contractNumber() != null ? "Hợp đồng " + ctx.contractNumber() : ""),
                "PRODUCTION_STAGE", next.getId());
    }

    private void ensureOrderStarted(ProductionStage stage) {
        ProductionOrder order = stage.getProductionOrder();
        if (order == null) {
            throw new RuntimeException("Công đoạn chưa thuộc đơn sản xuất nào.");
        }
        String status = order.getExecutionStatus();
        if (status == null || status.isBlank()) {
            status = order.getStatus();
        }
        if ("WAITING_PRODUCTION".equalsIgnoreCase(status)
                || "PENDING_APPROVAL".equalsIgnoreCase(status)
                || "DRAFT".equalsIgnoreCase(status)) {
            throw new RuntimeException("Quản lý sản xuất chưa bắt đầu lệnh làm việc cho đơn hàng này.");
        }
    }

    @Transactional
    public MaterialRequisition createMaterialRequest(Long issueId, Long techUserId, String notes) {
        QualityIssue issue = issueRepo.findById(issueId).orElseThrow();
        ProductionStage stage = issue.getProductionStage();
        issue.setIssueType("MATERIAL_REQUEST");
        issue.setSeverity("MAJOR");
        issue.setMaterialNeeded(true);

        // Update issue status to PROCESSED as per Technical workflow
        issue.setStatus("PROCESSED");
        issue.setProcessedAt(Instant.now());
        issue.setProcessedBy(userRepo.findById(techUserId).orElseThrow());

        issueRepo.save(issue);

        MaterialRequisition req = new MaterialRequisition();
        req.setRequisitionNumber("MR-" + System.currentTimeMillis());
        req.setProductionStage(stage);
        req.setRequestedBy(userRepo.findById(techUserId).orElseThrow());
        req.setStatus("PENDING");
        req.setSourceIssue(issue);
        req.setRequisitionType("YARN_SUPPLY");
        req.setNotes(notes);
        materialReqRepo.save(req);

        ProductionOrder order = resolveOrder(stage);
        if (order != null) {
            order.setExecutionStatus("WAITING_MATERIAL_APPROVAL");
            orderRepo.save(order);
        }

        notificationService.notifyRole("PRODUCTION_MANAGER", "PRODUCTION", "WARNING", "Yêu cầu cấp sợi",
                "Có yêu cầu cấp sợi cho công đoạn " + stage.getStageType(), "MATERIAL_REQUISITION", req.getId());

        return req;
    }

    public MaterialRequisition approveMaterialRequest(Long requisitionId, Long pmUserId, boolean force) {
        // Delegate to ProductionService for full logic (Time validation, Supplementary
        // Order creation)
        // Note: Legacy call, auto-approves all details based on requested quantity
        productionService.approveMaterialRequest(requisitionId, pmUserId, force);

        return materialReqRepo.findById(requisitionId).orElseThrow();
    }

    @Transactional(readOnly = true)
    public java.util.List<ProductionStage> listStagesForLeader(Long leaderUserId) {
        return stageRepo.findByAssignedLeaderIdAndExecutionStatusIn(leaderUserId,
                java.util.List.of("READY", "IN_PROGRESS", "WAITING_QC", "REWORK_IN_PROGRESS", "WAITING_REWORK"));
    }

    @Transactional(readOnly = true)
    public java.util.List<ProductionStage> listStagesForQc(Long qcUserId) {
        return stageRepo.findByQcAssigneeIdAndExecutionStatusIn(qcUserId,
                java.util.List.of("WAITING_QC", "QC_IN_PROGRESS"));
    }

    @Transactional(readOnly = true)
    public java.util.List<ProductionStage> listStagesForTechnical() {
        // Lấy các stage FAILED hoặc WAITING_REWORK thông qua issue PENDING
        return issueRepo.findByStatus("PENDING").stream().map(QualityIssue::getProductionStage).distinct().toList();
    }

    @Transactional(readOnly = true)
    public ProductionStage findByQrToken(String token) {
        return stageRepo.findByQrToken(token).orElseThrow(() -> new RuntimeException("Invalid QR token"));
    }

    @Transactional
    public java.util.List<ProductionStage> listStagesForPm(Long orderId) {
        List<ProductionStage> stages = stageRepo.findStagesByOrderId(orderId);
        // Lazy generation of QR tokens for existing stages
        boolean updated = false;
        for (ProductionStage stage : stages) {
            if (stage.getQrToken() == null || stage.getQrToken().isEmpty()) {
                stage.setQrToken(java.util.UUID.randomUUID().toString());
                updated = true;
            }
        }
        if (updated) {
            stageRepo.saveAll(stages);
        }
        return stages;
    }

    @Transactional(readOnly = true)
    public java.util.List<MaterialRequisition> listMaterialRequisitions(String status) {
        if (status != null && !status.isEmpty()) {
            return materialReqRepo.findByStatus(status);
        }
        return materialReqRepo.findAll();
    }

    @Transactional(readOnly = true)
    public MaterialRequisition getMaterialRequisition(Long reqId) {
        return materialReqRepo.findById(reqId)
                .orElseThrow(() -> new RuntimeException("Material requisition not found: " + reqId));
    }

    @Transactional(readOnly = true)
    public java.util.List<QcInspection> getStageInspections(Long stageId) {
        ProductionStage stage = stageRepo.findById(stageId).orElseThrow();
        return qcInspectionRepository.findByProductionStageId(stage.getId());
    }

    /**
     * Check if a stage can be started (for frontend validation to show/hide start
     * button)
     * Returns: { canStart: true/false, blockedBy: orderNumber, blockedByStageType:
     * stageType, message: reason }
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> checkCanStartStage(Long stageId) {
        ProductionStage stage = stageRepo.findById(stageId).orElseThrow();
        java.util.Map<String, Object> result = new java.util.HashMap<>();

        // Default: can start
        result.put("canStart", true);
        result.put("stageType", stage.getStageType());

        // Check if stage is in a startable state
        String currentStatus = stage.getExecutionStatus();
        if (!"READY".equals(currentStatus) && !"WAITING".equals(currentStatus)
                && !"READY_TO_PRODUCE".equals(currentStatus) && !"WAITING_REWORK".equals(currentStatus)) {
            result.put("canStart", false);
            result.put("message", "Công đoạn không ở trạng thái sẵn sàng");
            return result;
        }

        // Check if this is a parallel stage (DYEING can run in parallel)
        boolean isParallelStage = "DYEING".equalsIgnoreCase(stage.getStageType()) ||
                "NHUOM".equalsIgnoreCase(stage.getStageType());

        if (isParallelStage) {
            // DYEING is always allowed
            return result;
        }

        // Check if any other stage of the same type is blocking
        // BUG FIX: Include WAITING_QC and QC_IN_PROGRESS because stage is still
        // "occupying" the machine until QC completes
        java.util.List<ProductionStage> activeStages = stageRepo.findByExecutionStatusIn(
                java.util.List.of("IN_PROGRESS", "REWORK_IN_PROGRESS", "WAITING_REWORK", "WAITING_QC",
                        "QC_IN_PROGRESS"));

        for (ProductionStage s : activeStages) {
            if (s.getId().equals(stageId))
                continue; // Skip self

            // Check same stage type (consider aliases)
            String sType = s.getStageType() != null ? s.getStageType().toUpperCase() : "";
            String thisType = stage.getStageType() != null ? stage.getStageType().toUpperCase() : "";

            boolean sameType = sType.equals(thisType);
            if (!sameType && STAGE_TYPE_ALIASES.containsKey(thisType)) {
                sameType = sType.equals(STAGE_TYPE_ALIASES.get(thisType));
            }
            if (!sameType && STAGE_TYPE_ALIASES.containsKey(sType)) {
                sameType = thisType.equals(STAGE_TYPE_ALIASES.get(sType));
            }

            if (sameType) {
                // Blocked by this stage - get lot code instead of PO number
                result.put("canStart", false);
                StageContext blockedContext = buildContext(s);
                String blockedByLot = blockedContext.lotCode() != null ? blockedContext.lotCode()
                        : (s.getProductionOrder() != null ? s.getProductionOrder().getPoNumber() : "N/A");
                result.put("blockedBy", blockedByLot);
                result.put("blockedByStageType", s.getStageType());
                result.put("blockedByStageId", s.getId());
                result.put("message", "Công đoạn " + getVietnameseStageType(stage.getStageType())
                        + " đang được sử dụng bởi lô " + blockedByLot);
                return result;
            }
        }

        return result;
    }

    private String getVietnameseStageType(String stageType) {
        if (stageType == null)
            return "";
        return switch (stageType.toUpperCase()) {
            case "WARPING", "CUONG_MAC" -> "Cuộn mắc";
            case "WEAVING", "DET" -> "Dệt";
            case "DYEING", "NHUOM" -> "Nhuộm";
            case "CUTTING", "CAT" -> "Cắt";
            case "HEMMING", "MAY" -> "May viền";
            case "PACKAGING", "DONG_GOI" -> "Đóng gói";
            default -> stageType;
        };
    }
}
