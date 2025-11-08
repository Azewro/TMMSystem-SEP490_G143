# BÁO CÁO LUỒNG ĐẶT HÀNG & HƯỚNG DẪN TÍCH HỢP API (Khăn bông)

Cập nhật: 2025-11-08

Tài liệu này tách nhỏ mô tả nghiệp vụ đã cung cấp thành từng ý rõ ràng và ghép với API hiện có của hệ thống. Kèm theo hướng dẫn truyền dữ liệu (payload, header) và ví dụ gọi API ở mỗi bước.

---

## 1) Vai trò, trạng thái và nguyên tắc chung

- Vai trò chính
  - Khách hàng (Customer)
  - Sales (Nhân viên kinh doanh)
  - Kế hoạch (Planning)
  - Giám đốc (Director)
  - Quản lý sản xuất, Kỹ thuật (trong giai đoạn PO/WO)

- Header và xác thực
  - Với API theo vai trò Sales/Planning: sử dụng header `X-User-Id: <userId>` khi lấy dữ liệu theo phân công.
  - Với API Khách hàng: dùng JWT trong header `Authorization: Bearer <token>` (xem nhóm `/v1/auth/customer/*`).

- Ngày tháng
  - Một số API hỗ trợ nhiều định dạng ngày: `yyyy-MM-dd` hoặc `dd-MM-yyyy`. Riêng Production Order tạo từ hợp đồng còn chấp nhận nhập mỗi "ngày" (1-31) và giả định tháng/năm hiện tại.

- Upload file
  - Các API upload dùng `multipart/form-data` (file PDF báo giá/hợp đồng).

- Trạng thái chính (rút gọn)
  - RFQ: DRAFT → SENT → PRELIMINARY_CHECKED → FORWARDED_TO_PLANNING → (Capacity) → ... → CANCELED
  - Quotation: PENDING → SENT_TO_CUSTOMER → APPROVED/REJECTED
  - Contract: PENDING → UPLOADED_SIGNED → DIRECTOR_APPROVED/REJECTED
  - Production Plan: DRAFT → PENDING_APPROVAL → APPROVED/REJECTED (khi APPROVED hệ thống tự tạo Production Order)
  - Production Order: DRAFT/PENDING_APPROVAL → APPROVED/REJECTED → ...

Ghi chú: Tên trạng thái có thể rút gọn trong UI; tham chiếu chính xác theo response của API.

---

## 2) Luồng nghiệp vụ chi tiết (từ RFQ đến lệnh sản xuất)

### 2.1 Tạo yêu cầu báo giá (RFQ)

Có 3 kiểu khởi tạo RFQ theo mô tả:

1) Khách hàng không dùng hệ thống lúc đầu
- Khách liên hệ Sale; Sale tạo RFQ hộ khách.
- Sau khi Kế hoạch tạo báo giá xong, hệ thống gửi URL báo giá + thông tin đăng nhập để khách phê duyệt. Lần đăng nhập đầu khách phải đổi mật khẩu; nếu đồng ý báo giá, khách điền thêm thông tin doanh nghiệp để làm hợp đồng.
- API sử dụng:
  - Sales tạo RFQ hộ khách: `POST /v1/rfqs/by-sales` (Header `X-User-Id` là userId của Sale)
  - (Giám đốc) phân công nhân sự nếu cần: `POST /v1/rfqs/{id}/assign`

2) Khách hàng lần đầu sử dụng hệ thống (tự tạo ngoài hệ thống)
- Trên homepage (chưa có tài khoản) khách vẫn tạo được RFQ (public form). Sau khi có báo giá, hệ thống gửi URL + thông tin đăng nhập; khách đăng nhập, đổi mật khẩu lần đầu và bổ sung thông tin doanh nghiệp để làm hợp đồng.
- API sử dụng:
  - Public tạo RFQ: `POST /v1/rfqs/public`
  - Đăng nhập khách hàng: `POST /v1/auth/customer/login`
  - Đổi mật khẩu lần đầu: `POST /v1/auth/customer/change-password`
  - Bổ sung thông tin doanh nghiệp: `POST /v1/auth/customer/create-company` (Bearer token)

