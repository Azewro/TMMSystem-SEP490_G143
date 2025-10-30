# 🛠️ **GIAI ĐOẠN 4: TOÀN BỘ QUY TRÌNH LẬP KẾ HOẠCH SẢN XUẤT – API FLOW CHI TIẾT**

## 1️⃣ **Tạo kế hoạch sản xuất từ hợp đồng**

**API:**
```
POST /v1/production-plans
```
**Description:** Tạo mới kế hoạch sản xuất xuất phát từ hợp đồng đã duyệt. Nếu body chỉ có contractId, backend tự tạo detail và tự scaffold 6 stage mặc định.

**Request body tối thiểu:**
```json
{
  "contractId": 2,
  "notes": "Kế hoạch sản xuất cho đơn hàng khăn mặt cao cấp"
}
```

**Có thể truyền kèm details (custom/gộp nhiều sản phẩm):**
```json
{
  "contractId": 2,
  "notes": "Kế hoạch sản xuất cho đơn hàng khăn mặt cao cấp",
  "details": [
    {
      "productId": 10,
      "plannedQuantity": 1000,
      "proposedStartDate": "2025-11-01",
      "proposedEndDate": "2025-11-10",
      "notes": "Khăn mặt bamboo khổ lớn",
      "stages": [
        { "stageType": "WARPING",   "sequenceNo": 1, "plannedStartTime": "2025-11-01T08:00:00", "plannedEndTime": "2025-11-01T12:00:00", "inChargeUserId": 2 },
        { "stageType": "WEAVING",   "sequenceNo": 2, "plannedStartTime": "2025-11-01T13:00:00", "plannedEndTime": "2025-11-02T12:00:00", "inChargeUserId": 2 },
        { "stageType": "DYEING",    "sequenceNo": 3, "plannedStartTime": "2025-11-03T08:00:00", "plannedEndTime": "2025-11-03T18:00:00", "inChargeUserId": 5, "notes": "Outsourced" },
        { "stageType": "CUTTING",   "sequenceNo": 4, "plannedStartTime": "2025-11-04T08:00:00", "plannedEndTime": "2025-11-04T12:00:00", "inChargeUserId": 7 },
        { "stageType": "SEWING",    "sequenceNo": 5, "plannedStartTime": "2025-11-05T08:00:00", "plannedEndTime": "2025-11-06T18:00:00", "inChargeUserId": 8 },
        { "stageType": "PACKAGING", "sequenceNo": 6, "plannedStartTime": "2025-11-07T08:00:00", "plannedEndTime": "2025-11-07T12:00:00", "capacityPerHour": 500, "inChargeUserId": 9 }
      ]
      // Không gửi stages → backend tự scaffold 6 công đoạn; gửi stages → có thể gán sẵn inChargeUserId mỗi công đoạn
    }
  ]
}
```
**Các trường trong details:**
- `productId` (Long, bắt buộc)
- `plannedQuantity` (Decimal, bắt buộc)
- `proposedStartDate`, `proposedEndDate` (yyyy-MM-dd, khuyên nên truyền nếu muốn chủ động timeline)
- `notes` (String, tùy chọn)
  
**Các trường trong details.stages[] (tùy chọn, nếu muốn tự set ngay khi tạo):**
- `stageType` (String: WARPING/WEAVING/DYEING/CUTTING/SEWING/PACKAGING)
- `sequenceNo` (Int)
- `assignedMachineId` (Long, tùy chọn)
- `inChargeUserId` (Long, tùy chọn) ← Người phụ trách công đoạn
- `plannedStartTime`, `plannedEndTime` (yyyy-MM-ddTHH:mm:ss)
- `capacityPerHour` (Decimal, tùy chọn)
- `notes` (String)

**Kết quả:**
- Backend trả về plan có trường `details[]`, mỗi detail sẽ có đầy đủ `stages[]` cho từng công đoạn mặc định.


## 2️⃣ **Lấy chi tiết kế hoạch để render UI**

