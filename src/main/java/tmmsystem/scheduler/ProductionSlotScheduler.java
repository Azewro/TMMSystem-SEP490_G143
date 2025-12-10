package tmmsystem.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.service.ProductionService;

import java.util.List;

/**
 * Scheduler that runs every minute to check and promote pending stages.
 * Ensures cross-PO stage promotion when a slot becomes available.
 * 
 * Rules:
 * 1. Each stage type processes one lot at a time (except DYEING - outsourced)
 * 2. Lots are processed in order: priority DESC, createdAt ASC (FIFO)
 * 3. A lot can only have one active stage at a time (sequential:
 * WARPING→WEAVING→DYEING→CUTTING→HEMMING→PACKAGING)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductionSlotScheduler {

    private final ProductionService productionService;

    private static final List<String> STAGE_TYPES = List.of(
            "WARPING", "WEAVING", "DYEING", "CUTTING", "HEMMING", "PACKAGING");

    /**
     * Runs every minute to check for available slots and promote pending stages.
     */
    @Scheduled(cron = "0 * * * * *") // Every 1 minute
    @Transactional
    public void checkAndPromoteStages() {
        log.debug("Production slot scheduler: checking for promotable stages...");

        for (String stageType : STAGE_TYPES) {
            try {
                productionService.promoteNextOrderForStageType(stageType);
            } catch (Exception e) {
                log.error("Error promoting stage type {}: {}", stageType, e.getMessage());
            }
        }
    }
}