3) Khách hàng đã có tài khoản (đã dùng hệ thống)
- Vào homepage đăng nhập rồi tạo RFQ, hoặc tạo từ ngoài (public) cũng được.
- Khi tạo trong hệ thống: các trường khách hàng được auto-fill từ hồ sơ; khách chỉ nhập số lượng, ngày giao hàng mong muốn, vẫn có quyền chỉnh sửa thông tin.
- API sử dụng:
  - Tạo RFQ khi đã đăng nhập: `POST /v1/rfqs` (yêu cầu `customerId`)
  - Hoặc vẫn có thể `POST /v1/rfqs/public`

Trường "mã nhân viên sale" (employeeCode)
- Nếu khách/sale nhập `employeeCode` hợp lệ: hệ thống tự gán Sale tương ứng vào RFQ; Giám đốc chỉ cần chỉ định thêm Kế hoạch.
- Nếu không nhập hoặc không hợp lệ: Giám đốc sẽ phân công cả Sale và Kế hoạch bằng API `POST /v1/rfqs/{id}/assign`.

Ví dụ payload
- Public tạo RFQ (`POST /v1/rfqs/public`):
```json
{
  "contactPerson": "Nguyễn Văn B",
  "contactEmail": "khach@example.com",
  "contactPhone": "+84 912345678",
  "contactAddress": "Số 1 Đường A, Quận B",
  "expectedDeliveryDate": "2025-11-30",
  "notes": "Cần giao gấp",
  "employeeCode": "SALE-001",
  "details": [
    { "productId": 101, "quantity": 1000, "unit": "cai", "noteColor": "Xanh", "notes": "Size 30x30" }
  ]
}
```
- Sale tạo RFQ hộ khách (`POST /v1/rfqs/by-sales` + `X-User-Id`):
```json
{
  "contactPerson": "Công ty ABC",
  "contactEmail": "lienhe@abc.com",
  "contactPhone": "0912345678",
  "contactAddress": "KCN XYZ",
  "expectedDeliveryDate": "30-11-2025",
  "notes": "Yêu cầu mẫu trước",
  "details": [
    { "productId": 101, "quantity": 500, "unit": "cai", "noteColor": "Đỏ" }
  ]
}
```
- Khách đã đăng nhập tạo RFQ (`POST /v1/rfqs`):
```json
{
  "customerId": 2001,
  "expectedDeliveryDate": "2025-12-05",
  "notes": "Giao chia 2 đợt",
  "details": [
    { "productId": 101, "quantity": 1200, "unit": "cai", "noteColor": "Vàng" }
  ]
}
```

### 2.2 Xác nhận RFQ bởi Sales (Preliminary check) và khóa sửa

- Sales gọi điện xác nhận với khách (ngoài hệ thống), có thể sửa thông tin trước khi xác nhận.
- Khi Sales ấn nút xác nhận, RFQ bị khóa, không sửa tiếp tại màn này.
- API sử dụng:
  - Trước khi xác nhận: Sales có thể chỉnh sửa RFQ + thông tin khách bằng `PATCH /v1/rfqs/{id}/sales-edit` (Header `X-User-Id`).
  - Xác nhận sơ bộ: `POST /v1/rfqs/{id}/preliminary-check`.
  - Chuyển sang phòng Kế hoạch: `POST /v1/rfqs/{id}/forward-to-planning`.
  - Kế hoạch nhận RFQ: `POST /v1/rfqs/{id}/receive-by-planning`.

### 2.3 Kiểm tra năng lực sản xuất (Planning)

- Kế hoạch kiểm tra máy móc và kho NVL xem có đủ để kịp ngày giao hàng mong muốn hay không.
- Nếu đủ: tiếp tục tạo báo giá.
- Nếu không đủ: nhập lý do/đề xuất ngày khác, hệ thống gửi thông tin này cho Sales để thương lượng với khách; nếu khách đồng ý, cập nhật lại ngày mong muốn mới và tiếp tục.
- API sử dụng:
  - Kiểm tra máy: `POST /v1/rfqs/{id}/check-machine-capacity`
  - Kiểm tra kho: `POST /v1/rfqs/{id}/check-warehouse-capacity`
  - Ghi nhận kết quả năng lực: `POST /v1/rfqs/{id}/capacity-evaluate?status=SUFFICIENT|INSUFFICIENT&reason=...&proposedNewDate=2025-11-27`
  - Cập nhật ngày giao mong muốn mới (khi khách đổi): `PUT /v1/rfqs/{id}/expected-delivery-date?expectedDeliveryDate=2025-11-27`

