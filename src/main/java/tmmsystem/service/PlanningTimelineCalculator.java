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

    public record StageTimeline(String stageType, LocalDateTime start, LocalDateTime end) {
    }

    @Value("${planning.timeline.startHour:9}")
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

    @Value("${planning.timeline.waitHours.hemmingPackaging:2}") // 0.2 days * 8 hours = 1.6 hours -> round to 2
    private int waitHemmingPackaging;

    public List<StageTimeline> buildTimeline(LocalDate startDate,
            tmmsystem.service.timeline.SequentialCapacityResult durations) {
        return buildTimeline(startDate.atTime(startHour, 0), durations);
    }

    public List<StageTimeline> buildTimeline(LocalDateTime start,
            tmmsystem.service.timeline.SequentialCapacityResult durations) {
        List<StageTimeline> result = new ArrayList<>();
        LocalDateTime cursor = nextBusinessStart(start);
        LocalDateTime end;
        double[] stageHours = new double[] {
                durations.getWarpingDays().doubleValue() * dailyHours,
                durations.getWeavingDays().doubleValue() * dailyHours,
                durations.getDyeingDays().doubleValue() * dailyHours,
                durations.getCuttingDays().doubleValue() * dailyHours,
                durations.getSewingDays().doubleValue() * dailyHours,
                durations.getPackagingDays().doubleValue() * dailyHours
        };
        String[] types = new String[] { "WARPING", "WEAVING", "DYEING", "CUTTING", "HEMMING", "PACKAGING" };
        int[] waits = new int[] {
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

    private LocalDateTime addWait(LocalDateTime current, int waitHours) {
        if (waitHours <= 0)
            return current;
        return addWorkingHours(current, waitHours);
    }

    private LocalDateTime addWorkingHours(LocalDateTime start, double hours) {
        if (hours <= 0)
            return start;
        long remainingMinutes = Math.max(1, Math.round(hours * 60));
        long dailyMinutes = dailyHours * 60L;
        LocalDateTime time = alignToWorkingWindow(start);

        LocalDateTime dayStart = time.withHour(startHour).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime dayEnd = dayStart.plusMinutes(dailyMinutes);
        long availableToday = ChronoUnit.MINUTES.between(time, dayEnd);
        if (availableToday >= remainingMinutes) {
            return time.plusMinutes(remainingMinutes);
        }
        remainingMinutes -= availableToday;

        long fullDays = remainingMinutes / dailyMinutes;
        long finalMinutes = remainingMinutes % dailyMinutes;

        if (finalMinutes == 0) {
            // Ends exactly at the end of a working day
            return dayStart.plusDays(fullDays).withHour(startHour + dailyHours).withMinute(0).withSecond(0).withNano(0);
        } else {
            // Spills over to the next day
            return dayStart.plusDays(1 + fullDays).withHour(startHour).withMinute(0).withSecond(0).withNano(0)
                    .plusMinutes(finalMinutes);
        }
    }

    private LocalDateTime alignToWorkingWindow(LocalDateTime time) {
        LocalDateTime dayStart = time.withHour(startHour).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime dayEnd = dayStart.plusHours(dailyHours);
        if (time.isBefore(dayStart)) {
            return dayStart;
        }
        if (!time.isBefore(dayEnd)) {
            return dayStart.plusDays(1);
        }
        return time;
    }
}
