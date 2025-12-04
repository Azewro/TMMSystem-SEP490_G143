# Báo cáo so sánh Validation Frontend vs Backend

**Ngày kiểm tra**: Hôm nay  
**Mục tiêu**: Xác định các validation còn thiếu ở backend so với frontend

---

## Tổng quan

### ✅ Đã có đầy đủ
1. **RFQ Details (RfqDetailDto)**: ✅ Đã có đầy đủ validation
   - `@NotNull` cho productId
   - `@DecimalMin(value = "100")` cho quantity
   - `@NotBlank` cho unit
   - `@Valid` trên List<RfqDetailDto> trong các DTO cha

### ⚠️ Còn thiếu hoặc chưa đúng

---

## 1. Auth Controller

### 1.1. LoginRequest (record)
**File**: `tmmsystem.dto.auth.LoginRequest`

**Hiện trạng**: ❌ Không có validation annotations

**Cần thêm**:
- `@NotBlank(message = "Email không được để trống.")` cho email
- `@Email(message = "Email không hợp lệ.")` cho email
- `@Size(max = 150)` cho email
- `@NotBlank(message = "Mật khẩu không được để trống.")` cho password

**Controller**: ❌ Không có `@Valid` trên `@RequestBody LoginRequest req`

---

### 1.2. ForgotPasswordRequest
**File**: `tmmsystem.dto.auth.ForgotPasswordRequest`

**Hiện trạng**: ❓ Cần kiểm tra file

**Cần có**:
- `@NotBlank(message = "Email không được để trống.")` cho email
- `@Email(message = "Vui lòng nhập đúng định dạng Email")` cho email
- `@Size(max = 150)` cho email

**Controller**: ❌ Không có `@Valid` (theo BACKEND_VALIDATIONS.md)

---

### 1.3. ChangePasswordRequest
**File**: `tmmsystem.dto.auth.ChangePasswordRequest`

**Hiện trạng**: ❓ Cần kiểm tra file

**Cần có**:
- `@NotBlank` cho email, currentPassword, newPassword
- `@Size(min = 8)` cho newPassword
- Custom validator cho password strength (không khoảng trắng, có số, có chữ hoa)
- Custom validator để kiểm tra newPassword != currentPassword

**Controller**: ❌ Không có `@Valid` (theo BACKEND_VALIDATIONS.md)

---

### 1.4. VerifyResetCodeRequest
**File**: `tmmsystem.dto.auth.VerifyResetCodeRequest`

**Hiện trạng**: ❓ Cần kiểm tra file

**Cần có**:
- `@NotBlank` cho email và code
- `@Email` cho email
- `@Size(max = 150)` cho email

**Controller**: ❌ Không có `@Valid` (theo BACKEND_VALIDATIONS.md)

---

## 2. User Controller

### 2.1. CreateUserRequest
**File**: `tmmsystem.dto.user.CreateUserRequest`

**Hiện trạng**: ⚠️ Thiếu một số validation

**Đã có**:
- ✅ `@Email @NotBlank @Size(max = 150)` cho email
- ✅ `@NotBlank @Size(min = 6, max = 100)` cho password (cần sửa min = 8)
- ✅ `@NotNull` cho roleId

**Còn thiếu**:
- ❌ `@NotBlank(message = "Họ và tên là bắt buộc")` cho name
- ❌ `@Size(max = 255)` cho name
- ❌ Custom validator cho name (không chứa ký tự đặc biệt)
- ❌ `@NotBlank(message = "Số điện thoại là bắt buộc")` cho phoneNumber
- ❌ `@Size(max = 30)` cho phoneNumber
- ❌ Custom validator cho phoneNumber (regex Việt Nam)
- ❌ Sửa `@Size(min = 6)` thành `@Size(min = 8)` cho password

**Controller**: ❌ Không có `@Valid` trên `@RequestBody CreateUserRequest req`

---

### 2.2. UpdateUserRequest
**File**: `tmmsystem.dto.user.UpdateUserRequest`

**Hiện trạng**: ❓ Cần kiểm tra file

