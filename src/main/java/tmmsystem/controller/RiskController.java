package tmmsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import tmmsystem.entity.*;
import tmmsystem.repository.*;

@RestController
@RequestMapping("/v1/production")
@Validated
public class RiskController {
    private final StageRiskAssessmentRepository riskRepo;
    private final StageRiskAttachmentRepository attachRepo;
    private final ProductionStageRepository stageRepo;
    private final UserRepository userRepo;

    public RiskController(StageRiskAssessmentRepository riskRepo, StageRiskAttachmentRepository attachRepo, ProductionStageRepository stageRepo, UserRepository userRepo) {
        this.riskRepo = riskRepo; this.attachRepo = attachRepo; this.stageRepo = stageRepo; this.userRepo = userRepo;
    }

    @Operation(summary = "Create Stage Risk Assessment")
    @PostMapping("/stages/{stageId}/risk")
    public StageRiskAssessment create(@PathVariable Long stageId,
                                      @RequestParam String severity,
                                      @RequestParam(required = false) String description,
                                      @RequestParam(required = false) String rootCause,
                                      @RequestParam(required = false) String solutionProposal,
                                      @RequestParam(required = false) String notes) {
        ProductionStage stage = stageRepo.findById(stageId).orElseThrow();
        StageRiskAssessment r = new StageRiskAssessment();
        r.setProductionStage(stage); r.setSeverity(severity); r.setDescription(description); r.setRootCause(rootCause);
        r.setSolutionProposal(solutionProposal); r.setNotes(notes); r.setStatus("OPEN");
        return riskRepo.save(r);
    }

    @Operation(summary = "Approve/Reject Risk Assessment")
    @PutMapping("/risks/{riskId}/approve")
    public StageRiskAssessment approve(@PathVariable Long riskId, @RequestParam Long approverId) {
        StageRiskAssessment r = riskRepo.findById(riskId).orElseThrow();
        User approver = userRepo.findById(approverId).orElseThrow();
        r.setStatus("APPROVED"); r.setApprovedBy(approver); r.setApprovedAt(java.time.Instant.now());
        return riskRepo.save(r);
    }

    @PutMapping("/risks/{riskId}/reject")
    public StageRiskAssessment reject(@PathVariable Long riskId, @RequestParam Long approverId) {
        StageRiskAssessment r = riskRepo.findById(riskId).orElseThrow();
        User approver = userRepo.findById(approverId).orElseThrow();
        r.setStatus("REJECTED"); r.setApprovedBy(approver); r.setApprovedAt(java.time.Instant.now());
        return riskRepo.save(r);
    }

    @Operation(summary = "Set impact of Risk Assessment")
    @PutMapping("/risks/{riskId}/set-impact")
    public StageRiskAssessment setImpact(@PathVariable Long riskId,
                                         @RequestParam boolean impacted,
                                         @RequestParam(required = false) java.time.LocalDate proposedDate) {
        StageRiskAssessment r = riskRepo.findById(riskId).orElseThrow();
        r.setImpactedDelivery(impacted); r.setProposedNewDate(proposedDate);
        return riskRepo.save(r);
    }
}

