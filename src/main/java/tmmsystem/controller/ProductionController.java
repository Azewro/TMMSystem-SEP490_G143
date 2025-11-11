package tmmsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import tmmsystem.dto.production.*;
import tmmsystem.entity.*;
import tmmsystem.mapper.ProductionMapper;
import tmmsystem.service.ProductionService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/production")
@Validated
public class ProductionController {
    private final ProductionService service;
    private final ProductionMapper mapper;

    public ProductionController(ProductionService service, ProductionMapper mapper) { this.service = service; this.mapper = mapper; }

    // Production Orders
    @GetMapping("/orders")
    public List<ProductionOrderDto> listPO() { return service.findAllPO().stream().map(mapper::toDto).collect(Collectors.toList()); }
    @GetMapping("/orders/{id}")
    public ProductionOrderDto getPO(@PathVariable Long id) { return mapper.toDto(service.findPO(id)); }
    @Operation(summary = "Tạo Production Order")
    @PostMapping("/orders")
    public ProductionOrderDto createPO(
            @RequestBody(description = "Payload tạo PO", required = true,
                    content = @Content(schema = @Schema(implementation = ProductionOrderDto.class)))
            @org.springframework.web.bind.annotation.RequestBody ProductionOrderDto body) {
        ProductionOrder e = new ProductionOrder();
        if (body.getContractId() != null) { Contract c = new Contract(); c.setId(body.getContractId()); e.setContract(c); }
        e.setPoNumber(body.getPoNumber()); e.setTotalQuantity(body.getTotalQuantity());
        e.setPlannedStartDate(body.getPlannedStartDate()); e.setPlannedEndDate(body.getPlannedEndDate());
        e.setStatus(body.getStatus()); e.setPriority(body.getPriority()); e.setNotes(body.getNotes());
        return mapper.toDto(service.createPO(e));
    }
    @Operation(summary = "Cập nhật Production Order")
    @PutMapping("/orders/{id}")
    public ProductionOrderDto updatePO(@PathVariable Long id,
            @RequestBody(description = "Payload cập nhật PO", required = true,
                    content = @Content(schema = @Schema(implementation = ProductionOrderDto.class)))
            @org.springframework.web.bind.annotation.RequestBody ProductionOrderDto body) {
        ProductionOrder e = new ProductionOrder();
        if (body.getContractId() != null) { Contract c = new Contract(); c.setId(body.getContractId()); e.setContract(c); }
        e.setPoNumber(body.getPoNumber()); e.setTotalQuantity(body.getTotalQuantity());
        e.setPlannedStartDate(body.getPlannedStartDate()); e.setPlannedEndDate(body.getPlannedEndDate());
        e.setStatus(body.getStatus()); e.setPriority(body.getPriority()); e.setNotes(body.getNotes());
        return mapper.toDto(service.updatePO(id, e));
    }
    @DeleteMapping("/orders/{id}")
    public void deletePO(@PathVariable Long id) { service.deletePO(id); }

