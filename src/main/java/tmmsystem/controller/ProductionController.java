package tmmsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import tmmsystem.dto.production.*;
import tmmsystem.entity.*;
import tmmsystem.mapper.ProductionMapper;
import tmmsystem.service.ProductionService;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/production")
@Validated
@Tag(name = "Production")
public class ProductionController {
    private final ProductionService service;
    private final tmmsystem.service.ExecutionOrchestrationService executionService;
    private final ProductionMapper mapper;

    public ProductionController(ProductionService service,
            tmmsystem.service.ExecutionOrchestrationService executionService, ProductionMapper mapper) {
        this.service = service;
        this.executionService = executionService;
        this.mapper = mapper;
    }

    // Production Orders
    @GetMapping("/orders")
    public List<ProductionOrderDto> listPO() {
        return service.findAllPO().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @GetMapping("/orders/{id}")
    public ProductionOrderDto getPO(@PathVariable Long id) {
        ProductionOrder po = service.findPO(id);
        return service.enrichProductionOrderDto(po);
    }

    @GetMapping("/orders/by-quotation/{quotationId}")
    public List<ProductionOrderDto> getPOByQuotation(@PathVariable Long quotationId) {
        return service.findPOByQuotationId(quotationId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Operation(summary = "PM: Lấy danh sách đơn hàng")
    @GetMapping("/manager/orders")
    public List<ProductionOrderDto> getManagerOrders() {
        return service.findAllPO().stream()
                .map(po -> service.enrichProductionOrderDto(po))
                .collect(Collectors.toList());
    }

    // Leader Defect APIs
    @GetMapping("/leader/defects")
    public List<tmmsystem.dto.qc.QualityIssueDto> getLeaderDefects(@RequestParam Long leaderUserId) {
        return service.getLeaderDefects(leaderUserId);
    }

    @GetMapping("/tech/defects")
    public List<tmmsystem.dto.qc.QualityIssueDto> getTechnicalDefects() {
        return service.getTechnicalDefects();
    }

    @GetMapping("/defects/{id}")
    public tmmsystem.dto.qc.QualityIssueDto getDefectDetail(@PathVariable Long id) {
        return service.getDefectDetail(id);
    }

    @PostMapping("/defects/{id}/start-rework")
    public ResponseEntity<?> startReworkFromDefect(@PathVariable Long id, @RequestParam Long userId) {
        service.startReworkFromDefect(id, userId);
        return ResponseEntity.ok(Map.of("message", "Đã bắt đầu làm lại công đoạn"));
    }

    @Operation(summary = "PM: Bắt đầu lệnh làm việc (gửi thông báo đến Leaders và QC)")
    @PostMapping("/orders/{orderId}/start-work-order")
    public List<ProductionStageDto> startWorkOrder(@PathVariable Long orderId) {
        List<ProductionStage> stages = service.startWorkOrder(orderId);
        return stages.stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Operation(summary = "Leader: Bắt đầu công đoạn (Rolling Production)")
    @PostMapping("/stages/{id}/start-rolling")
    public ProductionStageDto startStage(@PathVariable Long id, @RequestParam Long userId) {
        return mapper.toDto(service.startStage(id, userId));
    }

    @Operation(summary = "Tạo Production Order")
    @PostMapping("/orders")
    public ProductionOrderDto createPO(
            @RequestBody(description = "Payload tạo PO", required = true, content = @Content(schema = @Schema(implementation = ProductionOrderDto.class))) @org.springframework.web.bind.annotation.RequestBody ProductionOrderDto body) {
        ProductionOrder e = new ProductionOrder();
        if (body.getContractId() != null) {
            Contract c = new Contract();
            c.setId(body.getContractId());
            e.setContract(c);
        }
        e.setPoNumber(body.getPoNumber());
        e.setTotalQuantity(body.getTotalQuantity());
        e.setPlannedStartDate(body.getPlannedStartDate());
        e.setPlannedEndDate(body.getPlannedEndDate());
        e.setStatus(body.getStatus());
        e.setPriority(body.getPriority());
        e.setNotes(body.getNotes());
        return mapper.toDto(service.createPO(e));
    }

    @Operation(summary = "Cập nhật Production Order")
    @PutMapping("/orders/{id}")
    public ProductionOrderDto updatePO(@PathVariable Long id,
            @RequestBody(description = "Payload cập nhật PO", required = true, content = @Content(schema = @Schema(implementation = ProductionOrderDto.class))) @org.springframework.web.bind.annotation.RequestBody ProductionOrderDto body) {
        ProductionOrder e = new ProductionOrder();
        if (body.getContractId() != null) {
            Contract c = new Contract();
            c.setId(body.getContractId());
            e.setContract(c);
        }
        e.setPoNumber(body.getPoNumber());
        e.setTotalQuantity(body.getTotalQuantity());
        e.setPlannedStartDate(body.getPlannedStartDate());
        e.setPlannedEndDate(body.getPlannedEndDate());
        e.setStatus(body.getStatus());
        e.setPriority(body.getPriority());
        e.setNotes(body.getNotes());
        return mapper.toDto(service.updatePO(id, e));
    }

    @DeleteMapping("/orders/{id}")
    public void deletePO(@PathVariable Long id) {
        service.deletePO(id);
    }

    // PO Details
    @GetMapping("/orders/{poId}/details")
    public List<ProductionOrderDetailDto> listPODetails(@PathVariable Long poId) {
        return service.findPODetails(poId).stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @GetMapping("/order-details/{id}")
    public ProductionOrderDetailDto getPODetail(@PathVariable Long id) {
        return mapper.toDto(service.findPODetail(id));
    }

    @Operation(summary = "Thêm chi tiết PO")
    @PostMapping("/order-details")
    public ProductionOrderDetailDto createPODetail(
            @RequestBody(description = "Payload tạo chi tiết PO", required = true, content = @Content(schema = @Schema(implementation = ProductionOrderDetailDto.class))) @org.springframework.web.bind.annotation.RequestBody ProductionOrderDetailDto body) {
        ProductionOrderDetail d = new ProductionOrderDetail();
        if (body.getProductionOrderId() != null) {
            ProductionOrder po = new ProductionOrder();
            po.setId(body.getProductionOrderId());
            d.setProductionOrder(po);
        }
        if (body.getProductId() != null) {
            Product p = new Product();
            p.setId(body.getProductId());
            d.setProduct(p);
        }
        if (body.getBomId() != null) {
            Bom b = new Bom();
            b.setId(body.getBomId());
            d.setBom(b);
        }
        d.setBomVersion(body.getBomVersion());
        d.setQuantity(body.getQuantity());
        d.setUnit(body.getUnit());
        d.setNoteColor(body.getNoteColor());
        return mapper.toDto(service.createPODetail(d));
    }

    @Operation(summary = "Cập nhật chi tiết PO")
    @PutMapping("/order-details/{id}")
    public ProductionOrderDetailDto updatePODetail(@PathVariable Long id,
            @RequestBody(description = "Payload cập nhật chi tiết PO", required = true, content = @Content(schema = @Schema(implementation = ProductionOrderDetailDto.class))) @org.springframework.web.bind.annotation.RequestBody ProductionOrderDetailDto body) {
        ProductionOrderDetail d = new ProductionOrderDetail();
        if (body.getProductionOrderId() != null) {
            ProductionOrder po = new ProductionOrder();
            po.setId(body.getProductionOrderId());
            d.setProductionOrder(po);
        }
        if (body.getProductId() != null) {
            Product p = new Product();
            p.setId(body.getProductId());
            d.setProduct(p);
        }
        if (body.getBomId() != null) {
            Bom b = new Bom();
            b.setId(body.getBomId());
            d.setBom(b);
        }
        d.setBomVersion(body.getBomVersion());
        d.setQuantity(body.getQuantity());
        d.setUnit(body.getUnit());
        d.setNoteColor(body.getNoteColor());
        return mapper.toDto(service.updatePODetail(id, d));
    }

    @DeleteMapping("/order-details/{id}")
    public void deletePODetail(@PathVariable Long id) {
        service.deletePODetail(id);
    }

    // Technical Sheet
    @GetMapping("/tech-sheets/{id}")
    public TechnicalSheetDto getTechSheet(@PathVariable Long id) {
        return mapper.toDto(service.findTechSheet(id));
    }

    @Operation(summary = "Tạo Technical Sheet")
    @PostMapping("/tech-sheets")
    public TechnicalSheetDto createTechSheet(
            @RequestBody(description = "Payload tạo Technical Sheet", required = true, content = @Content(schema = @Schema(implementation = TechnicalSheetDto.class))) @org.springframework.web.bind.annotation.RequestBody TechnicalSheetDto body) {
        TechnicalSheet t = new TechnicalSheet();
        if (body.getProductionOrderId() != null) {
            ProductionOrder po = new ProductionOrder();
            po.setId(body.getProductionOrderId());
            t.setProductionOrder(po);
        }
        t.setSheetNumber(body.getSheetNumber());
        t.setYarnSpecifications(body.getYarnSpecifications());
        t.setMachineSettings(body.getMachineSettings());
        t.setQualityStandards(body.getQualityStandards());
        t.setSpecialInstructions(body.getSpecialInstructions());
        return mapper.toDto(service.createTechSheet(t));
    }

    @Operation(summary = "Cập nhật Technical Sheet")
    @PutMapping("/tech-sheets/{id}")
    public TechnicalSheetDto updateTechSheet(@PathVariable Long id,
            @RequestBody(description = "Payload cập nhật Technical Sheet", required = true, content = @Content(schema = @Schema(implementation = TechnicalSheetDto.class))) @org.springframework.web.bind.annotation.RequestBody TechnicalSheetDto body) {
        TechnicalSheet t = new TechnicalSheet();
        if (body.getProductionOrderId() != null) {
            ProductionOrder po = new ProductionOrder();
            po.setId(body.getProductionOrderId());
            t.setProductionOrder(po);
        }
        t.setSheetNumber(body.getSheetNumber());
        t.setYarnSpecifications(body.getYarnSpecifications());
        t.setMachineSettings(body.getMachineSettings());
        t.setQualityStandards(body.getQualityStandards());
        t.setSpecialInstructions(body.getSpecialInstructions());
        return mapper.toDto(service.updateTechSheet(id, t));
    }

    @DeleteMapping("/tech-sheets/{id}")
    public void deleteTechSheet(@PathVariable Long id) {
        service.deleteTechSheet(id);
    }

    // REMOVED: Tất cả endpoints liên quan WorkOrder và WorkOrderDetail - không còn
    // dùng nữa
    // Stages giờ được query trực tiếp theo ProductionOrder

    // Stages - Query theo ProductionOrder
    @Operation(summary = "Lấy danh sách stages của ProductionOrder")
    @GetMapping("/orders/{orderId}/stages")
    public List<ProductionStageDto> listStagesByOrder(@PathVariable Long orderId) {
        return service.findStagesByOrderId(orderId).stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @GetMapping("/stages/{id}")
    public ProductionStageDto getStage(@PathVariable Long id) {
        return service.getStageDto(id);
    }

    @Operation(summary = "Tạo Production Stage")
    @PostMapping("/stages")
    public ProductionStageDto createStage(
            @RequestBody(description = "Payload tạo Stage", required = true, content = @Content(schema = @Schema(implementation = ProductionStageDto.class))) @org.springframework.web.bind.annotation.RequestBody ProductionStageDto body) {
        ProductionStage s = new ProductionStage();
        // NEW: Link trực tiếp với ProductionOrder (không qua WorkOrderDetail)
        if (body.getProductionOrderId() != null) {
            ProductionOrder po = new ProductionOrder();
            po.setId(body.getProductionOrderId());
            s.setProductionOrder(po);
        }
        s.setStageType(body.getStageType());
        s.setStageSequence(body.getStageSequence());
        if (body.getMachineId() != null) {
            Machine m = new Machine();
            m.setId(body.getMachineId());
            s.setMachine(m);
        }
        if (body.getAssignedToId() != null) {
            User u = new User();
            u.setId(body.getAssignedToId());
            s.setAssignedTo(u);
        }
        if (body.getAssignedLeaderId() != null) {
            User ul = new User();
            ul.setId(body.getAssignedLeaderId());
            s.setAssignedLeader(ul);
        }
        s.setBatchNumber(body.getBatchNumber());
        s.setPlannedOutput(body.getPlannedOutput());
        s.setActualOutput(body.getActualOutput());
        s.setStartAt(body.getStartAt());
        s.setCompleteAt(body.getCompleteAt());
        s.setStatus(body.getStatus());
        s.setOutsourced(body.getIsOutsourced());
        s.setOutsourceVendor(body.getOutsourceVendor());
        s.setNotes(body.getNotes());
        s.setPlannedStartAt(body.getPlannedStartAt());
        s.setPlannedEndAt(body.getPlannedEndAt());
        s.setPlannedDurationHours(body.getPlannedDurationHours());
        return mapper.toDto(service.createStage(s));
    }

    @Operation(summary = "Cập nhật Production Stage")
    @PutMapping("/stages/{id}")
    public ProductionStageDto updateStage(@PathVariable Long id,
            @RequestBody(description = "Payload cập nhật Stage", required = true, content = @Content(schema = @Schema(implementation = ProductionStageDto.class))) @org.springframework.web.bind.annotation.RequestBody ProductionStageDto body) {
        ProductionStage s = new ProductionStage();
        // REMOVED: setWorkOrderDetail - field đã bị xóa
        // NEW: Set ProductionOrder nếu có
        if (body.getProductionOrderId() != null) {
            ProductionOrder po = new ProductionOrder();
            po.setId(body.getProductionOrderId());
            s.setProductionOrder(po);
        }
        s.setStageType(body.getStageType());
        s.setStageSequence(body.getStageSequence());
        if (body.getMachineId() != null) {
            Machine m = new Machine();
            m.setId(body.getMachineId());
            s.setMachine(m);
        }
        if (body.getAssignedToId() != null) {
            User u = new User();
            u.setId(body.getAssignedToId());
            s.setAssignedTo(u);
        }
        if (body.getAssignedLeaderId() != null) {
            User ul = new User();
            ul.setId(body.getAssignedLeaderId());
            s.setAssignedLeader(ul);
        }
        s.setBatchNumber(body.getBatchNumber());
        s.setPlannedOutput(body.getPlannedOutput());
        s.setActualOutput(body.getActualOutput());
        s.setStartAt(body.getStartAt());
        s.setCompleteAt(body.getCompleteAt());
        s.setStatus(body.getStatus());
        s.setOutsourced(body.getIsOutsourced());
        s.setOutsourceVendor(body.getOutsourceVendor());
        s.setNotes(body.getNotes());
        s.setPlannedStartAt(body.getPlannedStartAt());
        s.setPlannedEndAt(body.getPlannedEndAt());
        s.setPlannedDurationHours(body.getPlannedDurationHours());
        return mapper.toDto(service.updateStage(id, s));
    }

    @DeleteMapping("/stages/{id}")
    public void deleteStage(@PathVariable Long id) {
        service.deleteStage(id);
    }

    // ===== GIAI ĐOẠN 4: PRODUCTION ORDER CREATION & APPROVAL =====

    @Operation(summary = "Tạo lệnh sản xuất từ hợp đồng", description = "Planning Department tạo lệnh sản xuất từ hợp đồng đã duyệt")
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
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                        .ofPattern("dd-MM-yyyy");
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
                    throw new IllegalArgumentException(
                            "Invalid date format. Supported formats: yyyy-MM-dd, dd-MM-yyyy, or day number (1-31)");
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

    @Operation(summary = "Lấy lệnh sản xuất chờ duyệt", description = "Lấy danh sách lệnh sản xuất đang chờ duyệt")
    @GetMapping("/orders/pending-approval")
    public List<ProductionOrderDto> getProductionOrdersPendingApproval() {
        return service.getProductionOrdersPendingApproval().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Gửi lệnh sản xuất để duyệt", description = "Planning Department gửi lệnh sản xuất để Director duyệt")
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
    @Operation(summary = "Lấy lệnh sản xuất chờ duyệt (Director)", description = "Director lấy danh sách lệnh sản xuất chờ duyệt")
    @GetMapping("/orders/director/pending")
    public List<ProductionOrderDto> getDirectorPendingProductionOrders() {
        return service.getDirectorPendingProductionOrders().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    // Leader APIs
    @Operation(summary = "Lấy danh sách orders cho Team Leader", description = "Dùng cho màn hình danh sách đơn hàng của Team Leader")
    @GetMapping("/leader/orders")
    public java.util.List<ProductionOrderDto> getLeaderOrders(@RequestParam Long leaderUserId) {
        return service.getLeaderOrders(leaderUserId).stream()
                .map(po -> service.enrichProductionOrderDto(po))
                .collect(java.util.stream.Collectors.toList());
    }

    @Operation(summary = "Lấy chi tiết order với stage của Leader", description = "Dùng cho màn hình chi tiết đơn hàng của Team Leader")
    @GetMapping("/leader/orders/{orderId}")
    public ProductionOrderDto getLeaderOrderDetail(@PathVariable Long orderId, @RequestParam Long leaderUserId) {
        ProductionOrder po = service.findPO(orderId);
        ProductionOrderDto dto = service.enrichProductionOrderDto(po);

        // Lấy stage của leader này cho order này
        List<ProductionStage> leaderStages = service.getLeaderStagesForOrder(orderId, leaderUserId);

        // NEW: Restrict access - If leader is not assigned to any stage, deny access
        if (leaderStages.isEmpty()) {
            throw new RuntimeException("Bạn không có quyền truy cập đơn hàng này do không được phân công.");
        }

        if (!leaderStages.isEmpty()) {
            ProductionStage leaderStage = leaderStages.get(0);
            // Update the specific stage in the full list with extra info (Total Hours,
            // Defect)
            if (dto.getStages() != null) {
                for (ProductionStageDto s : dto.getStages()) {
                    if (s.getId().equals(leaderStage.getId())) {
                        java.math.BigDecimal totalHours = service.calculateTotalHoursForStage(leaderStage.getId());
                        s.setTotalHours(totalHours);

                        // Populate Defect Info for Rework
                        if (Boolean.TRUE.equals(leaderStage.getIsRework())) {
                            tmmsystem.entity.QualityIssue issue = service.getDefectForStage(leaderStage.getId());
                            if (issue != null) {
                                s.setDefectId(issue.getId());
                                s.setDefectDescription(issue.getDescription());
                                s.setDefectSeverity(issue.getSeverity());
                            }
                        }
                        // Break after finding the leader's stage
                        break;
                    }
                }
            }
            // REMOVED: dto.setStages(java.util.List.of(stageDto)); -> We now keep all
            // stages
        }

        return dto;
    }

    @Operation(summary = "Lấy chi tiết stage với progress history cho Leader", description = "Dùng cho màn hình cập nhật tiến độ của Team Leader")
    @GetMapping("/leader/stages/{stageId}")
    public ProductionStageDto getLeaderStageDetail(@PathVariable Long stageId) {
        ProductionStage stage = service.findStage(stageId);
        ProductionStageDto dto = mapper.toDto(stage);
        java.math.BigDecimal totalHours = service.calculateTotalHoursForStage(stageId);
        dto.setTotalHours(totalHours);
        return dto;
    }

    // QA/KCS APIs
    @Operation(summary = "Lấy danh sách orders cho QA/KCS", description = "Dùng cho màn hình danh sách đơn hàng của QA/KCS")
    @GetMapping("/qa/orders")
    public java.util.List<ProductionOrderDto> getQaOrders(@RequestParam Long qcUserId) {
        return service.getQaOrders(qcUserId).stream()
                .map(po -> service.enrichProductionOrderDto(po))
                .collect(java.util.stream.Collectors.toList());
    }

    @Operation(summary = "Duyệt lệnh sản xuất", description = "Director duyệt lệnh sản xuất")
    @PostMapping("/orders/{id}/approve")
    public ProductionOrderDto approveProductionOrder(
            @PathVariable Long id,
            @RequestParam Long directorId,
            @RequestParam(required = false) String notes) {
        return mapper.toDto(service.approveProductionOrder(id, directorId, notes));
    }

    @Operation(summary = "Từ chối lệnh sản xuất", description = "Director từ chối lệnh sản xuất")
    @PostMapping("/orders/{id}/reject")
    public ProductionOrderDto rejectProductionOrder(
            @PathVariable Long id,
            @RequestParam Long directorId,
            @RequestParam String rejectionNotes) {
        return mapper.toDto(service.rejectProductionOrder(id, directorId, rejectionNotes));
    }

    // ===== PRODUCTION PLAN INTEGRATION ENDPOINTS =====

    @Operation(summary = "Get production plans for contract", description = "Retrieve all production plans for a specific contract")
    @GetMapping("/contracts/{contractId}/plans")
    public List<tmmsystem.dto.production_plan.ProductionPlanDto> getProductionPlansForContract(
            @PathVariable Long contractId) {
        return service.getProductionPlansForContract(contractId);
    }

    @Operation(summary = "Create production order from approved plan", description = "Create production order from an approved production plan")
    @PostMapping("/plans/{planId}/create-order")
    public ProductionOrderDto createOrderFromApprovedPlan(@PathVariable Long planId) {
        return mapper.toDto(service.createFromApprovedPlan(planId));
    }

    @Operation(summary = "Get production plans pending approval", description = "Retrieve all production plans waiting for director approval")
    @GetMapping("/plans/pending-approval")
    public List<tmmsystem.dto.production_plan.ProductionPlanDto> getProductionPlansPendingApproval() {
        return service.getProductionPlansPendingApproval();
    }

    // REMOVED: Work Order approval endpoints - không còn dùng nữa
    // ProductionOrder được tạo trực tiếp từ ProductionPlan khi director approve

    @Operation(summary = "Resolve Stage by QR token", description = "KCS quét QR để lấy stage + checklist")
    @GetMapping("/stages/qr/{token}")
    public ProductionStageDto getStageByQr(@PathVariable String token) {
        return mapper.toDto(service.findStageByQrToken(token));
    }

    @Operation(summary = "Redo Stage (minor fail)")
    @PostMapping("/stages/{id}/redo")
    public ProductionStageDto redoStage(@PathVariable Long id) {
        return mapper.toDto(service.redoStage(id));
    }

    @Operation(summary = "List stages for Leader")
    @GetMapping("/stages/for-leader")
    public java.util.List<ProductionStageDto> listForLeader(@RequestParam Long userId) {
        return service.findStagesForLeader(userId).stream().map(mapper::toDto)
                .collect(java.util.stream.Collectors.toList());
    }

    @Operation(summary = "List stages for KCS")
    @GetMapping("/stages/for-kcs")
    public java.util.List<ProductionStageDto> listForKcs(@RequestParam(required = false) String status) {
        return service.findStagesForKcs(status).stream().map(mapper::toDto)
                .collect(java.util.stream.Collectors.toList());
    }

    // REMOVED: createStandardWorkOrder endpoint - không còn dùng nữa
    // Stages được tạo trực tiếp từ ProductionPlan khi director approve

    @Operation(summary = "Leader bắt đầu công đoạn")
    @PostMapping("/stages/{id}/start")
    public ProductionStageDto leaderStart(@PathVariable Long id, @RequestParam Long leaderUserId,
            @RequestParam(required = false) String evidencePhotoUrl,
            @RequestParam(required = false) java.math.BigDecimal qtyCompleted) {
        return mapper.toDto(service.startStage(id, leaderUserId, evidencePhotoUrl, qtyCompleted));
    }

    @Operation(summary = "Leader tạm dừng công đoạn")
    @PostMapping("/stages/{id}/pause")
    public ProductionStageDto leaderPause(@PathVariable Long id, @RequestParam Long leaderUserId,
            @RequestParam String pauseReason,
            @RequestParam(required = false) String pauseNotes) {
        return mapper.toDto(service.pauseStage(id, leaderUserId, pauseReason, pauseNotes));
    }

    @Operation(summary = "Leader bắt đầu sửa lỗi (Pre-emption)")
    @PostMapping("/stages/{id}/start-rework")
    public ProductionStageDto leaderStartRework(@PathVariable Long id, @RequestParam Long leaderUserId) {
        return mapper.toDto(executionService.startRework(id, leaderUserId));
    }

    @Operation(summary = "Leader tiếp tục công đoạn")
    @PostMapping("/stages/{id}/resume")
    public ProductionStageDto leaderResume(@PathVariable Long id, @RequestParam Long leaderUserId) {
        return mapper.toDto(service.resumeStage(id, leaderUserId));
    }

    @Operation(summary = "Leader hoàn thành công đoạn")
    @PostMapping("/stages/{id}/complete")
    public ProductionStageDto leaderComplete(@PathVariable Long id, @RequestParam Long leaderUserId,
            @RequestParam(required = false) String evidencePhotoUrl,
            @RequestParam(required = false) java.math.BigDecimal qtyCompleted) {
        return mapper.toDto(service.completeStage(id, leaderUserId, evidencePhotoUrl, qtyCompleted));
    }

    @Operation(summary = "Leader cập nhật tiến độ")
    @PostMapping("/stages/{id}/progress")
    public ProductionStageDto leaderUpdateProgress(@PathVariable Long id, @RequestParam Long leaderUserId,
            @RequestParam java.math.BigDecimal progressPercent) {
        return mapper.toDto(service.leaderUpdateProgress(id, leaderUserId, progressPercent));
    }

    @Operation(summary = "PM phân công kỹ thuật viên cho PO")
    @PostMapping("/orders/{id}/assign-technician")
    public ProductionOrderDto assignTechnician(@PathVariable Long id, @RequestParam Long technicianUserId) {
        return mapper.toDto(service.assignTechnician(id, technicianUserId));
    }

    @Operation(summary = "Phân công leader cho Stage")
    @PostMapping("/stages/{id}/assign-leader")
    public ProductionStageDto assignLeader(@PathVariable Long id, @RequestParam Long leaderUserId) {
        return mapper.toDto(service.assignStageLeader(id, leaderUserId));
    }

    @Operation(summary = "Phân công QC cho Stage")
    @PostMapping("/stages/{id}/assign-kcs")
    public ProductionStageDto assignKcs(@PathVariable Long id, @RequestParam Long kcsUserId) {
        return mapper.toDto(service.assignStageQc(id, kcsUserId));
    }

    @Operation(summary = "Bulk phân công leader cho các stage của ProductionOrder")
    @PostMapping("/orders/{orderId}/assign-leaders")
    public java.util.List<ProductionStageDto> bulkAssignLeaders(@PathVariable Long orderId,
            @RequestBody java.util.Map<String, Long> stageLeaderMap) {
        return service.bulkAssignStageLeaders(orderId, stageLeaderMap).stream().map(s -> mapper.toDto(s))
                .collect(java.util.stream.Collectors.toList());
    }

    @Operation(summary = "Fix Data Consistency (Admin/Dev)")
    @PostMapping("/fix-data")
    public ResponseEntity<?> fixDataConsistency() {
        service.fixDataConsistency();
        return ResponseEntity.ok(Map.of("message", "Data consistency fixed successfully"));
    }

    // Material Requisition APIs
    @Operation(summary = "Lấy chi tiết yêu cầu cấp sợi")
    @GetMapping("/material-requests/{id}")
    public tmmsystem.dto.MaterialRequisitionResponseDto getMaterialRequest(@PathVariable Long id) {
        return service.getMaterialRequestDetails(id);
    }

    @Operation(summary = "Phê duyệt yêu cầu cấp sợi")
    @PostMapping("/material-requests/{id}/approve")
    public ProductionOrderDto approveMaterialRequest(@PathVariable Long id,
            @RequestBody tmmsystem.dto.execution.MaterialRequisitionApprovalDto body) {
        return mapper.toDto(service.approveMaterialRequest(id, body));
    }

    @Operation(summary = "PM: Bắt đầu đơn hàng bổ sung")
    @PostMapping("/orders/{id}/start-supplementary")
    public ProductionOrderDto startSupplementaryOrder(@PathVariable Long id) {
        return mapper.toDto(service.startSupplementaryOrder(id));
    }
}
