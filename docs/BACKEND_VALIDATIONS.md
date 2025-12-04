### Tổng hợp validate backend cần implement theo frontend

**Mục tiêu**: Ghi rõ, chi tiết, **cho từng API endpoint cụ thể**: mỗi field trong DTO/Request cần được validate thế nào (required, regex, min/max, rule custom) để đảm bảo đồng bộ với frontend và tránh người dùng bypass qua F12.

---

## 1. Nhóm Auth Controller (`tmmsystem.controller.AuthController`)

### 1.1. `POST /v1/auth/customer/login` - Đăng nhập khách hàng

**DTO**: `tmmsystem.dto.auth.LoginRequest` (record)

**Fields cần validate**:
- **`email`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Email không được để trống.")`
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Format email**: `@Email(message = "Email không hợp lệ.")` hoặc regex `/\S+@\S+\.\S+/`
  - **Max length**: `@Size(max = 150, message = "Email không được quá 150 ký tự")`

- **`password`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Mật khẩu không được để trống.")`
  - **Trim whitespace**: Kiểm tra sau trim, nếu rỗng → trả lỗi
  - **Min length**: `@Size(min = 1, message = "Mật khẩu không được để trống.")`

**Lưu ý**: Controller hiện tại không có `@Valid` annotation trên `@RequestBody LoginRequest req`. Cần thêm `@Valid` để kích hoạt validation.

---

### 1.2. `POST /v1/auth/user/login` - Đăng nhập nội bộ

**DTO**: `tmmsystem.dto.auth.LoginRequest` (record)

**Fields cần validate** (giống customer login):
- **`email`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Email không được để trống.")`
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Format email**: `@Email(message = "Email không hợp lệ.")`
  - **Max length**: `@Size(max = 150, message = "Email không được quá 150 ký tự")`

- **`password`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Mật khẩu không được để trống.")`
  - **Trim whitespace**: Kiểm tra sau trim, nếu rỗng → trả lỗi

**Lưu ý**: Controller hiện tại không có `@Valid` annotation. Cần thêm `@Valid` và cập nhật `LoginRequest` record để có validation annotations.

---

### 1.3. `POST /v1/auth/customer/forgot-password` - Khách hàng quên mật khẩu

**DTO**: `tmmsystem.dto.auth.ForgotPasswordRequest` (record)

**Fields cần validate**:
- **`email`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Email không được để trống.")`
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Format email**: `@Email(message = "Vui lòng nhập đúng định dạng Email")` hoặc regex `/\S+@\S+\.\S+/`
  - **Max length**: `@Size(max = 150, message = "Email không được quá 150 ký tự")`

**Lưu ý**: Controller hiện tại không có `@Valid` annotation. Cần thêm `@Valid` và cập nhật `ForgotPasswordRequest` record để có validation annotations.

---

### 1.4. `POST /v1/auth/forgot-password` - Nhân viên nội bộ quên mật khẩu

**DTO**: `tmmsystem.dto.auth.ForgotPasswordRequest` (record)

**Fields cần validate** (giống customer forgot password):
- **`email`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Email không được để trống.")`
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Format email**: `@Email(message = "Vui lòng nhập đúng định dạng Email")`
  - **Max length**: `@Size(max = 150, message = "Email không được quá 150 ký tự")`

**Lưu ý**: Controller hiện tại không có `@Valid` annotation. Cần thêm `@Valid` và cập nhật `ForgotPasswordRequest` record để có validation annotations.

---

### 1.5. `POST /v1/auth/customer/change-password` - Khách hàng đổi mật khẩu

**DTO**: `tmmsystem.dto.auth.ChangePasswordRequest` (record)

**Fields cần validate**:
- **`email`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Email không được để trống.")`
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Format email**: `@Email(message = "Email không hợp lệ.")`
  - **Max length**: `@Size(max = 150, message = "Email không được quá 150 ký tự")`

- **`currentPassword`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Mật khẩu hiện tại không được để trống.")`
  - **Trim whitespace**: Kiểm tra sau trim, nếu rỗng → trả lỗi

- **`newPassword`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Mật khẩu mới không được để trống.")`
  - **Min length**: `@Size(min = 8, message = "Mật khẩu mới phải có ít nhất 8 ký tự.")`
  - **Không chứa khoảng trắng**: Custom validation với regex `^[^\s]+$` hoặc kiểm tra `!newPassword.contains(" ")` → message: "Mật khẩu không được chứa khoảng trắng"
  - **Phải chứa ít nhất 1 chữ số**: Custom validation với regex `.*\d.*` → message: "Mật khẩu phải chứa ít nhất 1 chữ số và 1 chữ in hoa"
  - **Phải chứa ít nhất 1 chữ in hoa**: Custom validation với regex `.*[A-Z].*` → message: "Mật khẩu phải chứa ít nhất 1 chữ số và 1 chữ in hoa"
  - **Không được trùng với currentPassword**: Custom validation → message: "Mật khẩu mới không được trùng với mật khẩu hiện tại."

**Lưu ý**: Controller hiện tại không có `@Valid` annotation. Cần thêm `@Valid` và cập nhật `ChangePasswordRequest` record để có validation annotations. Cần implement custom validator cho `newPassword` để kiểm tra các rule phức tạp.

---

### 1.6. `POST /v1/auth/change-password` - Nhân viên nội bộ đổi mật khẩu

**DTO**: `tmmsystem.dto.auth.ChangePasswordRequest` (record)

**Fields cần validate** (giống customer change password):
- **`email`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Email không được để trống.")`
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Format email**: `@Email(message = "Email không hợp lệ.")`
  - **Max length**: `@Size(max = 150, message = "Email không được quá 150 ký tự")`

- **`currentPassword`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Mật khẩu hiện tại không được để trống.")`
  - **Trim whitespace**: Kiểm tra sau trim, nếu rỗng → trả lỗi

