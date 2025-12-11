package tmmsystem.service.timeline;

import org.springframework.stereotype.Component;
import tmmsystem.repository.MachineRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Component
public class SequentialCapacityCalculator {
    private final MachineRepository machineRepository;

    private static final BigDecimal WORKING_HOURS_PER_DAY = new BigDecimal("8");
    // Dyeing: 5000 items / 8 hours = 625 items/hour
    private static final BigDecimal DYEING_CAPACITY_PER_HOUR = new BigDecimal("625");
    // Packaging: 500 items/person/hour * 2 people = 1000 items/hour
    private static final BigDecimal PACKAGING_CAPACITY_PER_HOUR = new BigDecimal("1000");

    public SequentialCapacityCalculator(MachineRepository machineRepository) {
        this.machineRepository = machineRepository;
    }

    public SequentialCapacityResult calculate(BigDecimal totalWeightKg,
            BigDecimal faceQty,
            BigDecimal bathQty,
            BigDecimal sportQty) {
        SequentialCapacityResult result = new SequentialCapacityResult();

        BigDecimal warpingCapacity = getTotalCapacityPerDay("WARPING");
        BigDecimal weavingCapacity = getTotalCapacityPerDay("WEAVING");
        BigDecimal cuttingCapacityFace = getCapacityPerDay("CUTTING", "faceTowels");
        BigDecimal cuttingCapacityBath = getCapacityPerDay("CUTTING", "bathTowels");
        BigDecimal cuttingCapacitySport = getCapacityPerDay("CUTTING", "sportsTowels");
        BigDecimal sewingCapacityFace = getCapacityPerDay("SEWING", "faceTowels");
        BigDecimal sewingCapacityBath = getCapacityPerDay("SEWING", "bathTowels");
        BigDecimal sewingCapacitySport = getCapacityPerDay("SEWING", "sportsTowels");

        result.setWarpingDays(divide(totalWeightKg, warpingCapacity));
        result.setWeavingDays(divide(totalWeightKg, weavingCapacity));

        // Dyeing: 5000 items/day
        BigDecimal totalQty = faceQty.add(bathQty).add(sportQty);
        BigDecimal dyeingCapacityPerDay = DYEING_CAPACITY_PER_HOUR.multiply(WORKING_HOURS_PER_DAY);
        result.setDyeingDays(divide(totalQty, dyeingCapacityPerDay));

        result.setCuttingDays(max(
                divide(faceQty, cuttingCapacityFace),
                divide(bathQty, cuttingCapacityBath),
                divide(sportQty, cuttingCapacitySport)));
        result.setSewingDays(max(
                divide(faceQty, sewingCapacityFace),
                divide(bathQty, sewingCapacityBath),
                divide(sportQty, sewingCapacitySport)));

        // Packaging: 2500 items/hour * 8 hours = 20000 items/day
        // totalQty is already calculated above
        BigDecimal packagingCapacityPerDay = PACKAGING_CAPACITY_PER_HOUR.multiply(WORKING_HOURS_PER_DAY);
        result.setPackagingDays(divide(totalQty, packagingCapacityPerDay));

        BigDecimal totalProcessing = result.getWarpingDays()
                .add(result.getWeavingDays())
                .add(result.getDyeingDays())
                .add(result.getCuttingDays())
                .add(result.getSewingDays())
                .add(result.getPackagingDays());
        BigDecimal waitTimes = new BigDecimal("0.5")
                .add(new BigDecimal("0.5"))
                .add(new BigDecimal("1.0"))
                .add(new BigDecimal("0.2"))
                .add(new BigDecimal("0.3"))
                .add(new BigDecimal("0.2")); // Packaging wait time
        result.setTotalDays(totalProcessing.add(waitTimes));
        result.setBottleneck(findBottleneck(result));

        return result;
    }

    private String findBottleneck(SequentialCapacityResult result) {
        Map<String, BigDecimal> data = Map.of(
                "WARPING", result.getWarpingDays(),
                "WEAVING", result.getWeavingDays(),
                "DYEING", result.getDyeingDays(),
                "CUTTING", result.getCuttingDays(),
                "SEWING", result.getSewingDays(),
                "PACKAGING", result.getPackagingDays());
        return data.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("WARPING");
    }

