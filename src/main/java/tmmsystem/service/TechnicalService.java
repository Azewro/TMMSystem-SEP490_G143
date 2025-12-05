package tmmsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.entity.ProductionStage;
import tmmsystem.repository.ProductionStageRepository;

@Service
public class TechnicalService {
    private final ProductionStageRepository stageRepo;
    private final NotificationService notificationService;
    private final tmmsystem.repository.MaterialRequisitionRepository reqRepo;
    private final tmmsystem.repository.MaterialRequisitionDetailRepository reqDetailRepo;
    private final tmmsystem.repository.MaterialRepository materialRepo;
    private final tmmsystem.repository.UserRepository userRepo;

    public TechnicalService(ProductionStageRepository stageRepo,
            NotificationService notificationService,
            tmmsystem.repository.MaterialRequisitionRepository reqRepo,
            tmmsystem.repository.MaterialRequisitionDetailRepository reqDetailRepo,
            tmmsystem.repository.MaterialRepository materialRepo,
            tmmsystem.repository.UserRepository userRepo) {
        this.stageRepo = stageRepo;
        this.notificationService = notificationService;
        this.reqRepo = reqRepo;
        this.reqDetailRepo = reqDetailRepo;
        this.materialRepo = materialRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public void handleDefect(Long stageId, String decision, String notes, Long technicalUserId,
            java.math.BigDecimal quantity,
            java.util.List<tmmsystem.dto.execution.MaterialRequisitionDetailDto> details) {
        ProductionStage stage = stageRepo.findById(stageId).orElseThrow();
        // ... (rest of the logic)

        if ("REWORK".equalsIgnoreCase(decision)) {
            // Minor defect -> Rework
            // Reset stage to PENDING/WAITING_REWORK
            stage.setExecutionStatus("WAITING_REWORK"); // FIX: Update execution status
            stage.setStatus("WAITING_REWORK");
            stage.setIsRework(true);
            stage.setQcLastResult(null);
            stage.setProgressPercent(0);

            // FIX: Save notes to defect description for Leader to see
            if (notes != null && !notes.isEmpty()) {
                stage.setDefectDescription(notes);
            }

            stageRepo.save(stage);

            // Notify Leader
            if (stage.getAssignedLeader() != null) {
                notificationService.notifyUser(stage.getAssignedLeader(), "PRODUCTION", "WARNING", "Yêu cầu sửa lại",
                        "Công đoạn " + stage.getStageType() + " cần được sửa lại. " + (notes != null ? notes : ""),
                        "PRODUCTION_STAGE", stage.getId());
            }
        } else if ("MATERIAL_REQUEST".equalsIgnoreCase(decision)) {
            // Major defect -> Request Material -> Notify PM
            tmmsystem.entity.MaterialRequisition req = new tmmsystem.entity.MaterialRequisition();
            req.setRequisitionNumber("REQ-" + stage.getId() + "-" + System.currentTimeMillis());
            req.setProductionStage(stage);
            req.setRequestedBy(userRepo.findById(technicalUserId).orElseThrow());
            req.setQuantityRequested(quantity);
            req.setNotes(notes);
            req.setStatus("PENDING");
            req.setRequisitionType("YARN_SUPPLY");

            tmmsystem.entity.MaterialRequisition savedReq = reqRepo.save(req);

            // Save Details
            if (details != null && !details.isEmpty()) {
                for (tmmsystem.dto.execution.MaterialRequisitionDetailDto dDto : details) {
                    if (dDto.getQuantityRequested() != null
                            && dDto.getQuantityRequested().compareTo(java.math.BigDecimal.ZERO) > 0) {
                        tmmsystem.entity.MaterialRequisitionDetail detail = new tmmsystem.entity.MaterialRequisitionDetail();
                        detail.setRequisition(savedReq);
                        if (dDto.getMaterialId() != null) {
                            detail.setMaterial(materialRepo.findById(dDto.getMaterialId()).orElse(null));
                        }
                        detail.setQuantityRequested(dDto.getQuantityRequested());
                        detail.setUnit(dDto.getUnit());
                        detail.setNotes(dDto.getNotes());
                        reqDetailRepo.save(detail);
                    }
                }
            }

            // Update Stage Status to block production
            stage.setExecutionStatus("WAITING_MATERIAL");
            stage.setStatus("WAITING_MATERIAL");
            stageRepo.save(stage);

            // Notify Production Manager
            notificationService.notifyRole("PRODUCTION_MANAGER", "PRODUCTION", "WARNING",
                    "Yêu cầu cấp sợi mới",
                    "Có yêu cầu cấp sợi từ công đoạn " + stage.getStageType() + " cần phê duyệt.",
                    "MATERIAL_REQUISITION", req.getId());
        }
    }
}
