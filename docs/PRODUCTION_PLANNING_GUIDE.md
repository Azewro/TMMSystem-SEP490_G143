# HƯỚNG DẪN LẬP KẾ HOẠCH SẢN XUẤT (PRODUCTION PLANNING)

Tài liệu này mô tả luồng lập kế hoạch sau khi báo giá được khách duyệt → hình thành Hợp đồng (Contract) → gom đơn thành Lot → tạo phiên bản Kế hoạch (Production Plan) → sinh & chỉnh sửa các Stage (công đoạn) với gợi ý máy, gán nhân sự & QC, kiểm tra nguyên vật liệu → phê duyệt → sinh Production Order.

> Phiên bản: 2025-11-11. (Cập nhật: bổ sung "GET lot" và quy tắc khoá lot; khẳng định gán máy/nhân sự/NVL theo lô; sửa trạng thái endpoint QC = ĐÃ CÓ; thêm hướng dẫn FE kéo dữ liệu theo màn mẫu; bổ sung công thức/giả định tính theo từng công đoạn.)

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
3. Merge vào Lot: logic trong `ProductionPlanService#createOrMergeLotFromContract(contractId)` (gọi ngầm khi POST /v1/production-plans) và cũng được gọi NGAY SAU khi hợp đồng được duyệt để tự động gom đơn.
4. Tạo Production Plan phiên bản mới (DRAFT): POST `/v1/production-plans` với `contractId`.
5. Lấy danh sách Lot: GET `/v1/production-lots?status=READY_FOR_PLANNING` và chi tiết Lot: GET `/v1/production-lots/{id}`.
6. Lấy kế hoạch chi tiết: GET `/v1/production-plans/{id}`.
7. Điều chỉnh Stage:
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
- Ngày ký hợp đồng nằm trong [contractDate ± 1 ngày].
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

Lưu ý trạng thái & chuyển tiếp hiện có:
- Khi tạo version kế hoạch: `createPlanVersion(lotId)` sẽ đặt `lot.status = PLANNING`.
- Khi Director duyệt kế hoạch: đặt `lot.status = PLAN_APPROVED`.
- Trạng thái `IN_PRODUCTION` và `COMPLETED` hiện được phản ánh chính xác ở cấp PO/WO; việc đồng bộ ngược lại sang Lot là tùy chọn, có thể bổ sung sau (roadmap).

## 3.1 Quy tắc khóa Lot khi bắt đầu lập kế hoạch
- Trong `createPlanVersion(lotId)` hệ thống đặt `lot.status = PLANNING`.
- Khi status = PLANNING hoặc cao hơn (PLAN_APPROVED, IN_PRODUCTION, COMPLETED) thì KHÔNG merge thêm contract vào lot nữa.
- Chỉ các lot ở trạng thái FORMING hoặc READY_FOR_PLANNING mới nhận thêm hợp đồng.
- Nếu cần thêm đơn sau khi đã PLANNING: phải tạo lot mới hoặc huỷ kế hoạch hiện tại rồi rollback status thủ công (không khuyến khích trong môi trường production).

## 3.2 Máy móc/NVL/Nhân sự gán theo LÔ (qua các Stage của Plan)
- Mỗi Stage trong Production Plan đại diện cho công đoạn xử lý TOÀN BỘ khối lượng `lot.totalQuantity` (đã gom các hợp đồng/đơn con).
- Không gán máy theo từng contract sau khi đã merge lot; mọi gán máy, in-charge, QC, thời gian đều thuộc các stage của kế hoạch gắn với lô.
- Khi tạo version mới (plan mới cho cùng lot): dữ liệu version cũ vẫn lưu để audit; version mới cần gán lại (hoặc auto-assign) cho các stage.

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
- stageType (WARPING, WEAVING, DYEING, CUTTING, HEMMING, PACKAGING)
  - Lưu ý: HEMMING tương ứng “viền/may”. Một số UI có thể gọi SEWING, nhưng giá trị stageType chuẩn trên backend là HEMMING.