### 2.4 Tạo và gửi báo giá (Planning → Sales → Customer)

- Kế hoạch tính giá và tạo báo giá từ RFQ.
- Sales gửi báo giá cho khách hàng.
- API sử dụng:
  - Tính giá sơ bộ: `POST /v1/quotations/calculate-price` với `{ "rfqId": <id>, "profitMargin": 0.15 }`
  - Tạo báo giá chính thức: `POST /v1/quotations/create-from-rfq`
  - Sales xem danh sách báo giá chờ gửi: `GET /v1/quotations/pending` (có thể kèm `X-User-Id` để lọc theo Sales được phân công)
  - Gửi báo giá cho khách: `POST /v1/quotations/{id}/send-to-customer`

### 2.5 Khách hàng phê duyệt báo giá

- Khách đăng nhập, đổi mật khẩu lần đầu (nếu cần), xem báo giá và phê duyệt hoặc từ chối.
- Khi khách phê duyệt: hệ thống tự động tạo Đơn hàng (Contract) ở trạng thái pending.
- Nếu từ chối: quy trình dừng.
- API:
  - Khách lấy danh sách báo giá: `GET /v1/quotations/customer/{customerId}`
  - Phê duyệt: `POST /v1/quotations/{id}/approve` (auto tạo đơn hàng)
  - Từ chối: `POST /v1/quotations/{id}/reject`

### 2.6 Upload hợp đồng đã ký và Giám đốc phê duyệt

- Sales ký kết hợp đồng ngoài hệ thống, upload PDF hợp đồng; khách cũng xem được hợp đồng đã phê duyệt.
- Giám đốc phê duyệt hợp đồng. Nếu từ chối, Sales re-upload rồi gửi lại.
- API:
  - Upload hợp đồng (PDF): `POST /v1/contracts/{id}/upload-signed` (multipart, tham số `saleUserId`)
  - Danh sách chờ duyệt: `GET /v1/contracts/pending-approval` hoặc `GET /v1/contracts/director/pending`
  - Duyệt hợp đồng: `POST /v1/contracts/{id}/approve?directorId=...&notes=...`
  - Từ chối hợp đồng: `POST /v1/contracts/{id}/reject?directorId=...&rejectionNotes=...`
  - Lấy URL file: `GET /v1/contracts/{id}/file-url` hoặc tải trực tiếp `GET /v1/contracts/{id}/download`

### 2.7 Gộp đơn hàng thành Lô (Lot) – tiêu chí và triển khai hiện tại

Tiêu chí nghiệp vụ mong muốn:
1. Cùng tên sản phẩm (mỗi sản phẩm chỉ có 1 kích thước; trong tên đã bao gồm chất liệu ... ví dụ: "Khăn mặt màu cotton cuộn tròn").
2. Ngày giao mong muốn (+-1 ngày).
3. Ngày ký hợp đồng (+-1 ngày).

Triển khai trong mã nguồn hiện tại (ProductionPlanService.createOrMergeLotFromContract):
- Đang gộp theo: cùng Product + cửa sổ ngày giao mục tiêu (delivery date) ±1 ngày, và Lot ở trạng thái `FORMING`/`READY_FOR_PLANNING`.
- Hệ thống có lưu dấu mốc ngày ký vào Lot (`contractDateMin/Max`) nhưng điều kiện lọc gộp hiện tại chưa kiểm ngày ký (có thể bổ sung nếu cần).

Kết quả gộp: tạo/ghép vào `ProductionLot`, cộng dồn số lượng, sinh các `ProductionLotOrder` trỏ về từng đơn hàng/chi tiết báo giá.

### 2.8 Lập Kế hoạch Sản xuất (Production Plan) và phê duyệt

