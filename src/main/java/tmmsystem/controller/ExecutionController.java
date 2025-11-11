package tmmsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springframework.web.bind.annotation.*;
import tmmsystem.dto.execution.*;
import tmmsystem.entity.*;
import tmmsystem.mapper.ExecutionMapper;
import tmmsystem.service.ExecutionService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/execution")
public class ExecutionController {
    private final ExecutionService service;
    private final ExecutionMapper mapper;

    public ExecutionController(ExecutionService service, ExecutionMapper mapper) { this.service = service; this.mapper = mapper; }

    // Stage Tracking
    @GetMapping("/stages/{stageId}/trackings")
    public List<StageTrackingDto> listTrackings(@PathVariable Long stageId) { return service.findTrackings(stageId).stream().map(mapper::toDto).collect(Collectors.toList()); }
    @GetMapping("/trackings/{id}")
    public StageTrackingDto getTracking(@PathVariable Long id) { return mapper.toDto(service.findTracking(id)); }
    @Operation(summary = "Ghi nhận event Stage")
    @PostMapping("/trackings")
    public StageTrackingDto createTracking(
            @RequestBody(description = "Payload event Stage", required = true,
                    content = @Content(schema = @Schema(implementation = StageTrackingDto.class)))
            @org.springframework.web.bind.annotation.RequestBody StageTrackingDto body) {
        StageTracking e = new StageTracking();
        if (body.getProductionStageId() != null) { ProductionStage s = new ProductionStage(); s.setId(body.getProductionStageId()); e.setProductionStage(s); }
        if (body.getOperatorId() != null) { User u = new User(); u.setId(body.getOperatorId()); e.setOperator(u); }
        e.setAction(body.getAction()); e.setQuantityCompleted(body.getQuantityCompleted()); e.setNotes(body.getNotes());
        e.setEvidencePhotoUrl(body.getEvidencePhotoUrl());
        return mapper.toDto(service.createTracking(e));
    }
    @DeleteMapping("/trackings/{id}")
    public void deleteTracking(@PathVariable Long id) { service.deleteTracking(id); }

    // Pause Log
    @GetMapping("/stages/{stageId}/pauses")
    public List<StagePauseLogDto> listPauses(@PathVariable Long stageId) { return service.findPauses(stageId).stream().map(mapper::toDto).collect(Collectors.toList()); }
    @GetMapping("/pauses/{id}")
    public StagePauseLogDto getPause(@PathVariable Long id) { return mapper.toDto(service.findPause(id)); }
    @Operation(summary = "Tạo/cập nhật Pause Log")
    @PostMapping("/pauses")
    public StagePauseLogDto createPause(
            @RequestBody(description = "Payload tạo pause", required = true,
                    content = @Content(schema = @Schema(implementation = StagePauseLogDto.class)))
            @org.springframework.web.bind.annotation.RequestBody StagePauseLogDto body) {
        StagePauseLog e = new StagePauseLog();
        if (body.getProductionStageId() != null) { ProductionStage s = new ProductionStage(); s.setId(body.getProductionStageId()); e.setProductionStage(s); }
        if (body.getPausedById() != null) { User u = new User(); u.setId(body.getPausedById()); e.setPausedBy(u); }
        if (body.getResumedById() != null) { User u2 = new User(); u2.setId(body.getResumedById()); e.setResumedBy(u2); }
        e.setPauseReason(body.getPauseReason()); e.setPauseNotes(body.getPauseNotes()); e.setPausedAt(body.getPausedAt()); e.setResumedAt(body.getResumedAt()); e.setDurationMinutes(body.getDurationMinutes());
        return mapper.toDto(service.createPause(e));
    }
    @PutMapping("/pauses/{id}")
    public StagePauseLogDto updatePause(@PathVariable Long id, @RequestBody StagePauseLogDto body) {
        StagePauseLog e = new StagePauseLog();
        if (body.getProductionStageId() != null) { ProductionStage s = new ProductionStage(); s.setId(body.getProductionStageId()); e.setProductionStage(s); }
        if (body.getPausedById() != null) { User u = new User(); u.setId(body.getPausedById()); e.setPausedBy(u); }
        if (body.getResumedById() != null) { User u2 = new User(); u2.setId(body.getResumedById()); e.setResumedBy(u2); }
        e.setPauseReason(body.getPauseReason()); e.setPauseNotes(body.getPauseNotes()); e.setPausedAt(body.getPausedAt()); e.setResumedAt(body.getResumedAt()); e.setDurationMinutes(body.getDurationMinutes());
        return mapper.toDto(service.updatePause(id, e));
    }
    @DeleteMapping("/pauses/{id}")
    public void deletePause(@PathVariable Long id) { service.deletePause(id); }

