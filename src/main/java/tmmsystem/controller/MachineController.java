package tmmsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import tmmsystem.dto.MachineDto;
import tmmsystem.dto.machine.MachineRequest;
import tmmsystem.mapper.MachineMapper;
import tmmsystem.service.MachineService;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import tmmsystem.dto.PageResponse;

@RestController
@RequestMapping("/v1/machines")
@Validated
public class MachineController {
    private final MachineService service;
    private final MachineMapper mapper;

    public MachineController(MachineService service, MachineMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public PageResponse<MachineDto> list(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String search,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String type,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<tmmsystem.entity.Machine> machinePage = service.findAll(pageable, search, type, status);
        List<MachineDto> content = machinePage.getContent().stream().map(mapper::toDto).collect(Collectors.toList());
        return new PageResponse<>(content, machinePage.getNumber(), machinePage.getSize(),
                machinePage.getTotalElements(), machinePage.getTotalPages(), machinePage.isFirst(),
                machinePage.isLast());
    }

    @GetMapping("/{id}")
    public MachineDto get(@PathVariable Long id) {
        return mapper.toDto(service.findById(id));
    }

    @Operation(summary = "Tạo máy")
    @PostMapping
    public MachineDto create(
            @RequestBody(description = "Payload tạo máy", required = true, content = @Content(schema = @Schema(implementation = MachineRequest.class))) @Valid @org.springframework.web.bind.annotation.RequestBody MachineRequest body) {
        tmmsystem.entity.Machine e = new tmmsystem.entity.Machine();
        e.setCode(body.getCode());
        e.setName(body.getName());
        e.setType(body.getType());
        e.setStatus(body.getStatus() != null ? body.getStatus() : "AVAILABLE");
        e.setLocation(body.getLocation());
        e.setSpecifications(body.getSpecifications());
        e.setLastMaintenanceAt(body.getLastMaintenanceAt());
        e.setNextMaintenanceAt(body.getNextMaintenanceAt());
        e.setMaintenanceIntervalDays(
                body.getMaintenanceIntervalDays() != null ? body.getMaintenanceIntervalDays() : 90);
        return mapper.toDto(service.create(e));
    }

    @Operation(summary = "Cập nhật máy")
    @PutMapping("/{id}")
    public MachineDto update(
            @PathVariable Long id,
            @RequestBody(description = "Payload cập nhật máy", required = true, content = @Content(schema = @Schema(implementation = MachineRequest.class))) @Valid @org.springframework.web.bind.annotation.RequestBody MachineRequest body) {
        tmmsystem.entity.Machine e = new tmmsystem.entity.Machine();
        e.setCode(body.getCode());
        e.setName(body.getName());
        e.setType(body.getType());
        e.setStatus(body.getStatus());
        e.setLocation(body.getLocation());
        e.setSpecifications(body.getSpecifications());
        e.setLastMaintenanceAt(body.getLastMaintenanceAt());
        e.setNextMaintenanceAt(body.getNextMaintenanceAt());
        e.setMaintenanceIntervalDays(body.getMaintenanceIntervalDays());
        return mapper.toDto(service.update(id, e));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reset và đồng bộ trạng thái máy")
    @PostMapping("/reset-status")
    public ResponseEntity<?> resetStatus() {
        service.syncMachineStatuses();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Lấy lịch sử sử dụng máy")
    @GetMapping("/{id}/assignments")
    public ResponseEntity<org.springframework.data.domain.Page<tmmsystem.entity.MachineAssignment>> getAssignments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity
                .ok(service.getAssignments(id, org.springframework.data.domain.PageRequest.of(page, size)));
    }
}
