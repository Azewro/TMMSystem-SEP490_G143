# Trạng thái triển khai Validation Backend

**Ngày cập nhật**: Hôm nay  
**Mục tiêu**: Theo dõi tiến độ triển khai validation backend theo frontend

---

## ✅ Đã hoàn thành

### 1. Custom Validators (8 validators)

#### ✅ VietnamesePhoneNumber
- **File**: `tmmsystem.validation.VietnamesePhoneNumber`
- **Regex**: `^(?:\+84|84|0)(?:2\d{1,2}([-.]?)\d{7,8}|(?:3\d|5\d|7\d|8\d|9\d)([-.]?)\d{3}\2\d{4})$`
- **Message**: "Số điện thoại không hợp lệ."
- **Khớp frontend**: ✅ `src/utils/validators.js` - `isVietnamesePhoneNumber`

#### ✅ TaxCode
- **File**: `tmmsystem.validation.TaxCode`
- **Regex**: `^[0-9]{10,13}$`
- **Message**: "Mã số thuế không hợp lệ."
- **Khớp frontend**: ✅ `CreateCustomerModal.jsx` - `validateTaxCode`

#### ✅ ValidName
- **File**: `tmmsystem.validation.ValidName`
- **Regex**: `^[^!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]*$`
- **Message**: "Tên người liên hệ không hợp lệ."
- **Khớp frontend**: ✅ `CreateUserModal.jsx`, `CreateCustomerModal.jsx` - `validateName`, `validateContactPerson`

#### ✅ PasswordStrength
- **File**: `tmmsystem.validation.PasswordStrength`
- **Rules**: 
  - Min 8 ký tự
  - Không khoảng trắng (`^[^\s]+$`)
  - Có ít nhất 1 chữ số (`.*\d.*`)
  - Có ít nhất 1 chữ in hoa (`.*[A-Z].*`)
- **Messages**: 
  - "Mật khẩu mới phải có ít nhất 8 ký tự."
  - "Mật khẩu không được chứa khoảng trắng"
  - "Mật khẩu phải chứa ít nhất 1 chữ số và 1 chữ in hoa"
- **Khớp frontend**: ✅ `ChangePasswordModal.jsx`

#### ✅ OptionalPasswordStrength
- **File**: `tmmsystem.validation.OptionalPasswordStrength`
- **Rules**: Giống PasswordStrength nhưng chỉ validate khi password có giá trị (không null/empty)
- **Dùng cho**: `UpdateUserRequest.password` (optional field)

#### ✅ MachineCode
- **File**: `tmmsystem.validation.MachineCode`
- **Regex**: `^[A-Z0-9_-]+$` (case-insensitive)
- **Message**: "Mã máy không hợp lệ. VD: WEAV-001"
- **Khớp frontend**: ✅ `CreateMachineModal.jsx` - `validateCode`

#### ✅ PowerFormat
- **File**: `tmmsystem.validation.PowerFormat`
- **Regex**: `^\d+(\.\d+)?\s*(kw|w|kW|W)?$` (case-insensitive)
- **Message**: "Công suất không hợp lệ. VD: 5kW, 3kW"
- **Khớp frontend**: ✅ `CreateMachineModal.jsx` - `validatePower`
- **Lưu ý**: Sẽ validate trong service layer khi parse specifications JSON

#### ✅ ModelYear
- **File**: `tmmsystem.validation.ModelYear`
- **Rules**: 
  - 4 chữ số (`^\d{4}$`)
  - Từ 1900 đến năm hiện tại + 1
- **Message**: "Năm sản xuất không hợp lệ. Phải là năm từ 1900 đến năm hiện tại + 1"
- **Khớp frontend**: ✅ `CreateMachineModal.jsx` - `validateYear`
- **Lưu ý**: Sẽ validate trong service layer khi parse specifications JSON

#### ✅ ExpectedDeliveryDate
- **File**: `tmmsystem.validation.ExpectedDeliveryDate`
- **Rules**: Phải >= hôm nay + 30 ngày
- **Message**: "Ngày giao hàng phải ít nhất 30 ngày kể từ hôm nay."
- **Khớp frontend**: ✅ `CreateRfqPage.jsx` - `getMinExpectedDeliveryDate`

---

### 2. Auth DTOs

#### ✅ LoginRequest
- **File**: `tmmsystem.dto.auth.LoginRequest`
- **Validation**:
  - `email`: `@NotBlank`, `@Email`, `@Size(max = 150)`
  - `password`: `@NotBlank`
- **Messages**: Khớp frontend ✅
- **Controller**: ✅ `AuthController` - đã có `@Valid`

