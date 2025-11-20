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
    private static final BigDecimal VENDOR_DYEING_DAYS = new BigDecimal("2.0");

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
        result.setDyeingDays(VENDOR_DYEING_DAYS);
        result.setCuttingDays(max(
                divide(faceQty, cuttingCapacityFace),
                divide(bathQty, cuttingCapacityBath),
                divide(sportQty, cuttingCapacitySport)
        ));
        result.setSewingDays(max(
                divide(faceQty, sewingCapacityFace),
                divide(bathQty, sewingCapacityBath),
                divide(sportQty, sewingCapacitySport)
        ));

        BigDecimal totalProcessing = result.getWarpingDays()
                .add(result.getWeavingDays())
                .add(result.getDyeingDays())
                .add(result.getCuttingDays())
                .add(result.getSewingDays());
        BigDecimal waitTimes = new BigDecimal("0.5")
                .add(new BigDecimal("0.5"))
                .add(new BigDecimal("1.0"))
                .add(new BigDecimal("0.2"))
                .add(new BigDecimal("0.3"));
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
                "SEWING", result.getSewingDays()
        );
        return data.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("WARPING");
    }

    private BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || numerator.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal max(BigDecimal... values) {
        BigDecimal max = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            if (v != null && v.compareTo(max) > 0) max = v;
        }
        return max;
    }

    private BigDecimal getTotalCapacityPerDay(String machineType) {
        return machineRepository.findAll().stream()
                .filter(machine -> machineType.equalsIgnoreCase(machine.getType()))
                .map(machine -> extractCapacityFromSpecs(machine.getSpecifications(), "capacityPerDay"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getCapacityPerDay(String machineType, String productKey) {
        return machineRepository.findAll().stream()
                .filter(machine -> machineType.equalsIgnoreCase(machine.getType()))
                .map(machine -> extractCapacityFromSpecs(machine.getSpecifications(), productKey))
                .map(capacityPerHour -> capacityPerHour.multiply(WORKING_HOURS_PER_DAY))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal extractCapacityFromSpecs(String specs, String key) {
        if (specs == null) return BigDecimal.ZERO;
        String pattern = "\"" + key + "\"\\s*:\\s*(\\d+(?:\\.\\d+)?)";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(specs);
        if (matcher.find()) {
            return new BigDecimal(matcher.group(1));
        }
        return BigDecimal.ZERO;
    }
}