- sequenceNo (thứ tự hiển thị 1.,2.,3., ...)
- assignedMachine (id, code, name) – có thể null với DYEING (outsourced) & PACKAGING (manual).
- inChargeUser (Người phụ trách)
- qcUser (Người kiểm tra chất lượng) – endpoint gán QC: ĐÃ CÓ (`PUT /v1/production-plans/stages/{stageId}/assign-qc?userId=...`).
- plannedStartTime / plannedEndTime (thời gian kế hoạch)
- minRequiredDurationMinutes (thời lượng tối thiểu từ machine suggestion)
- capacityPerHour (năng suất dùng để tính estimatedOutput)
- transferBatchQuantity (nếu áp dụng chuyển mẻ giữa các stage – hiện có field, UI chưa dùng)
- stageStatus (PENDING, READY, IN_PROGRESS, PAUSED, COMPLETED, CANCELED)
- setupTimeMinutes / teardownTimeMinutes (thời gian chuẩn bị/kết thúc – có thể map tooltip)
- actualStartTime / actualEndTime / downtimeMinutes / downtimeReason (phục vụ tracking thực tế)
- quantityInput / quantityOutput (thực tế – dùng để đánh giá hao hụt nội bộ)
- notes (Ghi chú)

DTO `ProductionPlanStageDto` có calculated fields:
- durationMinutes = end - start
- estimatedOutput = capacityPerHour * durationMinutes / 60

### 6.1. Cách tính chi tiết theo từng công đoạn (giả định khả dụng sẵn trong MachineSelectionService/Machine.specifications)
- Warping (Mắc):
  - capacityPerHour lấy từ máy loại WARPING (mặc định nếu máy thiếu cấu hình).
  - Thời lượng ước tính (giờ) = `lot.totalQuantity / capacityPerHour`.
  - Có thể cộng `setupTimeMinutes` (ví dụ 30) và `teardownTimeMinutes` (ví dụ 15) nếu có cấu hình.
- Weaving (Dệt):
  - capacityPerHour theo máy dệt.
  - Áp dụng tương tự Warping; chú ý tránh overlap với lịch máy đã gán (sử dụng check-conflicts).
- Dyeing (Nhuộm – Vendor):
  - Không gán máy nội bộ (assignedMachine=null), coi như outsource.
  - Lead time chuẩn (ví dụ 24–48h) cấu hình trong service; FE hiển thị như một block thời gian cố định giữa Weaving → Cutting.
- Cutting (Cắt), Hemming (Viền/May), Packaging (Đóng gói):
  - capacityPerHour theo từng loại máy (PACKAGING có thể manual → machine=null, capacity từ cấu hình mặc định).
  - Công thức ước tính tương tự.

Lưu ý: Nếu cần override thời lượng thủ công, xem mục "Thiếu Endpoint" bên dưới.

### Thiếu Endpoint hiện tại so với UI
| Nhu cầu UI | Trạng thái hỗ trợ |
|------------|------------------|
| Bulk update nhiều stage một lần | CHƯA có (đề xuất `PUT /v1/production-plans/{id}/stages/bulk`) |
| Override thời lượng thủ công (khác với end-start) | CHƯA có field riêng (đề xuất thêm `plannedDurationMinutes`) |
| Split một stage cho nhiều máy song song | CHƯA có (roadmap) |

> Các nhu cầu khác (update stage, assign QC, assign inCharge, auto-assign, check-conflicts) đã được hỗ trợ bằng endpoint hiện hành.

Đề xuất payload update stage (đã hỗ trợ):
```
PUT /v1/production-plans/stages/{stageId}
{
  "plannedStartTime": "2025-11-12T08:00:00",
  "plannedEndTime":   "2025-11-12T12:00:00",
  "assignedMachineId": 3,
  "inChargeUserId": 5,
  "qcUserId": 7,
  "notes": "Ưu tiên máy vừa bảo trì xong"
}
```

## 7. Machine Selection & Priority Score
- Nguồn công suất: `Machine.specifications` hoặc default trong `MachineSelectionService#getDefaultCapacityForMachineType`.
- Điểm ưu tiên tổng hợp (priority) gồm: availability score, phù hợp công suất, vị trí, mức ưu tiên loại máy.
- Các stage đặc biệt:
  - DYEING: outsourced → machineId=null, priorityScore≈90, lead time cố định.
  - PACKAGING: manual → machineId=null, priorityScore≈85.

## 8. Kiểm tra khả dụng máy (Availability)
Thứ tự kiểm tra: maintenance → stages đã gán → work orders active → assignments. Nếu conflict, thuật toán gợi ý slot 8h tiếp theo trong 7 ngày.

