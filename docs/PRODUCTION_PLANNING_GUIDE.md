# HƯỚNG DẪN LẬP KẾ HOẠCH SẢN XUẤT (PRODUCTION PLANNING)

Tài liệu này mô tả luồng lập kế hoạch sau khi báo giá được khách duyệt → hình thành Hợp đồng (Contract) → gom đơn thành Lot → tạo phiên bản Kế hoạch (Production Plan) → sinh & chỉnh sửa các Stage (công đoạn) với gợi ý máy, gán nhân sự & QC, kiểm tra nguyên vật liệu → phê duyệt → sinh Production Order.

> Phiên bản: 2025-11-10. (Cập nhật: bổ sung stage fields, UI mapping, ví dụ payload, lot status.)

## 1. Mục tiêu
- Chuẩn hoá các bước từ Contract đến Production Order.
- Giải thích cơ chế gộp (merge) đơn hàng thành Lot, versioning kế hoạch.
- Nêu rõ công thức tính thời gian cho từng stage và cách hệ thống chọn máy.
- Trình bày toàn bộ API liên quan (Machine Selection, Production Plan, Material Consumption, Stage operations).
- Làm rõ cách kiểm tra nguyên liệu, máy móc, và gán người phụ trách + QC.
- Mapping trực tiếp cho màn hình UI (Form Lập Kế Hoạch).

## 2. Tổng quan luồng (End-to-End API Flow)
1. Quotation ACCEPTED → sinh Contract (status=PENDING_APPROVAL).
2. Director duyệt Contract → status=APPROVED.
3. Merge vào Lot: logic trong `ProductionPlanService#createOrMergeLotFromContract(contractId)` (gọi ngầm khi POST /v1/production-plans).
4. Tạo Production Plan phiên bản mới (DRAFT): POST `/v1/production-plans` với `contractId`.
5. Lấy danh sách Lot qua API chính thức: GET `/v1/production-lots?status=READY_FOR_PLANNING` và chi tiết Lot: GET `/v1/production-lots/{id}`.
6. Lấy kế hoạch chi tiết: GET `/v1/production-plans/{id}`.
7. Điều chỉnh Stage (đÃ có đầy đủ endpoint):
   - Gợi ý máy cho stage: GET `/v1/production-plans/stages/{stageId}/machine-suggestions`.
   - Gợi ý máy khi chuẩn bị tạo mới: GET `/v1/production-plans/machine-suggestions`.
   - Tự động gán máy: POST `/v1/production-plans/stages/{stageId}/auto-assign-machine`.
   - Cập nhật tổng hợp stage: PUT `/v1/production-plans/stages/{stageId}` (máy, inCharge, QC, thời gian, notes...).
   - Gán người phụ trách nhanh: PUT `/v1/production-plans/stages/{stageId}/assign-incharge?userId=...`.
   - Gán QC nhanh: PUT `/v1/production-plans/stages/{stageId}/assign-qc?userId=...`.
   - Kiểm tra xung đột: GET `/v1/production-plans/stages/{stageId}/check-conflicts`.
8. Kiểm tra nguyên vật liệu (mặc định 10% waste): GET `/v1/material-consumption/production-plan/{planId}`.
9. (Tuỳ chọn) Tính với waste tuỳ chỉnh: GET `/v1/material-consumption/production-plan/{planId}/with-waste?wastePercentage=0.15`.
10. Kiểm tra availability NVL: GET `/v1/material-consumption/production-plan/{planId}/availability`.
11. Tạo material requisition (nếu đủ): POST `/v1/material-consumption/production-plan/{planId}/create-requisition?createdById=...`.
12. Submit kế hoạch: PUT `/v1/production-plans/{id}/submit`.
13. Approve / Reject: PUT `/v1/production-plans/{id}/approve` hoặc `/reject`.
14. Sau APPROVED hệ thống sinh Production Order tự động.

## 3. Cơ chế gộp Lot (Lot Merging)
Điều kiện đưa Contract vào Lot hiện hữu:
- Cùng `productId` (lấy từ QuotationDetail đầu tiên).
- Ngày giao hàng contract nằm trong [lot.deliveryDateTarget ± 1 ngày].
- Lot.status ∈ {FORMING, READY_FOR_PLANNING}.
Nếu không có Lot phù hợp → tạo Lot mới:
```
lotCode = LOT-YYYYMMDD-<seq>
status  = FORMING → sau khi thêm xong chuyển READY_FOR_PLANNING
sizeSnapshot = product.standardDimensions
quantity cộng dồn từ mỗi QuotationDetail.quantity
```
Sau merge xong: Lot.status = READY_FOR_PLANNING.

