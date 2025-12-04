package tmmsystem.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to validate machine specifications JSON.
 * Matches frontend validation in CreateMachineModal.jsx
 */
public class MachineSpecificationsValidator {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String POWER_REGEX = "^\\d+(\\.\\d+)?\\s*(kw|w|kW|W)?$";
    private static final int MIN_YEAR = 1900;
    private static final int MAX_CAPACITY_PER_DAY = 1000000;
    private static final int MAX_CAPACITY_PER_HOUR = 10000;
    
    /**
     * Validates machine specifications JSON string based on machine type.
     * 
     * @param specificationsJson JSON string containing specifications
     * @param machineType Type of machine (WEAVING, WARPING, SEWING, CUTTING)
     * @return List of error messages (empty if valid)
     */
    public static List<String> validate(String specificationsJson, String machineType) {
        List<String> errors = new ArrayList<>();
        
        if (specificationsJson == null || specificationsJson.trim().isEmpty()) {
            errors.add("Thông số máy là bắt buộc");
            return errors;
        }
        
        try {
            JsonNode specs = objectMapper.readTree(specificationsJson);
            
            // Validate brand (required for all types)
            if (!specs.has("brand") || specs.get("brand").asText().trim().isEmpty()) {
                errors.add("Thương hiệu là bắt buộc");
            } else {
                String brand = specs.get("brand").asText().trim();
                if (brand.length() < 2) {
                    errors.add("Thương hiệu phải có ít nhất 2 ký tự");
                } else if (brand.length() > 50) {
                    errors.add("Thương hiệu không được vượt quá 50 ký tự");
                }
            }
            
            // Validate power (required for all types)
            if (!specs.has("power") || specs.get("power").asText().trim().isEmpty()) {
                errors.add("Công suất là bắt buộc");
            } else {
                String power = specs.get("power").asText().trim();
                if (!power.matches(POWER_REGEX)) {
                    errors.add("Công suất không hợp lệ. VD: 5kW, 3kW");
                }
            }
            
            // Validate modelYear (required for all types)
            if (!specs.has("modelYear")) {
                errors.add("Năm sản xuất là bắt buộc");
            } else {
                String modelYearStr = specs.get("modelYear").asText().trim();
                if (modelYearStr.isEmpty()) {
                    errors.add("Năm sản xuất là bắt buộc");
                } else {
                    if (!modelYearStr.matches("^\\d{4}$")) {
                        errors.add("Năm sản xuất phải là 4 chữ số");
                    } else {
                        int modelYear = Integer.parseInt(modelYearStr);
                        int currentYear = java.time.Year.now().getValue();
                        int maxYear = currentYear + 1;
                        if (modelYear < MIN_YEAR || modelYear > maxYear) {
                            errors.add("Năm sản xuất phải từ 1900 đến " + maxYear);
                        }
                    }
                }
            }
            
            // Validate capacity based on type
            if ("WEAVING".equals(machineType) || "WARPING".equals(machineType)) {
                if (!specs.has("capacityPerDay")) {
                    errors.add("Công suất/ngày là bắt buộc");
                } else {
                    JsonNode capacityNode = specs.get("capacityPerDay");
                    if (capacityNode.isNull() || capacityNode.asText().trim().isEmpty()) {
                        errors.add("Công suất/ngày là bắt buộc");
                    } else {
                        try {
                            double capacityPerDay = capacityNode.asDouble();
                            if (capacityPerDay <= 0) {
                                errors.add("Công suất/ngày phải là số lớn hơn 0");
                            } else if (capacityPerDay > MAX_CAPACITY_PER_DAY) {
                                errors.add("Công suất/ngày không được vượt quá 1,000,000");
                            }
                        } catch (Exception e) {
                            errors.add("Công suất/ngày phải là số hợp lệ");
                        }
                    }
                }
            } else if ("SEWING".equals(machineType) || "CUTTING".equals(machineType)) {
                if (!specs.has("capacityPerHour")) {
                    errors.add("Công suất/giờ là bắt buộc");
                } else {
                    JsonNode capacityPerHour = specs.get("capacityPerHour");
                    
                    // Validate bathTowels
                    if (!capacityPerHour.has("bathTowels") || 
                        capacityPerHour.get("bathTowels").asText().trim().isEmpty()) {
                        errors.add("Công suất khăn tắm/giờ là bắt buộc");
                    } else {
                        try {
                            double bathTowels = capacityPerHour.get("bathTowels").asDouble();
                            if (bathTowels <= 0) {
                                errors.add("Công suất khăn tắm/giờ phải là số lớn hơn 0");
                            } else if (bathTowels > MAX_CAPACITY_PER_HOUR) {
                                errors.add("Công suất khăn tắm/giờ không được vượt quá 10,000");
                            }
                        } catch (Exception e) {
                            errors.add("Công suất khăn tắm/giờ phải là số hợp lệ");
                        }
                    }
                    
                    // Validate faceTowels
                    if (!capacityPerHour.has("faceTowels") || 
                        capacityPerHour.get("faceTowels").asText().trim().isEmpty()) {
                        errors.add("Công suất khăn mặt/giờ là bắt buộc");
                    } else {
                        try {
                            double faceTowels = capacityPerHour.get("faceTowels").asDouble();
                            if (faceTowels <= 0) {
                                errors.add("Công suất khăn mặt/giờ phải là số lớn hơn 0");
                            } else if (faceTowels > MAX_CAPACITY_PER_HOUR) {
                                errors.add("Công suất khăn mặt/giờ không được vượt quá 10,000");
                            }
                        } catch (Exception e) {
                            errors.add("Công suất khăn mặt/giờ phải là số hợp lệ");
                        }
                    }
                    
                    // Validate sportsTowels
                    if (!capacityPerHour.has("sportsTowels") || 
                        capacityPerHour.get("sportsTowels").asText().trim().isEmpty()) {
                        errors.add("Công suất khăn thể thao/giờ là bắt buộc");
                    } else {
                        try {
                            double sportsTowels = capacityPerHour.get("sportsTowels").asDouble();
                            if (sportsTowels <= 0) {
                                errors.add("Công suất khăn thể thao/giờ phải là số lớn hơn 0");
                            } else if (sportsTowels > MAX_CAPACITY_PER_HOUR) {
                                errors.add("Công suất khăn thể thao/giờ không được vượt quá 10,000");
                            }
                        } catch (Exception e) {
                            errors.add("Công suất khăn thể thao/giờ phải là số hợp lệ");
                        }
                    }
                }
            }
            
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            errors.add("Thông số máy không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            errors.add("Lỗi khi xử lý thông số máy: " + e.getMessage());
        }
        
        return errors;
    }
}