    // PO Details
    @GetMapping("/orders/{poId}/details")
    public List<ProductionOrderDetailDto> listPODetails(@PathVariable Long poId) { return service.findPODetails(poId).stream().map(mapper::toDto).collect(Collectors.toList()); }
    @GetMapping("/order-details/{id}")
    public ProductionOrderDetailDto getPODetail(@PathVariable Long id) { return mapper.toDto(service.findPODetail(id)); }
    @Operation(summary = "Thêm chi tiết PO")
    @PostMapping("/order-details")
    public ProductionOrderDetailDto createPODetail(
            @RequestBody(description = "Payload tạo chi tiết PO", required = true,
                    content = @Content(schema = @Schema(implementation = ProductionOrderDetailDto.class)))
            @org.springframework.web.bind.annotation.RequestBody ProductionOrderDetailDto body) {
        ProductionOrderDetail d = new ProductionOrderDetail();
        if (body.getProductionOrderId() != null) { ProductionOrder po = new ProductionOrder(); po.setId(body.getProductionOrderId()); d.setProductionOrder(po); }
        if (body.getProductId() != null) { Product p = new Product(); p.setId(body.getProductId()); d.setProduct(p); }
        if (body.getBomId() != null) { Bom b = new Bom(); b.setId(body.getBomId()); d.setBom(b); }
        d.setBomVersion(body.getBomVersion()); d.setQuantity(body.getQuantity()); d.setUnit(body.getUnit()); d.setNoteColor(body.getNoteColor());
        return mapper.toDto(service.createPODetail(d));
    }
    @Operation(summary = "Cập nhật chi tiết PO")
    @PutMapping("/order-details/{id}")
    public ProductionOrderDetailDto updatePODetail(@PathVariable Long id,
            @RequestBody(description = "Payload cập nhật chi tiết PO", required = true,
                    content = @Content(schema = @Schema(implementation = ProductionOrderDetailDto.class)))
            @org.springframework.web.bind.annotation.RequestBody ProductionOrderDetailDto body) {
        ProductionOrderDetail d = new ProductionOrderDetail();
        if (body.getProductionOrderId() != null) { ProductionOrder po = new ProductionOrder(); po.setId(body.getProductionOrderId()); d.setProductionOrder(po); }
        if (body.getProductId() != null) { Product p = new Product(); p.setId(body.getProductId()); d.setProduct(p); }
        if (body.getBomId() != null) { Bom b = new Bom(); b.setId(body.getBomId()); d.setBom(b); }
        d.setBomVersion(body.getBomVersion()); d.setQuantity(body.getQuantity()); d.setUnit(body.getUnit()); d.setNoteColor(body.getNoteColor());
        return mapper.toDto(service.updatePODetail(id, d));
    }
    @DeleteMapping("/order-details/{id}")
    public void deletePODetail(@PathVariable Long id) { service.deletePODetail(id); }

    // Technical Sheet
    @GetMapping("/tech-sheets/{id}")
    public TechnicalSheetDto getTechSheet(@PathVariable Long id) { return mapper.toDto(service.findTechSheet(id)); }
    @Operation(summary = "Tạo Technical Sheet")
    @PostMapping("/tech-sheets")
    public TechnicalSheetDto createTechSheet(
            @RequestBody(description = "Payload tạo Technical Sheet", required = true,
                    content = @Content(schema = @Schema(implementation = TechnicalSheetDto.class)))
            @org.springframework.web.bind.annotation.RequestBody TechnicalSheetDto body) {
        TechnicalSheet t = new TechnicalSheet();
        if (body.getProductionOrderId() != null) { ProductionOrder po = new ProductionOrder(); po.setId(body.getProductionOrderId()); t.setProductionOrder(po); }
        t.setSheetNumber(body.getSheetNumber()); t.setYarnSpecifications(body.getYarnSpecifications()); t.setMachineSettings(body.getMachineSettings());
        t.setQualityStandards(body.getQualityStandards()); t.setSpecialInstructions(body.getSpecialInstructions());
        return mapper.toDto(service.createTechSheet(t));
    }
    @Operation(summary = "Cập nhật Technical Sheet")
    @PutMapping("/tech-sheets/{id}")
    public TechnicalSheetDto updateTechSheet(@PathVariable Long id,
            @RequestBody(description = "Payload cập nhật Technical Sheet", required = true,
                    content = @Content(schema = @Schema(implementation = TechnicalSheetDto.class)))
            @org.springframework.web.bind.annotation.RequestBody TechnicalSheetDto body) {
        TechnicalSheet t = new TechnicalSheet();
        if (body.getProductionOrderId() != null) { ProductionOrder po = new ProductionOrder(); po.setId(body.getProductionOrderId()); t.setProductionOrder(po); }
        t.setSheetNumber(body.getSheetNumber()); t.setYarnSpecifications(body.getYarnSpecifications()); t.setMachineSettings(body.getMachineSettings());
        t.setQualityStandards(body.getQualityStandards()); t.setSpecialInstructions(body.getSpecialInstructions());
        return mapper.toDto(service.updateTechSheet(id, t));
    }
    @DeleteMapping("/tech-sheets/{id}")
    public void deleteTechSheet(@PathVariable Long id) { service.deleteTechSheet(id); }

