package tmmsystem.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import tmmsystem.entity.*;
import tmmsystem.dto.production.ProductionStageDto;
import tmmsystem.mapper.ProductionMapper;
import tmmsystem.service.ExecutionOrchestrationService;

@RestController
@RequestMapping("/v1/execution")
@Validated
public class ExecutionController {
    private final ExecutionOrchestrationService service;
    private final ProductionMapper mapper;

    public ExecutionController(ExecutionOrchestrationService service, ProductionMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping("/orders/{orderId}/start")
    public ProductionOrder startOrder(@PathVariable Long orderId, @RequestParam Long pmUserId) {
        return service.startProductionOrder(orderId, pmUserId);
    }

    @PostMapping("/stages/{stageId}/start")
    public ProductionStageDto startStage(@PathVariable Long stageId, @RequestParam Long userId) {
        ProductionStage stage = service.startStage(stageId, userId);
        return mapper.toDto(stage);
    }

    @PutMapping("/stages/{stageId}/progress")
    public ProductionStageDto updateProgress(@PathVariable Long stageId, @RequestParam Long userId,
            @RequestParam Integer percent) {
        ProductionStage stage = service.updateProgress(stageId, userId, percent);
        return mapper.toDto(stage);
    }

    @PostMapping("/stages/{stageId}/qc/start")
    public QcSession startQc(@PathVariable Long stageId, @RequestParam Long qcUserId) {
        return service.startQcSession(stageId, qcUserId);
    }

    @GetMapping("/stages/{stageId}/checkpoints")
    public java.util.List<QcCheckpoint> getStageCheckpoints(@PathVariable Long stageId) {
        return service.getCheckpointsForStage(stageId);
    }

    @PostMapping("/qc-sessions/{sessionId}/submit")
    public QcSession submitQc(@PathVariable Long sessionId, @RequestParam String result,
            @RequestParam(required = false) String notes, @RequestParam Long qcUserId,
            @RequestParam(required = false) String defectLevel,
            @RequestParam(required = false) String defectDescription,
            @RequestBody(required = false) java.util.List<tmmsystem.dto.qc.QcInspectionDto> criteriaResults) {
        return service.submitQcSession(sessionId, result, notes, qcUserId, defectLevel, defectDescription,
                criteriaResults);
    }

    @PostMapping("/issues/{issueId}/rework-request")
    public ProductionStageDto requestRework(@PathVariable Long issueId, @RequestParam Long techUserId) {
        ProductionStage stage = service.requestRework(issueId, techUserId);
        return mapper.toDto(stage);
    }

    @PostMapping("/stages/{stageId}/rework/start")
    public ProductionStageDto startRework(@PathVariable Long stageId, @RequestParam Long leaderUserId) {
        ProductionStage stage = service.startRework(stageId, leaderUserId);
        return mapper.toDto(stage);
    }

    @PostMapping("/issues/{issueId}/material-request")
    public tmmsystem.entity.MaterialRequisition createMaterialRequest(@PathVariable Long issueId,
            @RequestParam Long techUserId, @RequestParam(required = false) String notes) {
        return service.createMaterialRequest(issueId, techUserId, notes);
    }

    @PostMapping("/material-requisitions/{reqId}/approve")
    public tmmsystem.entity.MaterialRequisition approveMaterialRequest(@PathVariable Long reqId,
            @RequestParam Long pmUserId) {
        return service.approveMaterialRequest(reqId, pmUserId);
    }

    @GetMapping("/leader/stages")
    public java.util.List<ProductionStageDto> leaderStages(@RequestParam Long leaderUserId) {
        return service.listStagesForLeader(leaderUserId).stream().map(mapper::toDto).toList();
    }

    @GetMapping("/qc/stages")
    public java.util.List<ProductionStageDto> qcStages(@RequestParam Long qcUserId) {
        return service.listStagesForQc(qcUserId).stream().map(mapper::toDto).toList();
    }

    @GetMapping("/technical/stages")
    public java.util.List<ProductionStageDto> technicalStages() {
        return service.listStagesForTechnical().stream().map(mapper::toDto).toList();
    }

    @GetMapping("/pm/orders/{orderId}/stages")
    public java.util.List<ProductionStageDto> pmStages(@PathVariable Long orderId) {
        return service.listStagesForPm(orderId).stream().map(mapper::toDto).toList();
    }

    @GetMapping("/material-requisitions")
    public java.util.List<tmmsystem.entity.MaterialRequisition> listMaterialRequisitions(
            @RequestParam(required = false) String status) {
        return service.listMaterialRequisitions(status);
    }

    @GetMapping("/material-requisitions/{reqId}")
    public tmmsystem.entity.MaterialRequisition getMaterialRequisition(@PathVariable Long reqId) {
        return service.getMaterialRequisition(reqId);
    }
}
