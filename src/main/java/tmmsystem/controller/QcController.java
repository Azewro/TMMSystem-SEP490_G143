package tmmsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import tmmsystem.dto.qc.*;
import tmmsystem.entity.*;
import tmmsystem.mapper.QcMapper;
import tmmsystem.service.QcService;
import tmmsystem.service.ProductionService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/qc")
@Validated
public class QcController {
    private final QcService service;
    private final QcMapper mapper;
    private final ProductionService productionService;

    public QcController(QcService service, QcMapper mapper, ProductionService productionService) { this.service = service; this.mapper = mapper; this.productionService = productionService; }

    // Checkpoints
    @GetMapping("/checkpoints")
    public List<QcCheckpointDto> listCheckpoints(@RequestParam String stageType) {
        return service.listCheckpoints(stageType).stream().map(mapper::toDto).collect(Collectors.toList());
    }
    @GetMapping("/checkpoints/{id}")
    public QcCheckpointDto getCheckpoint(@PathVariable Long id) { return mapper.toDto(service.getCheckpoint(id)); }
    @Operation(summary = "Tạo QC Checkpoint")
    @PostMapping("/checkpoints")
    public QcCheckpointDto createCheckpoint(
            @RequestBody(description = "Payload tạo checkpoint", required = true,
                    content = @Content(schema = @Schema(implementation = QcCheckpointDto.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody QcCheckpointDto body) {
        QcCheckpoint e = new QcCheckpoint();
        e.setStageType(body.getStageType()); e.setCheckpointName(body.getCheckpointName());
        e.setInspectionCriteria(body.getInspectionCriteria()); e.setSamplingPlan(body.getSamplingPlan());
        e.setMandatory(body.getIsMandatory()); e.setDisplayOrder(body.getDisplayOrder());
        return mapper.toDto(service.createCheckpoint(e));
    }
    @PutMapping("/checkpoints/{id}")
    public QcCheckpointDto updateCheckpoint(@PathVariable Long id, @RequestBody QcCheckpointDto body) {
        QcCheckpoint e = new QcCheckpoint();
        e.setStageType(body.getStageType()); e.setCheckpointName(body.getCheckpointName());
        e.setInspectionCriteria(body.getInspectionCriteria()); e.setSamplingPlan(body.getSamplingPlan());
        e.setMandatory(body.getIsMandatory()); e.setDisplayOrder(body.getDisplayOrder());
        return mapper.toDto(service.updateCheckpoint(id, e));
    }
    @DeleteMapping("/checkpoints/{id}")
    public void deleteCheckpoint(@PathVariable Long id) { service.deleteCheckpoint(id); }

    // Inspections
    @GetMapping("/stages/{stageId}/inspections")
    public List<QcInspectionDto> listInspections(@PathVariable Long stageId) { return service.listInspectionsByStage(stageId).stream().map(mapper::toDto).collect(Collectors.toList()); }
    @GetMapping("/inspections/{id}")
    public QcInspectionDto getInspection(@PathVariable Long id) { return mapper.toDto(service.getInspection(id)); }
    @Operation(summary = "Tạo QC Inspection")
    @PostMapping("/inspections")
    public QcInspectionDto createInspection(
            @RequestBody(description = "Payload tạo inspection", required = true,
                    content = @Content(schema = @Schema(implementation = QcInspectionDto.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody QcInspectionDto body) {
        QcInspection e = new QcInspection();
        if (body.getProductionStageId() != null) { ProductionStage s = new ProductionStage(); s.setId(body.getProductionStageId()); e.setProductionStage(s); }
        if (body.getQcCheckpointId() != null) { QcCheckpoint cp = new QcCheckpoint(); cp.setId(body.getQcCheckpointId()); e.setQcCheckpoint(cp); }
        if (body.getInspectorId() != null) { User u = new User(); u.setId(body.getInspectorId()); e.setInspector(u); }
        e.setSampleSize(body.getSampleSize()); e.setPassCount(body.getPassCount()); e.setFailCount(body.getFailCount()); e.setResult(body.getResult()); e.setNotes(body.getNotes());
        QcInspection saved = service.createInspection(e);
        if ("PASS".equalsIgnoreCase(saved.getResult())) {
            productionService.markStageQcPass(saved.getProductionStage().getId());
        }
        return mapper.toDto(saved);
    }
    @PutMapping("/inspections/{id}")
    public QcInspectionDto updateInspection(@PathVariable Long id, @RequestBody QcInspectionDto body) {
        QcInspection e = new QcInspection();
        if (body.getProductionStageId() != null) { ProductionStage s = new ProductionStage(); s.setId(body.getProductionStageId()); e.setProductionStage(s); }
        if (body.getQcCheckpointId() != null) { QcCheckpoint cp = new QcCheckpoint(); cp.setId(body.getQcCheckpointId()); e.setQcCheckpoint(cp); }
        if (body.getInspectorId() != null) { User u = new User(); u.setId(body.getInspectorId()); e.setInspector(u); }
        e.setSampleSize(body.getSampleSize()); e.setPassCount(body.getPassCount()); e.setFailCount(body.getFailCount()); e.setResult(body.getResult()); e.setNotes(body.getNotes());
        return mapper.toDto(service.updateInspection(id, e));
    }
    @DeleteMapping("/inspections/{id}")
    public void deleteInspection(@PathVariable Long id) { service.deleteInspection(id); }

    // Defects
    @GetMapping("/inspections/{inspectionId}/defects")
    public List<QcDefectDto> listDefects(@PathVariable Long inspectionId) { return service.listDefects(inspectionId).stream().map(mapper::toDto).collect(Collectors.toList()); }
    @GetMapping("/defects/{id}")
    public QcDefectDto getDefect(@PathVariable Long id) { return mapper.toDto(service.getDefect(id)); }
    @Operation(summary = "Tạo QC Defect")
    @PostMapping("/defects")
    public QcDefectDto createDefect(
            @RequestBody(description = "Payload tạo defect", required = true,
                    content = @Content(schema = @Schema(implementation = QcDefectDto.class)))
            @org.springframework.web.bind.annotation.RequestBody QcDefectDto body) {
        QcDefect e = new QcDefect();
        if (body.getQcInspectionId() != null) { QcInspection ins = new QcInspection(); ins.setId(body.getQcInspectionId()); e.setQcInspection(ins); }
        e.setDefectType(body.getDefectType()); e.setDefectDescription(body.getDefectDescription()); e.setQuantityAffected(body.getQuantityAffected()); e.setSeverity(body.getSeverity()); e.setActionTaken(body.getActionTaken());
        return mapper.toDto(service.createDefect(e));
    }
    @PutMapping("/defects/{id}")
    public QcDefectDto updateDefect(@PathVariable Long id, @RequestBody QcDefectDto body) {
        QcDefect e = new QcDefect();
        if (body.getQcInspectionId() != null) { QcInspection ins = new QcInspection(); ins.setId(body.getQcInspectionId()); e.setQcInspection(ins); }
        e.setDefectType(body.getDefectType()); e.setDefectDescription(body.getDefectDescription()); e.setQuantityAffected(body.getQuantityAffected()); e.setSeverity(body.getSeverity()); e.setActionTaken(body.getActionTaken());
        return mapper.toDto(service.updateDefect(id, e));
    }
    @DeleteMapping("/defects/{id}")
    public void deleteDefect(@PathVariable Long id) { service.deleteDefect(id); }

    // Photos
    @GetMapping("/inspections/{inspectionId}/photos")
    public List<QcPhotoDto> listPhotos(@PathVariable Long inspectionId) { return service.listPhotos(inspectionId).stream().map(mapper::toDto).collect(Collectors.toList()); }
    @GetMapping("/photos/{id}")
    public QcPhotoDto getPhoto(@PathVariable Long id) { return mapper.toDto(service.getPhoto(id)); }
    @Operation(summary = "Tạo QC Photo")
    @PostMapping("/photos")
    public QcPhotoDto createPhoto(
            @RequestBody(description = "Payload tạo photo", required = true,
                    content = @Content(schema = @Schema(implementation = QcPhotoDto.class)))
            @org.springframework.web.bind.annotation.RequestBody QcPhotoDto body) {
        QcPhoto e = new QcPhoto();
        if (body.getQcInspectionId() != null) { QcInspection ins = new QcInspection(); ins.setId(body.getQcInspectionId()); e.setQcInspection(ins); }
        e.setPhotoUrl(body.getPhotoUrl()); e.setCaption(body.getCaption());
        return mapper.toDto(service.createPhoto(e));
    }
    @DeleteMapping("/photos/{id}")
    public void deletePhoto(@PathVariable Long id) { service.deletePhoto(id); }

    // Standards
    @GetMapping("/standards")
    public List<QcStandardDto> listStandards() { return service.listStandards().stream().map(mapper::toDto).collect(Collectors.toList()); }
    @GetMapping("/standards/{id}")
    public QcStandardDto getStandard(@PathVariable Long id) { return mapper.toDto(service.getStandard(id)); }
    @Operation(summary = "Tạo QC Standard")
    @PostMapping("/standards")
    public QcStandardDto createStandard(
            @RequestBody(description = "Payload tạo standard", required = true,
                    content = @Content(schema = @Schema(implementation = QcStandardDto.class)))
            @org.springframework.web.bind.annotation.RequestBody QcStandardDto body) {
        QcStandard e = new QcStandard();
        e.setStandardName(body.getStandardName()); e.setStandardCode(body.getStandardCode()); e.setDescription(body.getDescription()); e.setApplicableStages(body.getApplicableStages()); e.setActive(body.getIsActive());
        return mapper.toDto(service.createStandard(e));
    }
    @PutMapping("/standards/{id}")
    public QcStandardDto updateStandard(@PathVariable Long id, @RequestBody QcStandardDto body) {
        QcStandard e = new QcStandard();
        e.setStandardName(body.getStandardName()); e.setStandardCode(body.getStandardCode()); e.setDescription(body.getDescription()); e.setApplicableStages(body.getApplicableStages()); e.setActive(body.getIsActive());
        return mapper.toDto(service.updateStandard(id, e));
    }
    @DeleteMapping("/standards/{id}")
    public void deleteStandard(@PathVariable Long id) { service.deleteStandard(id); }
}
