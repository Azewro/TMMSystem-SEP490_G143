package tmmsystem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.concurrent.Future;

@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async("mailExecutor")
    public Future<Boolean> send(String to, String subject, String text) {
        try {
            log.info("Sending plain text email to: {} with subject: {}", to, subject);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Plain text email sent successfully to: {}", to);
            return new AsyncResult<>(true);
        } catch (Exception ex) {
            log.error("Failed to send email to {}: {}", to, ex.getMessage(), ex);
            return new AsyncResult<>(false);
        }
    }

    @Async("mailExecutor")
    public Future<Boolean> sendHtml(String to, String subject, String htmlContent) {
        try {
            log.info("Sending HTML email to: {} with subject: {}", to, subject);
            MimeMessage message = mailSender.createMimeMessage();
            // Use multipart=false for simple HTML emails (no attachments)
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML content
            mailSender.send(message);
            log.info("HTML email sent successfully to: {}", to);
            return new AsyncResult<>(true);
        } catch (Exception ex) {
            log.error("Failed to send HTML email to {}: {}", to, ex.getMessage(), ex);
            return new AsyncResult<>(false);
        }
    }
}