    // Work Orders
    @GetMapping("/orders/{poId}/work-orders")
    public List<WorkOrderDto> listWOs(@PathVariable Long poId) { return service.findWOs(poId).stream().map(mapper::toDto).collect(Collectors.toList()); }
    @GetMapping("/work-orders/{id}")
    public WorkOrderDto getWO(@PathVariable Long id) { return mapper.toDto(service.findWO(id)); }
    @Operation(summary = "Tạo Work Order")
    @PostMapping("/work-orders")
    public WorkOrderDto createWO(
            @RequestBody(description = "Payload tạo WO", required = true,
                    content = @Content(schema = @Schema(implementation = WorkOrderDto.class)))
            @org.springframework.web.bind.annotation.RequestBody WorkOrderDto body) {
        WorkOrder w = new WorkOrder();
        if (body.getProductionOrderId() != null) { ProductionOrder po = new ProductionOrder(); po.setId(body.getProductionOrderId()); w.setProductionOrder(po); }
        w.setWoNumber(body.getWoNumber()); w.setDeadline(body.getDeadline()); w.setStatus(body.getStatus()); w.setSendStatus(body.getSendStatus()); w.setProduction(body.getIsProduction());
        return mapper.toDto(service.createWO(w));
    }
    @Operation(summary = "Cập nhật Work Order")
    @PutMapping("/work-orders/{id}")
    public WorkOrderDto updateWO(@PathVariable Long id,
            @RequestBody(description = "Payload cập nhật WO", required = true,
                    content = @Content(schema = @Schema(implementation = WorkOrderDto.class)))
            @org.springframework.web.bind.annotation.RequestBody WorkOrderDto body) {
        WorkOrder w = new WorkOrder();
        if (body.getProductionOrderId() != null) { ProductionOrder po = new ProductionOrder(); po.setId(body.getProductionOrderId()); w.setProductionOrder(po); }
        w.setWoNumber(body.getWoNumber()); w.setDeadline(body.getDeadline()); w.setStatus(body.getStatus()); w.setSendStatus(body.getSendStatus()); w.setProduction(body.getIsProduction());
        return mapper.toDto(service.updateWO(id, w));
    }
    @DeleteMapping("/work-orders/{id}")
    public void deleteWO(@PathVariable Long id) { service.deleteWO(id); }