- **`newPassword`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Mật khẩu mới không được để trống.")`
  - **Min length**: `@Size(min = 8, message = "Mật khẩu mới phải có ít nhất 8 ký tự.")`
  - **Không chứa khoảng trắng**: Custom validation với regex `^[^\s]+$` → message: "Mật khẩu không được chứa khoảng trắng"
  - **Phải chứa ít nhất 1 chữ số và 1 chữ in hoa**: Custom validation → message: "Mật khẩu phải chứa ít nhất 1 chữ số và 1 chữ in hoa"
  - **Không được trùng với currentPassword**: Custom validation → message: "Mật khẩu mới không được trùng với mật khẩu hiện tại."

**Lưu ý**: Controller hiện tại không có `@Valid` annotation. Cần thêm `@Valid` và cập nhật `ChangePasswordRequest` record để có validation annotations.

---

### 1.7. `POST /v1/auth/customer/verify-reset-code` - Xác minh mã reset password khách hàng

**DTO**: `tmmsystem.dto.auth.VerifyResetCodeRequest` (record)

**Fields cần validate**:
- **`email`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Email không được để trống.")`
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Format email**: `@Email(message = "Email không hợp lệ.")`
  - **Max length**: `@Size(max = 150, message = "Email không được quá 150 ký tự")`

- **`code`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Mã xác minh không được để trống.")`
  - **Trim whitespace**: Kiểm tra sau trim, nếu rỗng → trả lỗi

**Lưu ý**: Controller hiện tại không có `@Valid` annotation. Cần thêm `@Valid` và cập nhật `VerifyResetCodeRequest` record để có validation annotations.

---

### 1.8. `POST /v1/auth/verify-reset-code` - Xác minh mã reset password nội bộ

**DTO**: `tmmsystem.dto.auth.VerifyResetCodeRequest` (record)

**Fields cần validate** (giống customer verify reset code):
- **`email`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Email không được để trống.")`
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Format email**: `@Email(message = "Email không hợp lệ.")`
  - **Max length**: `@Size(max = 150, message = "Email không được quá 150 ký tự")`

- **`code`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Mã xác minh không được để trống.")`
  - **Trim whitespace**: Kiểm tra sau trim, nếu rỗng → trả lỗi

**Lưu ý**: Controller hiện tại không có `@Valid` annotation. Cần thêm `@Valid` và cập nhật `VerifyResetCodeRequest` record để có validation annotations.

---

### 1.9. `POST /v1/auth/customer/create-company` - Khách hàng tạo/cập nhật công ty

**DTO**: `tmmsystem.dto.CustomerCreateRequest` (record)

**Fields cần validate**:
- **`companyName`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Tên công ty là bắt buộc")` (cần sửa từ "Tên công ty không được để trống" thành "Tên công ty là bắt buộc" để khớp frontend ConfirmOrderProfileModal)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Max length**: `@Size(max = 255, message = "Tên công ty không được quá 255 ký tự")` (đã có)

- **`contactPerson`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Người liên hệ là bắt buộc")` (hiện tại chỉ có `@Size`, cần thêm `@NotBlank`)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Không chứa ký tự đặc biệt**: Custom validation với regex `^[^!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]*$` → message: "Tên người liên hệ không hợp lệ."
  - **Max length**: `@Size(max = 150, message = "Người liên hệ không được quá 150 ký tự")` (đã có)

- **`email`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Email là bắt buộc")` (hiện tại chỉ có `@Email`, cần thêm `@NotBlank`)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Format email**: `@Email(message = "Email không hợp lệ.")` (cần thêm dấu chấm để khớp frontend CreateCustomerModal và CreateUserModal)
  - **Max length**: `@Size(max = 150, message = "Email không được quá 150 ký tự")` (đã có)

- **`phoneNumber`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Số điện thoại là bắt buộc")` (hiện tại chỉ có `@Size`, cần thêm `@NotBlank`)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Format số điện thoại Việt Nam**: Custom validation với regex `^(?:\+84|84|0)(?:2\d{1,2}([-.]?)\d{7,8}|(?:3\d|5\d|7\d|8\d|9\d)([-.]?)\d{3}\2\d{4})$` → message: "Số điện thoại không hợp lệ."
  - **Max length**: `@Size(max = 30, message = "Số điện thoại không được quá 30 ký tự")` (đã có)

- **`address`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Địa chỉ là bắt buộc")` (hiện tại chỉ có `@Size`, cần thêm `@NotBlank`)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Max length**: `@Size(max = 1000, message = "Địa chỉ không được quá 1000 ký tự")` (đã có)

- **`taxCode`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Mã số thuế là bắt buộc")` (hiện tại chỉ có `@Size`, cần thêm `@NotBlank`)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Format mã số thuế**: Custom validation với regex `^[0-9]{10,13}$` → message: "Mã số thuế không hợp lệ."
  - **Max length**: `@Size(max = 50, message = "Mã số thuế không được quá 50 ký tự")` (đã có)

**Lưu ý**: Controller đã có `@Valid` annotation. Cần cập nhật `CustomerCreateRequest` record để thêm các validation annotations còn thiếu và implement custom validators cho `contactPerson`, `phoneNumber`, `taxCode`.

---

## 2. Nhóm User Controller (`tmmsystem.controller.UserController`)

### 2.1. `POST /api/admin/users` - Tạo user mới

**DTO**: `tmmsystem.dto.user.CreateUserRequest` (class)

**Fields cần validate**:
- **`email`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Email là bắt buộc")` (đã có `@Email @NotBlank`)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Format email**: `@Email(message = "Email không hợp lệ.")` (đã có)
  - **Max length**: `@Size(max = 150, message = "Email không được quá 150 ký tự")` (đã có)

- **`password`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Mật khẩu là bắt buộc")` (đã có `@NotBlank`)
  - **Trim whitespace**: Kiểm tra sau trim, nếu rỗng → trả lỗi
  - **Min length**: `@Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự.")` (hiện tại có `@Size(min = 6, max = 100)`, cần sửa thành min = 8)
  - **Max length**: `@Size(max = 100, message = "Mật khẩu không được quá 100 ký tự")` (đã có)

- **`name`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Họ và tên là bắt buộc")` (hiện tại không có `@NotBlank`, cần thêm)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Không chứa ký tự đặc biệt**: Custom validation với regex `^[^!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]*$` → message: "Tên người liên hệ không hợp lệ."
  - **Max length**: `@Size(max = 255, message = "Họ và tên không được quá 255 ký tự")` (cần thêm)