- Sau khi hợp đồng được duyệt, hệ thống quét đơn chưa lập kế hoạch, đã gộp Lot sẵn. Kế hoạch vào danh sách, lập kế hoạch chi tiết.
- Gửi kế hoạch cho Giám đốc duyệt. Khi duyệt, hệ thống tự động tạo Production Order và gửi thông báo đến Quản lý sản xuất.
- API:
  - Tạo kế hoạch từ hợp đồng (tự gộp Lot nếu cần): `POST /v1/production-plans` với `{ "contractId": <id>, "notes": "..." }`
  - Lấy danh sách kế hoạch: `GET /v1/production-plans`
  - Gửi duyệt: `PUT /v1/production-plans/{id}/submit`
  - Giám đốc phê duyệt: `PUT /v1/production-plans/{id}/approve`
  - Giám đốc từ chối: `PUT /v1/production-plans/{id}/reject`
  - Gợi ý/auto gán máy cho từng công đoạn: `GET /v1/production-plans/stages/{stageId}/machine-suggestions`, `POST /v1/production-plans/stages/{stageId}/auto-assign-machine`
  - Gán người phụ trách công đoạn: `PUT /v1/production-plans/stages/{stageId}/assign-incharge?userId=...`

### 2.9 Tạo Lệnh sản xuất (Production Order) và Work Order

- Khi Kế hoạch được duyệt, hệ thống tự tạo Production Order. Quản lý sản xuất phân công kỹ thuật tiếp nhận lệnh này; Kỹ thuật tạo Work Order.
- API:
  - Danh sách/tạo/cập nhật PO: `GET/POST/PUT /v1/production/orders` (+ chi tiết)
  - Tạo từ hợp đồng: `POST /v1/production/orders/create-from-contract`
  - Gửi PO chờ duyệt, duyệt/từ chối PO: `POST /v1/production/orders/{id}/submit-approval`, `POST /v1/production/orders/{id}/approve`, `POST /v1/production/orders/{id}/reject`
  - Work Order: `POST /v1/production/work-orders` và CRUD các chi tiết/công đoạn liên quan.

---

## 3) Mô tả màn hình (theo yêu cầu) và mapping API

1) Homepage chưa đăng nhập (Khách): danh sách sản phẩm, nút “Yêu cầu báo giá”, nút “Đăng nhập”.
   - Gọi UI public → gửi RFQ: `POST /v1/rfqs/public`.

2) Homepage đã đăng nhập (Khách): danh sách sản phẩm, nút “Thêm sản phẩm”, có giỏ hàng và gửi yêu cầu báo giá từ giỏ.
   - Gửi RFQ đã đăng nhập: `POST /v1/rfqs` (kèm `customerId` và `details`).

3) Form “Khách tự tạo yêu cầu báo giá” – Trường: tên KH, sđt, email, địa chỉ giao hàng, mã NV sale (optional), sản phẩm (tên/số lượng/kích thước), thêm sản phẩm, thời gian nhận hàng mong muốn, ghi chú, nút gửi/hủy.
   - Map API: `POST /v1/rfqs/public`.

4) Form “Sale tạo yêu cầu báo giá cho khách” – cùng trường như trên.
   - Map API: `POST /v1/rfqs/by-sales` với `X-User-Id`.

5) Danh sách RFQ của Giám đốc: mã RFQ (RFQ-00000001), tên KH, sđt, tổng SL, ngày tạo, trạng thái, nút Phân công và Chi tiết.
   - Map API: `GET /v1/rfqs` (hoặc `GET /v1/rfqs/drafts/unassigned` để lọc nháp chưa gán) + `POST /v1/rfqs/{id}/assign`.

6) Danh sách RFQ của Sales: mã RFQ, tên KH, tổng SL, ngày tạo, trạng thái, nút Chi tiết (Sửa đổi và Xác nhận).
   - Map API: `GET /v1/rfqs/for-sales` (header `X-User-Id`)
   - Chi tiết, sửa đổi: `GET /v1/rfqs/{id}/for-sales` + `PATCH /v1/rfqs/{id}/sales-edit` + `POST /v1/rfqs/{id}/preliminary-check`.

