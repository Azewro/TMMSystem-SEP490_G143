package tmmsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.entity.*;
import tmmsystem.repository.*;

import java.util.List;

@Service
public class QcService {
    private final QcCheckpointRepository checkpointRepo;
    private final QcInspectionRepository inspectionRepo;
    private final QcDefectRepository defectRepo;
    private final QcPhotoRepository photoRepo;
    private final QcStandardRepository standardRepo;
    private final tmmsystem.service.ProductionService productionService;
    private final tmmsystem.service.NotificationService notificationService;
    private final tmmsystem.repository.ProductionStageRepository stageRepo;

    public QcService(QcCheckpointRepository checkpointRepo,
            QcInspectionRepository inspectionRepo,
            QcDefectRepository defectRepo,
            QcPhotoRepository photoRepo,
            QcStandardRepository standardRepo,
            tmmsystem.service.ProductionService productionService,
            tmmsystem.service.NotificationService notificationService,
            tmmsystem.repository.ProductionStageRepository stageRepo) {
        this.checkpointRepo = checkpointRepo;
        this.inspectionRepo = inspectionRepo;
        this.defectRepo = defectRepo;
        this.photoRepo = photoRepo;
        this.standardRepo = standardRepo;
        this.productionService = productionService;
        this.notificationService = notificationService;
        this.stageRepo = stageRepo;
    }

    // Checkpoint
    public List<QcCheckpoint> listCheckpoints(String stageType) {
        return checkpointRepo.findByStageTypeOrderByDisplayOrderAsc(stageType);
    }

    public QcCheckpoint getCheckpoint(Long id) {
        return checkpointRepo.findById(id).orElseThrow();
    }

    @Transactional
    public QcCheckpoint createCheckpoint(QcCheckpoint e) {
        return checkpointRepo.save(e);
    }

    @Transactional
    public QcCheckpoint updateCheckpoint(Long id, QcCheckpoint upd) {
        QcCheckpoint e = checkpointRepo.findById(id).orElseThrow();
        e.setStageType(upd.getStageType());
        e.setCheckpointName(upd.getCheckpointName());
        e.setInspectionCriteria(upd.getInspectionCriteria());
        e.setSamplingPlan(upd.getSamplingPlan());
        e.setMandatory(upd.getMandatory());
        e.setDisplayOrder(upd.getDisplayOrder());
        return e;
    }

    public void deleteCheckpoint(Long id) {
        checkpointRepo.deleteById(id);
    }

    // Inspection
    public List<QcInspection> listInspectionsByStage(Long stageId) {
        return inspectionRepo.findByProductionStageId(stageId);
    }

    public QcInspection getInspection(Long id) {
        return inspectionRepo.findById(id).orElseThrow();
    }

    @Transactional
    public QcInspection createInspection(QcInspection e) {
        return inspectionRepo.save(e);
    }

    @Transactional
    public QcInspection updateInspection(Long id, QcInspection upd) {
        QcInspection e = inspectionRepo.findById(id).orElseThrow();
        e.setProductionStage(upd.getProductionStage());
        e.setQcCheckpoint(upd.getQcCheckpoint());
        e.setInspector(upd.getInspector());
        e.setSampleSize(upd.getSampleSize());
        e.setPassCount(upd.getPassCount());
        e.setFailCount(upd.getFailCount());
        e.setResult(upd.getResult());
        e.setNotes(upd.getNotes());
        return e;
    }

    public void deleteInspection(Long id) {
        inspectionRepo.deleteById(id);
    }

    // Defect
    public List<QcDefect> listDefects(Long inspectionId) {
        return defectRepo.findByQcInspectionId(inspectionId);
    }

    public QcDefect getDefect(Long id) {
        return defectRepo.findById(id).orElseThrow();
    }

    @Transactional
    public QcDefect createDefect(QcDefect e) {
        return defectRepo.save(e);
    }

    @Transactional
    public QcDefect updateDefect(Long id, QcDefect upd) {
        QcDefect e = defectRepo.findById(id).orElseThrow();
        e.setQcInspection(upd.getQcInspection());
        e.setDefectType(upd.getDefectType());
        e.setDefectDescription(upd.getDefectDescription());
        e.setQuantityAffected(upd.getQuantityAffected());
        e.setSeverity(upd.getSeverity());
        e.setActionTaken(upd.getActionTaken());
        return e;
    }

    public void deleteDefect(Long id) {
        defectRepo.deleteById(id);
    }

    // Photo
    public List<QcPhoto> listPhotos(Long inspectionId) {
        return photoRepo.findByQcInspectionId(inspectionId);
    }

    public QcPhoto getPhoto(Long id) {
        return photoRepo.findById(id).orElseThrow();
    }

    @Transactional
    public QcPhoto createPhoto(QcPhoto e) {
        return photoRepo.save(e);
    }

    public void deletePhoto(Long id) {
        photoRepo.deleteById(id);
    }

    // Standard
    public List<QcStandard> listStandards() {
        return standardRepo.findAll();
    }

