package tmmsystem.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
public class PlanningTimelineCalculator {

    public record StageTimeline(String stageType, LocalDateTime start, LocalDateTime end) {}

    @Value("${planning.timeline.startHour:8}")
    private int startHour;

    @Value("${planning.timeline.dailyHours:8}")
    private int dailyHours;

    @Value("${planning.timeline.waitHours.warpingWeaving:4}")
    private int waitWarpingWeaving;

    @Value("${planning.timeline.waitHours.weavingDyeing:4}")
    private int waitWeavingDyeing;

    @Value("${planning.timeline.waitHours.dyeingCutting:8}")
    private int waitDyeingCutting;

    @Value("${planning.timeline.waitHours.cuttingHemming:2}")
    private int waitCuttingHemming;

    @Value("${planning.timeline.waitHours.hemmingPackaging:3}")
    private int waitHemmingPackaging;

    public List<StageTimeline> buildTimeline(LocalDate startDate, tmmsystem.service.timeline.SequentialCapacityResult durations) {
        return buildTimeline(startDate.atTime(startHour, 0), durations);
    }

    public List<StageTimeline> buildTimeline(LocalDateTime start, tmmsystem.service.timeline.SequentialCapacityResult durations) {
        List<StageTimeline> result = new ArrayList<>();
        LocalDateTime cursor = nextBusinessStart(start);
        LocalDateTime end;
        double[] stageHours = new double[]{
                durations.getWarpingDays().doubleValue() * dailyHours,
                durations.getWeavingDays().doubleValue() * dailyHours,
                durations.getDyeingDays().doubleValue() * dailyHours,
                durations.getCuttingDays().doubleValue() * dailyHours,
                durations.getSewingDays().doubleValue() * dailyHours,
                dailyHours // packaging placeholder
        };
        String[] types = new String[]{"WARPING","WEAVING","DYEING","CUTTING","HEMMING","PACKAGING"};
        int[] waits = new int[]{
                waitWarpingWeaving,
                waitWeavingDyeing,
                waitDyeingCutting,
                waitCuttingHemming,
                waitHemmingPackaging,
                0
        };

        for (int i = 0; i < types.length; i++) {
            cursor = addWait(cursor, waits[i]);
            end = addWorkingHours(cursor, stageHours[i]);
            result.add(new StageTimeline(types[i], cursor, end));
            cursor = end;
        }

        return result;
    }

    private LocalDateTime nextBusinessStart(LocalDateTime time) {
        LocalDateTime startOfDay = time.withHour(startHour).withMinute(0).withSecond(0).withNano(0);
        if (time.isBefore(startOfDay)) {
            return startOfDay;
        }
        if (time.isAfter(startOfDay.plusHours(dailyHours))) {
            return startOfDay.plusDays(1);
        }
        return time;
    }

    private LocalDateTime addWait(LocalDateTime current, double waitDays) {
        if (waitDays <= 0) return current;
        long fullDays = (long) waitDays;
        double fractional = waitDays - fullDays;
        LocalDateTime time = current.plusDays(fullDays);
        return addWorkingHours(time, fractional * dailyHours);
    }

    private LocalDateTime addWorkingHours(LocalDateTime start, double hours) {
        if (hours <= 0) return start;
        LocalDateTime time = start;
        double remaining = hours;

        while (remaining > 0.0) {
            LocalDateTime dayStart = time.withHour(startHour).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime dayEnd = dayStart.plusHours(dailyHours);
            if (time.isBefore(dayStart)) {
                time = dayStart;
            }
            long available = Math.max(0, ChronoUnit.MINUTES.between(time, dayEnd));
            if (available <= 0) {
                time = dayStart.plusDays(1);
                continue;
            }
            long consume = (long) Math.min(remaining * 60, available);
            time = time.plusMinutes(consume);
            remaining -= consume / 60.0;
        }
        return time;
    }
}