**API:**
```
GET /v1/production-plans/{planId}
```
**Trả về sample (cắt gọn):**
```json
{
  "id": 3,
  ...
  "details": [
    {
      "id": 12,
      "productId": 10,
      "plannedQuantity": 1000,
      "productName": "Khăn mặt Bamboo cao cấp",
      ...
      "stages": [
        { "id": 101, "stageType": "WARPING", "sequenceNo": 1, ... },
        { "id": 102, "stageType": "WEAVING", "sequenceNo": 2, ... },
        { "id": 103, "stageType": "DYEING", "sequenceNo": 3, ... },
        { "id": 104, "stageType": "CUTTING", "sequenceNo": 4, ... },
        { "id": 105, "stageType": "SEWING", "sequenceNo": 5, ... },
        { "id": 106, "stageType": "PACKAGING", "sequenceNo": 6, "capacityPerHour": 500, ... }
      ]
    }
  ]
}
```

## 3️⃣ **Xử lý từng công đoạn (accordion/collapse FE):**

### a) Gợi ý máy phù hợp cho công đoạn
**API:**
```
GET /v1/production-plans/stages/{stageId}/machine-suggestions
```

### b) Tự động gán máy tốt nhất cho công đoạn
**API:**
```
POST /v1/production-plans/stages/{stageId}/auto-assign-machine
```

### b2) (Gợi ý) Gán/đổi người phụ trách công đoạn (sẽ bổ sung API riêng nếu cần)
- Tạm thời: gán ngay từ khi tạo (details.stages[].inChargeUserId)
- Nếu cần chỉnh sau khi tạo: đề xuất bổ sung endpoint sau (ví dụ):
```
PUT /v1/production-plans/stages/{stageId}/assign-incharge?userId={userId}
```
Liên hệ backend để bật nhanh khi FE cần.

### c) Kiểm tra xung đột lịch trình cho công đoạn
**API:**
```
GET /v1/production-plans/stages/{stageId}/check-conflicts
```

### d) Gợi ý máy theo tham số (optional, không cần stageId)
**API:**
```
GET /v1/machine-selection/suitable-machines?stageType=WARPING&productId=10&requiredQuantity=1000&preferredStartTime=2025-11-01T08:00:00
```

### e) Kiểm tra khả dụng của máy bất kỳ (optional, hỗ trợ nghiên cứu lập lịch nâng cao)
**API:**
```
GET /v1/machine-selection/check-availability?machineId=22&start=2025-11-02T08:00:00&end=2025-11-02T16:00:00
```


## 4️⃣ **Block nguyên vật liệu tiêu hao**

### a) Tính toán định mức
**API:**
```
GET /v1/material-consumption/production-plan/{planId}
```

### b) Tùy chọn phần trăm hao hụt
**API:**
```
GET /v1/material-consumption/production-plan/{planId}/with-waste?wastePercentage=0.1
```

### c) Kiểm tra tồn kho
**API:**
```
GET /v1/material-consumption/production-plan/{planId}/availability
```

### d) Tạo phiếu lĩnh vật tư (nếu đã check tồn kho có đủ)
**API:**
```
POST /v1/material-consumption/production-plan/{planId}/create-requisition?createdById=4
```

## 5️⃣ **Lưu nháp, xem danh sách, lọc, chi tiết kế hoạch**
- **Lưu nháp:** Không cần API riêng — mọi thao tác update/kết quả gán máy/stage FE chỉ cần bắn lại kế hoạch nếu cần.
- **Xem danh sách:**
  - `GET /v1/production-plans`
  - Lọc trạng thái: `GET /v1/production-plans/status/{status}`
- **Xem chi tiết:** `GET /v1/production-plans/{planId}`

## 6️⃣ **Gửi duyệt, duyệt và từ chối**

### a) Gửi kế hoạch để phê duyệt
**API:**
```
PUT /v1/production-plans/{planId}/submit
```
**Body (tùy chọn):**
```json
{ "notes": "Đề nghị phê duyệt" }
```

### b) Giám đốc duyệt
**API:**
```
PUT /v1/production-plans/{planId}/approve
```
**Body (tùy chọn):**
```json
{ "approvalNotes": "Đồng ý sản xuất." }
```

### c) Từ chối kế hoạch
**API:**
```
PUT /v1/production-plans/{planId}/reject
```
**Body:**
```json
{ "rejectionReason": "Thông tin chưa đủ, cần bổ sung lịch máy móc." }
```


---

## 📝 **CHÚ THÍCH & QUY ƯỚC:**
- “stageId” lấy từ details[0].stages[x].id trả về sau khi tạo plan.
- “planId” = id kế hoạch vừa tạo.
- Nếu hợp đồng có nhiều dòng báo giá, backend sẽ tự tách thành nhiều detail.
- Nếu chưa setup đầy đủ máy móc, có thể auto-assign lại nhiều lần, FE chỉ lưu và submit khi đã chắc chắn.
- PACKAGING và DYEING sẽ self-label đặc biệt (manual/outsourced).
- Nếu cần, bạn có thể tự override detail & stages bằng cách truyền từ FE.