7) Danh sách RFQ của Kế hoạch: mã RFQ, tên KH, tổng SL, ngày tạo, trạng thái, người tạo, nút Chi tiết (Kiểm tra máy móc/kho, Tạo báo giá).
   - Map API: `GET /v1/rfqs/for-planning` (header `X-User-Id`) + `POST check-machine-capacity`, `POST check-warehouse-capacity`, `POST /v1/quotations/create-from-rfq`.

8) Màn hình Tạo báo giá (Kế hoạch): giá nguyên liệu, gia công, hoàn thiện, lợi nhuận (%), giá tổng, ghi chú; nút Tạo báo giá/Hủy.
   - Map API: `POST /v1/quotations/calculate-price`, `POST /v1/quotations/create-from-rfq`.

9) Danh sách báo giá của Khách: mã YC báo giá, tổng tiền, trạng thái, ngày giao dự kiến, nút Chi tiết (Phê duyệt).
   - Map API: `GET /v1/quotations/customer/{customerId}` + `POST /v1/quotations/{id}/approve|reject`.

10) Danh sách báo giá của Sales: mã YC báo giá, tên KH, người tạo, tổng tiền, trạng thái, ngày giao dự kiến, nút Chi tiết.
   - Map API: `GET /v1/quotations/pending` (có thể lọc theo Sales).

11) Chi tiết báo giá của Khách: hiển thị bản tính (PDF/UI): mã YC, sản phẩm, SL, kích thước, đơn vị, đơn giá, thành tiền, thuế, tổng, số tiền bằng chữ; nút Chấp nhận/Từ chối.
   - Map API: `GET /v1/quotations/{id}` + `POST approve/reject`.

12) Xem chi tiết báo giá của Sales (PDF): tương tự, nút quay lại.
   - Map API: `GET /v1/quotations/{id}` (nếu cần upload bản đã ký: `POST /v1/quotations/{id}/upload-signed`).

13) Màn hình fill thông tin KH để làm hợp đồng: Tên DN, Người đại diện, Chức vụ, Mã số thuế, STK, Ngân hàng.
   - Map API: `POST /v1/auth/customer/create-company` (Bearer token).

14) Danh sách đơn hàng của Khách/Sales/Giám đốc
   - Khách: `GET /v1/contracts` (lọc theo KH ở tầng service/UI) + chi tiết `GET /v1/contracts/{id}`.
   - Sales: upload hợp đồng `POST /v1/contracts/{id}/upload-signed`.
   - Giám đốc: phê duyệt/từ chối `POST /v1/contracts/{id}/approve|reject`, tải file `GET /v1/contracts/{id}/download`.

15) Danh sách đơn đã gộp của Kế hoạch (Lot): mã lô, tên SP, kích thước, tổng SL, mã đơn đã gộp, ngày giao, trạng thái, nút “Lập kế hoạch sản xuất”.
   - Map API: `GET /v1/production-plans` hoặc mở rộng qua lot endpoint (hiện thao tác gộp nằm trong service khi tạo plan).

16) Lập kế hoạch sản xuất & quản lý phê duyệt (chi tiết công đoạn: cuộn mắc, dệt, nhuộm, cắt, may, đóng gói)
   - Map API: `POST /v1/production-plans`, `PUT /v1/production-plans/{id}/submit`, `PUT /v1/production-plans/{id}/approve|reject`, `GET /v1/production-plans/stages/{stageId}/machine-suggestions`, `POST /v1/production-plans/stages/{stageId}/auto-assign-machine`, `PUT /v1/production-plans/stages/{stageId}/assign-incharge?userId=...`.

17) Tạo Production Order tự động và phân công Kỹ thuật tạo Work Order
   - Map API PO/WO: `GET/POST /v1/production/orders`, `POST /v1/production/work-orders`, kèm CRUD chi tiết/stage.

---

## 4) Hướng dẫn gọi API tiêu biểu (mẫu payload)

1) Sales phân công RFQ (Giám đốc thao tác)
```http
POST /v1/rfqs/123/assign
Content-Type: application/json

{
  "assignedSalesId": 11,
  "assignedPlanningId": 21,
  "approvedById": 31
}
```

2) Planning đánh giá năng lực không đủ, đề xuất ngày mới
```http
POST /v1/rfqs/123/capacity-evaluate?status=INSUFFICIENT&reason=Lich may day den 25-11&proposedNewDate=2025-11-27
```
Sau khi khách đồng ý, Sales cập nhật ngày mong muốn:
```http
PUT /v1/rfqs/123/expected-delivery-date?expectedDeliveryDate=2025-11-27
```