    // Work Order Details
    @GetMapping("/work-orders/{woId}/details")
    public List<WorkOrderDetailDto> listWODetails(@PathVariable Long woId) { return service.findWODetails(woId).stream().map(mapper::toDto).collect(Collectors.toList()); }
    @GetMapping("/work-order-details/{id}")
    public WorkOrderDetailDto getWODetail(@PathVariable Long id) { return mapper.toDto(service.findWODetail(id)); }
    @Operation(summary = "Thêm chi tiết WO")
    @PostMapping("/work-order-details")
    public WorkOrderDetailDto createWODetail(
            @RequestBody(description = "Payload tạo chi tiết WO", required = true,
                    content = @Content(schema = @Schema(implementation = WorkOrderDetailDto.class)))
            @org.springframework.web.bind.annotation.RequestBody WorkOrderDetailDto body) {
        WorkOrderDetail d = new WorkOrderDetail();
        if (body.getWorkOrderId() != null) { WorkOrder w = new WorkOrder(); w.setId(body.getWorkOrderId()); d.setWorkOrder(w); }
        if (body.getProductionOrderDetailId() != null) { ProductionOrderDetail pod = new ProductionOrderDetail(); pod.setId(body.getProductionOrderDetailId()); d.setProductionOrderDetail(pod); }
        d.setStageSequence(body.getStageSequence()); d.setPlannedStartAt(body.getPlannedStartAt()); d.setPlannedEndAt(body.getPlannedEndAt());
        d.setStartAt(body.getStartAt()); d.setCompleteAt(body.getCompleteAt()); d.setWorkStatus(body.getWorkStatus()); d.setNotes(body.getNotes());
        return mapper.toDto(service.createWODetail(d));
    }
    @Operation(summary = "Cập nhật chi tiết WO")
    @PutMapping("/work-order-details/{id}")
    public WorkOrderDetailDto updateWODetail(@PathVariable Long id,
            @RequestBody(description = "Payload cập nhật chi tiết WO", required = true,
                    content = @Content(schema = @Schema(implementation = WorkOrderDetailDto.class)))
            @org.springframework.web.bind.annotation.RequestBody WorkOrderDetailDto body) {
        WorkOrderDetail d = new WorkOrderDetail();
        if (body.getWorkOrderId() != null) { WorkOrder w = new WorkOrder(); w.setId(body.getWorkOrderId()); d.setWorkOrder(w); }
        if (body.getProductionOrderDetailId() != null) { ProductionOrderDetail pod = new ProductionOrderDetail(); pod.setId(body.getProductionOrderDetailId()); d.setProductionOrderDetail(pod); }
        d.setStageSequence(body.getStageSequence()); d.setPlannedStartAt(body.getPlannedStartAt()); d.setPlannedEndAt(body.getPlannedEndAt());
        d.setStartAt(body.getStartAt()); d.setCompleteAt(body.getCompleteAt()); d.setWorkStatus(body.getWorkStatus()); d.setNotes(body.getNotes());
        return mapper.toDto(service.updateWODetail(id, d));
    }
    @DeleteMapping("/work-order-details/{id}")
    public void deleteWODetail(@PathVariable Long id) { service.deleteWODetail(id); }