---

**Version:** 1.0.0 – Tài liệu quy trình Rest API Giai đoạn 4 (backend auto hóa tối đa cho UI/UX lập kế hoạch sản xuất)

---

## 🎨 PHỤ LỤC: HƯỚNG DẪN TÍCH HỢP FE THEO MÀN HÌNH

Mục tiêu: FE chỉ cần nối nút → gọi API đúng thứ tự, map đúng field để hiển thị.

### A. Khu "Thông tin chung của đơn hàng"
- Nút "Tạo kế hoạch" tại màn Hợp đồng → gọi:
  - `POST /v1/production-plans` với `{ contractId, notes }` (tối giản) hoặc kèm `details[]` nếu muốn kiểm soát.
- Sau khi tạo xong, điều hướng sang màn chi tiết với `planId` mới và gọi:
  - `GET /v1/production-plans/{planId}`
- Map trường hiển thị:
  - Mã đơn hàng: `contractNumber`
  - Tên sản phẩm, kích thước, số lượng: từ `details[].productName`, `details[].plannedQuantity`, (size nếu sản phẩm có mô tả/thuộc tính)
  - Ngày bắt đầu/kết thúc dự kiến: `details[].proposedStartDate`, `details[].proposedEndDate`
  - Ghi chú: `approvalNotes` hoặc `details[].notes`

### B. Khu "Chi tiết công đoạn sản xuất" (Accordion 6 dòng)
- Dữ liệu render: `details[0].stages[]` (hoặc lặp qua từng `detail`) với các field:
  - `stageType`, `sequenceNo`, `plannedStartTime`, `plannedEndTime`, `assignedMachineId`, `inChargeUserId`, `capacityPerHour`, `notes`.
- Icon/nhãn đặc biệt:
  - `DYEING` → gắn nhãn "Outsourced"
  - `PACKAGING` → hiển thị năng suất mặc định 500 cái/giờ nếu `capacityPerHour` trống

#### B1. Nút "Gợi ý máy"
- `GET /v1/production-plans/stages/{stageId}/machine-suggestions`
- Hiển thị danh sách suggestion, gồm: `machineName`, `capacityPerHour`, `estimatedDurationHours`, `availabilityScore`, `conflicts[]`, `suggestedStartTime/EndTime`.

#### B2. Nút "Tự gán máy tốt nhất"
- `POST /v1/production-plans/stages/{stageId}/auto-assign-machine`
- Sau khi thành công: cập nhật lại card công đoạn và hiển thị `assignedMachineName` + thời gian đã tối ưu.

#### B3. Nút "Kiểm tra xung đột lịch"
- `GET /v1/production-plans/stages/{stageId}/check-conflicts`
- Nếu trả về danh sách `conflicts[]` ≠ rỗng → hiển thị cảnh báo và cho phép đổi máy/khung giờ, sau đó gọi lại B1/B2/B3.

#### B4. Ô "Người phụ trách"
- Khi tạo plan có thể gán sẵn qua `details[].stages[].inChargeUserId`.
- Nếu cần chỉnh sau khi tạo (đề xuất API):
  - `PUT /v1/production-plans/stages/{stageId}/assign-incharge?userId={userId}` (liên hệ backend bật khi cần).
- FE: tạo dropdown user → chọn `userId` → gọi API cập nhật, sau đó refetch stage.

### C. Khu "Nguyên vật liệu tiêu hao"
- Nút "Tính vật tư": `GET /v1/material-consumption/production-plan/{planId}`
- Nút "% hao hụt": `GET /v1/material-consumption/production-plan/{planId}/with-waste?wastePercentage=0.10`
- Nút "Kiểm tra tồn kho": `GET /v1/material-consumption/production-plan/{planId}/availability`
- Nút "Tạo phiếu lĩnh": `POST /v1/material-consumption/production-plan/{planId}/create-requisition?createdById={userId}`
  - Lưu ý: hiện gắn theo execution stage; nếu cần phiếu ở cấp kế hoạch sẽ có bản mở rộng.