**Cần có** (theo BACKEND_VALIDATIONS.md):
- `@NotBlank` cho name
- Custom validator cho name
- `@Size(max = 255)` cho name
- `@NotBlank` cho phoneNumber
- Custom validator cho phoneNumber
- `@Size(max = 30)` cho phoneNumber
- `@Size(min = 8)` cho password (nếu có)

**Controller**: ❌ Không có `@Valid` (theo BACKEND_VALIDATIONS.md)

---

## 3. Customer Controller

### 3.1. CustomerCreateRequest
**File**: `tmmsystem.dto.CustomerCreateRequest`

**Hiện trạng**: ⚠️ Thiếu nhiều validation

**Đã có**:
- ✅ `@NotBlank` cho companyName (nhưng message sai: "Tên công ty không được để trống" thay vì "Tên công ty là bắt buộc")
- ✅ `@Size(max = 255)` cho companyName
- ✅ `@Size(max = 150)` cho contactPerson, email
- ✅ `@Size(max = 30)` cho phoneNumber
- ✅ `@Email` cho email
- ✅ `@Size(max = 1000)` cho address
- ✅ `@Size(max = 50)` cho taxCode

**Còn thiếu**:
- ❌ Sửa message cho companyName: "Tên công ty là bắt buộc"
- ❌ `@NotBlank(message = "Người liên hệ là bắt buộc")` cho contactPerson
- ❌ Custom validator cho contactPerson (không chứa ký tự đặc biệt)
- ❌ `@NotBlank(message = "Email là bắt buộc")` cho email
- ❌ Sửa message cho email: "Email không hợp lệ." (có dấu chấm)
- ❌ `@NotBlank(message = "Số điện thoại là bắt buộc")` cho phoneNumber
- ❌ Custom validator cho phoneNumber (regex Việt Nam)
- ❌ `@NotBlank(message = "Địa chỉ là bắt buộc")` cho address
- ❌ `@NotBlank(message = "Mã số thuế là bắt buộc")` cho taxCode (nếu bắt buộc)
- ❌ Custom validator cho taxCode (regex `^[0-9]{10,13}$`)

**Controller**: ✅ Đã có `@Valid`

---

### 3.2. CustomerUpdateRequest
**File**: `tmmsystem.dto.CustomerUpdateRequest`

**Hiện trạng**: ❓ Cần kiểm tra file

**Cần có** (theo BACKEND_VALIDATIONS.md):
- Tất cả các validation giống CustomerCreateRequest
- `@NotBlank` cho tất cả các field bắt buộc
- Custom validators cho contactPerson, phoneNumber, taxCode

**Controller**: ❌ Không có `@Valid` (theo BACKEND_VALIDATIONS.md)

---

## 4. RFQ Controller

### 4.1. RfqPublicCreateDto
**File**: `tmmsystem.dto.sales.RfqPublicCreateDto`

**Hiện trạng**: ⚠️ Đã cập nhật một phần

**Đã có** (sau khi cập nhật):
- ✅ `@NotBlank` cho contactPerson
- ✅ `@Email` cho contactEmail
- ✅ `@NotNull @NotEmpty @Valid` cho details

**Còn thiếu**:
- ❌ `@NotBlank(message = "Email là bắt buộc.")` cho contactEmail
- ❌ `@NotBlank(message = "Số điện thoại là bắt buộc.")` cho contactPhone
- ❌ Custom validator cho contactPhone (regex Việt Nam thay vì pattern hiện tại)
- ❌ `@NotNull(message = "Ngày giao hàng mong muốn là bắt buộc.")` cho expectedDeliveryDate
- ❌ Custom validator cho expectedDeliveryDate (>= hôm nay + 30 ngày)
- ❌ Validate địa chỉ đầy đủ (province, commune, detailedAddress)

**Controller**: ✅ Đã có `@Valid`

---

### 4.2. RfqCreateDto
**File**: `tmmsystem.dto.sales.RfqCreateDto`

**Hiện trạng**: ⚠️ Đã cập nhật một phần