- **`phoneNumber`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Số điện thoại là bắt buộc")` (hiện tại không có `@NotBlank`, cần thêm)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Format số điện thoại Việt Nam**: Custom validation với regex `^(?:\+84|84|0)(?:2\d{1,2}([-.]?)\d{7,8}|(?:3\d|5\d|7\d|8\d|9\d)([-.]?)\d{3}\2\d{4})$` → message: "Số điện thoại không hợp lệ."
  - **Max length**: `@Size(max = 30, message = "Số điện thoại không được quá 30 ký tự")` (cần thêm)

- **`roleId`** (Long):
  - **Bắt buộc**: `@NotNull(message = "Vai trò là bắt buộc")` (đã có `@NotNull`)

**Lưu ý**: Controller hiện tại không có `@Valid` annotation trên `@RequestBody CreateUserRequest req`. Cần thêm `@Valid` để kích hoạt validation. Cần cập nhật `CreateUserRequest` class để thêm các validation annotations còn thiếu và implement custom validators cho `name` và `phoneNumber`.

---

### 2.2. `PUT /api/admin/users/{id}` - Cập nhật user

**DTO**: `tmmsystem.dto.user.UpdateUserRequest` (class)

**Fields cần validate**:
- **`name`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Họ và tên là bắt buộc")` (hiện tại không có `@NotBlank`, cần thêm)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Không chứa ký tự đặc biệt**: Custom validation với regex `^[^!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]*$` → message: "Tên người liên hệ không hợp lệ."
  - **Max length**: `@Size(max = 255, message = "Họ và tên không được quá 255 ký tự")` (cần thêm)

- **`phoneNumber`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Số điện thoại là bắt buộc")` (hiện tại không có `@NotBlank`, cần thêm)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Format số điện thoại Việt Nam**: Custom validation với regex `^(?:\+84|84|0)(?:2\d{1,2}([-.]?)\d{7,8}|(?:3\d|5\d|7\d|8\d|9\d)([-.]?)\d{3}\2\d{4})$` → message: "Số điện thoại không hợp lệ."
  - **Max length**: `@Size(max = 30, message = "Số điện thoại không được quá 30 ký tự")` (cần thêm)

- **`password`** (String):
  - **Optional**: Nếu có giá trị (không null và không rỗng sau trim):
    - **Min length**: `@Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự.")` (hiện tại có `@Size(min = 0, max = 100)`, cần sửa thành min = 8)
    - **Max length**: `@Size(max = 100, message = "Mật khẩu không được quá 100 ký tự")` (đã có)

**Lưu ý**: Controller hiện tại không có `@Valid` annotation trên `@RequestBody UpdateUserRequest req`. Cần thêm `@Valid` để kích hoạt validation. Cần cập nhật `UpdateUserRequest` class để thêm các validation annotations còn thiếu và implement custom validators cho `name` và `phoneNumber`.

---

## 3. Nhóm Customer Controller (`tmmsystem.controller.CustomerController`)

### 3.1. `POST /v1/customers` - Tạo khách hàng

**DTO**: `tmmsystem.dto.CustomerCreateRequest` (record)

**Fields cần validate**:
- **`companyName`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Tên công ty là bắt buộc")` (cần sửa từ "Tên công ty không được để trống" thành "Tên công ty là bắt buộc" để khớp frontend CreateCustomerModal)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Max length**: `@Size(max = 255, message = "Tên công ty không được quá 255 ký tự")` (đã có)

- **`contactPerson`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Người liên hệ là bắt buộc")` (hiện tại chỉ có `@Size`, cần thêm `@NotBlank`)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Không chứa ký tự đặc biệt**: Custom validation với regex `^[^!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]*$` → message: "Tên người liên hệ không hợp lệ."
  - **Max length**: `@Size(max = 150, message = "Người liên hệ không được quá 150 ký tự")` (đã có)

- **`email`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Email là bắt buộc")` (hiện tại chỉ có `@Email`, cần thêm `@NotBlank`)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Format email**: `@Email(message = "Email không hợp lệ.")` (cần thêm dấu chấm để khớp frontend CreateCustomerModal và CreateUserModal)
  - **Max length**: `@Size(max = 150, message = "Email không được quá 150 ký tự")` (đã có)

- **`phoneNumber`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Số điện thoại là bắt buộc")` (hiện tại chỉ có `@Size`, cần thêm `@NotBlank`)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Format số điện thoại Việt Nam**: Custom validation với regex `^(?:\+84|84|0)(?:2\d{1,2}([-.]?)\d{7,8}|(?:3\d|5\d|7\d|8\d|9\d)([-.]?)\d{3}\2\d{4})$` → message: "Số điện thoại không hợp lệ."
  - **Max length**: `@Size(max = 30, message = "Số điện thoại không được quá 30 ký tự")` (đã có)

- **`taxCode`** (String):
  - **Optional**: Nếu có giá trị (không null và không rỗng sau trim):
    - **Format mã số thuế**: Custom validation với regex `^[0-9]{10,13}$` → message: "Mã số thuế không hợp lệ."
  - **Max length**: `@Size(max = 50, message = "Mã số thuế không được quá 50 ký tự")` (đã có)

**Lưu ý**: Controller đã có `@Valid` annotation. Cần cập nhật `CustomerCreateRequest` record để thêm các validation annotations còn thiếu và implement custom validators cho `contactPerson`, `phoneNumber`, `taxCode`.

---

### 3.2. `PUT /v1/customers/{id}` - Cập nhật khách hàng

**DTO**: `tmmsystem.dto.CustomerUpdateRequest` (class)

**Fields cần validate**:
- **`companyName`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Tên công ty là bắt buộc")` (hiện tại không có validation, cần thêm - message phải khớp frontend)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Max length**: `@Size(max = 255, message = "Tên công ty không được quá 255 ký tự")` (cần thêm)