    // Stages
    @GetMapping("/work-order-details/{woDetailId}/stages")
    public List<ProductionStageDto> listStages(@PathVariable Long woDetailId) { return service.findStages(woDetailId).stream().map(mapper::toDto).collect(Collectors.toList()); }
    @GetMapping("/stages/{id}")
    public ProductionStageDto getStage(@PathVariable Long id) { return mapper.toDto(service.findStage(id)); }
    @Operation(summary = "Tạo Production Stage")
    @PostMapping("/stages")
    public ProductionStageDto createStage(
            @RequestBody(description = "Payload tạo Stage", required = true,
                    content = @Content(schema = @Schema(implementation = ProductionStageDto.class)))
            @org.springframework.web.bind.annotation.RequestBody ProductionStageDto body) {
        ProductionStage s = new ProductionStage();
        if (body.getWorkOrderDetailId() != null) { WorkOrderDetail d = new WorkOrderDetail(); d.setId(body.getWorkOrderDetailId()); s.setWorkOrderDetail(d); }
        s.setStageType(body.getStageType()); s.setStageSequence(body.getStageSequence());
        if (body.getMachineId() != null) { Machine m = new Machine(); m.setId(body.getMachineId()); s.setMachine(m); }
        if (body.getAssignedToId() != null) { User u = new User(); u.setId(body.getAssignedToId()); s.setAssignedTo(u); }
        if (body.getAssignedLeaderId() != null) { User ul = new User(); ul.setId(body.getAssignedLeaderId()); s.setAssignedLeader(ul); }
        s.setBatchNumber(body.getBatchNumber()); s.setPlannedOutput(body.getPlannedOutput()); s.setActualOutput(body.getActualOutput());
        s.setStartAt(body.getStartAt()); s.setCompleteAt(body.getCompleteAt()); s.setStatus(body.getStatus());
        s.setOutsourced(body.getIsOutsourced()); s.setOutsourceVendor(body.getOutsourceVendor()); s.setNotes(body.getNotes());
        s.setPlannedStartAt(body.getPlannedStartAt()); s.setPlannedEndAt(body.getPlannedEndAt()); s.setPlannedDurationHours(body.getPlannedDurationHours());
        return mapper.toDto(service.createStage(s));
    }
    @Operation(summary = "Cập nhật Production Stage")
    @PutMapping("/stages/{id}")
    public ProductionStageDto updateStage(@PathVariable Long id,
            @RequestBody(description = "Payload cập nhật Stage", required = true,
                    content = @Content(schema = @Schema(implementation = ProductionStageDto.class)))
            @org.springframework.web.bind.annotation.RequestBody ProductionStageDto body) {
        ProductionStage s = new ProductionStage();
        if (body.getWorkOrderDetailId() != null) { WorkOrderDetail d = new WorkOrderDetail(); d.setId(body.getWorkOrderDetailId()); s.setWorkOrderDetail(d); }
        s.setStageType(body.getStageType()); s.setStageSequence(body.getStageSequence());
        if (body.getMachineId() != null) { Machine m = new Machine(); m.setId(body.getMachineId()); s.setMachine(m); }
        if (body.getAssignedToId() != null) { User u = new User(); u.setId(body.getAssignedToId()); s.setAssignedTo(u); }
        if (body.getAssignedLeaderId() != null) { User ul = new User(); ul.setId(body.getAssignedLeaderId()); s.setAssignedLeader(ul); }
        s.setBatchNumber(body.getBatchNumber()); s.setPlannedOutput(body.getPlannedOutput()); s.setActualOutput(body.getActualOutput());
        s.setStartAt(body.getStartAt()); s.setCompleteAt(body.getCompleteAt()); s.setStatus(body.getStatus());
        s.setOutsourced(body.getIsOutsourced()); s.setOutsourceVendor(body.getOutsourceVendor()); s.setNotes(body.getNotes());
        s.setPlannedStartAt(body.getPlannedStartAt()); s.setPlannedEndAt(body.getPlannedEndAt()); s.setPlannedDurationHours(body.getPlannedDurationHours());
        return mapper.toDto(service.updateStage(id, s));
    }
    @DeleteMapping("/stages/{id}")
    public void deleteStage(@PathVariable Long id) { service.deleteStage(id); }

    // ===== GIAI ĐOẠN 4: PRODUCTION ORDER CREATION & APPROVAL =====
    
    @Operation(summary = "Tạo lệnh sản xuất từ hợp đồng",
            description = "Planning Department tạo lệnh sản xuất từ hợp đồng đã duyệt")
    @PostMapping("/orders/create-from-contract")
    @SuppressWarnings("deprecation")
    public ProductionOrderDto createFromContract(
            @RequestParam Long contractId,
            @RequestParam Long planningUserId,
            @RequestParam String plannedStartDate,
            @RequestParam String plannedEndDate,
            @RequestParam(required = false) String notes) {
        // Parse dates with multiple format support
        java.time.LocalDate startDate;
        java.time.LocalDate endDate;
        
        try {
            // Try parsing as full date first (yyyy-MM-dd)
            startDate = java.time.LocalDate.parse(plannedStartDate);
            endDate = java.time.LocalDate.parse(plannedEndDate);
        } catch (java.time.format.DateTimeParseException e) {
            try {
                // Try parsing as dd-MM-yyyy format
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy");
                startDate = java.time.LocalDate.parse(plannedStartDate, formatter);
                endDate = java.time.LocalDate.parse(plannedEndDate, formatter);
            } catch (java.time.format.DateTimeParseException ex) {
                try {
                    // Try parsing as day only (dd) - assume current month/year
                    int day = Integer.parseInt(plannedStartDate);
                    int endDay = Integer.parseInt(plannedEndDate);
                    java.time.LocalDate now = java.time.LocalDate.now();
                    startDate = now.withDayOfMonth(day);
                    endDate = now.withDayOfMonth(endDay);
                } catch (NumberFormatException ex2) {
                    throw new IllegalArgumentException("Invalid date format. Supported formats: yyyy-MM-dd, dd-MM-yyyy, or day number (1-31)");
                }
            }
        }
        
        return mapper.toDto(service.createFromContract(
            contractId, 
            planningUserId, 
            startDate, 
            endDate, 
            notes));
    }
    