**Đã có** (sau khi cập nhật):
- ✅ `@NotNull` cho customerId
- ✅ `@NotNull @NotEmpty @Valid` cho details

**Còn thiếu**:
- ❌ `@NotNull(message = "Ngày giao hàng mong muốn là bắt buộc.")` cho expectedDeliveryDate
- ❌ Custom validator cho expectedDeliveryDate (>= hôm nay + 30 ngày)
- ❌ Validation cho contactPerson, contactEmail, contactPhone (nếu có)

**Controller**: ✅ Đã có `@Valid`

---

### 4.3. SalesRfqCreateRequest
**File**: `tmmsystem.dto.sales.SalesRfqCreateRequest`

**Hiện trạng**: ⚠️ Đã cập nhật một phần

**Đã có** (sau khi cập nhật):
- ✅ `@NotBlank` cho contactPerson
- ✅ `@Email` cho contactEmail
- ✅ `@NotNull @NotEmpty @Valid` cho details

**Còn thiếu**:
- ❌ `@NotBlank(message = "Email là bắt buộc.")` cho contactEmail
- ❌ `@NotBlank(message = "Số điện thoại là bắt buộc.")` cho contactPhone
- ❌ Custom validator cho contactPhone (regex Việt Nam)
- ❌ `@NotNull(message = "Ngày giao hàng mong muốn là bắt buộc.")` cho expectedDeliveryDate
- ❌ Custom validator cho expectedDeliveryDate (>= hôm nay + 30 ngày)
- ❌ Validate địa chỉ đầy đủ

**Controller**: ✅ Đã có `@Valid`

---

### 4.4. SalesRfqEditRequest
**File**: `tmmsystem.dto.sales.SalesRfqEditRequest`

**Hiện trạng**: ⚠️ Đã cập nhật một phần

**Đã có** (sau khi cập nhật):
- ✅ `@Email` cho contactEmail
- ✅ `@NotEmpty @Valid` cho details

**Còn thiếu**:
- ❌ Custom validator cho contactPhone (regex Việt Nam thay vì pattern hiện tại)
- ❌ Message cho contactEmail: "Email không hợp lệ. Vui lòng nhập đúng định dạng email."
- ❌ Message cho contactPhone: "Số điện thoại không hợp lệ. Vui lòng kiểm tra lại."

**Controller**: ✅ Đã có `@Valid`

---

## 5. Material Stock Management Controller

### 5.1. MaterialStockDto
**File**: `tmmsystem.dto.inventory.MaterialStockDto`

**Hiện trạng**: ❌ Không có validation annotations

**Cần thêm**:
- ❌ `@NotNull(message = "Vui lòng chọn nguyên liệu")` cho materialId
- ❌ `@NotNull(message = "Số lượng là bắt buộc")` cho quantity
- ❌ `@DecimalMin(value = "0.0001", inclusive = false, message = "Vui lòng nhập số lượng hợp lệ")` cho quantity
- ❌ `@NotNull(message = "Đơn giá là bắt buộc")` cho unitPrice
- ❌ `@DecimalMin(value = "0.0001", inclusive = false, message = "Vui lòng nhập đơn giá hợp lệ")` cho unitPrice
- ❌ `@NotNull(message = "Vui lòng chọn ngày nhập hàng")` cho receivedDate
- ❌ Custom validation: expiryDate >= receivedDate (nếu có)

**Controller**: ❌ Không có `@Valid` trên `@RequestBody MaterialStockDto body`

---

## 6. Machine Controller

### 6.1. MachineRequest
**File**: `tmmsystem.dto.machine.MachineRequest`

**Hiện trạng**: ⚠️ Thiếu nhiều validation

**Đã có**:
- ✅ `@NotBlank @Size(max = 50)` cho code
- ✅ `@NotBlank @Size(max = 255)` cho name (cần sửa thành max = 100)
- ✅ `@NotBlank @Size(max = 20)` cho type
- ✅ `@Size(max = 100)` cho location (cần sửa thành max = 50)