3) Tính giá và tạo báo giá từ RFQ
```http
POST /v1/quotations/calculate-price
Content-Type: application/json

{ "rfqId": 123, "profitMargin": 0.12 }
```
```http
POST /v1/quotations/create-from-rfq
Content-Type: application/json

{ "rfqId": 123, "planningUserId": 21, "profitMargin": 0.12, "capacityCheckNotes": "OK" }
```

4) Khách phê duyệt báo giá → hệ thống tạo Contract
```http
POST /v1/quotations/456/approve
```

5) Sales upload hợp đồng đã ký (PDF), Giám đốc duyệt
```http
POST /v1/contracts/789/upload-signed
Content-Type: multipart/form-data

file=<pdf_file>; notes="Hop dong so ..."; saleUserId=11
```
```http
POST /v1/contracts/789/approve?directorId=31&notes=Dong y
```

6) Lập kế hoạch sản xuất từ hợp đồng, gửi duyệt, phê duyệt
```http
POST /v1/production-plans
Content-Type: application/json

{ "contractId": 789, "notes": "Lap ke hoach lo khăn mặt" }
```
```http
PUT /v1/production-plans/1001/submit
Content-Type: application/json

{ "notes": "De nghi duyet" }
```
```http
PUT /v1/production-plans/1001/approve
Content-Type: application/json

{ "approvalNotes": "OK" }
```
Sau lệnh trên, hệ thống tự tạo Production Order tương ứng.

---

## 5) Lưu ý tích hợp và edge cases

- RFQ public bắt buộc `contactPerson`; `contactEmail` nên hợp lệ hoặc có `contactPhone` để hệ thống tạo tài khoản.
- `employeeCode` (nếu có) giúp tự gán Sales; nếu không, dùng API phân công cho Giám đốc.
- Xác nhận sơ bộ (preliminary-check) sẽ khóa luồng sửa RFQ bởi Sales tại endpoint chỉnh sửa dành riêng cho Sales.
- Năng lực không đủ: luôn ghi rõ `reason`, có thể kèm `proposedNewDate`; UI Sales phải phản hồi với khách và cập nhật lại ngày mong muốn.
- Gộp đơn (Lot) trong code hiện tại dựa vào Product + DeliveryDate ±1; ngày ký hợp đồng đang được lưu nhưng chưa là điều kiện gộp – có thể mở rộng nếu cần.
- Khi Production Plan được APPROVED, Production Order sinh tự động với số lượng tổng từ Lot đã gộp; planned dates mặc định (today → +14 ngày) có thể chỉnh qua PO API.
- File hợp đồng/ báo giá: chỉ nhận PDF (khuyến nghị). Dùng endpoint lấy URL/tải file khi hiển thị cho Giám đốc/Khách.

---

## 6) Phụ lục – Danh sách endpoint trọng yếu theo module

- Auth (Khách):
  - POST `/v1/auth/customer/login`
  - POST `/v1/auth/customer/change-password`
  - POST `/v1/auth/customer/forgot-password`, `/v1/auth/customer/verify-reset-code`
  - POST `/v1/auth/customer/register`
  - POST `/v1/auth/customer/create-company`
  - GET `/v1/auth/customer/profile`

- RFQ:
  - GET `/v1/rfqs`, GET `/v1/rfqs/{id}`
  - POST `/v1/rfqs` (khách đã login), POST `/v1/rfqs/public`, POST `/v1/rfqs/by-sales`
  - PATCH `/v1/rfqs/{id}/sales-edit`, POST `/v1/rfqs/{id}/preliminary-check`, POST `/v1/rfqs/{id}/forward-to-planning`, POST `/v1/rfqs/{id}/receive-by-planning`, POST `/v1/rfqs/{id}/cancel`
  - POST `/v1/rfqs/{id}/check-machine-capacity`, `/v1/rfqs/{id}/check-warehouse-capacity`, `/v1/rfqs/{id}/capacity-evaluate`
  - PUT `/v1/rfqs/{id}/expected-delivery-date`
  - GET `/v1/rfqs/for-sales`, `/v1/rfqs/for-planning`, GET `/v1/rfqs/drafts/unassigned`
  - Chi tiết dòng: `/{rfqId}/details` CRUD

