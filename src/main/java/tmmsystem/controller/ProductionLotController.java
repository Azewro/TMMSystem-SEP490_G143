package tmmsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import tmmsystem.dto.ProductionLotDto;
import tmmsystem.dto.ProductionLotContractDto;
import tmmsystem.mapper.ProductionPlanMapper;
import tmmsystem.repository.ProductionLotRepository;
import tmmsystem.repository.ProductionPlanRepository;

@RestController
@RequestMapping("/v1/production-lots")
@Tag(name = "Production Lots", description = "API danh sách lô đã merge phục vụ lập kế hoạch")
public class ProductionLotController {
    private final ProductionLotRepository lotRepo;
    private final ProductionPlanRepository planRepo;
    private final ProductionPlanMapper mapper;

    public ProductionLotController(ProductionLotRepository lotRepo, ProductionPlanRepository planRepo, ProductionPlanMapper mapper){
        this.lotRepo = lotRepo; this.planRepo = planRepo; this.mapper = mapper;
    }

    @Operation(summary = "Lấy danh sách lot", description = "Filter theo status nếu truyền, ví dụ READY_FOR_PLANNING")
    @GetMapping
    public List<ProductionLotDto> list(@RequestParam(required=false) String status){
        var lots = status==null? lotRepo.findAll(): lotRepo.findByStatus(status);
        var dtos = lots.stream().map(mapper::toDto).toList();
        // fill current plan info
        for (var dto : dtos){
            var plans = planRepo.findByLotId(dto.getId());
            var current = plans.stream().filter(p->Boolean.TRUE.equals(p.getCurrentVersion())).findFirst().orElse(null);
            if (current!=null){ dto.setCurrentPlanId(current.getId()); dto.setCurrentPlanStatus(current.getStatus()!=null? current.getStatus().name(): null); }
        }
        return dtos;
    }

    @Operation(summary = "Lấy chi tiết một lot", description = "Trả về thông tin lot, danh sách đơn liên quan và trạng thái kế hoạch hiện tại")
    @GetMapping("/{id}")
    public ProductionLotDto get(@PathVariable Long id){
        var lot = lotRepo.findById(id).orElseThrow(() -> new RuntimeException("Lot not found"));
        var dto = mapper.toDto(lot);
        var plans = planRepo.findByLotId(id);
        var current = plans.stream().filter(p->Boolean.TRUE.equals(p.getCurrentVersion())).findFirst().orElse(null);
        if (current!=null){ dto.setCurrentPlanId(current.getId()); dto.setCurrentPlanStatus(current.getStatus()!=null? current.getStatus().name(): null); }
        return dto;
    }

    @Operation(summary = "Lấy danh sách hợp đồng đã merge", description = "Trả về chi tiết các contract nằm trong lot với số lượng đã phân bổ")
    @GetMapping("/{id}/contracts")
    public List<ProductionLotContractDto> mergedContracts(@PathVariable Long id){
        var lot = lotRepo.findById(id).orElseThrow(() -> new RuntimeException("Lot not found"));
        var dto = mapper.toDto(lot);
        return dto.getMergedContracts();
    }
}