    public QcStandard getStandard(Long id) {
        return standardRepo.findById(id).orElseThrow();
    }

    @Transactional
    public QcStandard createStandard(QcStandard e) {
        return standardRepo.save(e);
    }

    @Transactional
    public QcStandard updateStandard(Long id, QcStandard upd) {
        QcStandard e = standardRepo.findById(id).orElseThrow();
        e.setStandardName(upd.getStandardName());
        e.setStandardCode(upd.getStandardCode());
        e.setDescription(upd.getDescription());
        e.setApplicableStages(upd.getApplicableStages());
        e.setActive(upd.getActive());
        return e;
    }

    public void deleteStandard(Long id) {
        standardRepo.deleteById(id);
    }

    @Transactional
    public void submitInspectionResult(Long stageId, String result, String notes, Long inspectorId) {
        ProductionStage stage = stageRepo.findById(stageId).orElseThrow();
        stage.setQcLastResult(result);
        stage.setQcLastCheckedAt(java.time.Instant.now());

        if ("PASS".equalsIgnoreCase(result)) {
            // Sử dụng ProductionService's syncStageStatus để đồng bộ cả hai trường
            productionService.syncStageStatus(stage, "QC_PASSED");
            stageRepo.save(stage);
            productionService.markStageQcPass(stageId);
        } else {
            // Sử dụng ProductionService's syncStageStatus để đồng bộ cả hai trường
            productionService.syncStageStatus(stage, "QC_FAILED");
            stageRepo.save(stage);
            // Notify Technical
            notificationService.notifyRole("TECHNICAL_STAFF", "QC", "WARNING", "QC Failed",
                    "Công đoạn " + stage.getStageType() + " không đạt QC. Cần xử lý.", "PRODUCTION_STAGE",
                    stage.getId());
        }
    }

    /**
     * KCS: Gửi kết quả kiểm tra với các tiêu chí và ảnh lỗi
     * Nếu đạt: gửi thông báo đến Tổ Trưởng tiếp theo (hoặc PM cho công đoạn nhuộm)
     * Nếu không đạt: gửi thông báo đến kỹ thuật
     */
    @Transactional
    public QcInspection submitInspectionWithCriteria(Long stageId, Long inspectorId, String overallResult,
            String defectLevel, String defectDescription, java.util.List<tmmsystem.dto.qc.QcInspectionDto> criteriaResults) {
        ProductionStage stage = stageRepo.findById(stageId).orElseThrow();
        
        // Create inspection records for each criterion
        for (tmmsystem.dto.qc.QcInspectionDto criterionDto : criteriaResults) {
            QcInspection inspection = new QcInspection();
            inspection.setProductionStage(stage);
            if (criterionDto.getQcCheckpointId() != null) {
                QcCheckpoint cp = new QcCheckpoint();
                cp.setId(criterionDto.getQcCheckpointId());
                inspection.setQcCheckpoint(cp);
            }
            User inspector = new User();
            inspector.setId(inspectorId);
            inspection.setInspector(inspector);
            inspection.setResult(criterionDto.getResult());
            inspection.setNotes(criterionDto.getNotes());
            QcInspection savedInspection = inspectionRepo.save(inspection);
            
            // Create defect if FAIL
            if ("FAIL".equalsIgnoreCase(criterionDto.getResult())) {
                QcDefect defect = new QcDefect();
                defect.setQcInspection(savedInspection);
                defect.setDefectType(criterionDto.getNotes()); // Use notes as defect type
                defect.setDefectDescription(defectDescription);
                defect.setSeverity(defectLevel);
                defectRepo.save(defect);
            }
        }
        
        // Submit overall result - this will trigger notifications
        submitInspectionResult(stageId, overallResult, defectDescription, inspectorId);
        
        return inspectionRepo.findByProductionStageId(stageId).stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Inspection not created"));
    }

    /**
     * KCS: Bắt đầu kiểm tra
     * Chuyển trạng thái từ WAITING_QC sang QC_IN_PROGRESS
     */
    @Transactional
    public ProductionStage startInspection(Long stageId, Long inspectorId) {
        ProductionStage stage = stageRepo.findById(stageId).orElseThrow();
        
        if (!"WAITING_QC".equals(stage.getExecutionStatus())) {
            throw new RuntimeException("Stage không ở trạng thái chờ kiểm tra. Trạng thái hiện tại: " + stage.getExecutionStatus());
        }
        
        // Sử dụng ProductionService's syncStageStatus để đồng bộ cả hai trường
        productionService.syncStageStatus(stage, "QC_IN_PROGRESS");
        
        // Gán inspector nếu chưa có
        if (stage.getQcAssignee() == null) {
            User inspector = new User();
            inspector.setId(inspectorId);
            stage.setQcAssignee(inspector);
        }
        
        return stageRepo.save(stage);
    }

    /**
     * Lấy danh sách stages chờ kiểm tra cho KCS
     */
    public java.util.List<ProductionStage> getStagesWaitingInspection() {
        return stageRepo.findByExecutionStatus("WAITING_QC");
    }
}