## 9. Check Conflicts Stage
GET `/v1/production-plans/stages/{stageId}/check-conflicts` → List<String>.

## 10. Gán nhân sự (In-charge & QC)
- In-charge: PUT `/v1/production-plans/stages/{stageId}/assign-incharge?userId=...`.
- QC: PUT `/v1/production-plans/stages/{stageId}/assign-qc?userId=...`.

## 11. Material Consumption
- Tính tiêu hao theo BOM hoạt động; nếu không có BOM active → lấy BOM mới nhất.
- Mặc định waste 10%; có thể tính với `with-waste?wastePercentage=...`.

## 12. Material Availability & Requisition
- Kiểm tra tồn và reserved theo các plan APPROVED khác.
- Chỉ tạo requisition khi đủ; nếu thiếu, trả danh sách shortage để FE hiển thị.

## 13. API Chi Tiết (Cập nhật)
### 13.1 Machine Selection
| Endpoint | Method | Mô tả |
|----------|--------|-------|
| /v1/machine-selection/suitable-machines | GET | Gợi ý máy theo query. |
| /v1/machine-selection/suggest-machines | POST | Gợi ý máy với body. |
| /v1/machine-selection/check-availability | GET | Kiểm tra availability máy. |

### 13.2 Production Lots, Plans & Stages
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

### 13.3 Material Consumption & Availability
| Endpoint | Method | Mô tả |
|----------|--------|-------|
| /v1/material-consumption/production-plan/{planId} | GET | Tính tiêu hao default. |
| /v1/material-consumption/production-plan/{planId}/with-waste | GET | Tính tiêu hao custom waste. |
| /v1/material-consumption/production-plan/{planId}/availability | GET | Khả dụng NVL. |
| /v1/material-consumption/production-plan/{planId}/create-requisition | POST | Tạo requisition. |

## 14. Mapping UI → API theo MÀN MẪU
A. Màn “Danh sách đơn hàng đã gộp” (của Kế hoạch)
- API: `GET /v1/production-lots?status=READY_FOR_PLANNING`.
- Cột hiển thị: `lotCode`, `productName`, `sizeSnapshot`, `totalQuantity`, `deliveryDateTarget`, `orderNumbers`, `status`, `currentPlanStatus`.
- Action “Lập kế hoạch”: gọi `POST /v1/production-plans` với `{ "contractId": <bất kỳ contract trong lot> }` hoặc (nếu dùng trực tiếp theo lot) endpoint mở rộng `POST /v1/production-plans/create-from-lot?lotId=...` (đã có service `createPlanFromLot`, có thể map ra controller khi cần).

B. Màn “Lập kế hoạch sản xuất” (Form chi tiết theo LÔ)
1) Header
- Lấy từ `GET /v1/production-plans/{planId}` → `plan.lot.*` và `plan.status`.
- NVL tiêu hao: `GET /v1/material-consumption/production-plan/{planId}` (+ availability nếu cần).
- Ngày tổng: FE tính `min(plannedStartTime)` và `max(plannedEndTime)` trên danh sách stage.

2) Khối Công đoạn (cuộn mắc, dệt, nhuộm, cắt, may/viền, đóng gói)
- `GET /v1/production-plans/{planId}/stages`.
- Cho từng stage:
  - Gợi ý máy: `GET /v1/production-plans/stages/{stageId}/machine-suggestions`.
  - Auto gán: `POST /v1/production-plans/stages/{stageId}/auto-assign-machine`.
  - Gán người: `PUT /v1/production-plans/stages/{stageId}/assign-incharge?userId=...` và `PUT /v1/production-plans/stages/{stageId}/assign-qc?userId=...`.
  - Cập nhật tổng hợp: `PUT /v1/production-plans/stages/{stageId}` (máy, người, thời gian, ghi chú).
  - Kiểm tra xung đột: `GET /v1/production-plans/stages/{stageId}/check-conflicts` → hiển thị badge cảnh báo nếu có.

3) Submit phê duyệt
- `PUT /v1/production-plans/{planId}/submit`.

C. Màn “Danh sách lập kế hoạch của Kế hoạch”
- API: `GET /v1/production-plans` hoặc lọc theo creator `GET /v1/production-plans/creator/{userId}`.
- Cột: `planCode`, `productName` (từ `plan.lot.product`), `totalQuantity` (`plan.lot.totalQuantity`), `plannedStart~End` (tính từ stages), `createdBy`, `status`.