### D. Thanh hành động cuối màn (Hủy / Lưu nháp / Gửi phê duyệt)
- "Lưu nháp": không cần API riêng; giữ trạng thái `DRAFT` cho tới khi gửi.
- "Gửi phê duyệt": `PUT /v1/production-plans/{planId}/submit` → status `PENDING_APPROVAL`.

### E. Màn Giám đốc
- "Danh sách chờ duyệt": `GET /v1/production-plans/pending-approval`
- "Phê duyệt": `PUT /v1/production-plans/{planId}/approve` → auto tạo Production Order
- "Từ chối": `PUT /v1/production-plans/{planId}/reject`

### F. Gợi ý UX/Validation nhanh
- Disable nút "Gửi phê duyệt" nếu còn stage chưa có `assignedMachine` hoặc còn `conflicts[]`.
- Hiển thị tổng thời lượng theo `estimatedDurationHours` từ máy đã chọn.
- Với PACKAGING, nếu thiếu `capacityPerHour` thì hiển thị 500 và thời lượng = `plannedQuantity/500`.

---

## 📒 PHỤ LỤC 2: MAPPING TRƯỜNG UI → TRƯỜNG API

Các ô trong màn hình bạn gửi được map như sau (đọc/ghi):

### 1) Khối "Thông tin chung của đơn hàng"
- Mã đơn hàng: READ từ `GET /v1/production-plans/{planId}` → `contractNumber` (không ghi)
- Tên sản phẩm: READ `details[].productName` (không ghi)
- Kích thước sản phẩm: READ từ mô tả sản phẩm (nếu có) → `details[].productDescription` hoặc thuộc tính sản phẩm (không ghi)
- Số lượng: READ `details[].plannedQuantity`; WRITE khi tạo plan: `POST /v1/production-plans` → `details[].plannedQuantity`
- Nguyên vật liệu tiêu hao (ghi chú): READ/WRITE `details[].notes` hoặc tổng quan `approvalNotes`
- Ngày bắt đầu dự kiến: READ `details[].proposedStartDate`; WRITE khi tạo plan: `details[].proposedStartDate`
- Ngày kết thúc dự kiến: READ `details[].proposedEndDate`; WRITE khi tạo plan: `details[].proposedEndDate`

### 2) Accordion từng công đoạn (ví dụ "Cuộn mắc")
- Máy móc/thiết bị phụ trách:
  - READ: `details[].stages[].assignedMachineId` + show name theo máy
  - WRITE tự động: `POST /v1/production-plans/stages/{stageId}/auto-assign-machine`
  - WRITE thủ công (nếu muốn): cần endpoint update stage (liên hệ backend), tạm thời nên dùng auto-assign
- Người phụ trách:
  - READ: `details[].stages[].inChargeUserId`
  - WRITE khi tạo plan: `details[].stages[].inChargeUserId` (truyền ngay trong payload tạo)
  - WRITE sau tạo: đề xuất `PUT /v1/production-plans/stages/{stageId}/assign-incharge?userId=...` (bật khi FE cần)
- Thời gian bắt đầu:
  - READ: `details[].stages[].plannedStartTime`
  - WRITE: qua auto-assign (gợi ý thời gian) hoặc truyền ngay trong payload tạo stage
- Thời gian kết thúc:
  - READ: `details[].stages[].plannedEndTime`
  - WRITE: như trên
- Khối "Thời lượng (giờ)":
  - READ: tính từ gợi ý máy `estimatedDurationHours` hoặc (End - Start)
  - WRITE: nếu muốn cố định, có thể set `minRequiredDurationMinutes` khi tạo stage
- Ghi chú:
  - READ/WRITE: `details[].stages[].notes`

### 3) Nút chức năng trong Accordion
- Gợi ý máy: `GET /v1/production-plans/stages/{stageId}/machine-suggestions`
- Tự gán máy: `POST /v1/production-plans/stages/{stageId}/auto-assign-machine`
- Kiểm tra xung đột: `GET /v1/production-plans/stages/{stageId}/check-conflicts`

### 4) Khối "Nguyên vật liệu tiêu hao"
- Tính vật tư 10%: `GET /v1/material-consumption/production-plan/{planId}`
- Tính vật tư % tùy chọn: `GET /v1/material-consumption/production-plan/{planId}/with-waste?wastePercentage=0.10`
- Kiểm tra tồn kho: `GET /v1/material-consumption/production-plan/{planId}/availability`
- Tạo phiếu lĩnh: `POST /v1/material-consumption/production-plan/{planId}/create-requisition?createdById=...`

