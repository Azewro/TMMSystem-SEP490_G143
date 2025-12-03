package tmmsystem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tmmsystem.entity.Quotation;
import tmmsystem.entity.QuotationDetail;
import tmmsystem.entity.Contract;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final MailService mailService;

    public EmailService(MailService mailService) {
        this.mailService = mailService;
    }

    public void sendQuotationEmail(Quotation quotation) {
        try {
            String emailContent = generateQuotationEmailHtml(quotation);
            String to = quotation.getCustomer().getEmail();
            String subject = "Báo giá mới từ TMM System";

            if (to == null || to.isBlank()) {
                log.warn("Cannot send quotation email: customer email is null or empty for quotation #{}",
                        quotation.getQuotationNumber());
                return;
            }

            mailService.sendHtml(to, subject, emailContent);
            log.info("Quotation email sent successfully to {} for quotation #{}", to, quotation.getQuotationNumber());
        } catch (Exception e) {
            log.error("Failed to send quotation email for quotation #{}: {}", quotation.getQuotationNumber(),
                    e.getMessage(), e);
        }
    }

    // NEW: gửi email có login info (mật khẩu tạm nếu có)
    public void sendQuotationEmailWithLogin(Quotation quotation, String tempPassword) {
        try {
            String htmlContent = generateQuotationEmailHtml(quotation);

            // Thêm phần login info vào HTML
            StringBuilder loginSection = new StringBuilder();
            loginSection.append(
                    "<div style='margin-top: 30px; padding: 20px; background-color: #f8f9fa; border-radius: 5px;'>");
            loginSection.append("<h3 style='color: #007bff; margin-top: 0;'>Đăng nhập để phê duyệt báo giá:</h3>");
            String baseUrl = "https://tmmsystem-sep490g143-front-production.up.railway.app";
            loginSection.append("<p><strong>Portal:</strong> <a href='").append(baseUrl).append("/login'>")
                    .append(baseUrl).append("/login</a></p>");
            loginSection.append("<p><strong>Email đăng nhập:</strong> ").append(quotation.getCustomer().getEmail())
                    .append("</p>");
            if (tempPassword != null && !tempPassword.isBlank()) {
                loginSection
                        .append("<p><strong>Mật khẩu tạm:</strong> <span style='color: #dc3545; font-weight: bold;'>")
                        .append(tempPassword).append("</span> (hãy đổi mật khẩu sau khi đăng nhập)</p>");
            } else {
                loginSection.append("<p>Bạn đã có tài khoản. Đăng nhập bằng mật khẩu hiện tại của bạn.</p>");
            }
            loginSection.append("<p><strong>Link báo giá:</strong> <a href='").append(baseUrl)
                    .append("/customer/quotations/").append(quotation.getId()).append("'>Xem chi tiết báo giá</a></p>");
            loginSection.append("</div>");

            // Chèn login section trước closing tag của body
            htmlContent = htmlContent.replace("</body>", loginSection.toString() + "</body>");

            String to = quotation.getCustomer().getEmail();
            String subject = "Báo giá mới từ TMM System";

            if (to == null || to.isBlank()) {
                log.warn("Cannot send quotation email with login: customer email is null or empty for quotation #{}",
                        quotation.getQuotationNumber());
                return;
            }

            mailService.sendHtml(to, subject, htmlContent);
            log.info("Quotation email with login sent successfully to {} for quotation #{}", to,
                    quotation.getQuotationNumber());
        } catch (Exception e) {
            log.error("Failed to send quotation email with login for quotation #{}: {}", quotation.getQuotationNumber(),
                    e.getMessage(), e);
        }
    }

    public void sendOrderConfirmationEmail(Contract contract) {
        try {
            String emailContent = generateOrderConfirmationEmailContent(contract);
            String to = contract.getCustomer().getEmail();
            String subject = "Xác nhận đơn hàng #" + contract.getContractNumber();

            if (to == null || to.isBlank()) {
                log.warn("Cannot send order confirmation email: customer email is null or empty for contract #{}",
                        contract.getContractNumber());
                return;
            }

            mailService.send(to, subject, emailContent);
            log.info("Order confirmation email sent successfully to {} for contract #{}", to,
                    contract.getContractNumber());
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for contract #{}: {}", contract.getContractNumber(),
                    e.getMessage(), e);
        }
    }

    private String generateQuotationEmailHtml(Quotation quotation) {
        StringBuilder html = new StringBuilder();

        // HTML Header
        html.append("<!DOCTYPE html>");
        html.append("<html lang='vi'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Bảng Báo Giá</title>");
        html.append("</head>");
        html.append(
                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 800px; margin: 0 auto; padding: 20px;'>");

        // Company Header
        html.append("<div style='text-align: center; margin-bottom: 30px;'>");
        html.append("<h1 style='color: #2c3e50; margin: 0; font-size: 24px;'>CÔNG TY TNHH DỆT MAY MỸ ĐỨC</h1>");
        html.append("<p style='margin: 5px 0; color: #666;'>Địa chỉ: X. Phùng Xá, H. Mỹ Đức, Hà Nội, Việt Nam</p>");
        if (quotation.getRfq() != null && quotation.getRfq().getRfqNumber() != null) {
            html.append("<p style='margin: 5px 0; color: #666;'><strong>Số:</strong> ")
                    .append(quotation.getRfq().getRfqNumber()).append("</p>");
        }
        html.append("</div>");

        // Title
        html.append(
                "<h2 style='text-align: center; color: #2c3e50; margin: 30px 0; font-size: 20px;'>BẢNG BÁO GIÁ</h2>");

        // Greeting
        html.append("<div style='margin-bottom: 20px;'>");
        html.append("<p><strong>Kính gửi:</strong> Quý khách hàng</p>");
        html.append("<p>Công Ty TNHH Dệt May Mỹ Đức xin trân trọng báo giá các sản phẩm như sau:</p>");
        html.append("</div>");

        // Product Table
        html.append("<table style='width: 100%; border-collapse: collapse; margin: 20px 0; border: 1px solid #ddd;'>");
        html.append("<thead>");
        html.append("<tr style='background-color: #f8f9fa;'>");
        html.append("<th style='border: 1px solid #ddd; padding: 12px; text-align: center;'>STT</th>");
        html.append("<th style='border: 1px solid #ddd; padding: 12px; text-align: left;'>THÔNG TIN SẢN PHẨM</th>");
        html.append("<th style='border: 1px solid #ddd; padding: 12px; text-align: center;'>SỐ LƯỢNG</th>");
        html.append("<th style='border: 1px solid #ddd; padding: 12px; text-align: center;'>KHỐI LƯỢNG (kg)</th>");
        html.append("<th style='border: 1px solid #ddd; padding: 12px; text-align: right;'>ĐƠN GIÁ (VNĐ)</th>");
        html.append("<th style='border: 1px solid #ddd; padding: 12px; text-align: right;'>THÀNH TIỀN (VNĐ)</th>");
        html.append("</tr>");
        html.append("</thead>");
        html.append("<tbody>");

        BigDecimal totalWeight = BigDecimal.ZERO;
        int stt = 1;

        if (quotation.getDetails() != null && !quotation.getDetails().isEmpty()) {
            for (QuotationDetail detail : quotation.getDetails()) {
                String productName = detail.getProduct() != null ? detail.getProduct().getName() : "N/A";
                String dimensions = detail.getProduct() != null && detail.getProduct().getStandardDimensions() != null
                        ? detail.getProduct().getStandardDimensions()
                        : "";
                BigDecimal quantity = detail.getQuantity() != null ? detail.getQuantity() : BigDecimal.ZERO;
                BigDecimal weight = detail.getProduct() != null && detail.getProduct().getStandardWeight() != null
                        ? detail.getProduct().getStandardWeight().multiply(quantity)
                        : BigDecimal.ZERO;
                BigDecimal unitPrice = detail.getUnitPrice() != null ? detail.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal totalPrice = detail.getTotalPrice() != null ? detail.getTotalPrice() : BigDecimal.ZERO;

                totalWeight = totalWeight.add(weight);

                html.append("<tr>");
                html.append("<td style='border: 1px solid #ddd; padding: 10px; text-align: center;'>").append(stt++)
                        .append("</td>");
                html.append("<td style='border: 1px solid #ddd; padding: 10px;'>");
                html.append("<strong>").append(productName).append("</strong>");
                if (!dimensions.isEmpty()) {
                    html.append("<br><span style='color: #666; font-size: 0.9em;'>Kích thước: ").append(dimensions)
                            .append("</span>");
                }
                html.append("</td>");
                html.append("<td style='border: 1px solid #ddd; padding: 10px; text-align: center;'>")
                        .append(String.format("%,.0f", quantity.doubleValue())).append("</td>");
                html.append("<td style='border: 1px solid #ddd; padding: 10px; text-align: center;'>")
                        .append(String.format("%,.2f", weight.doubleValue())).append("</td>");
                html.append("<td style='border: 1px solid #ddd; padding: 10px; text-align: right;'>")
                        .append(String.format("%,.0f", unitPrice.doubleValue())).append(" ₫</td>");
                html.append("<td style='border: 1px solid #ddd; padding: 10px; text-align: right; font-weight: bold;'>")
                        .append(String.format("%,.0f", totalPrice.doubleValue())).append(" ₫</td>");
                html.append("</tr>");
            }
        }

        // Total Row
        html.append("<tr style='background-color: #f8f9fa; font-weight: bold;'>");
        html.append(
                "<td colspan='3' style='border: 1px solid #ddd; padding: 12px; text-align: right;'>TỔNG CỘNG:</td>");
        html.append("<td style='border: 1px solid #ddd; padding: 12px; text-align: center;'>")
                .append(String.format("%,.2f", totalWeight.doubleValue())).append("</td>");
        html.append("<td style='border: 1px solid #ddd; padding: 12px;'></td>");
        html.append(
                "<td style='border: 1px solid #ddd; padding: 12px; text-align: right; color: #007bff; font-size: 1.1em;'>");
        if (quotation.getTotalAmount() != null) {
            html.append(String.format("%,.0f", quotation.getTotalAmount().doubleValue())).append(" ₫");
        }
        html.append("</td>");
        html.append("</tr>");

        html.append("</tbody>");
        html.append("</table>");

        // Total in words (simplified)
        if (quotation.getTotalAmount() != null) {
            html.append("<p style='margin: 20px 0;'><em>(Bằng chữ: ")
                    .append(numberToWords(quotation.getTotalAmount().longValue())).append(" đồng)</em></p>");
        }

        // Notes
        html.append(
                "<div style='margin: 20px 0; padding: 15px; background-color: #fff3cd; border-left: 4px solid #ffc107;'>");
        html.append("<p style='margin: 5px 0;'><strong>Ghi chú:</strong></p>");
        html.append("<ul style='margin: 5px 0; padding-left: 20px;'>");
        html.append("<li>Đơn giá chưa bao gồm thuế VAT</li>");
        html.append("<li>Đơn giá chưa bao gồm phí vận chuyển</li>");
        html.append("</ul>");
        html.append("</div>");

        // Closing
        html.append("<div style='margin-top: 40px; text-align: right;'>");
        html.append("<p style='margin: 5px 0;'>Trân trọng kính chào!</p>");
        if (quotation.getCreatedAt() != null) {
            String createdAtFormatted = quotation.getCreatedAt()
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            html.append("<p style='margin: 5px 0;'>Hà Nội, ngày ").append(createdAtFormatted).append("</p>");
        }
        html.append("<p style='margin: 20px 0 5px 0; font-weight: bold;'>CÔNG TY TNHH DỆT MAY MỸ ĐỨC</p>");
        html.append("</div>");

        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    private String getStatusText(String status) {
        if (status == null)
            return "Nháp";
        return switch (status) {
            case "DRAFT" -> "Nháp";
            case "SENT" -> "Đã gửi";
            case "ACCEPTED" -> "Đã chấp nhận";
            case "REJECTED" -> "Đã từ chối";
            case "EXPIRED" -> "Hết hạn";
            case "CANCELED" -> "Đã hủy";
            case "ORDER_CREATED" -> "Đã tạo đơn hàng";
            default -> status;
        };
    }

    private String getStatusColor(String status) {
        if (status == null)
            return "#6c757d";
        return switch (status) {
            case "DRAFT" -> "#6c757d";
            case "SENT" -> "#007bff";
            case "ACCEPTED", "ORDER_CREATED" -> "#28a745";
            case "REJECTED", "CANCELED" -> "#dc3545";
            case "EXPIRED" -> "#ffc107";
            default -> "#6c757d";
        };
    }

    private String numberToWords(long number) {
        // Simplified Vietnamese number to words converter
        // For full implementation, you might want to use a library
        if (number == 0)
            return "Không";

        String[] units = { "", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín" };
        String[] tens = { "", "mười", "hai mươi", "ba mươi", "bốn mươi", "năm mươi",
                "sáu mươi", "bảy mươi", "tám mươi", "chín mươi" };
        String[] hundreds = { "", "một trăm", "hai trăm", "ba trăm", "bốn trăm", "năm trăm",
                "sáu trăm", "bảy trăm", "tám trăm", "chín trăm" };

        if (number < 10) {
            return units[(int) number];
        } else if (number < 100) {
            int ten = (int) (number / 10);
            int unit = (int) (number % 10);
            if (unit == 0) {
                return tens[ten];
            } else if (unit == 5 && ten == 1) {
                return "mười lăm";
            } else if (unit == 1 && ten == 1) {
                return "mười một";
            } else if (unit == 1) {
                return tens[ten] + " mốt";
            } else {
                return tens[ten] + " " + units[unit];
            }
        } else if (number < 1000) {
            int hundred = (int) (number / 100);
            int remainder = (int) (number % 100);
            if (remainder == 0) {
                return hundreds[hundred];
            } else {
                return hundreds[hundred] + " " + numberToWords(remainder);
            }
        } else if (number < 1000000) {
            int thousand = (int) (number / 1000);
            int remainder = (int) (number % 1000);
            if (remainder == 0) {
                return numberToWords(thousand) + " nghìn";
            } else {
                return numberToWords(thousand) + " nghìn " + numberToWords(remainder);
            }
        } else {
            return String.format("%,d", number); // Fallback to number if too large
        }
    }

    private String generateOrderConfirmationEmailContent(Contract contract) {
        StringBuilder content = new StringBuilder();
        content.append("Kính gửi ").append(contract.getCustomer().getCompanyName()).append(",\n\n");
        content.append("Cảm ơn quý khách đã chấp nhận báo giá. Đơn hàng của quý khách đã được tạo:\n\n");
        content.append("Mã đơn hàng: ").append(contract.getContractNumber()).append("\n");
        if (contract.getContractDate() != null) {
            content.append("Ngày ký hợp đồng: ")
                    .append(contract.getContractDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        }
        if (contract.getDeliveryDate() != null) {
            content.append("Ngày giao hàng dự kiến: ")
                    .append(contract.getDeliveryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        }
        if (contract.getTotalAmount() != null) {
            content.append("Tổng giá trị: ").append(String.format("%,.0f", contract.getTotalAmount().doubleValue()))
                    .append(" VND\n\n");
        }

        content.append("Chúng tôi sẽ liên hệ với quý khách để thực hiện các bước tiếp theo.\n\n");
        content.append("Trân trọng,\nTMM System");

        return content.toString();
    }
}
