package tmmsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import tmmsystem.dto.sales.RfqDto;
import tmmsystem.dto.sales.RfqDetailDto;
import tmmsystem.dto.sales.CapacityCheckResultDto;
import tmmsystem.dto.sales.RfqAssignRequest;
import tmmsystem.dto.sales.RfqCreateDto;
import tmmsystem.dto.sales.RfqPublicCreateDto;
import tmmsystem.entity.Customer;
import tmmsystem.entity.Rfq;
import tmmsystem.entity.User;
import tmmsystem.mapper.RfqMapper;
import tmmsystem.service.RfqService;
import tmmsystem.service.CapacityCheckService;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import tmmsystem.dto.sales.SalesRfqCreateRequest;
import tmmsystem.dto.sales.SalesRfqEditRequest;
import tmmsystem.dto.sales.AssignSalesRequest;
import tmmsystem.dto.sales.AssignPlanningRequest;

@RestController
@RequestMapping("/v1/rfqs")
@Validated
@Tag(name = "RFQ")
public class RfqController {
    private final RfqService service;
    private final RfqMapper mapper;
    private final CapacityCheckService capacityCheckService;

    public RfqController(RfqService service, RfqMapper mapper, CapacityCheckService capacityCheckService) {
        this.service = service;
        this.mapper = mapper;
        this.capacityCheckService = capacityCheckService;
    }