Lot Status chính:
| Status | Ý nghĩa |
|--------|---------|
| FORMING | Đang gom đơn |
| READY_FOR_PLANNING | Sẵn sàng lập kế hoạch |
| PLANNING | Đang được sử dụng trong bản kế hoạch DRAFT hiện hành |
| PLAN_APPROVED | Lot đã có kế hoạch được duyệt |
| IN_PRODUCTION | Đã thành Production Order, đang sản xuất |
| COMPLETED | Đã hoàn thành sản xuất |
| CANCELED | Hủy |

## 3.1 Quy tắc khóa Lot khi bắt đầu lập kế hoạch
- Trong `createPlanVersion(lotId)` hệ thống đặt `lot.status = PLANNING`.
- Khi status = PLANNING hoặc cao hơn (PLAN_APPROVED, IN_PRODUCTION, COMPLETED) thì KHÔNG merge thêm contract vào lot nữa.
- Chỉ các lot ở trạng thái FORMING hoặc READY_FOR_PLANNING mới nhận thêm hợp đồng.
- Nếu cần thêm đơn sau khi đã PLANNING: phải tạo lot mới hoặc huỷ kế hoạch hiện tại rồi rollback status thủ công (không khuyến khích trong môi trường production).

## 3.2 Máy móc được gán ở cấp Stage của Plan (bao phủ toàn bộ Lot)
- Mỗi Stage trong Production Plan chính là đại diện cho công đoạn xử lý toàn bộ khối lượng `lot.totalQuantity`.
- Không gán máy theo từng contract riêng sau khi đã merge lot; mọi gán máy đều ở stage của plan.
- Khi tạo version mới (plan mới cho cùng lot): máy móc của version cũ không bị xóa, nhưng version mới sẽ cần gán lại (hoặc auto-assign) cho các stage.

## 4. Versioning Production Plan
Mỗi lần tạo version mới (createPlanVersion):
- Các plan hiện tại `currentVersion=true` → `currentVersion=false`, status=SUPERSEDED.
- Tạo plan mới với `versionNo = max+1`, status=DRAFT, `currentVersion=true`.
- Mục tiêu: audit lịch sử + cho phép rebuild.

## 5. Production Plan Status
| Trạng thái | Mô tả | Chuyển tiếp |
|-----------|------|------------|
| DRAFT | Mới tạo | submit → PENDING_APPROVAL |
| PENDING_APPROVAL | Chờ duyệt | approve → APPROVED, reject → REJECTED |
| APPROVED | Đã duyệt | Sinh Production Order |
| SUPERSEDED | Bị thay thế bởi version mới | Không dùng |
| REJECTED | Bị từ chối | Tạo version mới để sửa |

## 6. Structure & Fields của Stage (`ProductionPlanStage`)
Database entity chính (các field quan trọng cho UI Form):
- id
- plan (tham chiếu kế hoạch)
- stageType (WARPING, WEAVING, DYEING, CUTTING, HEMMING, SEWING?, PACKAGING) – trong dto hiển thị stageTypeName.
- sequenceNo (thứ tự hiển thị 1.,2.,3., ...)
- assignedMachine (id, code, name) – có thể null với DYEING (outsourced) & PACKAGING (manual).
- inChargeUser (Người phụ trách)
- qcUser (Người kiểm tra chất lượng) – UI hiện đã có combobox QC, cần endpoint gán QC (CHƯA CÓ → đề xuất thêm).
- plannedStartTime / plannedEndTime (thời gian kế hoạch)
- minRequiredDurationMinutes (thời lượng tối thiểu từ machine suggestion)
- capacityPerHour (năng suất dùng để tính estimatedOutput)
- transferBatchQuantity (nếu áp dụng chuyển mẻ giữa các stage – hiện có field, UI chưa dùng)
- stageStatus (PENDING, READY, IN_PROGRESS, PAUSED, COMPLETED, CANCELED)
- setupTimeMinutes / teardownTimeMinutes (thời gian chuẩn bị/kết thúc – chưa map UI, có thể thêm tooltip)
- actualStartTime / actualEndTime / downtimeMinutes / downtimeReason (phục vụ tracking thực tế – UI giai đoạn sau)
- quantityInput / quantityOutput (thực tế – dùng để đánh giá hao hụt nội bộ)
- notes (Ghi chú)

