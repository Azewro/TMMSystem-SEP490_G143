package tmmsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.entity.ProductionStage;
import tmmsystem.repository.ProductionStageRepository;

@Service
public class TechnicalService {
    private final ProductionStageRepository stageRepo;
    private final NotificationService notificationService;

    public TechnicalService(ProductionStageRepository stageRepo,
            NotificationService notificationService) {
        this.stageRepo = stageRepo;
        this.notificationService = notificationService;
    }

    @Transactional
    public void handleDefect(Long stageId, String decision, String notes, Long technicalUserId) {
        ProductionStage stage = stageRepo.findById(stageId).orElseThrow();

        if ("REWORK".equalsIgnoreCase(decision)) {
            // Minor defect -> Rework
            // Reset stage to PENDING/WAITING_REWORK
            stage.setStatus("WAITING_REWORK");
            stage.setIsRework(true);
            stage.setQcLastResult(null);
            stage.setProgressPercent(0);
            stageRepo.save(stage);

            // Notify Leader
            if (stage.getAssignedLeader() != null) {
                notificationService.notifyUser(stage.getAssignedLeader(), "PRODUCTION", "WARNING", "Yêu cầu sửa lại",
                        "Công đoạn " + stage.getStageType() + " cần được sửa lại. " + (notes != null ? notes : ""),
                        "PRODUCTION_STAGE", stage.getId());
            }
        } else if ("MATERIAL_REQUEST".equalsIgnoreCase(decision)) {
            // Major defect -> Request Material -> Notify PM
            // Stage stays in QC_FAILED or moves to WAITING_MATERIAL_APPROVAL
            stage.setStatus("WAITING_MATERIAL_APPROVAL");
            stageRepo.save(stage);

            notificationService.notifyRole("PRODUCTION_MANAGER", "PRODUCTION", "WARNING", "Yêu cầu cấp sợi/vật tư",
                    "Công đoạn " + stage.getStageType() + " gặp lỗi nặng, cần cấp vật tư. "
                            + (notes != null ? notes : ""),
                    "PRODUCTION_STAGE", stage.getId());
        } else {
            // Other decisions (e.g. SCRAP, ACCEPT)
            stage.setStatus("COMPLETED"); // Assume accepted with deviation
            stage.setQcLastResult("PASS_WITH_DEVIATION");
            stageRepo.save(stage);
        }
    }
}
