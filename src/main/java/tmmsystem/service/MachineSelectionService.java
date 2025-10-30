package tmmsystem.service;

import org.springframework.stereotype.Service;
import tmmsystem.entity.*;
import tmmsystem.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MachineSelectionService {
    
    private final MachineRepository machineRepository;
    private final MachineAssignmentRepository machineAssignmentRepository;
    private final MachineMaintenanceRepository machineMaintenanceRepository;
    private final ProductionPlanStageRepository productionPlanStageRepository;
    private final WorkOrderRepository workOrderRepository;
    private final ProductionStageRepository productionStageRepository;
    private final ProductRepository productRepository;
    
    public MachineSelectionService(MachineRepository machineRepository,
                                  MachineAssignmentRepository machineAssignmentRepository,
                                  MachineMaintenanceRepository machineMaintenanceRepository,
                                  ProductionPlanStageRepository productionPlanStageRepository,
                                  WorkOrderRepository workOrderRepository,
                                  ProductionStageRepository productionStageRepository,
                                  ProductRepository productRepository) {
        this.machineRepository = machineRepository;
        this.machineAssignmentRepository = machineAssignmentRepository;
        this.machineMaintenanceRepository = machineMaintenanceRepository;
        this.productionPlanStageRepository = productionPlanStageRepository;
        this.workOrderRepository = workOrderRepository;
        this.productionStageRepository = productionStageRepository;
        this.productRepository = productRepository;
    }
    
    /**
     * Lấy danh sách máy phù hợp với công đoạn
     */
    public List<MachineSuggestionDto> getSuitableMachines(String stageType, 
                                                          Long productId, 
                                                          BigDecimal requiredQuantity,
                                                          LocalDateTime preferredStartTime,
                                                          LocalDateTime preferredEndTime) {
        
        // 1. Xử lý các trường hợp đặc biệt không cần máy
        if ("DYEING".equals(stageType)) {
            // Công đoạn nhuộm: outsourced, không cần máy nội bộ
            return createOutsourcedStageSuggestion(stageType, productId, requiredQuantity, preferredStartTime, preferredEndTime);
        }
        
        if ("PACKAGING".equals(stageType)) {
            // Công đoạn đóng gói: làm thủ công, không cần máy
            return createManualStageSuggestion(stageType, productId, requiredQuantity, preferredStartTime, preferredEndTime);
        }
        
        // 2. Lọc máy theo loại công đoạn cho các công đoạn cần máy
        List<Machine> suitableMachines = machineRepository.findAll().stream()
            .filter(machine -> stageType.equals(machine.getType()))
            .filter(machine -> "AVAILABLE".equals(machine.getStatus()))
            .collect(Collectors.toList());
        
        List<MachineSuggestionDto> suggestions = new ArrayList<>();
        
        for (Machine machine : suitableMachines) {
            MachineSuggestionDto suggestion = new MachineSuggestionDto();
            suggestion.setMachineId(machine.getId());
            suggestion.setMachineCode(machine.getCode());
            suggestion.setMachineName(machine.getName());
            suggestion.setMachineType(machine.getType());
            suggestion.setLocation(machine.getLocation());
            
        // 2. Tính toán năng suất và thời gian cần thiết
        MachineCapacityInfo capacityInfo = calculateMachineCapacity(machine, productId, requiredQuantity);
        suggestion.setCapacityPerHour(capacityInfo.getCapacityPerHour());
        suggestion.setEstimatedDurationHours(capacityInfo.getEstimatedDurationHours());
        suggestion.setCanHandleQuantity(capacityInfo.isCanHandleQuantity());
            
            // 3. Kiểm tra khả năng sẵn sàng trong khoảng thời gian
            AvailabilityInfo availabilityInfo = checkMachineAvailability(machine, preferredStartTime, preferredEndTime);
            suggestion.setAvailable(availabilityInfo.isAvailable());
            suggestion.setAvailabilityScore(availabilityInfo.getScore());
            suggestion.setConflicts(availabilityInfo.getConflicts());
            suggestion.setSuggestedStartTime(availabilityInfo.getSuggestedStartTime());
            suggestion.setSuggestedEndTime(availabilityInfo.getSuggestedEndTime());
            
            // 4. Tính điểm ưu tiên tổng thể
            suggestion.setPriorityScore(calculatePriorityScore(suggestion, stageType, requiredQuantity));
            
            suggestions.add(suggestion);
        }
        
        // 5. Sắp xếp theo điểm ưu tiên
        suggestions.sort((a, b) -> Double.compare(b.getPriorityScore(), a.getPriorityScore()));
        
        return suggestions;
    }
    
    /**
     * Tạo gợi ý cho công đoạn outsourced (nhuộm)
     */
    private List<MachineSuggestionDto> createOutsourcedStageSuggestion(String stageType, Long productId, 
                                                                       BigDecimal requiredQuantity,
                                                                       LocalDateTime preferredStartTime, 
                                                                       LocalDateTime preferredEndTime) {
        MachineSuggestionDto suggestion = new MachineSuggestionDto();
        suggestion.setMachineId(null); // Không có máy nội bộ
        suggestion.setMachineCode("OUTSOURCE-DYEING");
        suggestion.setMachineName("Nhà cung cấp nhuộm bên ngoài");
        suggestion.setMachineType(stageType);
        suggestion.setLocation("Outsourced");
        suggestion.setCapacityPerHour(new BigDecimal("999999")); // Công suất vô hạn
        suggestion.setEstimatedDurationHours(new BigDecimal("24")); // Ước tính 1 ngày
        suggestion.setCanHandleQuantity(true);
        suggestion.setAvailable(true);
        suggestion.setAvailabilityScore(100.0);
        suggestion.setSuggestedStartTime(preferredStartTime);
        suggestion.setSuggestedEndTime(preferredEndTime.plusDays(1)); // Thêm 1 ngày cho outsourced
        suggestion.setConflicts(List.of("Cần liên hệ nhà cung cấp nhuộm trước"));
        suggestion.setPriorityScore(90.0); // Điểm cao vì luôn khả dụng
        
        return List.of(suggestion);
    }
    
    /**
     * Tạo gợi ý cho công đoạn làm thủ công (đóng gói)
     */
    private List<MachineSuggestionDto> createManualStageSuggestion(String stageType, Long productId,
                                                                   BigDecimal requiredQuantity,
                                                                   LocalDateTime preferredStartTime,
                                                                       LocalDateTime preferredEndTime) {
        MachineSuggestionDto suggestion = new MachineSuggestionDto();
        suggestion.setMachineId(null); // Không có máy
        suggestion.setMachineCode("MANUAL-PACKAGING");
        suggestion.setMachineName("Công nhân đóng gói");
        suggestion.setMachineType(stageType);
        suggestion.setLocation("Khu đóng gói");
        suggestion.setCapacityPerHour(new BigDecimal("500")); // 500 sản phẩm/giờ
        suggestion.setEstimatedDurationHours(requiredQuantity.divide(new BigDecimal("500"), 2, java.math.RoundingMode.HALF_UP));
        suggestion.setCanHandleQuantity(true);
        suggestion.setAvailable(true);
        suggestion.setAvailabilityScore(100.0);
        suggestion.setSuggestedStartTime(preferredStartTime);
        suggestion.setSuggestedEndTime(preferredStartTime.plusHours(suggestion.getEstimatedDurationHours().intValue()));
        suggestion.setConflicts(List.of("Cần đảm bảo đủ nhân công"));
        suggestion.setPriorityScore(85.0);
        
        return List.of(suggestion);
    }
    
    /**
     * Tính toán năng suất máy cho sản phẩm cụ thể
     */
    private MachineCapacityInfo calculateMachineCapacity(Machine machine, Long productId, BigDecimal requiredQuantity) {
        MachineCapacityInfo info = new MachineCapacityInfo();
        
        // Lấy thông tin sản phẩm
        Product product = getProductById(productId);
        if (product == null) {
            info.setCapacityPerHour(BigDecimal.ZERO);
            info.setEstimatedDurationHours(BigDecimal.ZERO);
            info.setCanHandleQuantity(false);
            return info;
        }
        
        // Parse specifications JSON để lấy capacityPerHour
        String specs = machine.getSpecifications();
        BigDecimal capacityPerHour = extractCapacityFromSpecs(specs, product);
        
        if (capacityPerHour.compareTo(BigDecimal.ZERO) > 0) {
            info.setCapacityPerHour(capacityPerHour);
            
            // Tính thời gian cần thiết
            BigDecimal estimatedHours = requiredQuantity.divide(capacityPerHour, 2, java.math.RoundingMode.HALF_UP);
            info.setEstimatedDurationHours(estimatedHours);
            
            // Kiểm tra máy có thể xử lý số lượng này không
            BigDecimal maxDailyCapacity = capacityPerHour.multiply(BigDecimal.valueOf(8)); // 8 giờ/ngày
            info.setCanHandleQuantity(requiredQuantity.compareTo(maxDailyCapacity) <= 0);
        } else {
            // Sử dụng capacity mặc định dựa trên loại máy
            BigDecimal defaultCapacity = getDefaultCapacityForMachineType(machine.getType());
            info.setCapacityPerHour(defaultCapacity);
            
            BigDecimal estimatedHours = requiredQuantity.divide(defaultCapacity, 2, java.math.RoundingMode.HALF_UP);
            info.setEstimatedDurationHours(estimatedHours);
            info.setCanHandleQuantity(true);
        }
        
        return info;
    }
    
    /**
     * Kiểm tra khả năng sẵn sàng của máy trong khoảng thời gian
     */
    private AvailabilityInfo checkMachineAvailability(Machine machine, 
                                                      LocalDateTime startTime, 
                                                      LocalDateTime endTime) {
        AvailabilityInfo info = new AvailabilityInfo();
        
        // 1. Kiểm tra bảo trì máy
        List<MachineMaintenance> maintenanceSchedules = machineMaintenanceRepository.findAll().stream()
            .filter(maintenance -> machine.getId().equals(maintenance.getMachine().getId()))
            .filter(maintenance -> "IN_PROGRESS".equals(maintenance.getStatus()) || 
                                  (maintenance.getStartedAt() != null && maintenance.getCompletedAt() == null))
            .collect(Collectors.toList());
        
        for (MachineMaintenance maintenance : maintenanceSchedules) {
            LocalDateTime maintenanceStart = maintenance.getStartedAt() != null ? 
                maintenance.getStartedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null;
            LocalDateTime maintenanceEnd = maintenance.getCompletedAt() != null ? 
                maintenance.getCompletedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null;
                
            if (isTimeOverlap(startTime, endTime, maintenanceStart, maintenanceEnd)) {
                info.setAvailable(false);
                info.getConflicts().add("Máy đang bảo trì từ " + maintenanceStart + " đến " + maintenanceEnd);
                return info;
            }
        }
        
        // 2. Kiểm tra ProductionPlanStage đã được gán
        List<ProductionPlanStage> assignedStages = productionPlanStageRepository.findAll().stream()
            .filter(stage -> stage.getAssignedMachine() != null)
            .filter(stage -> machine.getId().equals(stage.getAssignedMachine().getId()))
            .filter(stage -> isTimeOverlap(startTime, endTime, stage.getPlannedStartTime(), stage.getPlannedEndTime()))
            .collect(Collectors.toList());
        
        for (ProductionPlanStage stage : assignedStages) {
            info.setAvailable(false);
            info.getConflicts().add("Máy đã được gán cho kế hoạch sản xuất từ " + 
                stage.getPlannedStartTime() + " đến " + stage.getPlannedEndTime());
        }
        
        // 3. Kiểm tra WorkOrder đang chạy
        List<WorkOrder> activeWorkOrders = workOrderRepository.findAll().stream()
            .filter(wo -> "APPROVED".equals(wo.getStatus()) || "IN_PROGRESS".equals(wo.getStatus()))
            .collect(Collectors.toList());
        
        for (WorkOrder workOrder : activeWorkOrders) {
            List<ProductionStage> activeStages = productionStageRepository.findAll().stream()
                .filter(stage -> stage.getWorkOrderDetail() != null && stage.getWorkOrderDetail().getWorkOrder() != null)
                .filter(stage -> workOrder.getId().equals(stage.getWorkOrderDetail().getWorkOrder().getId()))
                .filter(stage -> "IN_PROGRESS".equals(stage.getStatus()))
                .collect(Collectors.toList());
            
            for (ProductionStage stage : activeStages) {
                // Kiểm tra MachineAssignment
                List<MachineAssignment> assignments = machineAssignmentRepository.findAll().stream()
                    .filter(assignment -> machine.getId().equals(assignment.getMachine().getId()))
                    .filter(assignment -> assignment.getReleasedAt() == null)
                    .collect(Collectors.toList());
                
                for (MachineAssignment assignment : assignments) {
                    LocalDateTime assignedAt = assignment.getAssignedAt() != null ? 
                        assignment.getAssignedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null;
                    LocalDateTime releasedAt = assignment.getReleasedAt() != null ? 
                        assignment.getReleasedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null;
                        
                    if (isTimeOverlap(startTime, endTime, assignedAt, releasedAt)) {
                        info.setAvailable(false);
                        info.getConflicts().add("Máy đang được sử dụng cho Work Order từ " + assignedAt);
                    }
                }
            }
        }
        
        // 4. Tính điểm khả dụng và gợi ý thời gian
        if (info.isAvailable()) {
            info.setScore(100.0);
            info.setSuggestedStartTime(startTime);
            info.setSuggestedEndTime(endTime);
        } else {
            // Tìm khoảng thời gian gần nhất có sẵn
            LocalDateTime suggestedStart = findNextAvailableTime(machine, startTime);
            if (suggestedStart != null) {
                info.setSuggestedStartTime(suggestedStart);
                info.setSuggestedEndTime(suggestedStart.plusHours(8)); // Giả định 8 giờ làm việc
                info.setScore(50.0); // Điểm thấp hơn vì không đúng thời gian mong muốn
            } else {
                info.setScore(0.0);
            }
        }
        
        return info;
    }
    
    /**
     * Tính điểm ưu tiên tổng thể
     */
    private double calculatePriorityScore(MachineSuggestionDto suggestion, String stageType, BigDecimal requiredQuantity) {
        double score = 0.0;
        
        // Xử lý các trường hợp đặc biệt
        if ("DYEING".equals(stageType) && "OUTSOURCE-DYEING".equals(suggestion.getMachineCode())) {
            // Công đoạn nhuộm outsourced: điểm cao cố định
            return 90.0;
        }
        
        if ("PACKAGING".equals(stageType) && "MANUAL-PACKAGING".equals(suggestion.getMachineCode())) {
            // Công đoạn đóng gói thủ công: điểm cao cố định
            return 85.0;
        }
        
        // Tính điểm cho các máy thông thường
        // Điểm khả dụng (40%)
        score += suggestion.getAvailabilityScore() * 0.4;
        
        // Điểm năng suất (30%) - điều chỉnh theo loại máy
        if (suggestion.getCapacityPerHour().compareTo(BigDecimal.ZERO) > 0) {
            double capacityScore;
            switch (stageType) {
                case "WARPING":
                case "WEAVING":
                    // Máy mắc và dệt: tính theo khối lượng
                    capacityScore = Math.min(100.0, suggestion.getCapacityPerHour().doubleValue() * 0.1);
                    break;
                case "CUTTING":
                case "SEWING":
                    // Máy cắt và may: tính theo số lượng sản phẩm
                    capacityScore = Math.min(100.0, suggestion.getCapacityPerHour().doubleValue() * 2);
                    break;
                default:
                    capacityScore = Math.min(100.0, suggestion.getCapacityPerHour().doubleValue() * 10);
            }
            score += capacityScore * 0.3;
        }
        
        // Điểm vị trí (20%) - ưu tiên máy gần vị trí trước đó
        if (suggestion.getLocation() != null) {
            score += 20.0; // Giả định điểm cố định
        }
        
        // Điểm phù hợp với số lượng (10%)
        if (suggestion.isCanHandleQuantity()) {
            score += 10.0;
        }
        
        return score;
    }
    
    /**
     * Tìm thời gian sẵn sàng tiếp theo
     */
    private LocalDateTime findNextAvailableTime(Machine machine, LocalDateTime fromTime) {
        // Logic đơn giản: tìm thời gian trống tiếp theo
        LocalDateTime nextAvailable = fromTime;
        
        // Kiểm tra trong 7 ngày tới
        for (int i = 0; i < 7; i++) {
            LocalDateTime dayStart = nextAvailable.toLocalDate().atStartOfDay().plusHours(8); // 8:00 AM
            LocalDateTime dayEnd = dayStart.plusHours(8); // 5:00 PM
            
            AvailabilityInfo availability = checkMachineAvailability(machine, dayStart, dayEnd);
            if (availability.isAvailable()) {
                return dayStart;
            }
            
            nextAvailable = nextAvailable.plusDays(1);
        }
        
        return null;
    }
    
    /**
     * Kiểm tra xem hai khoảng thời gian có chồng lấn không
     */
    private boolean isTimeOverlap(LocalDateTime start1, LocalDateTime end1, 
                                 LocalDateTime start2, LocalDateTime end2) {
        if (start1 == null || end1 == null || start2 == null) {
            return false;
        }
        
        if (end2 == null) {
            // Nếu end2 là null, coi như đang chạy đến hiện tại
            end2 = LocalDateTime.now();
        }
        
        return start1.isBefore(end2) && end1.isAfter(start2);
    }
    
    /**
     * Lấy capacity mặc định theo loại máy
     */
    private BigDecimal getDefaultCapacityForMachineType(String machineType) {
        switch (machineType) {
            case "WARPING":
                return new BigDecimal("200"); // kg/giờ - máy cuộn mắc
            case "WEAVING":
                return new BigDecimal("50"); // kg/giờ - máy dệt
            case "CUTTING":
                return new BigDecimal("150"); // cái/giờ - máy cắt
            case "SEWING":
                return new BigDecimal("100"); // cái/giờ - máy may
            case "DYEING":
                return new BigDecimal("999999"); // kg/giờ - outsourced, công suất vô hạn
            case "PACKAGING":
                return new BigDecimal("500"); // cái/giờ - làm thủ công
            default:
                return new BigDecimal("100");
        }
    }
    
    /**
     * Trích xuất capacity từ specifications JSON
     */
    private BigDecimal extractCapacityFromSpecs(String specs, Product product) {
        if (specs == null || specs.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        try {
            // Parse JSON đơn giản để lấy capacityPerHour
            // Giả định format: {"capacityPerHour": {"faceTowels": 150, "bathTowels": 70, "sportsTowels": 100}}
            
            String productName = product.getName().toLowerCase();
            if (productName.contains("khăn mặt")) {
                return extractValueFromJson(specs, "faceTowels");
            } else if (productName.contains("khăn tắm")) {
                return extractValueFromJson(specs, "bathTowels");
            } else if (productName.contains("khăn thể thao")) {
                return extractValueFromJson(specs, "sportsTowels");
            }
            
            // Fallback: lấy giá trị đầu tiên
            return extractValueFromJson(specs, "default");
            
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Trích xuất giá trị từ JSON string
     */
    private BigDecimal extractValueFromJson(String json, String key) {
        try {
            // Logic parse JSON đơn giản
            String pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            
            if (m.find()) {
                return new BigDecimal(m.group(1));
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Lấy Product by ID (cần inject ProductRepository)
     */
    private Product getProductById(Long productId) {
        if (productId == null) {
            return null;
        }
        return productRepository.findById(productId).orElse(null);
    }
    
    // ===== DTO Classes =====
    
    public static class MachineSuggestionDto {
        private Long machineId;
        private String machineCode;
        private String machineName;
        private String machineType;
        private String location;
        private BigDecimal capacityPerHour;
        private BigDecimal estimatedDurationHours;
        private boolean canHandleQuantity;
        private boolean available;
        private double availabilityScore;
        private List<String> conflicts = new ArrayList<>();
        private LocalDateTime suggestedStartTime;
        private LocalDateTime suggestedEndTime;
        private double priorityScore;
        
        // Getters and Setters
        public Long getMachineId() { return machineId; }
        public void setMachineId(Long machineId) { this.machineId = machineId; }
        
        public String getMachineCode() { return machineCode; }
        public void setMachineCode(String machineCode) { this.machineCode = machineCode; }
        
        public String getMachineName() { return machineName; }
        public void setMachineName(String machineName) { this.machineName = machineName; }
        
        public String getMachineType() { return machineType; }
        public void setMachineType(String machineType) { this.machineType = machineType; }
        
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        
        public BigDecimal getCapacityPerHour() { return capacityPerHour; }
        public void setCapacityPerHour(BigDecimal capacityPerHour) { this.capacityPerHour = capacityPerHour; }
        
        public BigDecimal getEstimatedDurationHours() { return estimatedDurationHours; }
        public void setEstimatedDurationHours(BigDecimal estimatedDurationHours) { this.estimatedDurationHours = estimatedDurationHours; }
        
        public boolean isCanHandleQuantity() { return canHandleQuantity; }
        public void setCanHandleQuantity(boolean canHandleQuantity) { this.canHandleQuantity = canHandleQuantity; }
        
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }
        
        public double getAvailabilityScore() { return availabilityScore; }
        public void setAvailabilityScore(double availabilityScore) { this.availabilityScore = availabilityScore; }
        
        public List<String> getConflicts() { return conflicts; }
        public void setConflicts(List<String> conflicts) { this.conflicts = conflicts; }
        
        public LocalDateTime getSuggestedStartTime() { return suggestedStartTime; }
        public void setSuggestedStartTime(LocalDateTime suggestedStartTime) { this.suggestedStartTime = suggestedStartTime; }
        
        public LocalDateTime getSuggestedEndTime() { return suggestedEndTime; }
        public void setSuggestedEndTime(LocalDateTime suggestedEndTime) { this.suggestedEndTime = suggestedEndTime; }
        
        public double getPriorityScore() { return priorityScore; }
        public void setPriorityScore(double priorityScore) { this.priorityScore = priorityScore; }
    }
    
    public static class MachineCapacityInfo {
        private BigDecimal capacityPerHour;
        private BigDecimal estimatedDurationHours;
        private boolean canHandleQuantity;
        
        // Getters and Setters
        public BigDecimal getCapacityPerHour() { return capacityPerHour; }
        public void setCapacityPerHour(BigDecimal capacityPerHour) { this.capacityPerHour = capacityPerHour; }
        
        public BigDecimal getEstimatedDurationHours() { return estimatedDurationHours; }
        public void setEstimatedDurationHours(BigDecimal estimatedDurationHours) { this.estimatedDurationHours = estimatedDurationHours; }
        
        public boolean isCanHandleQuantity() { return canHandleQuantity; }
        public void setCanHandleQuantity(boolean canHandleQuantity) { this.canHandleQuantity = canHandleQuantity; }
    }
    
    public static class AvailabilityInfo {
        private boolean available = true;
        private double score = 100.0;
        private List<String> conflicts = new ArrayList<>();
        private LocalDateTime suggestedStartTime;
        private LocalDateTime suggestedEndTime;
        
        // Getters and Setters
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }
        
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        
        public List<String> getConflicts() { return conflicts; }
        public void setConflicts(List<String> conflicts) { this.conflicts = conflicts; }
        
        public LocalDateTime getSuggestedStartTime() { return suggestedStartTime; }
        public void setSuggestedStartTime(LocalDateTime suggestedStartTime) { this.suggestedStartTime = suggestedStartTime; }
        
        public LocalDateTime getSuggestedEndTime() { return suggestedEndTime; }
        public void setSuggestedEndTime(LocalDateTime suggestedEndTime) { this.suggestedEndTime = suggestedEndTime; }
    }
}