- **`contactPerson`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Người liên hệ là bắt buộc")` (hiện tại không có validation, cần thêm)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Không chứa ký tự đặc biệt**: Custom validation với regex `^[^!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]*$` → message: "Tên người liên hệ không hợp lệ."
  - **Max length**: `@Size(max = 150, message = "Người liên hệ không được quá 150 ký tự")` (cần thêm)

- **`email`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Email là bắt buộc")` (hiện tại không có validation, cần thêm)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Format email**: `@Email(message = "Email không hợp lệ.")` (cần thêm dấu chấm - CreateCustomerModal và CreateUserModal dùng "Email không hợp lệ." có dấu chấm, nên dùng "Email không hợp lệ." để nhất quán)
  - **Max length**: `@Size(max = 150, message = "Email không được quá 150 ký tự")` (cần thêm)

- **`phoneNumber`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Số điện thoại là bắt buộc")` (hiện tại không có validation, cần thêm)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Format số điện thoại Việt Nam**: Custom validation với regex `^(?:\+84|84|0)(?:2\d{1,2}([-.]?)\d{7,8}|(?:3\d|5\d|7\d|8\d|9\d)([-.]?)\d{3}\2\d{4})$` → message: "Số điện thoại không hợp lệ."
  - **Max length**: `@Size(max = 30, message = "Số điện thoại không được quá 30 ký tự")` (cần thêm)

- **`address`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Địa chỉ là bắt buộc")` (hiện tại không có validation, cần thêm)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Max length**: `@Size(max = 1000, message = "Địa chỉ không được quá 1000 ký tự")` (cần thêm)

- **`taxCode`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Mã số thuế là bắt buộc")` (hiện tại không có validation, cần thêm)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Format mã số thuế**: Custom validation với regex `^[0-9]{10,13}$` → message: "Mã số thuế không hợp lệ."
  - **Max length**: `@Size(max = 50, message = "Mã số thuế không được quá 50 ký tự")` (cần thêm)

**Lưu ý**: Controller hiện tại không có `@Valid` annotation trên `@RequestBody CustomerUpdateRequest body`. Cần thêm `@Valid` để kích hoạt validation. Cần cập nhật `CustomerUpdateRequest` class để thêm các validation annotations và implement custom validators cho `contactPerson`, `phoneNumber`, `taxCode`.

---

## 4. Nhóm RFQ Controller (`tmmsystem.controller.RfqController`)

### 4.1. `POST /v1/rfqs/public` - Tạo RFQ public (khách chưa đăng nhập)

**DTO**: `tmmsystem.dto.sales.RfqPublicCreateDto` (class)

**Fields cần validate**:
- **`contactPerson`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Họ và tên là bắt buộc.")` (đã có `@NotBlank`)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi

- **`contactEmail`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Email là bắt buộc.")` (hiện tại chỉ có `@Email`, cần thêm `@NotBlank`)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Format email**: `@Email(message = "Email không hợp lệ.")` (đã có)

- **`contactPhone`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Số điện thoại là bắt buộc.")` (hiện tại chỉ có `@Pattern`, cần thêm `@NotBlank`)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Format số điện thoại Việt Nam**: Custom validation với regex `^(?:\+84|84|0)(?:2\d{1,2}([-.]?)\d{7,8}|(?:3\d|5\d|7\d|8\d|9\d)([-.]?)\d{3}\2\d{4})$` → message: "Số điện thoại không hợp lệ." (hiện tại có `@Pattern(regexp = "^$|^[0-9+\\-() ]{6,20}$")`, cần thay bằng regex Việt Nam chính xác)

- **`contactAddress`** (String):
  - **Bắt buộc**: Phải có đầy đủ thông tin địa chỉ (province, commune, detailedAddress). Cần validate trong service layer hoặc custom validator.

- **`expectedDeliveryDate`** (LocalDate):
  - **Bắt buộc**: `@NotNull(message = "Ngày giao hàng mong muốn là bắt buộc.")` (cần thêm)
  - **Phải >= hôm nay + 30 ngày**: Custom validation → message: "Ngày giao hàng phải ít nhất 30 ngày kể từ hôm nay."

- **`details`** (List<RfqDetailDto>):
  - **Bắt buộc**: `@NotNull(message = "Danh sách sản phẩm là bắt buộc")` và `@NotEmpty(message = "RFQ phải có ít nhất một sản phẩm.")` (cần thêm)
  - **Mỗi phần tử trong list**:
    - **`productId`** (Long): `@NotNull(message = "Vui lòng chọn sản phẩm.")` (cần thêm vào `RfqDetailDto`)
    - **`quantity`** (BigDecimal): `@DecimalMin(value = "100", message = "Số lượng tối thiểu là 100.")` (đã có)
    - **`unit`** (String): `@NotBlank(message = "Đơn vị là bắt buộc")` (cần thêm vào `RfqDetailDto`)

**Lưu ý**: Controller đã có `@Valid` annotation. Cần cập nhật `RfqPublicCreateDto` class để thêm các validation annotations còn thiếu và implement custom validators cho `contactPhone` (regex Việt Nam), `expectedDeliveryDate` (>= hôm nay + 30 ngày), và validate địa chỉ đầy đủ.

---

### 4.2. `POST /v1/rfqs` - Tạo RFQ (khách đã đăng nhập)

**DTO**: `tmmsystem.dto.sales.RfqCreateDto` (class)

**Fields cần validate**:
- **`customerId`** (Long):
  - **Bắt buộc**: `@NotNull(message = "Customer ID là bắt buộc")` (đã có `@NotNull`)

- **`expectedDeliveryDate`** (LocalDate):
  - **Bắt buộc**: `@NotNull(message = "Ngày giao hàng mong muốn là bắt buộc.")` (cần thêm)
  - **Phải >= hôm nay + 30 ngày**: Custom validation → message: "Ngày giao hàng phải ít nhất 30 ngày kể từ hôm nay."

- **`details`** (List<RfqDetailDto>):
  - **Bắt buộc**: `@NotNull(message = "Danh sách sản phẩm là bắt buộc")` và `@NotEmpty(message = "RFQ phải có ít nhất một sản phẩm.")` (cần thêm)
  - **Mỗi phần tử trong list**:
    - **`productId`** (Long): `@NotNull(message = "Vui lòng chọn sản phẩm.")` (cần thêm vào `RfqDetailDto`)
    - **`quantity`** (BigDecimal): `@DecimalMin(value = "100", message = "Số lượng tối thiểu là 100.")` (đã có)
    - **`unit`** (String): `@NotBlank(message = "Đơn vị là bắt buộc")` (cần thêm vào `RfqDetailDto`)

