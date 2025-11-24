package tmmsystem.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import tmmsystem.entity.*;
import tmmsystem.service.ExecutionOrchestrationService;

@RestController
@RequestMapping("/v1/execution")
@Validated
public class ExecutionController {
    private final ExecutionOrchestrationService service;

    public ExecutionController(ExecutionOrchestrationService service) {
        this.service = service;
    }

    @PostMapping("/orders/{orderId}/start")
    public ProductionOrder startOrder(@PathVariable Long orderId, @RequestParam Long pmUserId) {
        return service.startProductionOrder(orderId, pmUserId);
    }

    @PostMapping("/stages/{stageId}/start")
    public ProductionStage startStage(@PathVariable Long stageId, @RequestParam Long userId) {
        return service.startStage(stageId, userId);
    }

    @PutMapping("/stages/{stageId}/progress")
    public ProductionStage updateProgress(@PathVariable Long stageId, @RequestParam Long userId,
            @RequestParam Integer percent) {
        return service.updateProgress(stageId, userId, percent);
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
            @RequestBody(required = false) java.util.List<tmmsystem.dto.qc.QcInspectionDto> criteriaResults) {
        return service.submitQcSession(sessionId, result, notes, qcUserId, criteriaResults);
    }

    @PostMapping("/issues/{issueId}/rework-request")
    public ProductionStage requestRework(@PathVariable Long issueId, @RequestParam Long techUserId) {
        return service.requestRework(issueId, techUserId);
    }

    @PostMapping("/stages/{stageId}/rework/start")
    public ProductionStage startRework(@PathVariable Long stageId, @RequestParam Long leaderUserId) {
        return service.startRework(stageId, leaderUserId);
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
    public java.util.List<ProductionStage> leaderStages(@RequestParam Long leaderUserId) {
        return service.listStagesForLeader(leaderUserId);
    }

    @GetMapping("/qc/stages")
    public java.util.List<ProductionStage> qcStages(@RequestParam Long qcUserId) {
        return service.listStagesForQc(qcUserId);
    }

    @GetMapping("/technical/stages")
    public java.util.List<ProductionStage> technicalStages() {
        return service.listStagesForTechnical();
    }

    @GetMapping("/pm/orders/{orderId}/stages")
    public java.util.List<ProductionStage> pmStages(@PathVariable Long orderId) {
        return service.listStagesForPm(orderId);
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