    // Outsourcing
    @GetMapping("/stages/{stageId}/outsourcing")
    public List<OutsourcingTaskDto> listOutsourcing(@PathVariable Long stageId) { return service.findOutsourcing(stageId).stream().map(mapper::toDto).collect(Collectors.toList()); }
    @GetMapping("/outsourcing/{id}")
    public OutsourcingTaskDto getOutsourcing(@PathVariable Long id) { return mapper.toDto(service.findOutsourcingOne(id)); }
    @Operation(summary = "Tạo công việc gia công ngoài")
    @PostMapping("/outsourcing")
    public OutsourcingTaskDto createOutsourcing(
            @RequestBody(description = "Payload gia công ngoài", required = true,
                    content = @Content(schema = @Schema(implementation = OutsourcingTaskDto.class)))
            @org.springframework.web.bind.annotation.RequestBody OutsourcingTaskDto body) {
        OutsourcingTask e = new OutsourcingTask();
        if (body.getProductionStageId() != null) { ProductionStage s = new ProductionStage(); s.setId(body.getProductionStageId()); e.setProductionStage(s); }
        e.setVendorName(body.getVendorName()); e.setDeliveryNoteNumber(body.getDeliveryNoteNumber());
        e.setWeightSent(body.getWeightSent()); e.setWeightReturned(body.getWeightReturned()); e.setShrinkageRate(body.getShrinkageRate());
        e.setExpectedQuantity(body.getExpectedQuantity()); e.setReturnedQuantity(body.getReturnedQuantity()); e.setUnitCost(body.getUnitCost()); e.setTotalCost(body.getTotalCost());
        e.setSentAt(body.getSentAt()); e.setExpectedReturnDate(body.getExpectedReturnDate()); e.setActualReturnDate(body.getActualReturnDate());
        e.setStatus(body.getStatus()); e.setNotes(body.getNotes());
        return mapper.toDto(service.createOutsourcing(e));
    }
    @PutMapping("/outsourcing/{id}")
    public OutsourcingTaskDto updateOutsourcing(@PathVariable Long id, @RequestBody OutsourcingTaskDto body) {
        OutsourcingTask e = new OutsourcingTask();
        if (body.getProductionStageId() != null) { ProductionStage s = new ProductionStage(); s.setId(body.getProductionStageId()); e.setProductionStage(s); }
        e.setVendorName(body.getVendorName()); e.setDeliveryNoteNumber(body.getDeliveryNoteNumber());
        e.setWeightSent(body.getWeightSent()); e.setWeightReturned(body.getWeightReturned()); e.setShrinkageRate(body.getShrinkageRate());
        e.setExpectedQuantity(body.getExpectedQuantity()); e.setReturnedQuantity(body.getReturnedQuantity()); e.setUnitCost(body.getUnitCost()); e.setTotalCost(body.getTotalCost());
        e.setSentAt(body.getSentAt()); e.setExpectedReturnDate(body.getExpectedReturnDate()); e.setActualReturnDate(body.getActualReturnDate());
        e.setStatus(body.getStatus()); e.setNotes(body.getNotes());
        return mapper.toDto(service.updateOutsourcing(id, e));
    }
    @DeleteMapping("/outsourcing/{id}")
    public void deleteOutsourcing(@PathVariable Long id) { service.deleteOutsourcing(id); }