#### ✅ ForgotPasswordRequest
- **File**: `tmmsystem.dto.auth.ForgotPasswordRequest`
- **Validation**:
  - `email`: `@NotBlank`, `@Email`, `@Size(max = 150)`
- **Messages**: Khớp frontend ✅
- **Controller**: ✅ `AuthController` - đã có `@Valid`

#### ✅ ChangePasswordRequest
- **File**: `tmmsystem.dto.auth.ChangePasswordRequest`
- **Validation**:
  - `email`: `@NotBlank`, `@Email`, `@Size(max = 150)`
  - `currentPassword`: `@NotBlank`
  - `newPassword`: `@NotBlank`, `@Size(min = 8)`, `@PasswordStrength`
- **Messages**: Khớp frontend ✅
- **Controller**: ✅ `AuthController` - đã có `@Valid`

#### ✅ VerifyResetCodeRequest
- **File**: `tmmsystem.dto.auth.VerifyResetCodeRequest`
- **Validation**:
  - `email`: `@NotBlank`, `@Email`, `@Size(max = 150)`
  - `code`: `@NotBlank`
- **Messages**: Khớp frontend ✅
- **Controller**: ✅ `AuthController` - đã có `@Valid`

---

### 3. User DTOs

#### ✅ CreateUserRequest
- **File**: `tmmsystem.dto.user.CreateUserRequest`
- **Validation**:
  - `email`: `@NotBlank`, `@Email`, `@Size(max = 150)`
  - `password`: `@NotBlank`, `@Size(min = 8, max = 100)`
  - `name`: `@NotBlank`, `@ValidName`, `@Size(max = 255)`
  - `phoneNumber`: `@NotBlank`, `@VietnamesePhoneNumber`, `@Size(max = 30)`
  - `roleId`: `@NotNull`
- **Messages**: Khớp frontend ✅
- **Controller**: ✅ `UserController` - đã có `@Valid`

#### ✅ UpdateUserRequest
- **File**: `tmmsystem.dto.user.UpdateUserRequest`
- **Validation**:
  - `name`: `@NotBlank`, `@ValidName`, `@Size(max = 255)`
  - `phoneNumber`: `@NotBlank`, `@VietnamesePhoneNumber`, `@Size(max = 30)`
  - `password`: `@Size(min = 8, max = 100)`, `@OptionalPasswordStrength` (optional)
- **Messages**: Khớp frontend ✅
- **Controller**: ✅ `UserController` - đã có `@Valid`

---

### 4. Customer DTOs

#### ✅ CustomerCreateRequest
- **File**: `tmmsystem.dto.CustomerCreateRequest`
- **Validation**:
  - `companyName`: `@NotBlank`, `@Size(max = 255)` - message: "Tên công ty là bắt buộc"
  - `contactPerson`: `@NotBlank`, `@ValidName`, `@Size(max = 150)` - message: "Người liên hệ là bắt buộc"
  - `email`: `@NotBlank`, `@Email`, `@Size(max = 150)` - message: "Email là bắt buộc", "Email không hợp lệ."
  - `phoneNumber`: `@NotBlank`, `@VietnamesePhoneNumber`, `@Size(max = 30)` - message: "Số điện thoại là bắt buộc"
  - `address`: `@NotBlank`, `@Size(max = 1000)` - message: "Địa chỉ là bắt buộc"
  - `taxCode`: `@NotBlank`, `@TaxCode`, `@Size(max = 50)` - message: "Mã số thuế là bắt buộc"
- **Messages**: Khớp frontend ✅
- **Controller**: ✅ `CustomerController` - đã có `@Valid`

#### ✅ CustomerUpdateRequest
- **File**: `tmmsystem.dto.CustomerUpdateRequest`
- **Validation**: Giống CustomerCreateRequest
- **Messages**: Khớp frontend ✅
- **Controller**: ✅ `CustomerController` - đã có `@Valid`

---

### 5. RFQ DTOs

#### ✅ RfqDetailDto
- **File**: `tmmsystem.dto.sales.RfqDetailDto`
- **Validation**:
  - `productId`: `@NotNull(message = "Vui lòng chọn sản phẩm.")`
  - `quantity`: `@NotNull`, `@DecimalMin(value = "100", message = "Số lượng tối thiểu là 100.")`
  - `unit`: `@NotBlank(message = "Đơn vị là bắt buộc")`
- **Messages**: Khớp frontend ✅

#### ✅ RfqPublicCreateDto
- **File**: `tmmsystem.dto.sales.RfqPublicCreateDto`
- **Validation**:
  - `contactPerson`: `@NotBlank(message = "Họ và tên là bắt buộc.")`
  - `contactEmail`: `@NotBlank(message = "Email là bắt buộc.")`, `@Email(message = "Email không hợp lệ.")`
  - `contactPhone`: `@NotBlank(message = "Số điện thoại là bắt buộc.")`, `@VietnamesePhoneNumber`
  - `expectedDeliveryDate`: `@NotNull`, `@ExpectedDeliveryDate`
  - `details`: `@NotNull`, `@NotEmpty`, `@Valid`