    private BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || numerator.compareTo(BigDecimal.ZERO) == 0)
            return BigDecimal.ZERO;
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0)
            return BigDecimal.ZERO;
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal max(BigDecimal... values) {
        BigDecimal max = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            if (v != null && v.compareTo(max) > 0)
                max = v;
        }
        return max;
    }

    private BigDecimal getTotalCapacityPerDay(String machineType) {
        return machineRepository.findAll().stream()
                .filter(machine -> machineType.equalsIgnoreCase(machine.getType()))
                .map(machine -> {
                    // Check capacityPerDay first (used by WARPING and WEAVING)
                    BigDecimal daily = extractCapacityFromSpecs(machine.getSpecifications(), "capacityPerDay");
                    if (daily.compareTo(BigDecimal.ZERO) > 0) {
                        return daily;
                    }
                    // Fallback to capacityPerHour × 8
                    BigDecimal hourly = extractCapacityFromSpecs(machine.getSpecifications(), "capacityPerHour");
                    if (hourly.compareTo(BigDecimal.ZERO) > 0) {
                        return hourly.multiply(WORKING_HOURS_PER_DAY);
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get the bottleneck capacity (minimum of WARPING and WEAVING) in kg/day.
     * This is used for the hybrid rolling window capacity check.
     */
    public BigDecimal getBottleneckCapacityPerDay() {
        BigDecimal warpingCapacity = getTotalCapacityPerDay("WARPING");
        BigDecimal weavingCapacity = getTotalCapacityPerDay("WEAVING");

        // If either is 0, use the other one (avoid 0 bottleneck)
        if (warpingCapacity.compareTo(BigDecimal.ZERO) == 0) {
            return weavingCapacity.compareTo(BigDecimal.ZERO) > 0 ? weavingCapacity : new BigDecimal("500");
        }
        if (weavingCapacity.compareTo(BigDecimal.ZERO) == 0) {
            return warpingCapacity;
        }

        // Return the minimum (bottleneck)
        return warpingCapacity.min(weavingCapacity);
    }

    private BigDecimal getCapacityPerDay(String machineType, String productKey) {
        return machineRepository.findAll().stream()
                .filter(machine -> machineType.equalsIgnoreCase(machine.getType()))
                .map(machine -> extractCapacityFromSpecs(machine.getSpecifications(), productKey))
                .map(capacityPerHour -> capacityPerHour.multiply(WORKING_HOURS_PER_DAY))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal extractCapacityFromSpecs(String specs, String key) {
        if (specs == null)
            return BigDecimal.ZERO;
        String pattern = "\"" + key + "\"\\s*:\\s*(\\d+(?:\\.\\d+)?)";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(specs);
        if (matcher.find()) {
            return new BigDecimal(matcher.group(1));
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get all stage capacities for display purposes.
     * Shows machine count, capacity per machine, total capacity, and whether it's
     * the bottleneck.
     */
    public java.util.List<tmmsystem.dto.sales.CapacityCheckResultDto.StageCapacityDto> getAllStageCapacities() {
        java.util.List<tmmsystem.dto.sales.CapacityCheckResultDto.StageCapacityDto> capacities = new java.util.ArrayList<>();
        BigDecimal warpingTotal = getTotalCapacityPerDay("WARPING");
        BigDecimal weavingTotal = getTotalCapacityPerDay("WEAVING");
        // Bottleneck is the stage with LOWER capacity (in kg)
        boolean warpingIsBottleneck = warpingTotal.compareTo(weavingTotal) <= 0;

        // WARPING
        {
            var dto = new tmmsystem.dto.sales.CapacityCheckResultDto.StageCapacityDto();
            dto.setStageName("Mắc cuồng");
            dto.setStageType("WARPING");
            long count = machineRepository.findAll().stream().filter(m -> "WARPING".equalsIgnoreCase(m.getType()))
                    .count();
            dto.setMachineCount((int) count);
            dto.setTotalCapacityPerDay(warpingTotal.setScale(2, RoundingMode.HALF_UP));
            dto.setCapacityPerMachine(
                    count > 0 ? warpingTotal.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO);
            dto.setUnit("kg");
            dto.setBottleneck(warpingIsBottleneck);
            capacities.add(dto);
        }

        // WEAVING
        {
            var dto = new tmmsystem.dto.sales.CapacityCheckResultDto.StageCapacityDto();
            dto.setStageName("Dệt vải");
            dto.setStageType("WEAVING");
            long count = machineRepository.findAll().stream().filter(m -> "WEAVING".equalsIgnoreCase(m.getType()))
                    .count();
            dto.setMachineCount((int) count);
            dto.setTotalCapacityPerDay(weavingTotal.setScale(2, RoundingMode.HALF_UP));
            dto.setCapacityPerMachine(
                    count > 0 ? weavingTotal.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO);
            dto.setUnit("kg");
            dto.setBottleneck(!warpingIsBottleneck);
            capacities.add(dto);
        }

        // DYEING (outsourced)
        {
            var dto = new tmmsystem.dto.sales.CapacityCheckResultDto.StageCapacityDto();
            dto.setStageName("Nhuộm (thuê ngoài)");
            dto.setStageType("DYEING");
            dto.setMachineCount(0);
            BigDecimal total = DYEING_CAPACITY_PER_HOUR.multiply(WORKING_HOURS_PER_DAY);
            dto.setTotalCapacityPerDay(total.setScale(2, RoundingMode.HALF_UP));
            dto.setCapacityPerMachine(BigDecimal.ZERO);
            dto.setUnit("sản phẩm");
            dto.setBottleneck(false);
            capacities.add(dto);
        }

        // CUTTING
        {
            var dto = new tmmsystem.dto.sales.CapacityCheckResultDto.StageCapacityDto();
            dto.setStageName("Cắt vải");
            dto.setStageType("CUTTING");
            long count = machineRepository.findAll().stream().filter(m -> "CUTTING".equalsIgnoreCase(m.getType()))
                    .count();
            dto.setMachineCount((int) count);
            BigDecimal total = getCapacityPerDay("CUTTING", "faceTowels"); // Use faceTowels as representative
            dto.setTotalCapacityPerDay(total.setScale(2, RoundingMode.HALF_UP));
            dto.setCapacityPerMachine(
                    count > 0 ? total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            dto.setUnit("sản phẩm");
            dto.setBottleneck(false);
            capacities.add(dto);
        }

        // SEWING
        {
            var dto = new tmmsystem.dto.sales.CapacityCheckResultDto.StageCapacityDto();
            dto.setStageName("May thành phẩm");
            dto.setStageType("SEWING");
            long count = machineRepository.findAll().stream().filter(m -> "SEWING".equalsIgnoreCase(m.getType()))
                    .count();
            dto.setMachineCount((int) count);
            BigDecimal total = getCapacityPerDay("SEWING", "faceTowels");
            dto.setTotalCapacityPerDay(total.setScale(2, RoundingMode.HALF_UP));
            dto.setCapacityPerMachine(
                    count > 0 ? total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            dto.setUnit("sản phẩm");
            dto.setBottleneck(false);
            capacities.add(dto);
        }

        // PACKAGING (thủ công - theo người, không theo máy)
        {
            var dto = new tmmsystem.dto.sales.CapacityCheckResultDto.StageCapacityDto();
            dto.setStageName("Đóng gói (thủ công)");
            dto.setStageType("PACKAGING");
            dto.setMachineCount(2); // 2 người
            BigDecimal total = PACKAGING_CAPACITY_PER_HOUR.multiply(WORKING_HOURS_PER_DAY);
            dto.setTotalCapacityPerDay(total.setScale(2, RoundingMode.HALF_UP));
            dto.setCapacityPerMachine(new BigDecimal("4000")); // 500 sp/người/giờ × 8 giờ = 4000 sp/người/ngày
            dto.setUnit("sản phẩm (theo người)");
            dto.setBottleneck(false);
            capacities.add(dto);
        }

        return capacities;
    }
}