    // Loss
    @GetMapping("/orders/{poId}/losses")
    public List<ProductionLossDto> listLosses(@PathVariable Long poId) { return service.findLosses(poId).stream().map(mapper::toDto).collect(Collectors.toList()); }
    @GetMapping("/losses/{id}")
    public ProductionLossDto getLoss(@PathVariable Long id) { return mapper.toDto(service.findLoss(id)); }
    @Operation(summary = "Ghi nhận thất thoát")
    @PostMapping("/losses")
    public ProductionLossDto createLoss(
            @RequestBody(description = "Payload thất thoát", required = true,
                    content = @Content(schema = @Schema(implementation = ProductionLossDto.class)))
            @org.springframework.web.bind.annotation.RequestBody ProductionLossDto body) {
        ProductionLoss e = new ProductionLoss();
        if (body.getProductionOrderId() != null) { ProductionOrder po = new ProductionOrder(); po.setId(body.getProductionOrderId()); e.setProductionOrder(po); }
        if (body.getMaterialId() != null) { Material m = new Material(); m.setId(body.getMaterialId()); e.setMaterial(m); }
        if (body.getProductionStageId() != null) { ProductionStage s = new ProductionStage(); s.setId(body.getProductionStageId()); e.setProductionStage(s); }
        e.setQuantityLost(body.getQuantityLost()); e.setLossType(body.getLossType()); e.setNotes(body.getNotes());
        if (body.getRecordedById() != null) { User u = new User(); u.setId(body.getRecordedById()); e.setRecordedBy(u); }
        return mapper.toDto(service.createLoss(e));
    }
    @PutMapping("/losses/{id}")
    public ProductionLossDto updateLoss(@PathVariable Long id, @RequestBody ProductionLossDto body) {
        ProductionLoss e = new ProductionLoss();
        if (body.getProductionOrderId() != null) { ProductionOrder po = new ProductionOrder(); po.setId(body.getProductionOrderId()); e.setProductionOrder(po); }
        if (body.getMaterialId() != null) { Material m = new Material(); m.setId(body.getMaterialId()); e.setMaterial(m); }
        if (body.getProductionStageId() != null) { ProductionStage s = new ProductionStage(); s.setId(body.getProductionStageId()); e.setProductionStage(s); }
        e.setQuantityLost(body.getQuantityLost()); e.setLossType(body.getLossType()); e.setNotes(body.getNotes());
        if (body.getRecordedById() != null) { User u = new User(); u.setId(body.getRecordedById()); e.setRecordedBy(u); }
        return mapper.toDto(service.updateLoss(id, e));
    }
    @DeleteMapping("/losses/{id}")
    public void deleteLoss(@PathVariable Long id) { service.deleteLoss(id); }