- **Messages**: Khớp frontend ✅
- **Controller**: ✅ `RfqController` - đã có `@Valid`

#### ✅ RfqCreateDto
- **File**: `tmmsystem.dto.sales.RfqCreateDto`
- **Validation**:
  - `customerId`: `@NotNull`
  - `expectedDeliveryDate`: `@NotNull`, `@ExpectedDeliveryDate`
  - `details`: `@NotNull`, `@NotEmpty`, `@Valid`
  - `contactPerson`: `@ValidName` (optional override)
  - `contactEmail`: `@Email` (optional override)
  - `contactPhone`: `@VietnamesePhoneNumber` (optional override)
- **Messages**: Khớp frontend ✅
- **Controller**: ✅ `RfqController` - đã có `@Valid`

#### ✅ SalesRfqCreateRequest
- **File**: `tmmsystem.dto.sales.SalesRfqCreateRequest`
- **Validation**:
  - `contactPerson`: `@NotBlank(message = "Họ và tên là bắt buộc.")`
  - `contactEmail`: `@NotBlank(message = "Email là bắt buộc.")`, `@Email(message = "Email không hợp lệ.")`
  - `contactPhone`: `@NotBlank(message = "Số điện thoại là bắt buộc.")`, `@VietnamesePhoneNumber`
  - `expectedDeliveryDate`: `@NotNull`, `@ExpectedDeliveryDate`
  - `details`: `@NotNull`, `@NotEmpty`, `@Valid`
- **Messages**: Khớp frontend ✅
- **Controller**: ✅ `RfqController` - đã có `@Valid`

#### ✅ SalesRfqEditRequest
- **File**: `tmmsystem.dto.sales.SalesRfqEditRequest`
- **Validation**:
  - `contactEmail`: `@Email(message = "Email không hợp lệ. Vui lòng nhập đúng định dạng email.")` (optional)
  - `contactPhone`: `@VietnamesePhoneNumber(message = "Số điện thoại không hợp lệ. Vui lòng kiểm tra lại.")` (optional)
  - `details`: `@NotEmpty`, `@Valid` (optional nhưng nếu có thì không được rỗng)
- **Messages**: Khớp frontend ✅ (RFQDetailModal.jsx)
- **Controller**: ✅ `RfqController` - đã có `@Valid`

---

### 6. Material Stock DTO

#### ✅ MaterialStockDto
- **File**: `tmmsystem.dto.inventory.MaterialStockDto`
- **Validation**:
  - `materialId`: `@NotNull(message = "Vui lòng chọn nguyên liệu")`
  - `quantity`: `@NotNull`, `@DecimalMin(value = "0.0001", inclusive = false, message = "Vui lòng nhập số lượng hợp lệ")`
  - `unitPrice`: `@NotNull`, `@DecimalMin(value = "0.0001", inclusive = false, message = "Vui lòng nhập đơn giá hợp lệ")`
  - `receivedDate`: `@NotNull(message = "Vui lòng chọn ngày nhập hàng")`
- **Messages**: Khớp frontend ✅ (MaterialStockModal.jsx)
- **Controller**: ✅ `MaterialStockManagementController` - đã có `@Valid`

---

### 7. Machine Request DTO

#### ✅ MachineRequest
- **File**: `tmmsystem.dto.machine.MachineRequest`
- **Validation**:
  - `code`: `@NotBlank`, `@MachineCode`, `@Size(max = 50)`
  - `name`: `@NotBlank`, `@Size(min = 2, max = 100)`
  - `type`: `@NotBlank`, `@Size(max = 20)`
  - `location`: `@NotBlank`, `@Size(min = 2, max = 50)`
  - `maintenanceIntervalDays`: `@NotNull`, `@Min(1)`, `@Max(3650)`
- **Messages**: Khớp frontend ✅ (CreateMachineModal.jsx)
- **Controller**: ✅ `MachineController` - đã có `@Valid`
- **Lưu ý**: Validation cho `specifications` JSON (brand, power, modelYear, capacity) sẽ xử lý trong service layer vì phụ thuộc vào `type`

---

## ✅ Đã hoàn thành Service Layer Validation

### 1. ✅ Machine Specifications Validation

**File**: `tmmsystem.util.MachineSpecificationsValidator` và `tmmsystem.controller.MachineController`

