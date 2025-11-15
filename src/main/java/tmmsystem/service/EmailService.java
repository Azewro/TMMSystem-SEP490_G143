package tmmsystem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tmmsystem.entity.Quotation;
import tmmsystem.entity.Contract;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final MailService mailService;

    public EmailService(MailService mailService) {
        this.mailService = mailService;
    }

    public void sendQuotationEmail(Quotation quotation) {
        try {
            String emailContent = generateQuotationEmailContent(quotation);
            String to = quotation.getCustomer().getEmail();
            String subject = "Báo giá mới từ TMM System";
            
            if (to == null || to.isBlank()) {
                log.warn("Cannot send quotation email: customer email is null or empty for quotation #{}", quotation.getQuotationNumber());
                return;
            }
            
            mailService.send(to, subject, emailContent);
            log.info("Quotation email sent successfully to {} for quotation #{}", to, quotation.getQuotationNumber());
        } catch (Exception e) {
            log.error("Failed to send quotation email for quotation #{}: {}", quotation.getQuotationNumber(), e.getMessage(), e);
        }
    }

    // NEW: gửi email có login info (mật khẩu tạm nếu có)
    public void sendQuotationEmailWithLogin(Quotation quotation, String tempPassword) {
        try {
            StringBuilder content = new StringBuilder(generateQuotationEmailContent(quotation));
            content.append("\n\nĐăng nhập để phê duyệt báo giá:\n");
            content.append("Portal: https://portal.example.com/login\n");
            content.append("Email đăng nhập: ").append(quotation.getCustomer().getEmail()).append("\n");
            if (tempPassword != null) {
                content.append("Mật khẩu tạm: ").append(tempPassword).append(" (hãy đổi mật khẩu sau khi đăng nhập)\n");
            } else {
                content.append("Bạn đã có tài khoản. Đăng nhập bằng mật khẩu hiện tại của bạn.\n");
            }
            content.append("Link báo giá: https://portal.example.com/quotations/").append(quotation.getId()).append("\n");
            
            String to = quotation.getCustomer().getEmail();
            String subject = "Báo giá mới từ TMM System";
            
            if (to == null || to.isBlank()) {
                log.warn("Cannot send quotation email with login: customer email is null or empty for quotation #{}", quotation.getQuotationNumber());
                return;
            }
            
            mailService.send(to, subject, content.toString());
            log.info("Quotation email with login sent successfully to {} for quotation #{}", to, quotation.getQuotationNumber());
        } catch (Exception e) {
            log.error("Failed to send quotation email with login for quotation #{}: {}", quotation.getQuotationNumber(), e.getMessage(), e);
        }
    }

    public void sendOrderConfirmationEmail(Contract contract) {
        try {
            String emailContent = generateOrderConfirmationEmailContent(contract);
            String to = contract.getCustomer().getEmail();
            String subject = "Xác nhận đơn hàng #" + contract.getContractNumber();
            
            if (to == null || to.isBlank()) {
                log.warn("Cannot send order confirmation email: customer email is null or empty for contract #{}", contract.getContractNumber());
                return;
            }
            
            mailService.send(to, subject, emailContent);
            log.info("Order confirmation email sent successfully to {} for contract #{}", to, contract.getContractNumber());
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for contract #{}: {}", contract.getContractNumber(), e.getMessage(), e);
        }
    }

    private String generateQuotationEmailContent(Quotation quotation) {
        StringBuilder content = new StringBuilder();
        content.append("Kính gửi ").append(quotation.getCustomer().getCompanyName()).append(",\n\n");
        content.append("Chúng tôi xin gửi báo giá cho yêu cầu của quý khách:\n\n");
        content.append("Mã báo giá: ").append(quotation.getQuotationNumber()).append("\n");
        if (quotation.getCreatedAt() != null) {
            String createdAtFormatted = quotation.getCreatedAt()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            content.append("Ngày tạo: ").append(createdAtFormatted).append("\n");
        }
        if (quotation.getValidUntil() != null) {
            content.append("Hiệu lực đến: ").append(quotation.getValidUntil().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        }
        if (quotation.getTotalAmount() != null) {
            content.append("Tổng giá trị: ").append(String.format("%,.0f", quotation.getTotalAmount().doubleValue())).append(" VND\n\n");
        }
        
        content.append("Chi tiết sản phẩm:\n");
        if (quotation.getDetails() != null) {
            for (var detail : quotation.getDetails()) {
                String productName = detail.getProduct() != null ? detail.getProduct().getName() : "N/A";
                String quantity = detail.getQuantity() != null ? detail.getQuantity().toString() : "0";
                String totalPrice = detail.getTotalPrice() != null 
                    ? String.format("%,.0f", detail.getTotalPrice().doubleValue()) 
                    : "0";
                content.append("- ").append(productName)
                       .append(" x ").append(quantity)
                       .append(" = ").append(totalPrice).append(" VND\n");
            }
        }
        
        content.append("\nVui lòng truy cập hệ thống để xem chi tiết và phản hồi.\n\n");
        content.append("Trân trọng,\nTMM System");
        
        return content.toString();
    }

    private String generateOrderConfirmationEmailContent(Contract contract) {
        StringBuilder content = new StringBuilder();
        content.append("Kính gửi ").append(contract.getCustomer().getCompanyName()).append(",\n\n");
        content.append("Cảm ơn quý khách đã chấp nhận báo giá. Đơn hàng của quý khách đã được tạo:\n\n");
        content.append("Mã đơn hàng: ").append(contract.getContractNumber()).append("\n");
        if (contract.getContractDate() != null) {
            content.append("Ngày ký hợp đồng: ").append(contract.getContractDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        }
        if (contract.getDeliveryDate() != null) {
            content.append("Ngày giao hàng dự kiến: ").append(contract.getDeliveryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        }
        if (contract.getTotalAmount() != null) {
            content.append("Tổng giá trị: ").append(String.format("%,.0f", contract.getTotalAmount().doubleValue())).append(" VND\n\n");
        }
        
        content.append("Chúng tôi sẽ liên hệ với quý khách để thực hiện các bước tiếp theo.\n\n");
        content.append("Trân trọng,\nTMM System");
        
        return content.toString();
    }
}