DTO `ProductionPlanStageDto` trả về các calculated fields:
- durationMinutes = end - start
- estimatedOutput = capacityPerHour * durationMinutes / 60

### Thiếu Endpoint hiện tại so với UI:
| Nhu cầu UI | Trạng thái hỗ trợ |
|------------|------------------|
| Bulk update nhiều stage một lần | CHƯA có (có thể thêm PUT /v1/production-plans/{id}/stages/bulk) |
| Override thời lượng thủ công (khác với end-start) | CHƯA có field riêng (có thể thêm plannedDurationMinutes) |
| Split một stage cho nhiều máy song song | CHƯA có (roadmap) |

> Các nhu cầu khác (update stage, assign QC, assign inCharge) đã được hỗ trợ bằng endpoint hiện hành.

Đề xuất thêm endpoint:
```
PUT /v1/production-plans/stages/{stageId}
Body: {
  "plannedStartTime": "2025-11-10T08:00:00",
  "plannedEndTime": "2025-11-10T12:00:00",
  "assignedMachineId": 3,
  "inChargeUserId": 5,
  "qcUserId": 7,
  "notes": "Ưu tiên máy có bảo trì xong"
}
```
Response: `ProductionPlanStageDto` cập nhật.

PUT QC gọn: `/v1/production-plans/stages/{stageId}/assign-qc?userId=...` (ĐÃ CÓ)

## 7. Công thức tính thời gian từng Stage
Nguồn công suất: `Machine.specifications` hoặc default trong `MachineSelectionService#getDefaultCapacityForMachineType`.
```
capacityPerHour → estimatedDurationHours = requiredQuantity / capacityPerHour (scale=2)
maxDailyCapacity = capacityPerHour * 8
canHandleQuantity = requiredQuantity <= maxDailyCapacity
```
Chi tiết loại stage (đã liệt kê ở version trước) vẫn giữ nguyên.

## 8. Machine Selection & Priority Score
Logic chi tiết trong `MachineSelectionService` (đã mô tả trước). Các stage đặc biệt:
- DYEING: outsourced → machineId=null, priorityScore=90
- PACKAGING: manual → machineId=null, priorityScore=85

## 9. Kiểm tra khả dụng máy (Availability)
Thứ tự kiểm tra: maintenance → stages đã gán → work orders active → assignments. Nếu conflict tìm slot 8h tiếp theo trong 7 ngày.

## 10. Check Conflicts Stage
GET `/v1/production-plans/stages/{stageId}/check-conflicts` → List<String>.

## 11. Gán nhân sự (In-charge & QC)
- In-charge: PUT `/v1/production-plans/stages/{stageId}/assign-incharge?userId=...` (ĐÃ CÓ).
- QC: CHƯA CÓ – cần bổ sung như đề xuất ở mục 6.

## 12. Material Consumption
(Phần công thức chi tiết giữ nguyên – xem phiên bản cũ). Fallback 10% waste.

## 13. Material Availability & Requisition
- Kiểm tra tồn và reserved theo các plan APPROVED khác.
- Chỉ tạo requisition khi đủ.

## 14. API Chi Tiết (Cập nhật)
### 14.1 Machine Selection
| Endpoint | Method | Mô tả |
|----------|--------|-------|
| /v1/machine-selection/suitable-machines | GET | Gợi ý máy theo tham số query. |
| /v1/machine-selection/suggest-machines | POST | Gợi ý máy với body request. |
| /v1/machine-selection/check-availability | GET | Kiểm tra availability máy (stub – cần hoàn thiện). |

