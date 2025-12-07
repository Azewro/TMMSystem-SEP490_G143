package tmmsystem.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.entity.Quotation;
import tmmsystem.repository.QuotationRepository;
import tmmsystem.service.NotificationService;
import tmmsystem.service.EmailService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuotationExpirationScheduler {

    private final QuotationRepository quotationRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    /**
     * Runs every minute to check for expired quotations.
     */
    @Scheduled(cron = "0 * * * * *") // Every minute
    @Transactional
    public void checkQuotationStatus() {
        checkExpiringSoonQuotations();
        checkExpiredQuotations();
    }

    private void checkExpiringSoonQuotations() {
        // Warning threshold: 9 hours after sending (so 3 hours remaining)
        Instant warningCutoff = Instant.now().minus(9, ChronoUnit.HOURS);

        // Find quotations sent > 9 hours ago that haven't been warned yet
        List<Quotation> expiringSoon = quotationRepository
                .findByStatusAndExpirationWarningSentFalseAndSentAtBefore("SENT", warningCutoff);

        if (!expiringSoon.isEmpty()) {
            log.info("Found {} expiring soon quotations.", expiringSoon.size());
            for (Quotation quotation : expiringSoon) {
                try {
                    // Send warning notification (Sales)
                    notificationService.notifyQuotationExpiringSoon(quotation);
                    // Send warning email (Customer)
                    emailService.sendQuotationExpiringSoonEmail(quotation);

                    quotation.setExpirationWarningSent(true);
                    quotationRepository.save(quotation);
                } catch (Exception e) {
                    log.error("Failed to process warning for quotation ID: {}", quotation.getId(), e);
                }
            }
        }
    }

    private void checkExpiredQuotations() {
        // Expiration threshold: 12 hours
        Instant expirationCutoff = Instant.now().minus(12, ChronoUnit.HOURS);

        // Find applicable quotations
        List<Quotation> expiredQuotations = quotationRepository.findByStatusAndSentAtBefore("SENT", expirationCutoff);

        if (!expiredQuotations.isEmpty()) {
            log.info("Found {} expired quotations to process.", expiredQuotations.size());

            for (Quotation quotation : expiredQuotations) {
                try {
                    log.info("Expiring quotation ID: {}", quotation.getId());
                    quotation.setStatus("REJECTED");
                    quotation.setRejectReason("Hệ thống tự động từ chối: Quá hạn 12 giờ không phản hồi.");
                    quotation.setRejectedAt(Instant.now());

                    quotationRepository.save(quotation);

                    // Send notification (Sales)
                    notificationService.notifyQuotationExpired(quotation);
                    // Send email (Customer)
                    emailService.sendQuotationExpiredEmail(quotation);

                } catch (Exception e) {
                    log.error("Failed to expire quotation ID: {}", quotation.getId(), e);
                }
            }
        }
    }
}
