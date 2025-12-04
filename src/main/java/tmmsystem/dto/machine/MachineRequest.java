package tmmsystem.dto.machine;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tmmsystem.validation.MachineCode;

@Schema(name = "MachineRequest")
public class MachineRequest {
    @NotBlank(message = "Mã máy là bắt buộc")
    @MachineCode
    @Size(max = 50, message = "Mã máy không được quá 50 ký tự")
    @Schema(description = "Mã máy", example = "WEAVE-01", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @NotBlank(message = "Tên máy là bắt buộc")
    @Size(min = 2, max = 100, message = "Tên máy phải có ít nhất 2 ký tự và không được quá 100 ký tự")
    @Schema(description = "Tên máy", example = "Weaving Machine #1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "Loại máy là bắt buộc")
    @Size(max = 20, message = "Loại máy không được quá 20 ký tự")
    @Schema(description = "Loại", example = "WEAVING", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

    @Size(max = 20)
    @Schema(description = "Trạng thái", example = "AVAILABLE")
    private String status;

    @NotBlank(message = "Vị trí là bắt buộc")
    @Size(min = 2, max = 50, message = "Vị trí phải có ít nhất 2 ký tự và không được quá 50 ký tự")
    @Schema(description = "Vị trí", example = "A1")
    private String location;

    @Schema(description = "Thông số (JSON)", example = "{\"brand\":\"TMMS\",\"power\":\"5kW\",\"modelYear\":2022,\"capacityPerDay\":50}")
    private String specifications;

    @Schema(description = "Ngày bảo trì gần nhất")
    private java.time.Instant lastMaintenanceAt;

    @Schema(description = "Ngày bảo trì kế tiếp")
    private java.time.Instant nextMaintenanceAt;

    @NotNull(message = "Chu kỳ bảo trì là bắt buộc")
    @Min(value = 1, message = "Chu kỳ bảo trì phải lớn hơn 0")
    @Max(value = 3650, message = "Chu kỳ bảo trì không được quá 3650 ngày (10 năm)")
    @Schema(description = "Chu kỳ bảo trì (ngày)", example = "90")
    private Integer maintenanceIntervalDays;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getSpecifications() { return specifications; }
    public void setSpecifications(String specifications) { this.specifications = specifications; }
    public java.time.Instant getLastMaintenanceAt() { return lastMaintenanceAt; }
    public void setLastMaintenanceAt(java.time.Instant lastMaintenanceAt) { this.lastMaintenanceAt = lastMaintenanceAt; }
    public java.time.Instant getNextMaintenanceAt() { return nextMaintenanceAt; }
    public void setNextMaintenanceAt(java.time.Instant nextMaintenanceAt) { this.nextMaintenanceAt = nextMaintenanceAt; }
    public Integer getMaintenanceIntervalDays() { return maintenanceIntervalDays; }
    public void setMaintenanceIntervalDays(Integer maintenanceIntervalDays) { this.maintenanceIntervalDays = maintenanceIntervalDays; }
}