    // Requisition
    @GetMapping("/requisitions/{id}")
    public MaterialRequisitionDto getReq(@PathVariable Long id) { return mapper.toDto(service.findReq(id)); }
    @Operation(summary = "Tạo phiếu lĩnh vật tư")
    @PostMapping("/requisitions")
    public MaterialRequisitionDto createReq(
            @RequestBody(description = "Payload tạo phiếu lĩnh", required = true,
                    content = @Content(schema = @Schema(implementation = MaterialRequisitionDto.class)))
            @org.springframework.web.bind.annotation.RequestBody MaterialRequisitionDto body) {
        MaterialRequisition e = new MaterialRequisition();
        if (body.getProductionStageId() != null) { ProductionStage s = new ProductionStage(); s.setId(body.getProductionStageId()); e.setProductionStage(s); }
        e.setRequisitionNumber(body.getRequisitionNumber());
        if (body.getRequestedById() != null) { User u = new User(); u.setId(body.getRequestedById()); e.setRequestedBy(u); }
        if (body.getApprovedById() != null) { User u2 = new User(); u2.setId(body.getApprovedById()); e.setApprovedBy(u2); }
        e.setStatus(body.getStatus()); e.setApprovedAt(body.getApprovedAt()); e.setIssuedAt(body.getIssuedAt()); e.setNotes(body.getNotes());
        return mapper.toDto(service.createReq(e));
    }
    @PutMapping("/requisitions/{id}")
    public MaterialRequisitionDto updateReq(@PathVariable Long id, @RequestBody MaterialRequisitionDto body) {
        MaterialRequisition e = new MaterialRequisition();
        if (body.getProductionStageId() != null) { ProductionStage s = new ProductionStage(); s.setId(body.getProductionStageId()); e.setProductionStage(s); }
        e.setRequisitionNumber(body.getRequisitionNumber());
        if (body.getRequestedById() != null) { User u = new User(); u.setId(body.getRequestedById()); e.setRequestedBy(u); }
        if (body.getApprovedById() != null) { User u2 = new User(); u2.setId(body.getApprovedById()); e.setApprovedBy(u2); }
        e.setStatus(body.getStatus()); e.setApprovedAt(body.getApprovedAt()); e.setIssuedAt(body.getIssuedAt()); e.setNotes(body.getNotes());
        return mapper.toDto(service.updateReq(id, e));
    }
    @DeleteMapping("/requisitions/{id}")
    public void deleteReq(@PathVariable Long id) { service.deleteReq(id); }

    // Requisition Details
    @GetMapping("/requisitions/{reqId}/details")
    public List<MaterialRequisitionDetailDto> listReqDetails(@PathVariable Long reqId) { return service.findReqDetails(reqId).stream().map(mapper::toDto).collect(Collectors.toList()); }
    @GetMapping("/requisition-details/{id}")
    public MaterialRequisitionDetailDto getReqDetail(@PathVariable Long id) { return mapper.toDto(service.findReqDetail(id)); }
    @Operation(summary = "Thêm dòng vật tư cho phiếu lĩnh")
    @PostMapping("/requisition-details")
    public MaterialRequisitionDetailDto createReqDetail(
            @RequestBody(description = "Payload thêm dòng vật tư", required = true,
                    content = @Content(schema = @Schema(implementation = MaterialRequisitionDetailDto.class)))
            @org.springframework.web.bind.annotation.RequestBody MaterialRequisitionDetailDto body) {
        MaterialRequisitionDetail e = new MaterialRequisitionDetail();
        if (body.getRequisitionId() != null) { MaterialRequisition r = new MaterialRequisition(); r.setId(body.getRequisitionId()); e.setRequisition(r); }
        if (body.getMaterialId() != null) { Material m = new Material(); m.setId(body.getMaterialId()); e.setMaterial(m); }
        e.setQuantityRequested(body.getQuantityRequested()); e.setQuantityApproved(body.getQuantityApproved()); e.setQuantityIssued(body.getQuantityIssued());
        e.setUnit(body.getUnit()); e.setNotes(body.getNotes());
        return mapper.toDto(service.createReqDetail(e));
    }
    @PutMapping("/requisition-details/{id}")
    public MaterialRequisitionDetailDto updateReqDetail(@PathVariable Long id, @RequestBody MaterialRequisitionDetailDto body) {
        MaterialRequisitionDetail e = new MaterialRequisitionDetail();
        if (body.getRequisitionId() != null) { MaterialRequisition r = new MaterialRequisition(); r.setId(body.getRequisitionId()); e.setRequisition(r); }
        if (body.getMaterialId() != null) { Material m = new Material(); m.setId(body.getMaterialId()); e.setMaterial(m); }
        e.setQuantityRequested(body.getQuantityRequested()); e.setQuantityApproved(body.getQuantityApproved()); e.setQuantityIssued(body.getQuantityIssued());
        e.setUnit(body.getUnit()); e.setNotes(body.getNotes());
        return mapper.toDto(service.updateReqDetail(id, e));
    }
    @DeleteMapping("/requisition-details/{id}")
    public void deleteReqDetail(@PathVariable Long id) { service.deleteReqDetail(id); }
}