- **`contactPerson`** (String) - Optional override:
  - Nếu có giá trị: Trim whitespace và validate không chứa ký tự đặc biệt

- **`contactEmail`** (String) - Optional override:
  - Nếu có giá trị: Trim whitespace và `@Email(message = "Email không hợp lệ.")`

- **`contactPhone`** (String) - Optional override:
  - Nếu có giá trị: Trim whitespace và validate số điện thoại Việt Nam với regex `^(?:\+84|84|0)(?:2\d{1,2}([-.]?)\d{7,8}|(?:3\d|5\d|7\d|8\d|9\d)([-.]?)\d{3}\2\d{4})$`

**Lưu ý**: Controller đã có `@Valid` annotation. Cần cập nhật `RfqCreateDto` class để thêm các validation annotations còn thiếu và implement custom validators.

---

### 4.3. `POST /v1/rfqs/sales/create` - Sales tạo RFQ hộ khách hàng

**DTO**: `tmmsystem.dto.sales.SalesRfqCreateRequest` (class)

**Fields cần validate**:
- **`contactPerson`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Họ và tên là bắt buộc.")` (đã có `@NotBlank`)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi

- **`contactEmail`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Email là bắt buộc.")` (hiện tại chỉ có `@Email`, cần thêm `@NotBlank`)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Format email**: `@Email(message = "Email không hợp lệ.")` (đã có)

- **`contactPhone`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Số điện thoại là bắt buộc.")` (hiện tại chỉ có `@Pattern`, cần thêm `@NotBlank`)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Format số điện thoại Việt Nam**: Custom validation với regex `^(?:\+84|84|0)(?:2\d{1,2}([-.]?)\d{7,8}|(?:3\d|5\d|7\d|8\d|9\d)([-.]?)\d{3}\2\d{4})$` → message: "Số điện thoại không hợp lệ." (hiện tại có `@Pattern(regexp = "^$|^[0-9+\\-() ]{6,20}$")`, cần thay bằng regex Việt Nam chính xác)

- **`contactAddress`** (String):
  - **Bắt buộc**: Phải có đầy đủ thông tin địa chỉ (province, commune, detailedAddress). Cần validate trong service layer hoặc custom validator.

- **`expectedDeliveryDate`** (LocalDate):
  - **Bắt buộc**: `@NotNull(message = "Ngày giao hàng mong muốn là bắt buộc.")` (cần thêm)
  - **Phải >= hôm nay + 30 ngày**: Custom validation → message: "Ngày giao hàng phải ít nhất 30 ngày kể từ hôm nay."

- **`details`** (List<RfqDetailDto>):
  - **Bắt buộc**: `@NotNull(message = "Danh sách sản phẩm là bắt buộc")` và `@NotEmpty(message = "RFQ phải có ít nhất một sản phẩm.")` (cần thêm)
  - **Mỗi phần tử trong list**:
    - **`productId`** (Long): `@NotNull(message = "Vui lòng chọn sản phẩm.")` (cần thêm vào `RfqDetailDto`)
    - **`quantity`** (BigDecimal): `@DecimalMin(value = "100", message = "Số lượng tối thiểu là 100.")` (đã có)
    - **`unit`** (String): `@NotBlank(message = "Đơn vị là bắt buộc")` (cần thêm vào `RfqDetailDto`)

**Lưu ý**: Cần kiểm tra controller có `@Valid` annotation. Cần cập nhật `SalesRfqCreateRequest` class để thêm các validation annotations còn thiếu và implement custom validators.

---

### 4.4. `PUT /v1/rfqs/sales/{id}` - Sales chỉnh sửa RFQ

**DTO**: `tmmsystem.dto.sales.SalesRfqEditRequest` (class)

**Fields cần validate**:
- **`contactEmail`** (String) - Optional:
  - Nếu có giá trị: Trim whitespace và `@Email(message = "Email không hợp lệ. Vui lòng nhập đúng định dạng email.")` (đã có `@Email`)

- **`contactPhone`** (String) - Optional:
  - Nếu có giá trị: Trim whitespace và validate số điện thoại Việt Nam với regex `^(?:\+84|84|0)(?:2\d{1,2}([-.]?)\d{7,8}|(?:3\d|5\d|7\d|8\d|9\d)([-.]?)\d{3}\2\d{4})$` → message: "Số điện thoại không hợp lệ. Vui lòng kiểm tra lại." (hiện tại có `@Pattern(regexp = "^$|^[0-9+\\-() ]{6,20}$")`, cần thay bằng regex Việt Nam chính xác)

- **`details`** (List<RfqDetailDto>) - Optional:
  - Nếu có giá trị (không null và không empty):
    - **Mỗi phần tử trong list**:
      - **`productId`** (Long): `@NotNull(message = "ProductId không hợp lệ: ...")` (cần thêm vào `RfqDetailDto`)
      - **`quantity`** (BigDecimal): `@DecimalMin(value = "100", message = "Số lượng phải từ 100 trở lên. Giá trị hiện tại: ...")` (đã có)
      - **`unit`** (String): `@NotBlank(message = "Đơn vị không được để trống")` (cần thêm vào `RfqDetailDto`)
  - Nếu list rỗng (`details.length === 0`): Throw error "RFQ phải có ít nhất một sản phẩm."

**Lưu ý**: Cần kiểm tra controller có `@Valid` annotation. Cần cập nhật `SalesRfqEditRequest` class và `RfqDetailDto` để thêm các validation annotations còn thiếu và implement custom validators.

---

## 5. Nhóm Material Stock Management Controller (`tmmsystem.controller.MaterialStockManagementController`)

### 5.1. `POST /v1/production/material-stock` - Tạo nhập kho nguyên liệu

**DTO**: `tmmsystem.dto.inventory.MaterialStockDto` (class)