- Quotation:
  - GET `/v1/quotations`, `/v1/quotations/{id}`, `/v1/quotations/pending`, `/v1/quotations/customer/{customerId}`
  - POST `/v1/quotations/calculate-price`, `/v1/quotations/recalculate-price`, `/v1/quotations/create-from-rfq`
  - POST `/v1/quotations/{id}/send-to-customer`, `/v1/quotations/{id}/approve`, `/v1/quotations/{id}/reject`
  - POST `/v1/quotations/{id}/upload-signed`

- Contract:
  - GET `/v1/contracts`, `/v1/contracts/{id}`, `/v1/contracts/pending-approval`, `/v1/contracts/director/pending`
  - POST `/v1/contracts/{id}/upload-signed`, `/v1/contracts/{id}/re-upload`, `/v1/contracts/{id}/approve`, `/v1/contracts/{id}/reject`
  - GET `/v1/contracts/{id}/file-url`, `/v1/contracts/{id}/download`

- Production Plan:
  - GET `/v1/production-plans`, `/v1/production-plans/{id}`, `/v1/production-plans/pending-approval`
  - POST `/v1/production-plans`
  - PUT `/v1/production-plans/{id}/submit`, `/v1/production-plans/{id}/approve`, `/v1/production-plans/{id}/reject`
  - GET `.../stages/{stageId}/machine-suggestions`, POST `.../stages/{stageId}/auto-assign-machine`, PUT `.../stages/{stageId}/assign-incharge`

- Production (PO/WO):
  - GET/POST/PUT `/v1/production/orders` (+ chi tiết), GET `/v1/production/orders/pending-approval`, `.../director/pending`
  - POST `/v1/production/orders/create-from-contract`, `POST /v1/production/plans/{planId}/create-order`
  - Approve/Reject PO: `POST /v1/production/orders/{id}/approve|reject`
  - Work Orders: CRUD tại `/v1/production/work-orders` và `work-order-details`, `stages`

---

## 7) Bản đồ trường dữ liệu chính (rút gọn)

- RFQ Detail: `{ productId, quantity, unit, noteColor, notes }`
- Quotation Detail: `{ productId, quantity, unit, unitPrice, totalPrice, noteColor, discountPercentage }`
- Contract: `{ contractNumber, quotationId, customerId, contractDate, deliveryDate, totalAmount, status, ... }`
- Production Plan DTO: `{ id, contractId, planCode, status, createdById, approvedById, approvedAt, details[] }`
- Production Order DTO: `{ id, contractId, poNumber, totalQuantity, plannedStartDate, plannedEndDate, status, priority, notes }`

---

## 8) Gợi ý hiện thực UI theo mô tả đã cung cấp

- Luồng khách chưa có tài khoản: dùng form public → gửi RFQ → nhận SMS/Email chứa URL và thông tin đăng nhập → đăng nhập, đổi mật khẩu, điền thông tin doanh nghiệp trước khi phát hành hợp đồng.
- Luồng khách đã có tài khoản: form tạo RFQ trong hệ thống auto-fill thông tin khách; chỉ bắt buộc số lượng và ngày giao mong muốn (các trường khác có thể chỉnh sửa).
- Sau khi khách phê duyệt báo giá: hiển thị màn bổ sung thông tin hợp đồng (đã prefill), cho phép chỉnh sửa và lưu.
- Lập kế hoạch: hiển thị danh sách đơn đã gộp (Lot) và chi tiết công đoạn; hỗ trợ gợi ý/auto gán máy, gán người phụ trách; quy trình gửi duyệt/phê duyệt 2 bước như trên.

---

Tài liệu này bám sát các Controller và DTO hiện có trong mã nguồn. Nếu cần bổ sung tiêu chí gộp theo ngày ký hợp đồng đúng như đặc tả, đề xuất mở rộng điều kiện trong `ProductionPlanService#createOrMergeLotFromContract` để kiểm tra thêm khoảng ±1 ngày của ngày ký hợp đồng.
