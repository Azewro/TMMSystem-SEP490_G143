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
15. Khi APPROVED, hệ thống cũng sinh bản ghi `machine_assignment` cho từng stage đã gán máy (start/end theo plannedStartTime/EndTime) để khóa lịch máy phục vụ check capacity.

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
- Mỗi lần Planner chỉnh `assignedMachineId` + `plannedStartTime/plannedEndTime` qua `PUT /v1/production-plans/stages/{stageId}`, dữ liệu được ghi trực tiếp vào bảng `production_plan_stage`. `MachineSelectionService` sử dụng chính các bản ghi này như “lịch máy” hiện tại nên chỉ cần update stage là đủ để mọi lần gợi ý/check-conflicts nhận biết slot đã bị chiếm.

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

### 13.2 Production Lots, Plans & Stages (rút gọn API cho công đoạn)
| Endpoint | Method | Mô tả |
|----------|--------|------|
| /v1/production-lots | GET | Danh sách lot (filter theo status). |
| /v1/production-lots/{id} | GET | Chi tiết lot. |
| /v1/production-plans | GET/POST | Danh sách / tạo kế hoạch từ Contract. |
| /v1/production-plans/create-from-lot?lotId=... | POST | Tạo kế hoạch từ Lot. |
| /v1/production-plans/{id} | GET | Chi tiết kế hoạch. |
| /v1/production-plans/{id}/submit | PUT | Gửi duyệt. |
| /v1/production-plans/{id}/approve | PUT | Phê duyệt. |
| /v1/production-plans/{id}/reject | PUT | Từ chối. |
| /v1/production-plans/{planId}/stages | GET | Danh sách stage theo plan. |
| /v1/production-plans/stages/{stageId} | PUT | Cập nhật tổng hợp stage (máy, inCharge, QC, thời gian, ghi chú). |
| /v1/production-plans/stages/{stageId}/machine-suggestions | GET | Gợi ý máy theo stageId. |
| /v1/production-plans/stages/{stageId}/auto-assign-machine | POST | Tự động gán máy. |
| /v1/production-plans/stages/{stageId}/check-conflicts | GET | Kiểm tra xung đột lịch. |

Ghi chú: các API riêng lẻ để gán in-charge hoặc QC đã được gom về một API `PUT /v1/production-plans/stages/{stageId}` (truyền inChargeUserId, qcUserId trong body). API gợi ý máy tổng quát theo tham số tự do cũng được lược bỏ; FE lấy gợi ý theo stageId để đảm bảo dữ liệu đồng nhất (product/quantity/time lấy từ plan.lot và stage).

## 14. Mapping UI → API theo MÀN MẪU (Ảnh 3,4,5)
A. Ảnh 3: Danh sách “Đơn Hàng Đã Merge (Auto)”
- API: `GET /v1/production-lots?status=READY_FOR_PLANNING`.
- Cột: lotCode, productName, sizeSnapshot, totalQuantity, deliveryDateTarget, orderNumbers (badge ORD-xxx), status, currentPlanStatus.
- Hành động:
  - “Lập kế hoạch”: `POST /v1/production-plans/create-from-lot?lotId={id}` → trả về plan DRAFT; điều hướng sang màn 4/5.
  - “Xem”: nếu đã có `currentPlanId` → điều hướng `/production-plans/{planId}`.

B. Ảnh 4 + 5: Form “Lập Kế Hoạch Sản Xuất – BATCH-xxx”
- Header & tổng quan:
  - `GET /v1/production-plans/{planId}` → lấy lotCode, productName, totalQuantity.
  - NVL tiêu hao: `GET /v1/material-consumption/production-plan/{planId}` và (nếu cần) `/availability`.
- Chi tiết công đoạn:
  - `GET /v1/production-plans/{planId}/stages`.
  - Cho từng stage: gợi ý máy `GET .../stages/{stageId}/machine-suggestions`; auto gán `POST .../auto-assign-machine`.
  - Gán người `PUT .../assign-incharge?userId=...` + `PUT .../assign-qc?userId=...`.
  - Cập nhật tổng hợp `PUT /v1/production-plans/stages/{stageId}`.
  - Kiểm tra xung đột `GET .../check-conflicts`.
