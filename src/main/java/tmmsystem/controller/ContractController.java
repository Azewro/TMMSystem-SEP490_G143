package tmmsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import tmmsystem.dto.sales.ContractDto;
import tmmsystem.entity.Customer;
import tmmsystem.entity.Quotation;
import tmmsystem.entity.Contract;
import tmmsystem.entity.User;
import tmmsystem.mapper.ContractMapper;
import tmmsystem.service.ContractService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/contracts")
@Validated
public class ContractController {
    private final ContractService service;
    private final ContractMapper mapper;

    public ContractController(ContractService service, ContractMapper mapper) { this.service = service; this.mapper = mapper; }

    @GetMapping
    public List<ContractDto> list() { return service.findAll().stream().map(mapper::toDto).collect(Collectors.toList()); }

    @GetMapping("/{id}")
    public ContractDto get(@PathVariable Long id) { return mapper.toDto(service.findById(id)); }

    @Operation(summary = "Lấy chi tiết đơn hàng",
            description = "Lấy thông tin chi tiết đơn hàng bao gồm thông tin khách hàng và danh sách sản phẩm")
    @GetMapping("/{id}/order-details")
    public tmmsystem.dto.sales.OrderDetailsDto getOrderDetails(@Parameter(description = "ID hợp đồng") @PathVariable Long id) {
        return service.getOrderDetails(id);
    }

    @Operation(summary = "Tạo hợp đồng")
    @PostMapping
    public ContractDto create(
            @RequestBody(description = "Payload tạo hợp đồng", required = true,
                    content = @Content(schema = @Schema(implementation = ContractDto.class)))
            @org.springframework.web.bind.annotation.RequestBody ContractDto body) {
        Contract c = new Contract();
        c.setContractNumber(body.getContractNumber());
        if (body.getQuotationId() != null) { Quotation q = new Quotation(); q.setId(body.getQuotationId()); c.setQuotation(q); }
        if (body.getCustomerId() != null) { Customer cust = new Customer(); cust.setId(body.getCustomerId()); c.setCustomer(cust); }
        c.setContractDate(body.getContractDate());
        c.setDeliveryDate(body.getDeliveryDate());
        c.setTotalAmount(body.getTotalAmount());
        c.setFilePath(body.getFilePath());
        c.setStatus(body.getStatus());
        c.setDirectorApprovedAt(body.getDirectorApprovedAt());
        c.setDirectorApprovalNotes(body.getDirectorApprovalNotes());
        if (body.getCreatedById() != null) { User u = new User(); u.setId(body.getCreatedById()); c.setCreatedBy(u); }
        if (body.getApprovedById() != null) { User u = new User(); u.setId(body.getApprovedById()); c.setApprovedBy(u); }
        if (body.getAssignedSalesId() != null) { User u = new User(); u.setId(body.getAssignedSalesId()); c.setAssignedSales(u); }
        if (body.getAssignedPlanningId() != null) { User u = new User(); u.setId(body.getAssignedPlanningId()); c.setAssignedPlanning(u); }
        if (body.getSalesApprovedById() != null) { User u = new User(); u.setId(body.getSalesApprovedById()); c.setSalesApprovedBy(u); }
        if (body.getPlanningApprovedById() != null) { User u = new User(); u.setId(body.getPlanningApprovedById()); c.setPlanningApprovedBy(u); }
        c.setSalesApprovedAt(body.getSalesApprovedAt());
        c.setPlanningApprovedAt(body.getPlanningApprovedAt());
        return mapper.toDto(service.create(c));
    }