### 14.2 Production Plans & Stages (đã bao gồm lot & stage endpoints hiện có)
| Endpoint | Method | Mô tả |
|----------|--------|------|
| /v1/production-lots | GET | Danh sách lot (filter theo status). |
| /v1/production-lots/{id} | GET | Chi tiết lot (product, orders, current plan). |
| /v1/production-plans | GET | Danh sách kế hoạch. |
| /v1/production-plans/{id} | GET | Chi tiết kế hoạch. |
| /v1/production-plans/status/{status} | GET | Lọc theo trạng thái. |
| /v1/production-plans/pending-approval | GET | Danh sách chờ duyệt. |
| /v1/production-plans/creator/{userId} | GET | Theo người tạo. |
| /v1/production-plans/contract/{contractId} | GET | Theo hợp đồng. |
| /v1/production-plans/approved-not-converted | GET | Đã duyệt chưa convert. |
| /v1/production-plans | POST | Tạo kế hoạch (merge contract → lot). |
| /v1/production-plans/{id}/submit | PUT | Submit duyệt. |
| /v1/production-plans/{id}/approve | PUT | Phê duyệt. |
| /v1/production-plans/{id}/reject | PUT | Từ chối. |
| /v1/production-plans/{planId}/stages | GET | Danh sách stage theo plan. |
| /v1/production-plans/stages/{stageId} | PUT | Cập nhật tổng hợp stage. |
| /v1/production-plans/stages/{stageId}/assign-incharge | PUT | Gán in-charge. |
| /v1/production-plans/stages/{stageId}/assign-qc | PUT | Gán QC. |
| /v1/production-plans/stages/{stageId}/machine-suggestions | GET | Gợi ý máy cho stage. |
| /v1/production-plans/stages/{stageId}/auto-assign-machine | POST | Tự động gán máy. |
| /v1/production-plans/stages/{stageId}/check-conflicts | GET | Xung đột lịch. |
| /v1/production-plans/machine-suggestions | GET | Gợi ý máy khi tạo stage mới. |

### 14.3 Material Consumption & Availability
| Endpoint | Method | Mô tả |
|----------|--------|-------|
| /v1/material-consumption/production-plan/{planId} | GET | Tính tiêu hao default. |
| /v1/material-consumption/production-plan/{planId}/with-waste | GET | Tính tiêu hao custom waste. |
| /v1/material-consumption/production-plan/{planId}/availability | GET | Khả dụng NVL. |
| /v1/material-consumption/production-plan/{planId}/create-requisition | POST | Tạo requisition. |

## 15. Mapping UI Form → API (cập nhật)
Bảng Lot ("Đơn Hàng Đã Merge (Auto)"):
- GET `/v1/production-lots?status=READY_FOR_PLANNING` → render bảng (các cột: lotCode, productName, sizeSnapshot, totalQuantity, deliveryDateTarget, orderNumbers, currentPlanStatus).
- Click mã lô (nếu có chi tiết): GET `/v1/production-lots/{lotId}`.
- Action “Lập kế hoạch”: POST `/v1/production-plans` với `contractId` thuộc lô (hoặc endpoint riêng theo lotId nếu bổ sung sau).

Form chi tiết Lô (BATCH-001):
1) Header
- Thông tin lô: lấy từ `GET /v1/production-plans/{planId}` (plan.lot.*).
- NVL tiêu hao: `GET /v1/material-consumption/production-plan/{planId}`.
- Ngày tổng: FE tính từ min(plannedStartTime) & max(plannedEndTime) của stages.

2) Block công đoạn
- Load stages: `GET /v1/production-plans/{planId}/stages`.
- Actions per stage:
  - Gợi ý máy: GET `/v1/production-plans/stages/{stageId}/machine-suggestions`.
  - Auto gán: POST `/v1/production-plans/stages/{stageId}/auto-assign-machine`.
  - Cập nhật tổng hợp: PUT `/v1/production-plans/stages/{stageId}` (assignedMachineId, inChargeUserId, qcUserId, plannedStartTime, plannedEndTime, notes...).
  - Gán nhanh inCharge: PUT `/v1/production-plans/stages/{stageId}/assign-incharge?userId=`.
  - Gán nhanh QC: PUT `/v1/production-plans/stages/{stageId}/assign-qc?userId=`.
  - Check conflict: GET `/v1/production-plans/stages/{stageId}/check-conflicts`.

3) Submit phê duyệt: PUT `/v1/production-plans/{planId}/submit`.