- Gửi duyệt: `PUT /v1/production-plans/{planId}/submit`.

## 14bis. Luồng thao tác chi tiết theo ảnh 3 → 4 → 5
1) Auto-merge Lô (nền):
   - Sau khi hợp đồng APPROVED: hệ thống tách QuotationDetail theo productId và merge theo Option A (1 lot = 1 product).
   - Scheduler AutoMergeService chạy định kỳ (mặc định 5 phút) để bắt các hợp đồng còn sót.
2) Ảnh 3 – Danh sách Lô đã merge:
   - FE: GET `/v1/production-lots?status=READY_FOR_PLANNING` → render bảng.
   - Tooltip badge ORD-xxx: gọi GET `/v1/production-lots/{id}/contracts` khi cần.
   - Bấm “Lập kế hoạch” → POST `/v1/production-plans/create-from-lot?lotId=...`.
     - BE: tạo Plan version mới (DRAFT), `lot.status=PLANNING` (khóa lô), nếu `planning.autoInitStages=true` hệ thống sinh sẵn 6 công đoạn mặc định.
     - FE: điều hướng `/production-plans/{planId}`.
3) Ảnh 4 – Header kế hoạch (BATCH-xxx):
   - FE: GET `/v1/production-plans/{planId}` để lấy `plan.lot.*` (lotCode, productName, totalQuantity...).
   - FE: GET NVL `/v1/material-consumption/production-plan/{planId}` (+ `/availability`).
4) Ảnh 5 – Chi tiết công đoạn:
   - FE: GET `/v1/production-plans/{planId}/stages`.
   - Mỗi stage:
     - Gợi ý máy: GET `/v1/production-plans/stages/{stageId}/machine-suggestions`.
     - Auto gán: POST `/v1/production-plans/stages/{stageId}/auto-assign-machine`.
     - Gán in-charge: PUT `/v1/production-plans/stages/{stageId}/assign-incharge?userId=...`.
     - Gán QC: PUT `/v1/production-plans/stages/{stageId}/assign-qc?userId=...`.
     - Cập nhật tổng hợp: PUT `/v1/production-plans/stages/{stageId}`.
     - Check xung đột: GET `/v1/production-plans/stages/{stageId}/check-conflicts`.
5) Gửi duyệt / Duyệt:
   - FE: PUT `/v1/production-plans/{planId}/submit` → Director dùng PUT `/v1/production-plans/{planId}/approve`.
   - BE: sinh Production Order; `lot.status=PLAN_APPROVED`.

## 21. Khởi tạo 6 công đoạn mặc định (tận dụng sẵn trong service)
- Mặc định bật: `planning.autoInitStages=true` (cấu hình trong application.properties).
- Khi tạo kế hoạch từ Lot hoặc Contract, nếu plan chưa có stage nào, hệ thống tự khởi tạo 6 stage chuẩn theo thứ tự:
  1. WARPING (cuồng mắc)
  2. WEAVING (dệt)
  3. DYEING (nhuộm – mặc định vendor/outsource, có thể để machine=null)
  4. CUTTING (cắt)
  5. HEMMING (viền/may)
  6. PACKAGING (đóng gói)
- Mỗi stage mặc định có plannedStart/End kế tiếp nhau (4h/khối lượng giả định). FE có thể chỉnh lại thời gian, máy, người phụ trách, QC.
- Nếu muốn tắt tự khởi tạo (để planner tự thêm tay), đặt: `planning.autoInitStages=false`.

## 20. Bổ sung cấu hình
- Bật scheduler: đã mặc định `@EnableScheduling` trong ứng dụng.
- Cron auto-merge: `autoMerge.cron=0 0/5 * * * *` (mặc định). Có thể đổi trong `application.properties`.
