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
            stage.setStatus("QC_PASSED");
            stageRepo.save(stage);
            productionService.markStageQcPass(stageId);
        } else {
            stage.setStatus("QC_FAILED");
            stageRepo.save(stage);
            // Notify Technical
            notificationService.notifyRole("TECHNICAL_STAFF", "QC", "WARNING", "QC Failed",
                    "Công đoạn " + stage.getStageType() + " không đạt QC. Cần xử lý.", "PRODUCTION_STAGE",
                    stage.getId());
        }
    }
}