Ví dụ FE flow (updated)
- Step 1: GET Lots.
- Step 2: POST plan.
- Step 3: GET plan + stages + material-consumption.
- Step 4: Với mỗi stage, hiển thị gợi ý máy & cho phép update.
- Step 5: Check conflicts, validate NVL availability.
- Step 6: Submit plan.

### 16.5 Ví dụ Lot List & Lot Detail
#### Lot List
```http
GET /v1/production-lots?status=READY_FOR_PLANNING
```
Response (rút gọn):
```json
[
  {
    "id": 10,
    "lotCode": "LOT-20251110-003",
    "productName": "Túi vải canvas",
    "sizeSnapshot": "30x40cm",
    "totalQuantity": 1500,
    "deliveryDateTarget": "2025-12-15",
    "orderNumbers": ["ORD-001","ORD-003","ORD-005"],
    "status": "READY_FOR_PLANNING",
    "currentPlanId": null,
    "currentPlanStatus": null
  }
]
```
#### Lot Detail
```http
GET /v1/production-lots/10
```
Response (rút gọn):
```json
{
  "id": 10,
  "lotCode": "LOT-20251110-003",
  "productId": 4,
  "productName": "Túi vải canvas",
  "sizeSnapshot": "30x40cm",
  "totalQuantity": 1500,
  "deliveryDateTarget": "2025-12-15",
  "orderNumbers": ["ORD-001","ORD-003","ORD-005"],
  "status": "READY_FOR_PLANNING",
  "currentPlanId": 31,
  "currentPlanStatus": "DRAFT"
}
```

## 17. Error Handling & Edge Cases
| Case | Mô tả | Phản hồi / Giải pháp UI |
|------|------|-------------------------|
| Invalid status khi lọc /v1/production-plans/status/{status} | Sai ENUM | Hiển thị toast lỗi “Trạng thái không hợp lệ” |
| Không có BOM active | Tính NVL lỗi | Chặn submit kế hoạch, hiển thị cảnh báo để tạo BOM |
| Không tìm thấy máy phù hợp auto-assign | Danh sách gợi ý rỗng | Yêu cầu chọn thủ công hoặc điều chỉnh thời gian |
| Stage có machine conflicts | check-conflicts trả list | Hiện badge cảnh báo cạnh tên stage |
| WastePercentage > 0.50 | 400 | UI validate trước khi gửi |
| Plan không ở DRAFT nhưng gọi submit | 400 | Disable nút ở UI |
| Plan không ở PENDING_APPROVAL nhưng approve/reject | 400 | Kiểm tra status trước thao tác |
| Insufficient material khi tạo requisition | 400 | Hiện danh sách thiếu cùng số lượng shortage |

## 18. Đề xuất Mở Rộng (Roadmap)
- Thêm controller riêng cho Lot: list/filter, chi tiết, thống kê.
- Batch update nhiều stage một lần (PUT /v1/production-plans/{id}/stages/bulk).
- Tích hợp ca làm việc & lịch nghỉ (calendar service) → chính xác end time.
- Tự động tính gap giữa các stage để tránh overlap máy hoặc phụ trách.
- Tính cost/time hiệu suất thực tế (actualStart/End, downtime) → KPI.
- Cho phép split stage sang nhiều máy song song.

## 19. Checklist Frontend Implementation
1. POST kế hoạch khi user nhấn “Lập kế hoạch”.
2. Load plan + stages + NVL tiêu hao.
3. Cho phép người dùng chỉnh từng stage (PUT /stages/{id}).
4. Gán inCharge & QC nếu chưa có (assign-incharge / assign-qc hoặc dùng PUT tổng hợp).
5. Auto gán máy hoặc chọn thủ công rồi cập nhật.
6. Check conflicts + validate NVL availability.
7. Submit kế hoạch.
8. Director duyệt.

## 20. Tóm tắt
- Lot merging + versioning cung cấp nền tảng linh hoạt.
- Stage entity đủ thông tin cho UI lập kế hoạch chi tiết, cần bổ sung endpoint update & assign QC.
- Machine selection có thuật toán xếp hạng + xử lý outsourced/manual.
- Material consumption & availability đảm bảo tính khả thi trước phê duyệt.
- Guide này là điểm tham chiếu đồng bộ giữa backend & frontend.

---
Kết thúc tài liệu.