**Fields cần validate**:
- **`materialId`** (Long):
  - **Bắt buộc**: `@NotNull(message = "Vui lòng chọn nguyên liệu")` (cần thêm)
  - **Phải tồn tại trong database**: Custom validation trong service layer

- **`quantity`** (BigDecimal):
  - **Bắt buộc**: `@NotNull(message = "Số lượng là bắt buộc")` (cần thêm)
  - **Phải > 0**: `@DecimalMin(value = "0.0001", inclusive = false, message = "Vui lòng nhập số lượng hợp lệ")` (cần thêm)

- **`unitPrice`** (BigDecimal):
  - **Bắt buộc**: `@NotNull(message = "Đơn giá là bắt buộc")` (cần thêm)
  - **Phải > 0**: `@DecimalMin(value = "0.0001", inclusive = false, message = "Vui lòng nhập đơn giá hợp lệ")` (cần thêm)

- **`receivedDate`** (LocalDate):
  - **Bắt buộc**: `@NotNull(message = "Vui lòng chọn ngày nhập hàng")` (cần thêm)

- **`location`** (String) - Optional:
  - Nếu có giá trị: Trim whitespace

- **`batchNumber`** (String) - Optional:
  - Nếu có giá trị: Trim whitespace

- **`expiryDate`** (LocalDate) - Optional:
  - Nếu có giá trị: Phải >= receivedDate (custom validation)

**Lưu ý**: Controller hiện tại không có `@Valid` annotation trên `@RequestBody MaterialStockDto body`. Cần thêm `@Valid` để kích hoạt validation. Cần cập nhật `MaterialStockDto` class để thêm các validation annotations.

---

### 5.2. `PUT /v1/production/material-stock/{id}` - Cập nhật nhập kho nguyên liệu

**DTO**: `tmmsystem.dto.inventory.MaterialStockDto` (class)

**Fields cần validate** (giống create):
- **`materialId`** (Long):
  - **Bắt buộc**: `@NotNull(message = "Vui lòng chọn nguyên liệu")` (cần thêm)
  - **Phải tồn tại trong database**: Custom validation trong service layer

- **`quantity`** (BigDecimal):
  - **Bắt buộc**: `@NotNull(message = "Số lượng là bắt buộc")` (cần thêm)
  - **Phải > 0**: `@DecimalMin(value = "0.0001", inclusive = false, message = "Vui lòng nhập số lượng hợp lệ")` (cần thêm)

- **`unitPrice`** (BigDecimal):
  - **Bắt buộc**: `@NotNull(message = "Đơn giá là bắt buộc")` (cần thêm)
  - **Phải > 0**: `@DecimalMin(value = "0.0001", inclusive = false, message = "Vui lòng nhập đơn giá hợp lệ")` (cần thêm)

- **`receivedDate`** (LocalDate):
  - **Bắt buộc**: `@NotNull(message = "Vui lòng chọn ngày nhập hàng")` (cần thêm)

- **`location`** (String) - Optional:
  - Nếu có giá trị: Trim whitespace

- **`batchNumber`** (String) - Optional:
  - Nếu có giá trị: Trim whitespace

- **`expiryDate`** (LocalDate) - Optional:
  - Nếu có giá trị: Phải >= receivedDate (custom validation)

**Lưu ý**: Controller hiện tại không có `@Valid` annotation trên `@RequestBody MaterialStockDto body`. Cần thêm `@Valid` để kích hoạt validation. Cần cập nhật `MaterialStockDto` class để thêm các validation annotations.

---

## 6. Nhóm Machine Controller (`tmmsystem.controller.MachineController`)

### 6.1. `POST /v1/machines` - Tạo máy móc

**DTO**: `tmmsystem.dto.machine.MachineRequest` (class)

**Fields cần validate**:
- **`code`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Mã máy là bắt buộc")` (đã có `@NotBlank`)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Format mã máy**: Custom validation với regex `^[A-Z0-9_-]+$` (case-insensitive) → message: "Mã máy không hợp lệ. VD: WEAV-001" (cần thêm)
  - **Max length**: `@Size(max = 50, message = "Mã máy không được quá 50 ký tự")` (đã có)

- **`name`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Tên máy là bắt buộc")` (đã có `@NotBlank`)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Min length**: `@Size(min = 2, message = "Tên máy phải có ít nhất 2 ký tự")` (cần thêm)
  - **Max length**: `@Size(max = 100, message = "Tên máy không được quá 100 ký tự")` (cần thêm, hiện tại có `@Size(max = 255)`, cần sửa thành 100)

- **`type`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Loại máy là bắt buộc")` (đã có `@NotBlank`)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Max length**: `@Size(max = 20, message = "Loại máy không được quá 20 ký tự")` (đã có)

- **`location`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Vị trí là bắt buộc")` (hiện tại chỉ có `@Size`, cần thêm `@NotBlank`)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Min length**: `@Size(min = 2, message = "Vị trí phải có ít nhất 2 ký tự")` (cần thêm)
  - **Max length**: `@Size(max = 50, message = "Vị trí không được quá 50 ký tự")` (cần thêm, hiện tại có `@Size(max = 100)`, cần sửa thành 50)

- **`maintenanceIntervalDays`** (Integer):
  - **Bắt buộc**: `@NotNull(message = "Chu kỳ bảo trì là bắt buộc")` (cần thêm)
  - **Phải > 0**: `@Min(value = 1, message = "Chu kỳ bảo trì phải lớn hơn 0")` (cần thêm)
  - **Phải <= 3650**: `@Max(value = 3650, message = "Chu kỳ bảo trì không được quá 3650 ngày (10 năm)")` (cần thêm)

- **`brand`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Thương hiệu là bắt buộc")` (cần thêm)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Min length**: `@Size(min = 2, message = "Thương hiệu phải có ít nhất 2 ký tự")` (cần thêm)
  - **Max length**: `@Size(max = 50, message = "Thương hiệu không được quá 50 ký tự")` (cần thêm)

