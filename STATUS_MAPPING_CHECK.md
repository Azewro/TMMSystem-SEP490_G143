# Kiểm tra Mapping Status theo Workflow

## So sánh Trạng thái trong Ảnh vs Code

| Trạng thái (Ảnh) | executionStatus (Code) | status (Code) | Mapping hiện tại | Đã đúng? |
|------------------|------------------------|---------------|------------------|----------|
| "đợi" | `PENDING` | `PENDING` | ✅ | ✅ |
| "chờ làm" | `WAITING` | `WAITING` | ✅ | ✅ |
| "đang làm" | `IN_PROGRESS` | `IN_PROGRESS` | ✅ | ✅ |
| "chờ kiểm tra" | `WAITING_QC` | `WAITING_QC` | ✅ | ✅ |
| "đang kiểm tra" | `QC_IN_PROGRESS` | `QC_IN_PROGRESS` | ✅ | ✅ (đã sửa) |
| "đạt" | `QC_PASSED` | `QC_PASSED` | ✅ | ✅ (đã sửa) |
| "không đạt" | `QC_FAILED` | `QC_FAILED` | ✅ | ✅ (đã sửa) |
| "chờ sửa" | `WAITING_REWORK` | `WAITING_REWORK` | ✅ | ✅ (đã sửa) |
| "đang sửa" | `REWORK_IN_PROGRESS` | `REWORK_IN_PROGRESS` | ✅ | ✅ (đã sửa) |
| "hoàn thành" | `COMPLETED` | `COMPLETED` | ✅ | ✅ |

## Các thay đổi đã thực hiện

### 1. Cập nhật `syncStageStatus()` mapping
- **Trước:** `QC_IN_PROGRESS` → `status = "WAITING_QC"` (không phân biệt)
- **Sau:** `QC_IN_PROGRESS` → `status = "QC_IN_PROGRESS"` ✅

- **Trước:** `QC_PASSED` → `status = "COMPLETED"` (không phân biệt với hoàn thành)
- **Sau:** `QC_PASSED` → `status = "QC_PASSED"` ✅

- **Trước:** `WAITING_REWORK` → `status = "FAILED"` (không phân biệt)
- **Sau:** `WAITING_REWORK` → `status = "WAITING_REWORK"` ✅

- **Trước:** `REWORK_IN_PROGRESS` → `status = "IN_PROGRESS"` (không phân biệt)
- **Sau:** `REWORK_IN_PROGRESS` → `status = "REWORK_IN_PROGRESS"` ✅

### 2. Thêm endpoint cho KCS bắt đầu kiểm tra
- **Endpoint:** `POST /v1/qc/stages/{stageId}/start-inspection`
- **Mô tả:** KCS bấm nút "Kiểm tra" → chuyển từ `WAITING_QC` sang `QC_IN_PROGRESS`
- **Service method:** `QcService.startInspection()`

### 3. Cập nhật ExecutionOrchestrationService
- Đã inject `ProductionService` để dùng `syncStageStatus()`
- Đảm bảo khi set `QC_IN_PROGRESS` thì cả hai trường đều được sync

## Workflow Status Transitions

### Workflow chính:
1. **PM bắt đầu lệnh làm việc:**
   - Stage đầu tiên: `PENDING` → `WAITING` ("chờ làm")
   - Các stage khác: `PENDING` → `PENDING` ("đợi")

2. **Tổ Trưởng bắt đầu:**
   - `WAITING` → `IN_PROGRESS` ("đang làm")

3. **Tổ Trưởng cập nhật tiến độ = 100%:**
   - `IN_PROGRESS` → `WAITING_QC` ("chờ kiểm tra")

4. **KCS bấm nút "Kiểm tra":**
   - `WAITING_QC` → `QC_IN_PROGRESS` ("đang kiểm tra")

5. **KCS gửi kết quả kiểm tra:**
   - Nếu đạt: `QC_IN_PROGRESS` → `QC_PASSED` ("đạt")
   - Nếu không đạt: `QC_IN_PROGRESS` → `QC_FAILED` ("không đạt")

6. **Kỹ thuật yêu cầu làm lại:**
   - `QC_FAILED` → `WAITING_REWORK` ("chờ sửa")

7. **Tổ Trưởng bắt đầu sửa:**
   - `WAITING_REWORK` → `REWORK_IN_PROGRESS` ("đang sửa")

8. **Hoàn thành:**
   - `QC_PASSED` → `COMPLETED` ("hoàn thành") (sau khi tất cả stages pass)

## Kết luận

✅ **Tất cả các trạng thái trong ảnh đã được đáp ứng đúng:**
- Các trạng thái đã được map chính xác
- Frontend có thể phân biệt rõ ràng giữa các trạng thái
- Workflow transitions đã được implement đầy đủ
- Cả `status` và `executionStatus` đều được đồng bộ tự động

