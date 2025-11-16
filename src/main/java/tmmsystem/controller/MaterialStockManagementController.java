package tmmsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import tmmsystem.dto.PageResponse;
import tmmsystem.dto.inventory.MaterialStockDto;
import tmmsystem.entity.Material;
import tmmsystem.entity.MaterialStock;
import tmmsystem.mapper.InventoryMapper;
import tmmsystem.service.MaterialStockManagementService;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/production/material-stock")
@Tag(name = "Material Stock Management", description = "API quản lý nhập kho nguyên liệu cho Production Manager")
public class MaterialStockManagementController {
    private final MaterialStockManagementService service;
    private final InventoryMapper mapper;

    public MaterialStockManagementController(MaterialStockManagementService service, InventoryMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Operation(summary = "Danh sách nhập kho nguyên liệu",
            description = "Lấy danh sách nhập kho nguyên liệu với pagination, search và filter theo ngày nhập")
    @GetMapping
    public PageResponse<MaterialStockDto> list(
            @Parameter(description = "Số trang (bắt đầu từ 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số lượng bản ghi mỗi trang") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Tìm kiếm theo tên nguyên liệu, mã nguyên liệu") 
            @RequestParam(required = false) String search,
            @Parameter(description = "Lọc theo ngày nhập hàng (yyyy-MM-dd)") 
            @RequestParam(required = false) String receivedDate) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedDate", "lastUpdatedAt"));
        
        LocalDate receivedDateLocal = null;
        if (receivedDate != null && !receivedDate.trim().isEmpty()) {
            try {
                receivedDateLocal = LocalDate.parse(receivedDate);
            } catch (Exception e) {
                // Ignore invalid date format
            }
        }
        
        Page<MaterialStock> stockPage = service.findAll(pageable, search, receivedDateLocal);
        List<MaterialStockDto> content = stockPage.getContent().stream()
            .map(mapper::toDto)
            .collect(Collectors.toList());
        
        return new PageResponse<>(content, stockPage.getNumber(), stockPage.getSize(), 
                stockPage.getTotalElements(), stockPage.getTotalPages(), 
                stockPage.isFirst(), stockPage.isLast());
    }

    @Operation(summary = "Lấy chi tiết nhập kho nguyên liệu",
            description = "Lấy thông tin chi tiết của một lần nhập kho nguyên liệu")
    @GetMapping("/{id}")
    public MaterialStockDto get(
            @Parameter(description = "ID nhập kho") @PathVariable Long id) {
        return mapper.toDto(service.findById(id));
    }

    @Operation(summary = "Tạo nhập kho nguyên liệu mới",
            description = "Production Manager tạo bản ghi nhập kho nguyên liệu mới")
    @PostMapping
    public MaterialStockDto create(
            @RequestBody MaterialStockDto body) {
        MaterialStock stock = new MaterialStock();
        if (body.getMaterialId() != null) {
            Material material = new Material();
            material.setId(body.getMaterialId());
            stock.setMaterial(material);
        }
        stock.setQuantity(body.getQuantity());
        stock.setUnit(body.getUnit());
        stock.setUnitPrice(body.getUnitPrice());
        stock.setLocation(body.getLocation());
        stock.setBatchNumber(body.getBatchNumber());
        stock.setReceivedDate(body.getReceivedDate());
        stock.setExpiryDate(body.getExpiryDate());
        
        return mapper.toDto(service.create(stock));
    }

    @Operation(summary = "Cập nhật nhập kho nguyên liệu",
            description = "Production Manager cập nhật thông tin nhập kho nguyên liệu")
    @PutMapping("/{id}")
    public MaterialStockDto update(
            @Parameter(description = "ID nhập kho") @PathVariable Long id,
            @RequestBody MaterialStockDto body) {
        MaterialStock stock = new MaterialStock();
        if (body.getMaterialId() != null) {
            Material material = new Material();
            material.setId(body.getMaterialId());
            stock.setMaterial(material);
        }
        stock.setQuantity(body.getQuantity());
        stock.setUnit(body.getUnit());
        stock.setUnitPrice(body.getUnitPrice());
        stock.setLocation(body.getLocation());
        stock.setBatchNumber(body.getBatchNumber());
        stock.setReceivedDate(body.getReceivedDate());
        stock.setExpiryDate(body.getExpiryDate());
        
        return mapper.toDto(service.update(id, stock));
    }

    @Operation(summary = "Xóa nhập kho nguyên liệu",
            description = "Production Manager xóa bản ghi nhập kho nguyên liệu")
    @DeleteMapping("/{id}")
    public void delete(
            @Parameter(description = "ID nhập kho") @PathVariable Long id) {
        service.delete(id);
    }
}