    @Operation(summary = "Lấy lệnh sản xuất chờ duyệt",
            description = "Lấy danh sách lệnh sản xuất đang chờ duyệt")
    @GetMapping("/orders/pending-approval")
    public List<ProductionOrderDto> getProductionOrdersPendingApproval() {
        return service.getProductionOrdersPendingApproval().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
    
    @Operation(summary = "Gửi lệnh sản xuất để duyệt",
            description = "Planning Department gửi lệnh sản xuất để Director duyệt")
    @PostMapping("/orders/{id}/submit-approval")
    public ProductionOrderDto submitForApproval(@PathVariable Long id) {
        // Update status to PENDING_APPROVAL if not already
        ProductionOrder po = service.findPO(id);
        if (!"PENDING_APPROVAL".equals(po.getStatus())) {
            po.setStatus("PENDING_APPROVAL");
            service.updatePO(id, po);
        }
        return mapper.toDto(po);
    }
    
    // Director APIs
    @Operation(summary = "Lấy lệnh sản xuất chờ duyệt (Director)",
            description = "Director lấy danh sách lệnh sản xuất chờ duyệt")
    @GetMapping("/orders/director/pending")
    public List<ProductionOrderDto> getDirectorPendingProductionOrders() {
        return service.getDirectorPendingProductionOrders().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
    
    @Operation(summary = "Duyệt lệnh sản xuất",
            description = "Director duyệt lệnh sản xuất")
    @PostMapping("/orders/{id}/approve")
    public ProductionOrderDto approveProductionOrder(
            @PathVariable Long id,
            @RequestParam Long directorId,
            @RequestParam(required = false) String notes) {
        return mapper.toDto(service.approveProductionOrder(id, directorId, notes));
    }
    
    @Operation(summary = "Từ chối lệnh sản xuất",
            description = "Director từ chối lệnh sản xuất")
    @PostMapping("/orders/{id}/reject")
    public ProductionOrderDto rejectProductionOrder(
            @PathVariable Long id,
            @RequestParam Long directorId,
            @RequestParam String rejectionNotes) {
        return mapper.toDto(service.rejectProductionOrder(id, directorId, rejectionNotes));
    }
    
    // ===== PRODUCTION PLAN INTEGRATION ENDPOINTS =====
    
    @Operation(summary = "Get production plans for contract",
            description = "Retrieve all production plans for a specific contract")
    @GetMapping("/contracts/{contractId}/plans")
    public List<tmmsystem.dto.production_plan.ProductionPlanDto> getProductionPlansForContract(@PathVariable Long contractId) {
        return service.getProductionPlansForContract(contractId);
    }
    
    @Operation(summary = "Create production order from approved plan",
            description = "Create production order from an approved production plan")
    @PostMapping("/plans/{planId}/create-order")
    public ProductionOrderDto createOrderFromApprovedPlan(@PathVariable Long planId) {
        return mapper.toDto(service.createFromApprovedPlan(planId));
    }
    
    @Operation(summary = "Get production plans pending approval",
            description = "Retrieve all production plans waiting for director approval")
    @GetMapping("/plans/pending-approval")
    public List<tmmsystem.dto.production_plan.ProductionPlanDto> getProductionPlansPendingApproval() {
        return service.getProductionPlansPendingApproval();
    }
}