- **`power`** (String):
  - **Bắt buộc**: `@NotBlank(message = "Công suất là bắt buộc")` (cần thêm)
  - **Trim whitespace**: Sau khi nhận request, phải `.trim()` và kiểm tra nếu rỗng sau trim → trả lỗi
  - **Format công suất**: Custom validation với regex `^\d+(\.\d+)?\s*(kw|w|kW|W)?$` (case-insensitive) → message: "Công suất không hợp lệ. VD: 5kW, 3kW" (cần thêm)

- **`modelYear`** (Integer):
  - **Bắt buộc**: `@NotNull(message = "Năm sản xuất là bắt buộc")` (cần thêm)
  - **Format năm**: Custom validation với regex `^\d{4}$` → message: "Năm sản xuất phải là 4 chữ số" (cần thêm)
  - **Phải >= 1900 và <= năm hiện tại + 1**: Custom validation → message: "Năm sản xuất phải từ 1900 đến năm hiện tại + 1" (cần thêm)

- **Công suất theo loại máy** (trong `specifications` JSON hoặc field riêng):
  - Nếu `type` là `WEAVING` hoặc `WARPING`:
    - **`capacityPerDay`** (Integer hoặc BigDecimal):
      - **Bắt buộc**: `@NotNull(message = "Công suất/ngày là bắt buộc")`
      - **Phải > 0**: `@Min(value = 1, message = "Công suất/ngày phải lớn hơn 0")`
      - **Phải <= 1,000,000**: `@Max(value = 1000000, message = "Công suất/ngày không được quá 1,000,000")`
  - Nếu `type` là `SEWING` hoặc `CUTTING`:
    - **`capacityPerHour.bathTowels`** (Integer hoặc BigDecimal):
      - **Bắt buộc**: `@NotNull(message = "Công suất khăn tắm/giờ là bắt buộc")`
      - **Phải > 0**: `@Min(value = 1, message = "Công suất khăn tắm/giờ phải lớn hơn 0")`
      - **Phải <= 10,000**: `@Max(value = 10000, message = "Công suất khăn tắm/giờ không được quá 10,000")`
    - **`capacityPerHour.faceTowels`** (Integer hoặc BigDecimal):
      - **Bắt buộc**: `@NotNull(message = "Công suất khăn mặt/giờ là bắt buộc")`
      - **Phải > 0**: `@Min(value = 1, message = "Công suất khăn mặt/giờ phải lớn hơn 0")`
      - **Phải <= 10,000**: `@Max(value = 10000, message = "Công suất khăn mặt/giờ không được quá 10,000")`
    - **`capacityPerHour.sportsTowels`** (Integer hoặc BigDecimal):
      - **Bắt buộc**: `@NotNull(message = "Công suất khăn thể thao/giờ là bắt buộc")`
      - **Phải > 0**: `@Min(value = 1, message = "Công suất khăn thể thao/giờ phải lớn hơn 0")`
      - **Phải <= 10,000**: `@Max(value = 10000, message = "Công suất khăn thể thao/giờ không được quá 10,000")`

**Lưu ý**: Controller cần kiểm tra có `@Valid` annotation. Cần cập nhật `MachineRequest` class để thêm các validation annotations còn thiếu và implement custom validators cho `code`, `power`, `modelYear`, và công suất theo loại máy.

---

## 7. Tổng kết và hướng dẫn implement

### 7.1. Các bước thực hiện

1. **Cập nhật các DTO/Request classes**:
   - Thêm các validation annotations (`@NotBlank`, `@NotNull`, `@Email`, `@Size`, `@Min`, `@Max`, `@DecimalMin`, `@Pattern`) vào các fields theo yêu cầu ở trên
   - Implement custom validators cho các rule phức tạp (regex Việt Nam, password strength, date range, v.v.)

2. **Cập nhật các Controller methods**:
   - Thêm `@Valid` annotation trên `@RequestBody` parameters để kích hoạt validation
   - Đảm bảo tất cả các endpoint POST/PUT đều có `@Valid`

3. **Implement custom validators**:
   - Tạo các custom validator classes cho:
     - Số điện thoại Việt Nam: `@VietnamesePhoneNumber`
     - Mã số thuế: `@TaxCode`
     - Tên người (không chứa ký tự đặc biệt): `@ValidName`
     - Mã máy: `@MachineCode`
     - Công suất: `@PowerFormat`
     - Năm sản xuất: `@ModelYear`
     - Ngày giao hàng (>= hôm nay + 30 ngày): `@ExpectedDeliveryDate`
     - Password strength: `@PasswordStrength`

4. **Trim whitespace trong Service layer**:
   - Sau khi nhận request từ controller, trim tất cả các String fields trước khi xử lý
   - Kiểm tra lại sau trim nếu field là required

5. **Xử lý validation errors**:
   - Sử dụng `@ControllerAdvice` và `@ExceptionHandler` để xử lý `MethodArgumentNotValidException` và trả về error messages thân thiện với người dùng

### 7.2. Regex patterns cần sử dụng

- **Số điện thoại Việt Nam**: `^(?:\+84|84|0)(?:2\d{1,2}([-.]?)\d{7,8}|(?:3\d|5\d|7\d|8\d|9\d)([-.]?)\d{3}\2\d{4})$`
- **Mã số thuế**: `^[0-9]{10,13}$`
- **Tên người (không chứa ký tự đặc biệt)**: `^[^!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]*$`
- **Email**: Sử dụng `@Email` annotation hoặc regex `/\S+@\S+\.\S+/`
- **Mã máy**: `^[A-Z0-9_-]+$` (case-insensitive)
- **Công suất**: `^\d+(\.\d+)?\s*(kw|w|kW|W)?$` (case-insensitive)
- **Năm sản xuất**: `^\d{4}$`
- **Password không chứa khoảng trắng**: `^[^\s]+$`

### 7.3. Lưu ý quan trọng

- **Tất cả các String fields** đều phải được trim whitespace trước khi validate và lưu vào database
- **Validation phải được thực hiện ở cả 2 lớp**: DTO level (annotations) và Service level (business logic)
- **Error messages** phải rõ ràng, bằng tiếng Việt, và khớp với frontend
- **Custom validators** nên được tạo riêng để dễ maintain và reuse
- **Kiểm tra duplicate** (email, phoneNumber đã tồn tại) nên được thực hiện ở Service layer, không phải DTO level