**Còn thiếu**:
- ❌ Custom validator cho code (regex `^[A-Z0-9_-]+$`) → message: "Mã máy không hợp lệ. VD: WEAV-001"
- ❌ `@Size(min = 2, max = 100)` cho name (sửa từ max = 255)
- ❌ `@NotBlank(message = "Vị trí là bắt buộc")` cho location
- ❌ `@Size(min = 2, max = 50)` cho location (sửa từ max = 100)
- ❌ `@NotNull(message = "Chu kỳ bảo trì là bắt buộc")` cho maintenanceIntervalDays
- ❌ `@Min(value = 1)` và `@Max(value = 3650)` cho maintenanceIntervalDays
- ❌ Validation cho brand, power, modelYear (trong specifications JSON)
- ❌ Custom validator cho power (regex `^\d+(\.\d+)?\s*(kw|w|kW|W)?$`)
- ❌ Custom validator cho modelYear (4 chữ số, 1900 đến năm hiện tại + 1)
- ❌ Validation cho capacity theo type (WEAVING/WARPING vs SEWING/CUTTING)

**Controller**: ✅ Đã có `@Valid`

---

## 7. Tổng kết

### Số lượng validation còn thiếu

| Nhóm | Số lượng còn thiếu | Mức độ ưu tiên |
|------|-------------------|----------------|
| Auth Controller | ~15-20 annotations | 🔴 Cao |
| User Controller | ~10-15 annotations | 🔴 Cao |
| Customer Controller | ~10-15 annotations | 🔴 Cao |
| RFQ Controller | ~8-10 annotations + custom validators | 🟡 Trung bình |
| Material Stock | ~6-8 annotations | 🟡 Trung bình |
| Machine Controller | ~15-20 annotations + custom validators | 🟡 Trung bình |

### Các custom validators cần implement

1. **@VietnamesePhoneNumber** - Regex: `^(?:\+84|84|0)(?:2\d{1,2}([-.]?)\d{7,8}|(?:3\d|5\d|7\d|8\d|9\d)([-.]?)\d{3}\2\d{4})$`
2. **@TaxCode** - Regex: `^[0-9]{10,13}$`
3. **@ValidName** - Regex: `^[^!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]*$`
4. **@MachineCode** - Regex: `^[A-Z0-9_-]+$`
5. **@PowerFormat** - Regex: `^\d+(\.\d+)?\s*(kw|w|kW|W)?$`
6. **@ModelYear** - Regex: `^\d{4}$` + range 1900 đến năm hiện tại + 1
7. **@ExpectedDeliveryDate** - >= hôm nay + 30 ngày
8. **@PasswordStrength** - Không khoảng trắng, có số, có chữ hoa, min 8 ký tự

### Các controller cần thêm @Valid

1. ❌ AuthController - Tất cả các endpoint POST
2. ❌ UserController - POST và PUT endpoints
3. ❌ CustomerController - PUT endpoint
4. ❌ MaterialStockManagementController - POST và PUT endpoints

---

## 8. Khuyến nghị

### Ưu tiên cao (Cần làm ngay)
1. Thêm `@Valid` vào tất cả các controller endpoints
2. Cập nhật LoginRequest với validation annotations
3. Cập nhật CreateUserRequest và UpdateUserRequest
4. Cập nhật CustomerCreateRequest và CustomerUpdateRequest
5. Implement custom validators cho số điện thoại Việt Nam

### Ưu tiên trung bình
1. Cập nhật MaterialStockDto với validation
2. Cập nhật MachineRequest với đầy đủ validation
3. Implement custom validators còn lại
4. Thêm validation cho expectedDeliveryDate trong RFQ DTOs

### Lưu ý quan trọng
- Tất cả error messages phải khớp 100% với frontend (bao gồm dấu chấm, dấu phẩy)
- Tất cả regex patterns phải khớp chính xác với frontend
- Tất cả String fields phải được trim whitespace trong service layer
- Validation phải được thực hiện ở cả 2 lớp: DTO level và Service level

---

**File này sẽ được cập nhật khi hoàn thành các validation còn thiếu.**

