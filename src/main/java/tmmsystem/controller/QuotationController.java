package tmmsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import tmmsystem.dto.sales.QuotationDto;
import tmmsystem.dto.sales.CreateQuotationRequest;
import tmmsystem.dto.sales.PriceCalculationDto;
import tmmsystem.dto.sales.RecalculatePriceRequest;

import tmmsystem.entity.Customer;
import tmmsystem.entity.Quotation;
import tmmsystem.entity.Rfq;
import tmmsystem.entity.User;
import tmmsystem.mapper.QuotationMapper;
import tmmsystem.service.QuotationService;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/quotations")
@Validated
public class QuotationController {
    private final QuotationService service;
    private final QuotationMapper mapper;

    public QuotationController(QuotationService service, QuotationMapper mapper) { this.service = service; this.mapper = mapper; }

    @Operation(summary = "Danh sách báo giá",
            description = "Trả về danh sách tất cả báo giá trong hệ thống")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Danh sách báo giá",
                    content = @Content(schema = @Schema(implementation = QuotationDto.class)))
    })
    @GetMapping
    public List<QuotationDto> list() { return service.findAll().stream().map(mapper::toDto).collect(Collectors.toList()); }

    @Operation(summary = "Lấy chi tiết báo giá",
            description = "Lấy thông tin chi tiết của 1 báo giá theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chi tiết báo giá",
                    content = @Content(schema = @Schema(implementation = QuotationDto.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy báo giá")
    })
    @GetMapping("/{id}")
    public QuotationDto get(@Parameter(description = "ID báo giá") @PathVariable Long id) { 
        return mapper.toDto(service.findById(id)); 
    }

    @Operation(summary = "Tạo báo giá",
            description = "Tạo báo giá mới (thường được tạo tự động từ RFQ)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Báo giá đã được tạo",
                    content = @Content(schema = @Schema(implementation = QuotationDto.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    @PostMapping
    public QuotationDto create(
            @RequestBody(description = "Payload tạo báo giá", required = true,
                    content = @Content(schema = @Schema(implementation = QuotationDto.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody QuotationDto body) {
        Quotation q = new Quotation();
        q.setQuotationNumber(body.getQuotationNumber());
        if (body.getRfqId() != null) { Rfq r = new Rfq(); r.setId(body.getRfqId()); q.setRfq(r); }
        if (body.getCustomerId() != null) { Customer c = new Customer(); c.setId(body.getCustomerId()); q.setCustomer(c); }
        q.setValidUntil(body.getValidUntil());
        q.setTotalAmount(body.getTotalAmount());
        q.setStatus(body.getStatus());
        q.setAccepted(body.getIsAccepted());
        q.setCanceled(body.getIsCanceled());
        if (body.getCapacityCheckedById() != null) { User u = new User(); u.setId(body.getCapacityCheckedById()); q.setCapacityCheckedBy(u); }
        q.setCapacityCheckedAt(body.getCapacityCheckedAt());
        q.setCapacityCheckNotes(body.getCapacityCheckNotes());
        if (body.getAssignedSalesId() != null) { User u = new User(); u.setId(body.getAssignedSalesId()); q.setAssignedSales(u); }
        if (body.getAssignedPlanningId() != null) { User u = new User(); u.setId(body.getAssignedPlanningId()); q.setAssignedPlanning(u); }
        if (body.getCreatedById() != null) { User u = new User(); u.setId(body.getCreatedById()); q.setCreatedBy(u); }
        if (body.getApprovedById() != null) { User u = new User(); u.setId(body.getApprovedById()); q.setApprovedBy(u); }
        q.setSentAt(body.getSentAt());
        q.setFilePath(body.getFilePath());
        // contact snapshots
        q.setContactPersonSnapshot(body.getContactPersonSnapshot());
        q.setContactEmailSnapshot(body.getContactEmailSnapshot());
        q.setContactPhoneSnapshot(body.getContactPhoneSnapshot());
        q.setContactAddressSnapshot(body.getContactAddressSnapshot());
        q.setContactMethod(body.getContactMethod());
        return mapper.toDto(service.create(q));
    }

    @Operation(summary = "Cập nhật báo giá")
    @PutMapping("/{id}")
    public QuotationDto update(
            @PathVariable Long id,
            @RequestBody(description = "Payload cập nhật báo giá", required = true,
                    content = @Content(schema = @Schema(implementation = QuotationDto.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody QuotationDto body) {
        Quotation q = new Quotation();
        q.setQuotationNumber(body.getQuotationNumber());
        if (body.getRfqId() != null) { Rfq r = new Rfq(); r.setId(body.getRfqId()); q.setRfq(r); }
        if (body.getCustomerId() != null) { Customer c = new Customer(); c.setId(body.getCustomerId()); q.setCustomer(c); }
        q.setValidUntil(body.getValidUntil());
        q.setTotalAmount(body.getTotalAmount());
        q.setStatus(body.getStatus());
        q.setAccepted(body.getIsAccepted());
        q.setCanceled(body.getIsCanceled());
        if (body.getCapacityCheckedById() != null) { User u = new User(); u.setId(body.getCapacityCheckedById()); q.setCapacityCheckedBy(u); }
        q.setCapacityCheckedAt(body.getCapacityCheckedAt());
        q.setCapacityCheckNotes(body.getCapacityCheckNotes());
        if (body.getAssignedSalesId() != null) { User u = new User(); u.setId(body.getAssignedSalesId()); q.setAssignedSales(u); }
        if (body.getAssignedPlanningId() != null) { User u = new User(); u.setId(body.getAssignedPlanningId()); q.setAssignedPlanning(u); }
        if (body.getCreatedById() != null) { User u = new User(); u.setId(body.getCreatedById()); q.setCreatedBy(u); }
        if (body.getApprovedById() != null) { User u = new User(); u.setId(body.getApprovedById()); q.setApprovedBy(u); }
        q.setSentAt(body.getSentAt());
        q.setFilePath(body.getFilePath());
        // contact snapshots
        q.setContactPersonSnapshot(body.getContactPersonSnapshot());
        q.setContactEmailSnapshot(body.getContactEmailSnapshot());
        q.setContactPhoneSnapshot(body.getContactPhoneSnapshot());
        q.setContactAddressSnapshot(body.getContactAddressSnapshot());
        q.setContactMethod(body.getContactMethod());
        return mapper.toDto(service.update(id, q));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { service.delete(id); }

    // Planning Department APIs
    @Operation(summary = "Tính giá báo giá từ RFQ",
            description = "Planning Department tính giá báo giá từ RFQ để xem trước trước khi tạo báo giá chính thức")
    @PostMapping("/calculate-price")
    public PriceCalculationDto calculatePrice(
            @RequestBody(description = "Thông tin tính giá", required = true,
                    content = @Content(schema = @Schema(implementation = CreateQuotationRequest.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody CreateQuotationRequest request) {
        return service.calculateQuotationPrice(
                request.getRfqId(), 
                request.getProfitMargin());
    }
    
    @Operation(summary = "Tính lại giá khi thay đổi profit margin",
            description = "Tính lại giá báo giá khi user thay đổi % lợi nhuận, trả về tổng giá mới")
    @PostMapping("/recalculate-price")
    public PriceCalculationDto recalculatePrice(
            @RequestBody(description = "Thông tin tính lại giá", required = true,
                    content = @Content(schema = @Schema(implementation = RecalculatePriceRequest.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody RecalculatePriceRequest request) {
        return service.recalculateQuotationPrice(
                request.getRfqId(), 
                request.getProfitMargin());
    }
    
    @Operation(summary = "Planning tạo báo giá từ RFQ",
            description = "Planning Department tạo báo giá từ RFQ đã nhận, tự động tính giá theo công thức với lợi nhuận có thể thay đổi")
    @PostMapping("/create-from-rfq")
    public QuotationDto createFromRfq(
            @RequestBody(description = "Thông tin tạo báo giá", required = true,
                    content = @Content(schema = @Schema(implementation = CreateQuotationRequest.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody CreateQuotationRequest request) {
        return mapper.toDto(service.createQuotationFromRfq(
                request.getRfqId(), 
                request.getPlanningUserId(), 
                request.getProfitMargin(),
                request.getCapacityCheckNotes()));
    }

    // Sale Staff APIs
    @Operation(summary = "Lấy báo giá chờ gửi",
            description = "Sale Staff lấy danh sách báo giá đã tạo, chờ gửi cho khách hàng. Nếu header X-User-Id được cung cấp thì chỉ trả về báo giá được gán cho Sales đó.")
    @GetMapping("/pending")
    public List<QuotationDto> getPendingQuotations(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId != null) {
            return service.findPendingQuotationsByAssignedSales(userId).stream().map(mapper::toDto).collect(Collectors.toList());
        }
        return service.findPendingQuotations().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Operation(summary = "Gửi báo giá cho khách hàng",
            description = "Sale Staff gửi báo giá cho khách hàng, hệ thống sẽ gửi thông báo")
    @PostMapping("/{id}/send-to-customer")
    public QuotationDto sendToCustomer(@Parameter(description = "ID Quotation") @PathVariable Long id) {
        return mapper.toDto(service.sendQuotationToCustomer(id));
    }

    // NEW: List quotations assigned to a specific Sales
    @Operation(summary = "Danh sách báo giá theo Sales được phân công",
            description = "Trả về danh sách báo giá mà Sales (header X-User-Id) được gán vào")
    @GetMapping("/for-sales")
    public List<QuotationDto> listForSales(@RequestHeader("X-User-Id") Long userId) {
        return service.findAll().stream()
                .filter(q -> q.getAssignedSales() != null && userId.equals(q.getAssignedSales().getId()))
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    // NEW: List quotations assigned to a specific Planning
    @Operation(summary = "Danh sách báo giá theo Planning được phân công",
            description = "Trả về danh sách báo giá mà Planning (header X-User-Id) được gán vào")
    @GetMapping("/for-planning")
    public List<QuotationDto> listForPlanning(@RequestHeader("X-User-Id") Long userId) {
        return service.findAll().stream()
                .filter(q -> q.getAssignedPlanning() != null && userId.equals(q.getAssignedPlanning().getId()))
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    // Customer APIs
    @Operation(summary = "Lấy báo giá của khách hàng",
            description = "Customer lấy danh sách báo giá của mình")
    @GetMapping("/customer/{customerId}")
    public List<QuotationDto> getCustomerQuotations(@Parameter(description = "ID Customer") @PathVariable Long customerId) {
        return service.findQuotationsByCustomer(customerId).stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Operation(summary = "Duyệt báo giá",
            description = "Customer duyệt báo giá, hệ thống sẽ tạo đơn hàng tự động")
    @PostMapping("/{id}/approve")
    public QuotationDto approveQuotation(@Parameter(description = "ID Quotation") @PathVariable Long id) {
        return mapper.toDto(service.approveQuotation(id));
    }

    @Operation(summary = "Từ chối báo giá",
            description = "Customer từ chối báo giá, kết thúc quy trình")
    @PostMapping("/{id}/reject")
    public QuotationDto rejectQuotation(@Parameter(description = "ID Quotation") @PathVariable Long id) {
        return mapper.toDto(service.rejectQuotation(id));
    }

    @Operation(summary = "Tạo đơn hàng từ báo giá",
            description = "Hệ thống tự động tạo đơn hàng (Contract) khi Customer duyệt báo giá")
    @PostMapping("/{id}/create-order")
    public Object createOrderFromQuotation(@Parameter(description = "ID Quotation") @PathVariable Long id) {
        return service.createOrderFromQuotation(id);
    }

    @Operation(summary = "Upload báo giá đã ký",
            description = "Sale upload file PDF báo giá đã ký")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Upload thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy báo giá")
    })
    @PostMapping(value = "/{id}/upload-signed", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public QuotationDto uploadSignedQuotation(
            @Parameter(description = "ID báo giá") @PathVariable Long id,
            @Parameter(description = "File báo giá đã ký", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
            @RequestParam("file") MultipartFile file) {
        return mapper.toDto(service.attachQuotationFile(id, file));
    }

    // NEW: Get URL of signed quotation file
    @Operation(summary = "Lấy URL file báo giá",
            description = "Lấy URL để download file báo giá đã ký")
    @GetMapping("/{id}/file-url")
    public String getQuotationFileUrl(@Parameter(description = "ID báo giá") @PathVariable Long id) {
        return service.getQuotationFileUrl(id);
    }

    // NEW: Download signed quotation file directly
    @Operation(summary = "Download báo giá đã ký",
            description = "Tải trực tiếp file báo giá đã ký")
    @GetMapping(value = "/{id}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> downloadSignedQuotation(
            @Parameter(description = "ID báo giá") @PathVariable Long id) {
        try {
            byte[] fileContent = service.downloadQuotationFile(id);
            String fileName = service.getQuotationFileName(id);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .body(fileContent);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