**Đã implement**:
- Parse `specifications` JSON string
- Validate cho tất cả types:
  - `brand`: Required, min 2, max 50 ký tự
  - `power`: Required, format `^\d+(\.\d+)?\s*(kw|w|kW|W)?$`
  - `modelYear`: Required, 4 chữ số, từ 1900 đến năm hiện tại + 1
- Nếu `type` là `WEAVING` hoặc `WARPING`:
  - `capacityPerDay`: Required, > 0, <= 1,000,000
- Nếu `type` là `SEWING` hoặc `CUTTING`:
  - `capacityPerHour.bathTowels`: Required, > 0, <= 10,000
  - `capacityPerHour.faceTowels`: Required, > 0, <= 10,000
  - `capacityPerHour.sportsTowels`: Required, > 0, <= 10,000

**Error messages**: Khớp 100% với frontend ✅
- "Thương hiệu là bắt buộc"
- "Thương hiệu phải có ít nhất 2 ký tự"
- "Thương hiệu không được vượt quá 50 ký tự"
- "Công suất là bắt buộc"
- "Công suất không hợp lệ. VD: 5kW, 3kW"
- "Năm sản xuất là bắt buộc"
- "Năm sản xuất phải từ 1900 đến [năm hiện tại + 1]"
- "Công suất/ngày là bắt buộc"
- "Công suất/ngày phải là số lớn hơn 0"
- "Công suất/ngày không được vượt quá 1,000,000"
- "Công suất khăn tắm/giờ là bắt buộc"
- "Công suất khăn tắm/giờ phải là số lớn hơn 0"
- "Công suất khăn tắm/giờ không được vượt quá 10,000"
- (tương tự cho faceTowels và sportsTowels)

**Implementation**: 
- Utility class `MachineSpecificationsValidator` với method `validate(String specificationsJson, String machineType)`
- Được gọi trong `MachineController.create()` và `MachineController.update()`
- Throw `IllegalArgumentException` với danh sách lỗi (join bằng "; ")

---

### 2. ✅ RFQ Contact Address Validation

**File**: `tmmsystem.service.RfqService`

**Đã implement**:
- Validate `contactAddress` không được rỗng cho:
  - `RfqPublicCreateDto` trong `createFromPublic()`
  - `SalesRfqCreateRequest` trong `createBySales()`
- **Error message**: "Vui lòng điền đầy đủ địa chỉ nhận hàng." (khớp frontend ✅)

**Lưu ý**: Frontend gửi `contactAddress` dưới dạng string đã kết hợp (fullAddress), không phải JSON object. Validation kiểm tra string không rỗng.

---

### 3. ✅ Material Stock Expiry Date Validation

**File**: `tmmsystem.controller.MaterialStockManagementController`

**Đã implement**:
- Validate `expiryDate >= receivedDate` trong:
  - `create()` method
  - `update()` method
- **Error message**: "Ngày hết hạn phải sau ngày nhập hàng" (khớp frontend ✅)

**Implementation**: 
- Kiểm tra trong controller trước khi tạo/update entity
- Throw `IllegalArgumentException` nếu không hợp lệ

---

## 📊 Tổng kết

### Đã hoàn thành
- ✅ **9 Custom Validators** - Tất cả đã khớp với frontend
- ✅ **4 Auth DTOs** - Đầy đủ validation
- ✅ **2 User DTOs** - Đầy đủ validation
- ✅ **2 Customer DTOs** - Đầy đủ validation
- ✅ **5 RFQ DTOs** - Đầy đủ validation
- ✅ **1 Material Stock DTO** - Đầy đủ validation
- ✅ **1 Machine Request DTO** - Validation cơ bản + specifications trong service layer
- ✅ **Tất cả Controllers** - Đã có `@Valid` annotation
- ✅ **Service Layer Validations** - Đầy đủ:
  - Machine specifications JSON validation
  - RFQ contact address validation
  - Material stock expiry date validation

### Tỷ lệ hoàn thành
- **DTO Level**: 100% ✅
- **Service Level**: 100% ✅
- **Tổng thể**: 100% ✅

---

## 🎯 Đã hoàn thành tất cả

Tất cả validation đã được implement đầy đủ:
1. ✅ **Machine Specifications Validation** - Đã implement trong `MachineSpecificationsValidator` và `MachineController`
2. ✅ **RFQ Contact Address Validation** - Đã implement trong `RfqService`
3. ✅ **Material Stock Expiry Date Validation** - Đã implement trong `MaterialStockManagementController`
4. ✅ **Error messages** - Khớp 100% với frontend

---

## 📝 Lưu ý

- Tất cả validation đã được implement và test
- Error messages đã khớp 100% với frontend
- Custom validators đã được tạo và sử dụng đầy đủ
- Service layer validations đã được thêm vào các điểm cần thiết

**File này đã hoàn thành.**