    @Operation(summary = "Cập nhật hợp đồng")
    @PutMapping("/{id}")
    public ContractDto update(
            @PathVariable Long id,
            @RequestBody(description = "Payload cập nhật hợp đồng", required = true,
                    content = @Content(schema = @Schema(implementation = ContractDto.class)))
            @org.springframework.web.bind.annotation.RequestBody ContractDto body) {
        Contract c = new Contract();
        c.setContractNumber(body.getContractNumber());
        if (body.getQuotationId() != null) { Quotation q = new Quotation(); q.setId(body.getQuotationId()); c.setQuotation(q); }
        if (body.getCustomerId() != null) { Customer cust = new Customer(); cust.setId(body.getCustomerId()); c.setCustomer(cust); }
        c.setContractDate(body.getContractDate());
        c.setDeliveryDate(body.getDeliveryDate());
        c.setTotalAmount(body.getTotalAmount());
        c.setFilePath(body.getFilePath());
        c.setStatus(body.getStatus());
        c.setDirectorApprovedAt(body.getDirectorApprovedAt());
        c.setDirectorApprovalNotes(body.getDirectorApprovalNotes());
        if (body.getCreatedById() != null) { User u = new User(); u.setId(body.getCreatedById()); c.setCreatedBy(u); }
        if (body.getApprovedById() != null) { User u = new User(); u.setId(body.getApprovedById()); c.setApprovedBy(u); }
        if (body.getAssignedSalesId() != null) { User u = new User(); u.setId(body.getAssignedSalesId()); c.setAssignedSales(u); }
        if (body.getAssignedPlanningId() != null) { User u = new User(); u.setId(body.getAssignedPlanningId()); c.setAssignedPlanning(u); }
        if (body.getSalesApprovedById() != null) { User u = new User(); u.setId(body.getSalesApprovedById()); c.setSalesApprovedBy(u); }
        if (body.getPlanningApprovedById() != null) { User u = new User(); u.setId(body.getPlanningApprovedById()); c.setPlanningApprovedBy(u); }
        c.setSalesApprovedAt(body.getSalesApprovedAt());
        c.setPlanningApprovedAt(body.getPlanningApprovedAt());
        return mapper.toDto(service.update(id, c));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { service.delete(id); }

    // ===== GIAI ĐOẠN 3: CONTRACT UPLOAD & APPROVAL =====
    
    @Operation(summary = "Upload hợp đồng đã ký",
            description = "Sale Staff upload bản hợp đồng đã ký lên hệ thống")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Upload thành công"),
            @ApiResponse(responseCode = "400", description = "File không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy hợp đồng")
    })
    @PostMapping(value = "/{id}/upload-signed", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ContractDto uploadSignedContract(
            @Parameter(description = "ID hợp đồng") @PathVariable Long id,
            @Parameter(description = "File hợp đồng đã ký", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String notes,
            @RequestParam Long saleUserId) {
        return mapper.toDto(service.uploadSignedContract(id, file, notes, saleUserId));
    }
    
    @Operation(summary = "Lấy hợp đồng chờ duyệt",
            description = "Lấy danh sách hợp đồng đang chờ duyệt")
    @GetMapping("/pending-approval")
    public List<ContractDto> getContractsPendingApproval() {
        return service.getContractsPendingApproval().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
    
    @Operation(summary = "Re-upload hợp đồng",
            description = "Sale Staff upload lại hợp đồng sau khi bị từ chối")
    @PostMapping(value = "/{id}/re-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ContractDto reUploadContract(
            @Parameter(description = "ID hợp đồng") @PathVariable Long id,
            @Parameter(description = "File hợp đồng đã ký", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String notes,
            @RequestParam Long saleUserId) {
        return mapper.toDto(service.uploadSignedContract(id, file, notes, saleUserId));
    }
    
    // Director APIs
    @Operation(summary = "Lấy hợp đồng chờ duyệt (Director)",
            description = "Director lấy danh sách hợp đồng chờ duyệt")
    @GetMapping("/director/pending")
    public List<ContractDto> getDirectorPendingContracts() {
        return service.getDirectorPendingContracts().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
    
    @Operation(summary = "Duyệt hợp đồng",
            description = "Director duyệt hợp đồng")
    @PostMapping("/{id}/approve")
    public ContractDto approveContract(
            @Parameter(description = "ID hợp đồng") @PathVariable Long id,
            @RequestParam Long directorId,
            @RequestParam(required = false) String notes) {
        return mapper.toDto(service.approveContract(id, directorId, notes));
    }
    
    @Operation(summary = "Test MinIO connection")
    @GetMapping("/test-minio")
    public ResponseEntity<String> testMinIO() {
        try {
            // Test MinIO connection by listing buckets
            return ResponseEntity.ok("MinIO connection successful");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("MinIO connection failed: " + e.getMessage());
        }
    }
    
    @Operation(summary = "Từ chối hợp đồng",
            description = "Director từ chối hợp đồng")
    @PostMapping("/{id}/reject")
    public ContractDto rejectContract(
            @Parameter(description = "ID hợp đồng") @PathVariable Long id,
            @RequestParam Long directorId,
            @RequestParam String rejectionNotes) {
        return mapper.toDto(service.rejectContract(id, directorId, rejectionNotes));
    }
    
    @Operation(summary = "Lấy URL file hợp đồng",
            description = "Lấy URL để download file hợp đồng")
    @GetMapping("/{id}/file-url")
    public String getContractFileUrl(@Parameter(description = "ID hợp đồng") @PathVariable Long id) {
        return service.getContractFileUrl(id);
    }
    
    @Operation(summary = "Download file hợp đồng",
            description = "Download file hợp đồng trực tiếp")
    @GetMapping(value = "/{id}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> downloadContractFile(
            @Parameter(description = "ID hợp đồng") @PathVariable Long id) {
        try {
            byte[] fileContent = service.downloadContractFile(id);
            String fileName = service.getContractFileName(id);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .body(fileContent);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // REPLACED: query by assigned sales/planning id instead of approved-by
    @Operation(summary = "Danh sách hợp đồng theo Sales được phân công")
    @GetMapping("/assigned/sales/{userId}")
    public List<ContractDto> getByAssignedSales(@PathVariable Long userId) {
        return service.findByAssignedSalesUserId(userId).stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Operation(summary = "Danh sách hợp đồng theo Planning được phân công")
    @GetMapping("/assigned/planning/{userId}")
    public List<ContractDto> getByAssignedPlanning(@PathVariable Long userId) {
        return service.findByAssignedPlanningUserId(userId).stream().map(mapper::toDto).collect(Collectors.toList());
    }
}