---

## 8. Kiểm tra đối chiếu với Frontend

### 8.1. Error Messages phải khớp 100% với Frontend

**QUAN TRỌNG**: Tất cả error messages trong backend PHẢI khớp chính xác với frontend, bao gồm cả dấu chấm, dấu phẩy, và cách viết hoa/thường.

#### Các error messages đã được kiểm tra và cập nhật:

1. **"Tên công ty là bắt buộc"** (KHÔNG phải "Tên công ty không được để trống")
   - Dùng trong: CreateCustomerModal, ProfileModal (customer), ConfirmOrderProfileModal
   - Backend endpoints: `POST /v1/auth/customer/create-company`, `POST /v1/customers`, `PUT /v1/customers/{id}`

2. **"Email không hợp lệ."** (CÓ dấu chấm)
   - Dùng trong: CreateCustomerModal, CreateUserModal
   - Backend endpoints: Tất cả các endpoint liên quan đến email validation

3. **"Email không hợp lệ"** (KHÔNG có dấu chấm)
   - Dùng trong: ProfileModal, ConfirmOrderProfileModal
   - **Lưu ý**: Frontend có sự không nhất quán. Để đảm bảo nhất quán, backend nên dùng **"Email không hợp lệ."** (có dấu chấm) cho tất cả các endpoint.

4. **"Số điện thoại không hợp lệ."** (CÓ dấu chấm)
   - Dùng trong: Tất cả các modal/form có validate số điện thoại
   - Backend endpoints: Tất cả các endpoint liên quan đến phoneNumber validation

5. **"Tên người liên hệ không hợp lệ."** (CÓ dấu chấm)
   - Dùng trong: CreateUserModal, CreateCustomerModal
   - Backend endpoints: `POST /api/admin/users`, `PUT /api/admin/users/{id}`, `POST /v1/customers`, `PUT /v1/customers/{id}`

6. **"Mã số thuế không hợp lệ."** (CÓ dấu chấm)
   - Dùng trong: CreateCustomerModal, ConfirmOrderProfileModal
   - Backend endpoints: `POST /v1/customers`, `PUT /v1/customers/{id}`, `POST /v1/auth/customer/create-company`

### 8.2. Regex Patterns phải khớp 100% với Frontend

1. **Số điện thoại Việt Nam**: `^(?:\+84|84|0)(?:2\d{1,2}([-.]?)\d{7,8}|(?:3\d|5\d|7\d|8\d|9\d)([-.]?)\d{3}\2\d{4})$`
   - Frontend file: `src/utils/validators.js` - hàm `isVietnamesePhoneNumber`
   - Backend: Phải dùng chính xác regex này

2. **Email**: `/\S+@\S+\.\S+/`
   - Frontend: Dùng trong CreateUserModal, CreateCustomerModal, ProfileModal, ConfirmOrderProfileModal
   - Backend: Có thể dùng `@Email` annotation hoặc regex này

3. **Mã số thuế**: `/^[0-9]{10,13}$/`
   - Frontend: CreateCustomerModal - hàm `validateTaxCode`
   - Backend: Phải dùng chính xác regex này

4. **Tên người (không chứa ký tự đặc biệt)**: `/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/`
   - Frontend: CreateUserModal, CreateCustomerModal - hàm `validateName` và `validateContactPerson`
   - Backend: Phải dùng chính xác pattern này (hoặc regex `^[^!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]*$`)

5. **Mã máy**: `/^[A-Z0-9_-]+$/i` (case-insensitive)
   - Frontend: CreateMachineModal - hàm `validateCode`
   - Backend: Phải dùng chính xác regex này

6. **Công suất**: `/^\d+(\.\d+)?\s*(kw|w|kW|W)?$/i` (case-insensitive)
   - Frontend: CreateMachineModal - hàm `validatePower`
   - Backend: Phải dùng chính xác regex này

7. **Năm sản xuất**: `/^\d{4}$/`
   - Frontend: CreateMachineModal - hàm `validateYear`
   - Backend: Phải dùng chính xác regex này

### 8.3. Validation Rules phải khớp 100% với Frontend

1. **Password min length**: 8 ký tự (KHÔNG phải 6)
   - Frontend: CreateUserModal, ChangePasswordModal
   - Backend: `@Size(min = 8)` cho password fields

2. **Quantity min value**: 100 (cho RFQ details)
   - Frontend: QuoteRequest, CreateRfqPage, RFQDetailModal
   - Backend: `@DecimalMin(value = "100")` cho quantity trong RfqDetailDto

3. **Expected delivery date**: Phải >= hôm nay + 30 ngày
   - Frontend: CreateRfqPage - hàm `getMinExpectedDeliveryDate`
   - Backend: Custom validation cho expectedDeliveryDate

4. **Material stock quantity/unitPrice**: Phải > 0 (KHÔNG phải >= 0)
   - Frontend: MaterialStockModal - `parseFloat(quantity) <= 0`
   - Backend: `@DecimalMin(value = "0.0001", inclusive = false)`

5. **Machine capacity**: 
   - WEAVING/WARPING: capacityPerDay > 0, <= 1,000,000
   - SEWING/CUTTING: capacityPerHour.bathTowels, faceTowels, sportsTowels > 0, <= 10,000
   - Frontend: CreateMachineModal
   - Backend: Custom validation theo type

### 8.4. Checklist trước khi deploy

- [ ] Tất cả error messages đã khớp chính xác với frontend (bao gồm dấu chấm, dấu phẩy)
- [ ] Tất cả regex patterns đã khớp chính xác với frontend
- [ ] Tất cả validation rules (min/max values) đã khớp với frontend
- [ ] Tất cả String fields đã được trim whitespace trước khi validate
- [ ] Tất cả endpoints POST/PUT đã có `@Valid` annotation
- [ ] Custom validators đã được implement cho các rule phức tạp
- [ ] Test cases đã được viết và pass cho tất cả các validation rules

---

**File này sẽ được cập nhật khi có thay đổi từ frontend hoặc khi implement xong các validation.**

