package tmmsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import tmmsystem.dto.production_plan.*;
import tmmsystem.service.ProductionPlanService;

import java.util.List;

@RestController
@RequestMapping("/v1/production-plans")
@Validated
@Tag(name = "Quản lý Kế hoạch Sản xuất", description = "API quản lý kế hoạch sản xuất chi tiết với lựa chọn máy móc thông minh")
public class ProductionPlanController {
    
    private final ProductionPlanService service;
    
    public ProductionPlanController(ProductionPlanService service) {
        this.service = service;
    }
    
    // ===== CRUD Operations =====
    
    @Operation(summary = "Lấy danh sách tất cả kế hoạch sản xuất", 
               description = "Lấy danh sách tất cả kế hoạch sản xuất trong hệ thống, bao gồm cả thông tin chi tiết về hợp đồng, sản phẩm và trạng thái")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Thành công - Trả về danh sách kế hoạch sản xuất",
                    content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                        [
                          {
                            "id": 1,
                            "planCode": "PP-2025-001",
                            "status": "DRAFT",
                            "contractNumber": "ORD-101",
                            "customerName": "Công ty ABC",
                            "createdByName": "Nguyễn Văn A",
                            "createdAt": "2025-10-26T09:45:00Z"
                          }
                        ]
                        """))),
        @ApiResponse(responseCode = "500", description = "Lỗi server nội bộ")
    })
    @GetMapping
    public List<ProductionPlanDto> getAllPlans() {
        return service.findAllPlans();
    }
    
    @Operation(summary = "Lấy chi tiết kế hoạch sản xuất theo ID", 
               description = "Lấy thông tin chi tiết của một kế hoạch sản xuất cụ thể, bao gồm tất cả các công đoạn, máy móc được gán và nguyên vật liệu cần thiết")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Thành công - Trả về chi tiết kế hoạch sản xuất",
                    content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                        {
                          "id": 1,
                          "planCode": "PP-2025-001",
                          "status": "DRAFT",
                          "contractNumber": "ORD-101",
                          "customerName": "Công ty ABC",
                          "details": [
                            {
                              "productName": "Khăn mặt Bamboo cao cấp",
                              "plannedQuantity": 1000,
                              "stages": [
                                {
                                  "stageType": "WARPING",
                                  "assignedMachineName": "Máy cuộn mắc 01",
                                  "plannedStartTime": "2025-10-28T08:00:00",
                                  "plannedEndTime": "2025-10-28T12:00:00"
                                }
                              ]
                            }
                          ]
                        }
                        """))),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy kế hoạch sản xuất với ID này"),
        @ApiResponse(responseCode = "500", description = "Lỗi server nội bộ")
    })
    @GetMapping("/{id}")
    public ProductionPlanDto getPlanById(
            @Parameter(description = "ID của kế hoạch sản xuất cần lấy", required = true, example = "1")
            @PathVariable Long id) {
        return service.findPlanById(id);
    }
    
    @Operation(summary = "Lấy kế hoạch sản xuất theo trạng thái", 
               description = "Lấy danh sách kế hoạch sản xuất được lọc theo trạng thái cụ thể. Các trạng thái có thể: DRAFT (Nháp), PENDING_APPROVAL (Chờ duyệt), APPROVED (Đã duyệt), REJECTED (Từ chối)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Thành công - Trả về danh sách kế hoạch sản xuất theo trạng thái"),
        @ApiResponse(responseCode = "400", description = "Trạng thái không hợp lệ"),
        @ApiResponse(responseCode = "500", description = "Lỗi server nội bộ")
    })
    @GetMapping("/status/{status}")
    public List<ProductionPlanDto> getPlansByStatus(
            @Parameter(description = "Trạng thái kế hoạch sản xuất", required = true, 
                       example = "DRAFT", 
                       schema = @Schema(allowableValues = {"DRAFT", "PENDING_APPROVAL", "APPROVED", "REJECTED"}))
            @PathVariable String status) {
        return service.findPlansByStatus(status);
    }
    
    @Operation(summary = "Lấy kế hoạch sản xuất chờ duyệt", 
               description = "Lấy danh sách tất cả kế hoạch sản xuất đang chờ giám đốc phê duyệt. Chỉ hiển thị các kế hoạch có trạng thái PENDING_APPROVAL")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Thành công - Trả về danh sách kế hoạch chờ duyệt"),
        @ApiResponse(responseCode = "500", description = "Lỗi server nội bộ")
    })
    @GetMapping("/pending-approval")
    public List<ProductionPlanDto> getPendingApprovalPlans() {
        return service.findPendingApprovalPlans();
    }
    
    // ===== Production Plan Creation =====
    
    @Operation(summary = "Tạo kế hoạch sản xuất từ hợp đồng", 
               description = "Tạo kế hoạch sản xuất mới dựa trên hợp đồng đã được phê duyệt. Hệ thống sẽ tự động tạo các công đoạn sản xuất và tính toán nguyên vật liệu cần thiết")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Thành công - Kế hoạch sản xuất đã được tạo",
                    content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                        {
                          "id": 1,
                          "planCode": "PP-2025-001",
                          "status": "DRAFT",
                          "contractNumber": "ORD-101",
                          "customerName": "Công ty ABC",
                          "message": "Kế hoạch sản xuất đã được tạo thành công"
                        }
                        """))),
        @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ"),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy hợp đồng"),
        @ApiResponse(responseCode = "500", description = "Lỗi server nội bộ")
    })
    @PostMapping
    public ProductionPlanDto createPlanFromContract(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Thông tin tạo kế hoạch sản xuất",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                        {
                          "contractId": 1,
                          "notes": "Kế hoạch sản xuất cho đơn hàng khăn mặt cao cấp"
                        }
                        """)
                )
            )
            @RequestBody @Valid CreateProductionPlanRequest request) {
        return service.createPlanFromContract(request);
    }
    
    @Operation(summary = "Tạo kế hoạch từ Lot", description = "Tạo bản kế hoạch DRAFT và lock Lot sang PLANNING")
    @PostMapping(value = "/create-from-lot")
    public ProductionPlanDto createFromLot(@RequestParam Long lotId){
        return service.createPlanFromLot(lotId);
    }

    // ===== Approval Workflow =====
    
    @Operation(summary = "Gửi kế hoạch để phê duyệt", 
               description = "Gửi kế hoạch sản xuất đang ở trạng thái DRAFT để giám đốc phê duyệt. Kế hoạch sẽ chuyển sang trạng thái PENDING_APPROVAL")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Thành công - Kế hoạch đã được gửi để duyệt",
                    content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                        {
                          "id": 1,
                          "planCode": "PP-2025-001",
                          "status": "PENDING_APPROVAL",
                          "submittedAt": "2025-10-26T10:30:00Z",
                          "message": "Kế hoạch đã được gửi để phê duyệt"
                        }
                        """))),
        @ApiResponse(responseCode = "400", description = "Kế hoạch không ở trạng thái DRAFT"),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy kế hoạch sản xuất"),
        @ApiResponse(responseCode = "500", description = "Lỗi server nội bộ")
    })
    @PutMapping("/{id}/submit")
    public ProductionPlanDto submitForApproval(
            @Parameter(description = "ID của kế hoạch sản xuất cần gửi duyệt", required = true, example = "1")
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Thông tin gửi duyệt",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                        {
                          "notes": "Kế hoạch sản xuất đã hoàn thiện, xin phê duyệt"
                        }
                        """)
                )
            )
            @RequestBody @Valid SubmitForApprovalRequest request) {
        return service.submitForApproval(id, request);
    }
    
    @Operation(summary = "Phê duyệt kế hoạch sản xuất", 
               description = "Giám đốc phê duyệt kế hoạch sản xuất và hệ thống tự động tạo Production Order. Kế hoạch sẽ chuyển sang trạng thái APPROVED")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Thành công - Kế hoạch đã được phê duyệt và Production Order đã được tạo",
                    content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                        {
                          "id": 1,
                          "planCode": "PP-2025-001",
                          "status": "APPROVED",
                          "approvedAt": "2025-10-26T11:00:00Z",
                          "productionOrderId": 1,
                          "message": "Kế hoạch đã được phê duyệt và Production Order đã được tạo"
                        }
                        """))),
        @ApiResponse(responseCode = "400", description = "Kế hoạch không ở trạng thái PENDING_APPROVAL"),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy kế hoạch sản xuất"),
        @ApiResponse(responseCode = "500", description = "Lỗi server nội bộ")
    })
    @PutMapping("/{id}/approve")
    public ProductionPlanDto approvePlan(
            @Parameter(description = "ID của kế hoạch sản xuất cần phê duyệt", required = true, example = "1")
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Thông tin phê duyệt",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                        {
                          "notes": "Kế hoạch sản xuất được phê duyệt, có thể tiến hành sản xuất"
                        }
                        """)
                )
            )
            @RequestBody @Valid ApproveProductionPlanRequest request) {
        return service.approvePlan(id, request);
    }
    
    @Operation(summary = "Từ chối kế hoạch sản xuất", 
               description = "Giám đốc từ chối kế hoạch sản xuất với lý do cụ thể. Kế hoạch sẽ chuyển sang trạng thái REJECTED")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Thành công - Kế hoạch đã bị từ chối",
                    content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                        {
                          "id": 1,
                          "planCode": "PP-2025-001",
                          "status": "REJECTED",
                          "rejectedAt": "2025-10-26T11:30:00Z",
                          "rejectionReason": "Cần điều chỉnh lại thời gian sản xuất",
                          "message": "Kế hoạch đã bị từ chối"
                        }
                        """))),
        @ApiResponse(responseCode = "400", description = "Kế hoạch không ở trạng thái PENDING_APPROVAL"),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy kế hoạch sản xuất"),
        @ApiResponse(responseCode = "500", description = "Lỗi server nội bộ")
    })
    @PutMapping("/{id}/reject")
    public ProductionPlanDto rejectPlan(
            @Parameter(description = "ID của kế hoạch sản xuất cần từ chối", required = true, example = "1")
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Thông tin từ chối",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                        {
                          "rejectionReason": "Cần điều chỉnh lại thời gian sản xuất và phân bổ máy móc",
                          "notes": "Vui lòng xem xét lại và gửi lại để duyệt"
                        }
                        """)
                )
            )
            @RequestBody @Valid RejectProductionPlanRequest request) {
        return service.rejectPlan(id, request);
    }
    
    // ===== Additional Endpoints =====
    
    @Operation(summary = "Lấy kế hoạch sản xuất theo hợp đồng", 
               description = "Lấy danh sách tất cả kế hoạch sản xuất được tạo từ một hợp đồng cụ thể")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Thành công - Trả về danh sách kế hoạch sản xuất của hợp đồng"),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy hợp đồng"),
        @ApiResponse(responseCode = "500", description = "Lỗi server nội bộ")
    })
    @GetMapping("/contract/{contractId}")
    public List<ProductionPlanDto> getPlansByContract(
            @Parameter(description = "ID của hợp đồng", required = true, example = "1")
            @PathVariable Long contractId) {
        return service.findPlansByContract(contractId);
    }
    
    @Operation(summary = "Lấy kế hoạch sản xuất theo người tạo", 
               description = "Lấy danh sách tất cả kế hoạch sản xuất được tạo bởi một người dùng cụ thể")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Thành công - Trả về danh sách kế hoạch sản xuất của người dùng"),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng"),
        @ApiResponse(responseCode = "500", description = "Lỗi server nội bộ")
    })
    @GetMapping("/creator/{userId}")
    public List<ProductionPlanDto> getPlansByCreator(
            @Parameter(description = "ID của người dùng", required = true, example = "1")
            @PathVariable Long userId) {
        return service.findPlansByCreator(userId);
    }
    
    @Operation(summary = "Lấy kế hoạch đã duyệt chưa chuyển đổi", 
               description = "Lấy danh sách kế hoạch sản xuất đã được phê duyệt nhưng chưa được chuyển đổi thành Production Order")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Thành công - Trả về danh sách kế hoạch đã duyệt chưa chuyển đổi"),
        @ApiResponse(responseCode = "500", description = "Lỗi server nội bộ")
    })
    @GetMapping("/approved-not-converted")
    public List<ProductionPlanDto> getApprovedPlansNotConverted() {
        return service.findApprovedPlansNotConverted();
    }
    
    // ===== Machine Selection Endpoints (stage-based only) =====
    @Operation(summary = "Lấy gợi ý máy móc cho công đoạn sản xuất",
               description = "Gợi ý máy móc theo stageId với phân tích khả dụng và công suất")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Thành công - Trả về danh sách gợi ý máy móc")
    })
    @GetMapping("/stages/{stageId}/machine-suggestions")
    public List<tmmsystem.service.MachineSelectionService.MachineSuggestionDto> getMachineSuggestionsForStage(
            @Parameter(description = "ID của công đoạn sản xuất", required = true, example = "1")
            @PathVariable Long stageId) {
        tmmsystem.entity.ProductionPlanStage stage = service.findStageById(stageId);
        var lot = stage.getPlan().getLot();
        Long productId = lot != null && lot.getProduct()!=null ? lot.getProduct().getId() : null;
        java.math.BigDecimal qty = lot != null ? lot.getTotalQuantity() : java.math.BigDecimal.ZERO;
        return service.getMachineSuggestionsForStage(
            stage.getStageType(),
            productId,
            qty,
            stage.getPlannedStartTime(),
            stage.getPlannedEndTime()
        );
    }
    
    @Operation(summary = "Tự động gán máy móc cho công đoạn sản xuất",
               description = "Chọn máy có điểm số cao nhất cho stage")
    @PostMapping("/stages/{stageId}/auto-assign-machine")
    public tmmsystem.dto.production_plan.ProductionPlanStageDto autoAssignMachineToStage(
            @Parameter(description = "ID của công đoạn sản xuất cần gán máy", required = true, example = "1")
            @PathVariable Long stageId) {
        return service.autoAssignMachineToStage(stageId);
    }
    
    @Operation(summary = "Kiểm tra xung đột lịch trình cho công đoạn sản xuất",
               description = "Trả về danh sách xung đột (nếu có) cho stage")
    @GetMapping("/stages/{stageId}/check-conflicts")
    public List<String> checkStageScheduleConflicts(
            @Parameter(description = "ID của công đoạn sản xuất cần kiểm tra", required = true, example = "1")
            @PathVariable Long stageId) {
        return service.checkStageScheduleConflicts(stageId);
    }

    @Operation(summary = "Cập nhật công đoạn sản xuất",
               description = "Cập nhật máy, người phụ trách, QC, thời gian và ghi chú cho một stage bằng một endpoint duy nhất")
    @PutMapping("/stages/{stageId}")
    public tmmsystem.dto.production_plan.ProductionPlanStageDto updateStage(
            @PathVariable Long stageId,
            @RequestBody @Valid tmmsystem.dto.production_plan.ProductionPlanStageRequest request){
        return service.updateStage(stageId, request);
    }

    @Operation(summary = "Danh sách công đoạn của kế hoạch", description = "Lấy tất cả stage theo planId phục vụ hiển thị form lập kế hoạch")
    @GetMapping("/{planId}/stages")
    public java.util.List<ProductionPlanStageDto> listStages(@PathVariable Long planId){
        return service.listStagesOfPlan(planId);
    }

    @Operation(summary = "Tính toán và cập nhật ngày kế hoạch", description = "Tự động tính toán ngày bắt đầu và kết thúc dự kiến cho toàn bộ kế hoạch dựa trên thời gian của các công đoạn và cập nhật lại kế hoạch.")
    @PostMapping("/{id}/calculate-schedule")
    public ProductionPlanDto calculateSchedule(@PathVariable Long id) {
        return service.calculateScheduleAndReturnDto(id);
    }
}
