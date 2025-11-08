package tmmsystem.service;

import org.springframework.stereotype.Service;
import tmmsystem.entity.Quotation;
import tmmsystem.entity.Contract;

import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    public void sendQuotationEmail(Quotation quotation) {
        // TODO: Implement actual email sending
        // For now, just log the email content
        String emailContent = generateQuotationEmailContent(quotation);
        System.out.println("=== EMAIL SENT TO CUSTOMER ===");
        System.out.println("To: " + quotation.getCustomer().getEmail());
        System.out.println("Subject: Báo giá mới từ TMM System");
        System.out.println("Content:");
        System.out.println(emailContent);
        System.out.println("===============================");
    }

    // NEW: gửi email có login info (mật khẩu tạm nếu có)
    public void sendQuotationEmailWithLogin(Quotation quotation, String tempPassword) {
        StringBuilder content = new StringBuilder(generateQuotationEmailContent(quotation));
        content.append("\nĐăng nhập để phê duyệt báo giá:\n");
        content.append("Portal: https://portal.example.com/login\n");
        content.append("Email đăng nhập: ").append(quotation.getCustomer().getEmail()).append("\n");
        if (tempPassword != null) {
            content.append("Mật khẩu tạm: ").append(tempPassword).append(" (hãy đổi mật khẩu sau khi đăng nhập)\n");
        } else {
            content.append("Bạn đã có tài khoản. Đăng nhập bằng mật khẩu hiện tại của bạn.\n");
        }
        content.append("Link báo giá: https://portal.example.com/quotations/").append(quotation.getId()).append("\n");
        System.out.println("=== QUOTATION EMAIL ===\n" + content + "\n====================");
    }

    public void sendOrderConfirmationEmail(Contract contract) {
        // TODO: Implement actual email sending
        String emailContent = generateOrderConfirmationEmailContent(contract);
        System.out.println("=== ORDER CONFIRMATION EMAIL ===");
        System.out.println("To: " + contract.getCustomer().getEmail());
        System.out.println("Subject: Xác nhận đơn hàng #" + contract.getContractNumber());
        System.out.println("Content:");
        System.out.println(emailContent);
        System.out.println("===============================");
    }

    private String generateQuotationEmailContent(Quotation quotation) {
        StringBuilder content = new StringBuilder();
        content.append("Kính gửi ").append(quotation.getCustomer().getCompanyName()).append(",\n\n");
        content.append("Chúng tôi xin gửi báo giá cho yêu cầu của quý khách:\n\n");
        content.append("Mã báo giá: ").append(quotation.getQuotationNumber()).append("\n");
        content.append("Ngày tạo: ").append(quotation.getCreatedAt().toString()).append("\n");
        content.append("Hiệu lực đến: ").append(quotation.getValidUntil().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        content.append("Tổng giá trị: ").append(quotation.getTotalAmount()).append(" VND\n\n");
        
        content.append("Chi tiết sản phẩm:\n");
        if (quotation.getDetails() != null) {
            for (var detail : quotation.getDetails()) {
                content.append("- ").append(detail.getProduct().getName())
                       .append(" x ").append(detail.getQuantity())
                       .append(" = ").append(detail.getTotalPrice()).append(" VND\n");
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
        content.append("Ngày ký hợp đồng: ").append(contract.getContractDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        content.append("Ngày giao hàng dự kiến: ").append(contract.getDeliveryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        content.append("Tổng giá trị: ").append(contract.getTotalAmount()).append(" VND\n\n");
        
        content.append("Chúng tôi sẽ liên hệ với quý khách để thực hiện các bước tiếp theo.\n\n");
        content.append("Trân trọng,\nTMM System");
        
        return content.toString();
    }
}
