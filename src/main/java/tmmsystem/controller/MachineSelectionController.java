package tmmsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import tmmsystem.service.MachineSelectionService;
import tmmsystem.service.MachineSelectionService.MachineSuggestionDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/v1/machine-selection")
@Validated
@Tag(name = "Lựa chọn Máy móc Thông minh", description = "API lựa chọn máy móc thông minh và lập lịch sản xuất với phân tích khả dụng và công suất")
public class MachineSelectionController {

    private final MachineSelectionService machineSelectionService;

    public MachineSelectionController(MachineSelectionService machineSelectionService) {
        this.machineSelectionService = machineSelectionService;
    }

    @Operation(summary = "Lấy danh sách máy móc phù hợp cho công đoạn sản xuất",
               description = "Lấy danh sách máy móc phù hợp cho một công đoạn sản xuất cụ thể với phân tích khả dụng và công suất. Hệ thống sẽ tự động tính toán và xếp hạng các máy theo độ phù hợp")
    @GetMapping("/suitable-machines")
    public List<MachineSuggestionDto> getSuitableMachines(
            @io.swagger.v3.oas.annotations.Parameter(description = "Loại công đoạn sản xuất", required = true, 
                       example = "WARPING", 
                       schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {"WARPING", "WEAVING", "DYEING", "CUTTING", "SEWING", "PACKAGING"}))
            @RequestParam String stageType,
            @io.swagger.v3.oas.annotations.Parameter(description = "ID của sản phẩm", required = true, example = "1")
            @RequestParam Long productId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Số lượng sản phẩm cần sản xuất", required = true, example = "1000")
            @RequestParam BigDecimal requiredQuantity,
            @io.swagger.v3.oas.annotations.Parameter(description = "Thời gian bắt đầu mong muốn (tùy chọn)", example = "2025-10-28T08:00:00")
            @RequestParam(required = false) LocalDateTime preferredStartTime,
            @io.swagger.v3.oas.annotations.Parameter(description = "Thời gian kết thúc mong muốn (tùy chọn)", example = "2025-10-28T17:00:00")
            @RequestParam(required = false) LocalDateTime preferredEndTime) {
        
        // Set default time if not provided
        if (preferredStartTime == null) {
            preferredStartTime = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0);
        }
        if (preferredEndTime == null) {
            preferredEndTime = preferredStartTime.plusHours(8);
        }
        
        return machineSelectionService.getSuitableMachines(
            stageType, productId, requiredQuantity, preferredStartTime, preferredEndTime);
    }

    @Operation(summary = "Get machine suggestions for production plan stage",
               description = "Get intelligent machine suggestions when creating or updating a production plan stage")
    @PostMapping("/suggest-machines")
    public List<MachineSuggestionDto> suggestMachinesForStage(@RequestBody @Valid MachineSuggestionRequest request) {
        return machineSelectionService.getSuitableMachines(
            request.getStageType(),
            request.getProductId(),
            request.getRequiredQuantity(),
            request.getPreferredStartTime(),
            request.getPreferredEndTime()
        );
    }

    @Operation(summary = "Check machine availability",
               description = "Check if a specific machine is available during a time period")
    @GetMapping("/check-availability")
    public MachineAvailabilityResponse checkMachineAvailability(
            @RequestParam Long machineId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Thời gian bắt đầu",
                example = "2025-09-16 08:00",
                schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string"))
            @RequestParam LocalDateTime startTime,
            @io.swagger.v3.oas.annotations.Parameter(description = "Thời gian kết thúc",
                example = "17/09/2025 17:00",
                schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string"))
            @RequestParam LocalDateTime endTime) {
        
        // This would be implemented in MachineSelectionService
        return new MachineAvailabilityResponse();
    }

    // ===== Request/Response DTOs =====

    public static class MachineSuggestionRequest {
        private String stageType;
        private Long productId;
        private BigDecimal requiredQuantity;
        private LocalDateTime preferredStartTime;
        private LocalDateTime preferredEndTime;

        // Getters and Setters
        public String getStageType() { return stageType; }
        public void setStageType(String stageType) { this.stageType = stageType; }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public BigDecimal getRequiredQuantity() { return requiredQuantity; }
        public void setRequiredQuantity(BigDecimal requiredQuantity) { this.requiredQuantity = requiredQuantity; }

        public LocalDateTime getPreferredStartTime() { return preferredStartTime; }
        public void setPreferredStartTime(LocalDateTime preferredStartTime) { this.preferredStartTime = preferredStartTime; }

        public LocalDateTime getPreferredEndTime() { return preferredEndTime; }
        public void setPreferredEndTime(LocalDateTime preferredEndTime) { this.preferredEndTime = preferredEndTime; }
    }

    public static class MachineAvailabilityResponse {
        private boolean available;
        private String message;
        private List<String> conflicts;
        private LocalDateTime suggestedStartTime;
        private LocalDateTime suggestedEndTime;

        public MachineAvailabilityResponse() {
            this.available = true;
            this.message = "Machine is available";
            this.conflicts = List.of();
        }

        // Getters and Setters
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public List<String> getConflicts() { return conflicts; }
        public void setConflicts(List<String> conflicts) { this.conflicts = conflicts; }

        public LocalDateTime getSuggestedStartTime() { return suggestedStartTime; }
        public void setSuggestedStartTime(LocalDateTime suggestedStartTime) { this.suggestedStartTime = suggestedStartTime; }

        public LocalDateTime getSuggestedEndTime() { return suggestedEndTime; }
        public void setSuggestedEndTime(LocalDateTime suggestedEndTime) { this.suggestedEndTime = suggestedEndTime; }
    }
}
