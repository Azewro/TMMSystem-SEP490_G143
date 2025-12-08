package tmmsystem.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import tmmsystem.entity.*;
import tmmsystem.dto.production.ProductionStageDto;
import tmmsystem.dto.execution.StageTrackingDto;
import tmmsystem.mapper.ProductionMapper;
import tmmsystem.mapper.ExecutionMapper;
import tmmsystem.service.ExecutionOrchestrationService;
import tmmsystem.service.ExecutionService;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/execution")
@Validated
public class ExecutionController {
    private final ExecutionOrchestrationService orchestrationService;
    private final ProductionMapper productionMapper;
    private final ExecutionService executionService;
    private final ExecutionMapper executionMapper;

    public ExecutionController(ExecutionOrchestrationService service,
            ProductionMapper productionMapper,
            ExecutionService executionService,
            ExecutionMapper executionMapper) {
        this.orchestrationService = service;
        this.productionMapper = productionMapper;
        this.executionService = executionService;
        this.executionMapper = executionMapper;
    }

    @PostMapping("/orders/{orderId}/start")
    public ProductionOrder startOrder(@PathVariable Long orderId, @RequestParam Long pmUserId) {
        return orchestrationService.startProductionOrder(orderId, pmUserId);
    }

    @PostMapping("/stages/{stageId}/start")
    public ProductionStageDto startStage(@PathVariable Long stageId, @RequestParam Long userId) {
        ProductionStage stage = orchestrationService.startStage(stageId, userId);
        return productionMapper.toDto(stage);
    }

    // NEW: Check if a stage can be started (for frontend validation)
    @GetMapping("/stages/{stageId}/can-start")
    public java.util.Map<String, Object> canStartStage(@PathVariable Long stageId) {
        return orchestrationService.checkCanStartStage(stageId);
    }

    @PutMapping("/stages/{stageId}/progress")
    public ProductionStageDto updateProgress(@PathVariable Long stageId, @RequestParam Long userId,
            @RequestParam Integer percent) {
        ProductionStage stage = orchestrationService.updateProgress(stageId, userId, percent);
        return productionMapper.toDto(stage);
    }

    @PostMapping("/stages/{stageId}/qc/start")
    public java.util.Map<String, Object> startQc(@PathVariable Long stageId, @RequestParam Long qcUserId) {
        QcSession session = orchestrationService.startQcSession(stageId, qcUserId);
        return java.util.Map.of(
                "id", session.getId(),
                "stageId", stageId,
                "status", session.getStatus());
    }

    @GetMapping("/stages/{stageId}/checkpoints")
    public java.util.List<QcCheckpoint> getStageCheckpoints(@PathVariable Long stageId) {
        return orchestrationService.getCheckpointsForStage(stageId);
    }

    @GetMapping("/stages/{stageId}/inspections")
    public java.util.List<tmmsystem.dto.qc.QcInspectionDto> getStageInspections(@PathVariable Long stageId) {
        return orchestrationService.getStageInspections(stageId).stream()
                .map(i -> {
                    tmmsystem.dto.qc.QcInspectionDto dto = new tmmsystem.dto.qc.QcInspectionDto();
                    dto.setQcCheckpointId(i.getQcCheckpoint().getId());
                    dto.setResult(i.getResult());
                    dto.setNotes(i.getNotes());
                    dto.setPhotoUrl(i.getPhotoUrl());
                    if (i.getQcCheckpoint() != null) {
                        dto.setCheckpointName(i.getQcCheckpoint().getCheckpointName());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/qc-sessions/{sessionId}/submit")
    public java.util.Map<String, Object> submitQc(@PathVariable Long sessionId, @RequestParam String result,
            @RequestParam(required = false) String notes, @RequestParam Long qcUserId,
            @RequestParam(required = false) String defectLevel,
            @RequestParam(required = false) String defectDescription,
            @RequestBody(required = false) java.util.List<tmmsystem.dto.qc.QcInspectionDto> criteriaResults) {
        QcSession session = orchestrationService.submitQcSession(sessionId, result, notes, qcUserId, defectLevel,
                defectDescription, criteriaResults);
        return java.util.Map.of(
                "id", session.getId(),
                "status", session.getStatus(),
                "overallResult", session.getOverallResult(),
                "stageId", session.getProductionStage() != null ? session.getProductionStage().getId() : null);
    }

    @PostMapping("/issues/{issueId}/rework-request")
    public ProductionStageDto requestRework(@PathVariable Long issueId, @RequestParam Long techUserId) {
        ProductionStage stage = orchestrationService.requestRework(issueId, techUserId);
        return productionMapper.toDto(stage);
    }

    @PostMapping("/stages/{stageId}/rework/start")
    public ProductionStageDto startRework(@PathVariable Long stageId, @RequestParam Long leaderUserId) {
        ProductionStage stage = orchestrationService.startRework(stageId, leaderUserId);
        return productionMapper.toDto(stage);
    }

    @PostMapping("/issues/{issueId}/material-request")
    public tmmsystem.entity.MaterialRequisition createMaterialRequest(@PathVariable Long issueId,
            @RequestParam Long techUserId, @RequestParam(required = false) String notes) {
        return orchestrationService.createMaterialRequest(issueId, techUserId, notes);
    }

    @PostMapping("/material-requisitions/{reqId}/approve")
    public tmmsystem.entity.MaterialRequisition approveMaterialRequest(@PathVariable Long reqId,
            @RequestParam Long pmUserId, @RequestParam(defaultValue = "false") boolean force) {
        return orchestrationService.approveMaterialRequest(reqId, pmUserId, force);
    }

    @GetMapping("/leader/stages")
    public java.util.List<ProductionStageDto> leaderStages(@RequestParam Long leaderUserId) {
        return orchestrationService.listStagesForLeader(leaderUserId).stream()
                .map(productionMapper::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/qc/stages")
    public java.util.List<ProductionStageDto> qcStages(@RequestParam Long qcUserId) {
        return orchestrationService.listStagesForQc(qcUserId).stream()
                .map(productionMapper::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/technical/stages")
    public java.util.List<ProductionStageDto> technicalStages() {
        return orchestrationService.listStagesForTechnical().stream()
                .map(productionMapper::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/pm/orders/{orderId}/stages")
    public java.util.List<ProductionStageDto> pmStages(@PathVariable Long orderId) {
        return orchestrationService.listStagesForPm(orderId).stream()
                .map(productionMapper::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/stages/{stageId}/trackings")
    public java.util.List<StageTrackingDto> getStageTrackings(@PathVariable Long stageId) {
        return executionService.findTrackings(stageId).stream()
                .map(executionMapper::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/material-requisitions")
    public java.util.List<tmmsystem.dto.execution.MaterialRequisitionDto> listMaterialRequisitions(
            @RequestParam(required = false) String status) {
        return orchestrationService.listMaterialRequisitions(status).stream()
                .map(executionMapper::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/material-requisitions/{reqId}")
    public tmmsystem.entity.MaterialRequisition getMaterialRequisition(@PathVariable Long reqId) {
        return orchestrationService.getMaterialRequisition(reqId);
    }

    @GetMapping("/stages/by-token/{token}")
    public ProductionStageDto getStageByToken(@PathVariable String token) {
        ProductionStage stage = orchestrationService.findByQrToken(token);
        return productionMapper.toDto(stage);
    }
}
