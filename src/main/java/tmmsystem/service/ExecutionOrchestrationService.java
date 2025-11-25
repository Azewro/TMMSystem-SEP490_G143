package tmmsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.entity.MaterialRequisition;
import tmmsystem.entity.ProductionOrder;
import tmmsystem.entity.ProductionStage;
import tmmsystem.entity.QcSession;
import tmmsystem.entity.QualityIssue;
import tmmsystem.entity.User;
import tmmsystem.repository.MaterialRequisitionRepository;
import tmmsystem.repository.ProductionOrderRepository;
import tmmsystem.repository.ProductionStageRepository;
import tmmsystem.repository.QualityIssueRepository;
import tmmsystem.repository.QcSessionRepository;
import tmmsystem.repository.UserRepository;

import java.time.Instant;
import java.util.List;

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
    private final QcCheckpointRepository qcCheckpointRepository;
    private final QcInspectionRepository qcInspectionRepository;

    public ExecutionOrchestrationService(ProductionOrderRepository orderRepo,
            ProductionStageRepository stageRepo,
            QualityIssueRepository issueRepo,
            QcSessionRepository sessionRepo,
            UserRepository userRepo,
            NotificationService notificationService,
            MaterialRequisitionRepository materialReqRepo,
            ProductionService productionService,
            QcCheckpointRepository qcCheckpointRepository,
            QcInspectionRepository qcInspectionRepository) {
        this.orderRepo = orderRepo;
        this.stageRepo = stageRepo;
        this.issueRepo = issueRepo;
        this.sessionRepo = sessionRepo;
        this.userRepo = userRepo;
        this.notificationService = notificationService;
        this.materialReqRepo = materialReqRepo;
        this.productionService = productionService;
        this.qcCheckpointRepository = qcCheckpointRepository;
        this.qcInspectionRepository = qcInspectionRepository;
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
        String currentStatus = stage.getExecutionStatus();
        // Nếu đã ở trạng thái IN_PROGRESS thì coi như đã bắt đầu - trả về stage mà không báo lỗi
        if ("IN_PROGRESS".equals(currentStatus)) {
            return stage;
        }

        // Chấp nhận cả WAITING (chờ làm), READY (sẵn sàng) và WAITING_REWORK (chờ sửa)
        if (!"READY".equals(currentStatus) 
            && !"WAITING".equals(currentStatus)
            && !"WAITING_REWORK".equals(currentStatus)) {
            throw new RuntimeException("Công đoạn không ở trạng thái sẵn sàng bắt đầu. Trạng thái hiện tại: " + currentStatus);
        }
        validateStageStartPermission(stage, userId);
        stage.setStartAt(Instant.now());
        stage.setExecutionStatus("IN_PROGRESS");
        if (stage.getProgressPercent() == null)
            stage.setProgressPercent(0);
        return stageRepo.save(stage);
    }

    private void validateStageStartPermission(ProductionStage stage, Long userId) {
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
    }

    @Transactional
    public ProductionStage updateProgress(Long stageId, Long userId, Integer percent) {
        ProductionStage stage = stageRepo.findById(stageId).orElseThrow();
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
            if (stage.getQcAssignee() != null) {
                notificationService.notifyUser(stage.getQcAssignee(), "QC", "INFO", "Chờ kiểm tra",
                        "Công đoạn " + stage.getStageType() + " đã đạt 100%", "PRODUCTION_STAGE", stage.getId());
            } else {
                notificationService.notifyRole("QC_STAFF", "QC", "INFO", "Chờ kiểm tra",
                        "Công đoạn " + stage.getStageType() + " đã đạt 100%", "PRODUCTION_STAGE", stage.getId());
            }
        }
        return stageRepo.save(stage);
    }

    @Transactional
    public QcSession startQcSession(Long stageId, Long qcUserId) {
        ProductionStage stage = stageRepo.findById(stageId).orElseThrow();
        if (!"WAITING_QC".equals(stage.getExecutionStatus()))
            throw new RuntimeException("Không ở trạng thái chờ QC");
        User qc = userRepo.findById(qcUserId).orElseThrow();

        // Sử dụng ProductionService's syncStageStatus để đồng bộ cả hai trường
        productionService.syncStageStatus(stage, "QC_IN_PROGRESS");
        stageRepo.save(stage);

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
        return qcCheckpointRepository.findByStageTypeOrderByDisplayOrderAsc(stage.getStageType());
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
            openNextStage(stageRef);
        } else {
            stageRef.setExecutionStatus("QC_FAILED");
            stageRepo.save(stageRef);
            QualityIssue issue = new QualityIssue();
            issue.setProductionStage(stageRef);
            issue.setProductionOrder(resolveOrder(stageRef));
            issue.setSeverity(defectLevel != null ? defectLevel : "MINOR");
            issue.setIssueType("REWORK");
            issue.setDescription(defectDescription != null ? defectDescription : notes);
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
        if (!"WAITING_REWORK".equals(stage.getExecutionStatus()))
            throw new RuntimeException("Không ở trạng thái chờ sửa");
        if (stage.getAssignedLeader() == null || !stage.getAssignedLeader().getId().equals(leaderUserId))
            throw new RuntimeException("Không có quyền sửa");
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

    @Transactional
    public MaterialRequisition approveMaterialRequest(Long requisitionId, Long pmUserId) {
        MaterialRequisition req = materialReqRepo.findById(requisitionId).orElseThrow();
        if (!"PENDING".equals(req.getStatus()))
            return req;
        req.setStatus("APPROVED");
        req.setApprovedBy(userRepo.findById(pmUserId).orElseThrow());
        req.setApprovedAt(Instant.now());
        materialReqRepo.save(req);
        QualityIssue issue = req.getSourceIssue();
        if (issue != null) {
            issue.setStatus("PROCESSED");
            issue.setProcessedAt(Instant.now());
            issue.setProcessedBy(req.getApprovedBy());
            issueRepo.save(issue);
        }
        ProductionStage stage = req.getProductionStage();
        // Khi cấp sợi phê duyệt -> cho phép sửa lại các stage từ đầu tới stage bị lỗi
        // (đơn giản: set stage WAITING_REWORK)
        if (stage != null) {
            stage.setExecutionStatus("WAITING_REWORK");
            stage.setIsRework(true);
            stage.setProgressPercent(0);
            stageRepo.save(stage);
        }
        ProductionOrder order = resolveOrder(stage);
        if (order != null) {
            order.setExecutionStatus("IN_REWORK");
            orderRepo.save(order);
        }
        if (stage != null && stage.getAssignedLeader() != null) {
            notificationService.notifyUser(stage.getAssignedLeader(), "PRODUCTION", "INFO", "Chờ sửa (cấp sợi)",
                    "Công đoạn " + stage.getStageType() + " đã được cấp sợi, tiến hành sửa", "PRODUCTION_STAGE",
                    stage.getId());
        }
        return req;
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
    public java.util.List<ProductionStage> listStagesForPm(Long orderId) {
        return stageRepo.findStagesByOrderId(orderId);
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
}