D. Màn “Chi tiết kế hoạch của Kế hoạch”
- API: `GET /v1/production-plans/{id}` + `GET /v1/production-plans/{id}/stages` + NVL như mục B.
- Nút: Chỉnh sửa (dùng PUT stage), Quay lại.

E. Màn “Danh sách/Chi tiết duyệt của Giám đốc”
- List: `GET /v1/production-plans/pending-approval`.
- Detail: `GET /v1/production-plans/{id}` + stages.
- Nút: `PUT /v1/production-plans/{id}/approve` hoặc `/reject` (kèm lý do).

F. Khi APPROVED
- Hệ thống tự tạo Production Order từ LÔ đã duyệt (số lượng = `lot.totalQuantity`).
- Các vai trò PO/WO triển khai như tài liệu Production/Execution.

Ví dụ FE flow (chuẩn hoá theo màn mẫu)
- Step 1: `GET /v1/production-lots?status=READY_FOR_PLANNING` → render bảng.
- Step 2: user chọn 1 lô → `POST /v1/production-plans` với `contractId` thuộc lô (hoặc dùng create-from-lot).
- Step 3: load `GET /v1/production-plans/{planId}`, `/stages`, và `material-consumption`.
- Step 4: cho từng stage: gợi ý máy → auto-assign hoặc chọn thủ công → `PUT /stages/{id}`; gán inCharge/QC.
- Step 5: `GET .../check-conflicts` + `GET material-consumption/.../availability`.
- Step 6: `PUT /v1/production-plans/{planId}/submit` → Director approve/reject.

### 14.5 Ví dụ Lot List & Lot Detail
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

## 15. FAQ nhanh (trả lời các câu hỏi quan trọng)
- Trước khi lập kế hoạch có thể merge bao nhiêu contract vào lô cũng được? → Đúng, miễn là Lot ở trạng thái FORMING hoặc READY_FOR_PLANNING và cùng tiêu chí gộp.
- Sau khi bắt đầu lập kế hoạch có còn merge thêm vào lô được không? → Không. Khi tạo phiên bản kế hoạch (`createPlanVersion`) Lot sẽ bị khoá về trạng thái PLANNING và không nhận thêm hợp đồng.
- Khi lập kế hoạch gắn máy cho "plan riêng" hay cho "lô"? → Gắn ở cấp công đoạn (stage) của kế hoạch gắn với LÔ. Về bản chất là gắn cho lô (khối lượng đã gom), không phải cho từng contract nữa.
- Kiểm tra NVL/khả dụng máy áp dụng theo từng contract hay theo lô? → Theo lô. Tất cả phép tính tiêu hao, availability NVL và xếp máy đều dựa trên `lot.totalQuantity`.
- FE muốn lấy "GET lot" để hiện danh sách lô thì gọi gì? → `GET /v1/production-lots?status=READY_FOR_PLANNING`; chi tiết 1 lô: `GET /v1/production-lots/{id}`.

## 16. Error Handling & Edge Cases
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

## 17. Đề xuất Mở Rộng (Roadmap)
- Thêm controller riêng cho Lot: list/filter, chi tiết, thống kê.
- Batch update nhiều stage một lần (PUT /v1/production-plans/{id}/stages/bulk).
- Tích hợp ca làm việc & lịch nghỉ (calendar service) → chính xác end time.
- Tự động tính gap giữa các stage để tránh overlap máy hoặc phụ trách.
- Tính cost/time hiệu suất thực tế (actualStart/End, downtime) → KPI.
- Cho phép split stage sang nhiều máy song song.

## 18. Checklist Frontend Implementation
1. `GET /v1/production-lots?status=READY_FOR_PLANNING` → pick lô.
2. `POST /v1/production-plans` (hoặc create-from-lot) → tạo plan DRAFT, lock Lot=PLANNING.
3. Load plan + stages + NVL tiêu hao.
4. Cho phép người dùng chỉnh từng stage (PUT /stages/{id}).
5. Gán inCharge & QC (assign-incharge / assign-qc hoặc dùng PUT tổng hợp).
6. Auto gán máy hoặc chọn thủ công rồi cập nhật.
7. Check conflicts + validate NVL availability.
8. Submit kế hoạch; Director duyệt.

