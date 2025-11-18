# Kiểm tra Hệ thống Gửi Email trong Backend

## 📧 Các Loại Email Được Gửi

### 1. Email Báo Giá (Quotation Email)
**Phương thức:** `EmailService.sendQuotationEmail(Quotation)`
- **Khi nào:** Khi Sales Staff gửi báo giá cho khách hàng
- **Điểm gọi:** `QuotationService.sendQuotationToCustomer()`
- **Nội dung:**
  - Thông tin khách hàng
  - Mã báo giá
  - Ngày tạo
  - Hiệu lực đến
  - Tổng giá trị
  - Chi tiết sản phẩm (tên, số lượng, giá)
  - Link truy cập hệ thống

### 2. Email Báo Giá Kèm Thông Tin Đăng Nhập
**Phương thức:** `EmailService.sendQuotationEmailWithLogin(Quotation, String tempPassword)`
- **Khi nào:** Khi Planning tạo báo giá từ RFQ (tự động gửi cho khách hàng)
- **Điểm gọi:** `QuotationService.createQuotationFromRfq()`
- **Nội dung:**
  - Tất cả nội dung của email báo giá thông thường
  - Thêm thông tin đăng nhập:
    - Link portal đăng nhập
    - Email đăng nhập
    - Mật khẩu tạm (nếu khách hàng chưa có tài khoản)
    - Link trực tiếp đến báo giá

### 3. Email Xác Nhận Đơn Hàng (Order Confirmation Email)
**Phương thức:** `EmailService.sendOrderConfirmationEmail(Contract)`
- **Khi nào:** Khi Customer chấp nhận báo giá (tự động tạo đơn hàng)
- **Điểm gọi:** `QuotationService.approveQuotation()` → `createOrderFromQuotation()`
- **Nội dung:**
  - Thông tin khách hàng
  - Mã đơn hàng
  - Ngày ký hợp đồng
  - Ngày giao hàng dự kiến
  - Tổng giá trị
  - Thông báo sẽ liên hệ để thực hiện các bước tiếp theo

## 🔄 Luồng Gửi Email trong Quy Trình Đặt Hàng

### Giai Đoạn 1: RFQ → Quotation
1. **Planning tạo báo giá từ RFQ**
   - `POST /v1/quotations/create-from-rfq`
   - ✅ Gửi email: `sendQuotationEmailWithLogin()` (kèm thông tin đăng nhập)
   - 📧 Người nhận: Customer (email từ `quotation.getCustomer().getEmail()`)
   - **Logic:**
     - Nếu customer **chưa có tài khoản** (password = null/blank):
       - Hệ thống tự động cấp mật khẩu tạm
       - Email bao gồm: mật khẩu tạm + link đăng nhập + link báo giá
     - Nếu customer **đã có tài khoản**:
       - Email bao gồm: thông báo "Bạn đã có tài khoản" + link đăng nhập + link báo giá
   - **Mục đích:** Cho phép customer đăng nhập vào hệ thống để xem và phê duyệt báo giá

### Giai Đoạn 2: Quotation → Customer
2. **Sales gửi báo giá cho khách hàng**
   - `POST /v1/quotations/{id}/send-to-customer`
   - ✅ Gửi email: `sendQuotationEmail()` (email báo giá thông thường)
   - 📧 Người nhận: Customer (email từ `quotation.getCustomer().getEmail()`)
   - **Điều kiện:** Báo giá phải ở trạng thái `DRAFT`
   - **Logic:**
     - Customer **đã có tài khoản** và đã đăng nhập vào hệ thống
     - Customer đã tạo RFQ qua hệ thống (đã đăng nhập)
     - Email chỉ chứa thông tin báo giá, không có thông tin đăng nhập
   - **Mục đích:** Thông báo cho customer về báo giá mới (customer đã biết cách đăng nhập)

### Giai Đoạn 3: Customer Approve → Order Created
3. **Customer chấp nhận báo giá**
   - `POST /v1/quotations/{id}/approve`
   - ✅ Gửi email: `sendOrderConfirmationEmail()` (xác nhận đơn hàng)
   - 📧 Người nhận: Customer (email từ `contract.getCustomer().getEmail()`)

## ⚠️ Điều Kiện Gửi Email

### Kiểm Tra Email Trước Khi Gửi
Tất cả các phương thức gửi email đều kiểm tra:
```java
if (to == null || to.isBlank()) {
    log.warn("Cannot send email: customer email is null or empty");
    return; // Không gửi email nếu không có email
}
```

### Xử Lý Lỗi
- Tất cả các lần gửi email đều được bọc trong `try-catch`
- Nếu gửi email thất bại, hệ thống sẽ:
  - Log lỗi nhưng không throw exception
  - Tiếp tục xử lý business logic bình thường
  - Không ảnh hưởng đến quy trình chính

## 📝 Ghi Chú

### Email Service Implementation
- **MailService**: Sử dụng `JavaMailSender` từ Spring Boot
- **Async Processing**: Email được gửi bất đồng bộ (`@Async("mailExecutor")`)
- **Format**: Plain text email (không phải HTML)

### Notification vs Email
- **Notification**: Gửi thông báo trong hệ thống cho internal users (Sales, Planning, Director, etc.)
- **Email**: Gửi email thực tế cho Customer (external)

### Các Trường Hợp KHÔNG Gửi Email
1. ❌ RFQ mới được tạo → Chỉ có notification cho Sales Staff
2. ❌ RFQ được chuyển đến Planning → Chỉ có notification cho Planning Staff
3. ❌ Báo giá được tạo nhưng chưa gửi → Chỉ có notification cho Sales Staff
4. ❌ Hợp đồng được upload → Chỉ có notification cho Director
5. ❌ Hợp đồng được duyệt → Chỉ có notification cho Planning Staff
6. ❌ Production Order được tạo → Chỉ có notification cho Director

**Chỉ có 3 trường hợp gửi email thực tế cho Customer:**
1. ✅ Planning tạo báo giá từ RFQ 
   - **Trường hợp:** Customer có thể chưa có tài khoản hoặc đã có tài khoản
   - **Email:** `sendQuotationEmailWithLogin()` (luôn có thông tin đăng nhập)
   - **Lý do:** Planning tự động tạo và gửi báo giá, customer cần đăng nhập để xem/phê duyệt
   
2. ✅ Sales gửi báo giá cho khách hàng
   - **Trường hợp:** Customer đã có tài khoản và đã đăng nhập (đã tạo RFQ qua hệ thống)
   - **Email:** `sendQuotationEmail()` (email báo giá thông thường, không có login info)
   - **Lý do:** Customer đã biết cách đăng nhập, chỉ cần thông báo về báo giá mới
   
3. ✅ Customer chấp nhận báo giá (email xác nhận đơn hàng)

## 🔍 Cấu Hình Email

### MailService Configuration
- Sử dụng `JavaMailSender` từ Spring Boot
- Cần cấu hình trong `application.properties` hoặc `application.yml`:
  ```properties
  spring.mail.host=smtp.gmail.com
  spring.mail.port=587
  spring.mail.username=your-email@gmail.com
  spring.mail.password=your-password
  spring.mail.properties.mail.smtp.auth=true
  spring.mail.properties.mail.smtp.starttls.enable=true
  ```

### Async Executor Configuration
- Email được gửi bất đồng bộ qua `mailExecutor`
- Cần cấu hình executor trong Spring configuration