    @Operation(summary = "Danh sách RFQ",
            description = "Trả về danh sách RFQ đã tạo (bao gồm trạng thái hiện tại và chi tiết nếu có)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Danh sách RFQ",
                    content = @Content(schema = @Schema(implementation = RfqDto.class)))
    })
    @GetMapping
    public List<RfqDto> list() { return service.findAll().stream().map(mapper::toDto).collect(Collectors.toList()); }

    @Operation(summary = "Lấy chi tiết RFQ",
            description = "Lấy thông tin RFQ theo id, bao gồm danh sách sản phẩm chi tiết nếu có")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chi tiết RFQ",
                    content = @Content(schema = @Schema(implementation = RfqDto.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy RFQ")
    })
    @GetMapping("/{id}")
    public RfqDto get(@Parameter(description = "ID RFQ") @PathVariable Long id) { return mapper.toDto(service.findById(id)); }

    @Operation(summary = "Tạo RFQ (khách đã đăng nhập)",
            description = "Customer/Sale tạo yêu cầu báo giá ở trạng thái DRAFT. Bắt buộc cung cấp customerId (khách đã đăng nhập).")
    @PostMapping
    public RfqDto create(
            @RequestBody(description = "Payload tạo RFQ (khách đã đăng nhập)", required = true,
                    content = @Content(schema = @Schema(implementation = RfqCreateDto.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody RfqCreateDto body) {
        Rfq created = service.createFromLoggedIn(body);
        return mapper.toDto(created);
    }

    // New endpoint: public form submission (khách chưa đăng nhập)
    @Operation(summary = "Tạo RFQ (public form)",
            description = "Dành cho khách chưa đăng nhập: cung cấp contactPerson, contactEmail or contactPhone, contactAddress. Hệ thống sẽ tìm customer theo email/phone hoặc sinh customer mới (is_verified=false) và gán vào RFQ.")
    @PostMapping("/public")
    public RfqDto createPublic(
            @RequestBody(description = "Payload tạo RFQ public", required = true,
                    content = @Content(schema = @Schema(implementation = RfqPublicCreateDto.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody RfqPublicCreateDto body) {
        Rfq created = service.createFromPublic(body);
        return mapper.toDto(created);
    }

    @Operation(summary = "Cập nhật RFQ",
            description = "Cập nhật thông tin chung của RFQ. Không bao gồm chi tiết sản phẩm.")
    @PutMapping("/{id}")
    public RfqDto update(
            @Parameter(description = "ID RFQ") @PathVariable Long id,
            @RequestBody(description = "Payload cập nhật RFQ", required = true,
                    content = @Content(schema = @Schema(implementation = RfqDto.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody RfqDto body) {
        return mapper.toDto(service.updateRfqWithDetails(id, body));
    }

    @Operation(summary = "Xóa RFQ",
            description = "Xóa RFQ theo id khi chưa được xử lý tiếp.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Đã xóa"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy RFQ")
    })
    @DeleteMapping("/{id}")
    public void delete(@Parameter(description = "ID RFQ") @PathVariable Long id) { service.delete(id); }

    // RFQ Detail Management APIs
    @Operation(summary = "Danh sách chi tiết sản phẩm của RFQ",
            description = "Trả về các dòng sản phẩm (product, quantity, unit, notes) thuộc RFQ")
    @GetMapping("/{rfqId}/details")
    public List<RfqDetailDto> listDetails(@Parameter(description = "ID RFQ") @PathVariable Long rfqId) { 
        return service.findDetailsByRfqId(rfqId).stream().map(mapper::toDetailDto).collect(Collectors.toList()); 
    }

    @Operation(summary = "Lấy 1 dòng chi tiết RFQ",
            description = "Lấy thông tin chi tiết của 1 dòng sản phẩm trong RFQ")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chi tiết RFQ",
                    content = @Content(schema = @Schema(implementation = RfqDetailDto.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy chi tiết RFQ")
    })
    @GetMapping("/details/{id}")
    public RfqDetailDto getDetail(@Parameter(description = "ID chi tiết RFQ") @PathVariable Long id) { 
        return mapper.toDetailDto(service.findDetailById(id)); 
    }

    @Operation(summary = "Thêm chi tiết sản phẩm vào RFQ",
            description = "Thêm 1 dòng sản phẩm mới vào RFQ (product, quantity, unit, notes)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chi tiết RFQ đã được thêm",
                    content = @Content(schema = @Schema(implementation = RfqDetailDto.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy RFQ"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    @PostMapping("/{rfqId}/details")
    public RfqDetailDto addDetail(
            @Parameter(description = "ID RFQ") @PathVariable Long rfqId,
            @RequestBody(description = "Payload thêm chi tiết RFQ", required = true,
                    content = @Content(schema = @Schema(implementation = RfqDetailDto.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody RfqDetailDto body) {
        return mapper.toDetailDto(service.addDetail(rfqId, body));
    }

    @Operation(summary = "Cập nhật chi tiết sản phẩm trong RFQ",
            description = "Cập nhật thông tin của 1 dòng sản phẩm trong RFQ")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chi tiết RFQ đã được cập nhật",
                    content = @Content(schema = @Schema(implementation = RfqDetailDto.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy chi tiết RFQ"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    @PutMapping("/details/{id}")
    public RfqDetailDto updateDetail(
            @Parameter(description = "ID chi tiết RFQ") @PathVariable Long id,
            @RequestBody(description = "Payload cập nhật chi tiết RFQ", required = true,
                    content = @Content(schema = @Schema(implementation = RfqDetailDto.class)))
            @org.springframework.web.bind.annotation.RequestBody RfqDetailDto body) {
        return mapper.toDetailDto(service.updateDetail(id, body));
    }

    @Operation(summary = "Xóa 1 dòng chi tiết RFQ",
            description = "Xóa 1 dòng sản phẩm khỏi RFQ")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chi tiết RFQ đã được xóa"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy chi tiết RFQ")
    })
    @DeleteMapping("/details/{id}")
    public void deleteDetail(@Parameter(description = "ID chi tiết RFQ") @PathVariable Long id) { 
        service.deleteDetail(id); 
    }
    
    @Operation(summary = "Cập nhật ngày giao hàng mong muốn",
            description = "Cập nhật ngày giao hàng mong muốn của RFQ. Hỗ trợ 2 định dạng: yyyy-MM-dd (2025-05-10) hoặc dd-MM-yyyy (10-05-2025)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ngày giao hàng đã được cập nhật",
                    content = @Content(schema = @Schema(implementation = RfqDto.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy RFQ"),
            @ApiResponse(responseCode = "400", description = "Ngày không hợp lệ. Hỗ trợ định dạng: yyyy-MM-dd hoặc dd-MM-yyyy")
    })
    @PutMapping("/{id}/expected-delivery-date")
    public RfqDto updateExpectedDeliveryDate(
            @Parameter(description = "ID RFQ") @PathVariable Long id,
            @RequestParam("expectedDeliveryDate") String expectedDeliveryDate) {
        return mapper.toDto(service.updateExpectedDeliveryDate(id, expectedDeliveryDate));
    }

    // RFQ Workflow APIs
    @Operation(summary = "Gửi RFQ (chuyển từ DRAFT sang SENT)",
            description = "Sau khi khách hàng tạo, Sale Staff gửi yêu cầu để bắt đầu xử lý. Hệ thống sẽ thông báo cho Sale.")
    @PostMapping("/{id}/send")
    public RfqDto sendRfq(@Parameter(description = "ID RFQ") @PathVariable Long id) {
        return mapper.toDto(service.sendRfq(id));
    }

    @Operation(summary = "Kiểm tra sơ bộ RFQ",
            description = "Sale Staff kiểm tra sơ bộ các thông tin bắt buộc (ít nhất 1 dòng sản phẩm, ngày giao hàng). Trạng thái chuyển sang PRELIMINARY_CHECKED.")
    @PostMapping("/{id}/preliminary-check")
    public RfqDto preliminaryCheck(@Parameter(description = "ID RFQ") @PathVariable Long id) {
        return mapper.toDto(service.preliminaryCheck(id));
    }

    @Operation(summary = "Chuyển RFQ sang Planning Department",
            description = "Chỉ thực hiện được sau khi đã preliminary-check. Trạng thái chuyển sang FORWARDED_TO_PLANNING và hệ thống thông báo cho Planning.")
    @PostMapping("/{id}/forward-to-planning")
    public RfqDto forwardToPlanning(@Parameter(description = "ID RFQ") @PathVariable Long id) {
        return mapper.toDto(service.forwardToPlanning(id));
    }

    @Operation(summary = "Planning Department nhận RFQ",
            description = "Phòng kế hoạch xác nhận đã nhận RFQ để xử lý kiểm tra năng lực.")
    @PostMapping("/{id}/receive-by-planning")
    public RfqDto receiveByPlanning(@Parameter(description = "ID RFQ") @PathVariable Long id) {
        return mapper.toDto(service.receiveByPlanning(id));
    }

    @Operation(summary = "Hủy RFQ",
            description = "Hủy RFQ ở mọi trạng thái đang xử lý. Hệ thống sẽ gửi thông báo liên quan.")
    @PostMapping("/{id}/cancel")
    public RfqDto cancelRfq(@Parameter(description = "ID RFQ") @PathVariable Long id) {
        return mapper.toDto(service.cancelRfq(id));
    }

    // Capacity Check APIs
    @Operation(summary = "Kiểm tra năng lực máy móc",
            description = "Planning Department kiểm tra năng lực máy móc cho RFQ, tính toán bottleneck và xung đột với đơn hàng khác")
    @PostMapping("/{id}/check-machine-capacity")
    public CapacityCheckResultDto checkMachineCapacity(@Parameter(description = "ID RFQ") @PathVariable Long id) {
        return capacityCheckService.checkMachineCapacity(id);
    }

    @Operation(summary = "Kiểm tra năng lực kho hàng",
            description = "Planning Department kiểm tra năng lực kho hàng cho RFQ (luôn đủ theo giả định)")
    @PostMapping("/{id}/check-warehouse-capacity")
    public CapacityCheckResultDto checkWarehouseCapacity(@Parameter(description = "ID RFQ") @PathVariable Long id) {
        return capacityCheckService.checkWarehouseCapacity(id);
    }

    @Operation(summary = "Gán Sales và Planning cho RFQ (chỉ khi DRAFT)")
    @PostMapping("/{id}/assign")
    public RfqDto assign(
            @Parameter(description = "ID RFQ") @PathVariable Long id,
            @RequestBody(description = "Payload gán nhân sự", required = true,
                    content = @Content(schema = @Schema(implementation = RfqAssignRequest.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody RfqAssignRequest body) {
        Rfq updated = service.assignStaff(id, body.getAssignedSalesId(), body.getAssignedPlanningId(), body.getApprovedById());
        return mapper.toDto(updated);
    }

    // MỞ RỘNG: endpoint riêng cho Sales chỉ định (cần header X-User-Id)
    @Operation(summary = "Lấy RFQ cho Sales được giao",
            description = "Chỉ Sales được gán vào RFQ mới có thể xem chi tiết thông qua endpoint này")
    @GetMapping("/{id}/for-sales")
    public RfqDto getForSales(@Parameter(description = "ID RFQ") @PathVariable Long id,
                              @RequestHeader("X-User-Id") Long userId) {
        Rfq rfq = service.findById(id);
        if (rfq.getAssignedSales() == null || rfq.getAssignedSales().getId() == null || !userId.equals(rfq.getAssignedSales().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: not assigned sales");
        }
        return mapper.toDto(rfq);
    }

    // Endpoint riêng cho Planning được giao
    @Operation(summary = "Lấy RFQ cho Planning được giao",
            description = "Chỉ Planning được gán vào RFQ mới có thể xem chi tiết thông qua endpoint này")
    @GetMapping("/{id}/for-planning")
    public RfqDto getForPlanning(@Parameter(description = "ID RFQ") @PathVariable Long id,
                                 @RequestHeader("X-User-Id") Long userId) {
        Rfq rfq = service.findById(id);
        if (rfq.getAssignedPlanning() == null || rfq.getAssignedPlanning().getId() == null || !userId.equals(rfq.getAssignedPlanning().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: not assigned planning");
        }
        return mapper.toDto(rfq);
    }

    // NEW: Danh sách RFQ cho Sales (chỉ RFQ được gán cho sales này)
    @Operation(summary = "Danh sách RFQ được gán cho Sales",
            description = "Trả về danh sách RFQ mà Sales (header X-User-Id) được gán vào")
    @GetMapping("/for-sales")
    public List<RfqDto> listForSales(@RequestHeader("X-User-Id") Long userId) {
        return service.findByAssignedSales(userId).stream().map(mapper::toDto).collect(Collectors.toList());
    }

    // NEW: Danh sách RFQ cho Planning (chỉ RFQ được gán cho planning này)
    @Operation(summary = "Danh sách RFQ được gán cho Planning",
            description = "Trả về danh sách RFQ mà Planning (header X-User-Id) được gán vào")
    @GetMapping("/for-planning")
    public List<RfqDto> listForPlanning(@RequestHeader("X-User-Id") Long userId) {
        return service.findByAssignedPlanning(userId).stream().map(mapper::toDto).collect(Collectors.toList());
    }

    // NEW: Danh sách RFQ DRAFT chưa được gán (dành cho Director để phân công)
    @Operation(summary = "Danh sách RFQ DRAFT chưa được gán",
            description = "Trả về danh sách RFQ ở trạng thái DRAFT mà Sales or Planning chưa được gán. Dành cho Director để phân công.")
    @GetMapping("/drafts/unassigned")
    public List<RfqDto> listDraftsUnassigned(@RequestHeader(value = "X-User-Id", required = false) Long directorUserId) {
        // Hiện chưa kiểm tra role thật sự của user; giả sử caller là director.
        return service.findDraftUnassigned().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    // 1) Sales tạo RFQ hộ khách không dùng hệ thống (tự assign bản thân)
    @Operation(summary = "Sales tạo RFQ thay khách hàng (không dùng hệ thống)",
            description = "Sales tự tạo RFQ bằng thông tin liên hệ khách (email/phone). Sales sẽ tự được gán vào RFQ. Nếu customer chưa tồn tại sẽ tự tạo mới.")
    @PostMapping("/by-sales")
    public RfqDto createBySales(
            @RequestHeader("X-User-Id") Long salesUserId,
            @RequestBody(description = "Payload tạo RFQ bởi Sales", required = true,
                    content = @Content(schema = @Schema(implementation = SalesRfqCreateRequest.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody SalesRfqCreateRequest body) {
        Rfq created = service.createBySales(body, salesUserId);
        return mapper.toDto(created);
    }

    // 2) Sales sửa thông tin RFQ + Customer trước khi preliminary-checked
    @Operation(summary = "Sales chỉnh sửa RFQ và thông tin khách (trước Preliminary Check)",
            description = "Chỉ cho phép khi RFQ chưa PRELIMINARY_CHECKED. Cập nhật các trường cơ bản của RFQ và Customer (tên, email, phone, địa chỉ, ghi chú)")
    @PatchMapping("/{id}/sales-edit")
    public RfqDto salesEditRfqAndCustomer(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long salesUserId,
            @RequestBody(description = "Payload chỉnh sửa RFQ + Customer", required = true,
                    content = @Content(schema = @Schema(implementation = SalesRfqEditRequest.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody SalesRfqEditRequest body) {
        Rfq updated = service.salesEditRfqAndCustomer(id, salesUserId, body);
        return mapper.toDto(updated);
    }

    @Operation(summary = "Đánh giá năng lực (Planning ghi kết quả)",
            description = "Planning gửi kết quả đánh giá năng lực: SUFFICIENT/INSUFFICIENT + lý do + ngày đề xuất mới")
    @PostMapping("/{id}/capacity-evaluate")
    public RfqDto capacityEvaluate(@Parameter(description = "ID RFQ") @PathVariable Long id,
                                   @RequestParam String status,
                                   @RequestParam(required = false) String reason,
                                   @RequestParam(required = false) java.time.LocalDate proposedNewDate) {
        service.persistCapacityEvaluation(id, status, reason, proposedNewDate);
        return mapper.toDto(service.findById(id));
    }

    // Assign Sales separately (DRAFT only)
    @Operation(summary = "Assign Sales for RFQ (DRAFT)", description = "Assign Sales by userId or employeeCode while RFQ is in DRAFT. Planning assignment not required here.")
    @PostMapping("/{id}/assign-sales")
    public RfqDto assignSales(
            @Parameter(description = "RFQ ID") @PathVariable Long id,
            @RequestBody(description = "Assign Sales payload", required = true,
                    content = @Content(schema = @Schema(implementation = AssignSalesRequest.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody AssignSalesRequest body) {
        Rfq updated = service.assignSales(id, body.getAssignedSalesId(), body.getEmployeeCode());
        return mapper.toDto(updated);
    }

    // Assign Planning separately (DRAFT only)
    @Operation(summary = "Assign Planning for RFQ (DRAFT)", description = "Assign Planning by userId while RFQ is in DRAFT. Sales assignment not required here.")
    @PostMapping("/{id}/assign-planning")
    public RfqDto assignPlanning(
            @Parameter(description = "RFQ ID") @PathVariable Long id,
            @RequestBody(description = "Assign Planning payload", required = true,
                    content = @Content(schema = @Schema(implementation = AssignPlanningRequest.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody AssignPlanningRequest body) {
        Rfq updated = service.assignPlanning(id, body.getAssignedPlanningId());
        return mapper.toDto(updated);
    }
}