## 19. Tóm tắt
- Lot merging + versioning cung cấp nền tảng linh hoạt.
- Stage entity đủ thông tin cho UI lập kế hoạch chi tiết; gán QC đã có endpoint.
- Machine selection có thuật toán xếp hạng + xử lý outsourced/manual.
- Material consumption & availability đảm bảo tính khả thi trước phê duyệt.
- Guide này là điểm tham chiếu đồng bộ giữa backend & frontend.

---
Kết thúc tài liệu.

## 20. Bổ sung cập nhật (2025-11-11)
### 20.1 Chuẩn hóa gộp Lot theo 3 tiêu chí
- Cùng sản phẩm (productId)
- Ngày giao hàng nằm trong [deliveryDate ±1 ngày]
- Ngày ký hợp đồng nằm trong [contractDate ±1 ngày]
- Chỉ Lot ở trạng thái FORMING hoặc READY_FOR_PLANNING mới nhận thêm merge.
- Khi merge: cập nhật `contractDateMin/Max` nếu hợp đồng mới nằm ngoài khoảng hiện tại.

### 20.2 Tự động gộp ngay sau duyệt hợp đồng
- Sau `POST /v1/contracts/{id}/approve` service gọi `createOrMergeLotFromContract(contractId)` để đưa hợp đồng vào Lot phù hợp hoặc tạo Lot mới.
- Batch merge các hợp đồng APPROVED chưa được lập kế hoạch vẫn chạy để đảm bảo đồng bộ.

### 20.3 Tạo kế hoạch từ Lot hiện có
- Endpoint nội bộ service đã có `createPlanFromLot(lotId)`; có thể mở rộng controller (nếu cần) để: `POST /v1/production-plans/create-from-lot?lotId=...`.

### 20.4 Wizard tạo Work Order chuẩn
- Endpoint mới: `POST /v1/production/orders/{poId}/work-orders/create-standard` → Sinh 1 Work Order với các stage mặc định: WARPING → WEAVING → DYEING (outsourced) → CUTTING → HEMMING → PACKAGING cho mỗi ProductionOrderDetail.
- Mặc định status stage = PENDING, chưa gán máy/leader.

### 20.5 Leader actions (công đoạn sản xuất)
| Hành động | Endpoint | Params bắt buộc | Ghi chú |
|----------|----------|-----------------|---------|
| Bắt đầu | POST `/v1/production/stages/{id}/start` | leaderUserId | Optional: evidencePhotoUrl, qtyCompleted |
| Tạm dừng | POST `/v1/production/stages/{id}/pause` | leaderUserId, pauseReason | Lưu StagePauseLog |
| Tiếp tục | POST `/v1/production/stages/{id}/resume` | leaderUserId | Tính duration pause |
| Hoàn thành | POST `/v1/production/stages/{id}/complete` | leaderUserId | Optional: evidencePhotoUrl, qtyCompleted |

- Các action ghi StageTracking (START/PAUSE/RESUME/COMPLETE) + ảnh chứng cứ.
- Kiểm tra quyền: leaderUserId phải trùng với `assignedLeaderId` của stage.

### 20.6 Điều kiện hoàn tất Production Order
- Trước đây: hoàn tất ngay khi Packaging PASS của một WorkOrderDetail.
- Nay: Khi một Packaging stage PASS → kiểm tra toàn bộ WorkOrderDetail thuộc cùng PO:
  - Mỗi WorkOrderDetail phải có đầy đủ các stage.
  - Tất cả stage QC phải PASS.
  - Packaging stage của từng WorkOrderDetail đã COMPLETE.
- Nếu thỏa: PO.status = ORDER_COMPLETED và gửi notify đến Sales, Planning, Director, Technical, PM.

### 20.7 QC PASS hook
- PASS Inspection gọi `markStageQcPass(stageId)` → nếu là PACKAGING chạy logic kiểm tra hoàn tất PO như trên.

### 20.8 Lưu ý tương thích
- Các Lot đang ở trạng thái PLANNING / PLAN_APPROVED / IN_PRODUCTION không nhận merge mới.
- Nếu cần thêm hợp đồng mới sau khi Lot đã PLANNING → tạo Lot mới thay vì ép merge.
