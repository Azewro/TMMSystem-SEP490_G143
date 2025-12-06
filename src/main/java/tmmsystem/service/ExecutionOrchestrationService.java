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
import tmmsystem.repository.MachineRepository;
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
            MachineAssignmentRepository machineAssignmentRepository) {
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
        // Exception: Outsourced stages (e.g. Dyeing) and Packaging do not block
        boolean isParallelStage = "DYEING".equalsIgnoreCase(stage.getStageType()) ||
                "NHUOM".equalsIgnoreCase(stage.getStageType()) ||
                "PACKAGING".equalsIgnoreCase(stage.getStageType()) ||
                "DONG_GOI".equalsIgnoreCase(stage.getStageType()) ||
                "CUTTING".equalsIgnoreCase(stage.getStageType()) ||
                "CAT".equalsIgnoreCase(stage.getStageType()) ||
                "HEMMING".equalsIgnoreCase(stage.getStageType()) ||
                "MAY".equalsIgnoreCase(stage.getStageType());

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
            if (stage.getQcAssignee() != null) {
                notificationService.notifyUser(stage.getQcAssignee(), "QC", "INFO", "Chờ kiểm tra",
                        "Công đoạn " + stage.getStageType() + " đã đạt 100%", "PRODUCTION_STAGE", stage.getId());
            } else {
                notificationService.notifyRole("QC_STAFF", "QC", "INFO", "Chờ kiểm tra",
                        "Công đoạn " + stage.getStageType() + " đã đạt 100%", "PRODUCTION_STAGE", stage.getId());
            }

            // Update machine status to AVAILABLE and release MachineAssignment (SKIP for
            // Parallel Stages)
            boolean isParallelStage = "DYEING".equalsIgnoreCase(stage.getStageType()) ||
                    "NHUOM".equalsIgnoreCase(stage.getStageType()) ||
                    "PACKAGING".equalsIgnoreCase(stage.getStageType()) ||
                    "DONG_GOI".equalsIgnoreCase(stage.getStageType()) ||
                    "CUTTING".equalsIgnoreCase(stage.getStageType()) ||
                    "CAT".equalsIgnoreCase(stage.getStageType()) ||
                    "HEMMING".equalsIgnoreCase(stage.getStageType()) ||
                    "MAY".equalsIgnoreCase(stage.getStageType());

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

        QcSession existing = sessionRepo.findByProductionStageIdAndStatus(stageId, "IN_PROGRESS").orElse(null);
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
        if ("PASS".equalsIgnoreCase(overallResult)) {
            stageRef.setExecutionStatus("QC_PASSED");
            stageRepo.save(stageRef);

            // NEW: Merge Back Logic for Supplementary Orders
            if (stageRef.getIsRework() != null && stageRef.getIsRework()) {
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

        } else {
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

            // Populate evidence photo from inspections if available
            boolean photoFound = false;
            if (criteriaResults != null) {
                for (QcInspectionDto dto : criteriaResults) {
                    if (dto.getPhotoUrl() != null && !dto.getPhotoUrl().isEmpty()) {
                        issue.setEvidencePhoto(dto.getPhotoUrl());
                        photoFound = true;
                        break; // Use the first photo found
                    }
                }
            }

            // NEW: Enforce mandatory photo for FAIL result
            if (!photoFound) {
                throw new RuntimeException(
                        "QC_FAIL_NO_PHOTO: Bắt buộc phải có hình ảnh minh chứng khi đánh giá không đạt (FAIL).");
            }

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
                        "Công đoạn " + stageRef.getStageType() + " QC FAIL", "QUALITY_ISSUE", issue.getId());
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
        issue.setStatus("PROCESSED");
        issue.setProcessedAt(Instant.now());
        issue.setProcessedBy(userRepo.findById(techUserId).orElseThrow());
        issueRepo.save(issue);
        stage.setExecutionStatus("WAITING_REWORK");
        stage.setIsRework(true);
        stage.setProgressPercent(0);
        stageRepo.save(stage);
        if (stage.getAssignedLeader() != null) {
            notificationService.notifyUser(stage.getAssignedLeader(), "PRODUCTION", "INFO", "Chờ sửa",
                    "Công đoạn " + stage.getStageType() + " cần sửa lại", "PRODUCTION_STAGE", stage.getId());
        }
        return stage;
    }

    @Transactional
    public ProductionStage startRework(Long stageId, Long leaderUserId) {
        ProductionStage stage = stageRepo.findById(stageId).orElseThrow();
        if (!"WAITING_REWORK".equals(stage.getExecutionStatus())
                && !"WAITING".equals(stage.getExecutionStatus())
                && !"READY".equals(stage.getExecutionStatus())
                && !"READY_TO_PRODUCE".equals(stage.getExecutionStatus())
                && !"PENDING".equals(stage.getExecutionStatus())) // Allow PENDING for Rework to jump queue
            throw new RuntimeException(
                    "Không ở trạng thái chờ sửa hoặc chờ làm (Current: " + stage.getExecutionStatus() + ")");
        if (stage.getAssignedLeader() == null || !stage.getAssignedLeader().getId().equals(leaderUserId))
            throw new RuntimeException("Không có quyền sửa");

        // NEW: Pre-emption Logic (Auto-Pause other lots)
        // Exception: Parallel stages do not pre-empt
        boolean isParallelStage = "DYEING".equalsIgnoreCase(stage.getStageType()) ||
                "NHUOM".equalsIgnoreCase(stage.getStageType()) ||
                "PACKAGING".equalsIgnoreCase(stage.getStageType()) ||
                "DONG_GOI".equalsIgnoreCase(stage.getStageType()) ||
                "CUTTING".equalsIgnoreCase(stage.getStageType()) ||
                "CAT".equalsIgnoreCase(stage.getStageType()) ||
                "HEMMING".equalsIgnoreCase(stage.getStageType()) ||
                "MAY".equalsIgnoreCase(stage.getStageType());

        if (!isParallelStage) {
            productionService.pauseOtherOrdersAtStage(stage.getStageType(), stage.getProductionOrder().getId());
        }

        stage.setExecutionStatus("REWORK_IN_PROGRESS");
        stageRepo.save(stage);
        return stage;
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
                        "Đơn hàng đã hoàn thành", "PRODUCTION_ORDER", order.getId());
            }
            return;
        }
        // Use ProductionService to sync stage status
        productionService.syncStageStatus(next, "WAITING");
        stageRepo.save(next);
        if ("DYEING".equalsIgnoreCase(next.getStageType())) {
            notificationService.notifyRole("PRODUCTION_MANAGER", "PRODUCTION", "INFO", "Chuẩn bị nhuộm",
                    "Công đoạn Nhuộm đã sẵn sàng", "PRODUCTION_STAGE", next.getId());
        } else if (next.getAssignedLeader() != null) {
            notificationService.notifyUser(next.getAssignedLeader(), "PRODUCTION", "SUCCESS", "Sẵn sàng",
                    "Công đoạn " + next.getStageType() + " đã sẵn sàng", "PRODUCTION_STAGE", next.getId());
        } else {
            notificationService.notifyRole("PRODUCTION_STAFF", "PRODUCTION", "INFO", "Công đoạn tiếp theo",
                    "Công đoạn " + next.getStageType() + " sẵn sàng", "PRODUCTION_STAGE", next.getId());
        }
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
}
