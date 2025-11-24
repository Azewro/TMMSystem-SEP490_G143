# LUỒNG SẢN XUẤT VÀ CÁC BẢNG DATABASE

## TỔNG QUAN LUỒNG

```
LẬP KẾ HOẠCH → PHÊ DUYỆT → TẠO PRODUCTION ORDER → TẠO WORK ORDER → THỰC THI STAGES → QC → HOÀN THÀNH
```

---

## PHẦN 1: LẬP KẾ HOẠCH SẢN XUẤT (PLANNING)

### Các bảng chính:

#### 1. **Contract** (`contract`)
- **Mục đích**: Hợp đồng với khách hàng
- **Trạng thái**: `APPROVED` → trigger tạo ProductionLot
- **Quan hệ**: 
  - 1 Contract → N QuotationDetail
  - 1 Contract → N ProductionLotOrder

#### 2. **QuotationDetail** (`quotation_detail`)
- **Mục đích**: Chi tiết báo giá (sản phẩm, số lượng, giá)
- **Quan hệ**: 
  - N QuotationDetail → 1 Quotation
  - 1 QuotationDetail → N ProductionLotOrder

#### 3. **ProductionLot** (`production_lot`)
- **Mục đích**: Nhóm các đơn hàng cùng sản phẩm, cùng thời gian giao hàng
- **Trạng thái**: 
  - `FORMING` → đang hình thành
  - `READY_FOR_PLANNING` → sẵn sàng lập kế hoạch
  - `PLANNING` → đang lập kế hoạch (locked)
  - `PLAN_APPROVED` → kế hoạch đã được duyệt
- **Quan hệ**: 
  - 1 ProductionLot → N ProductionLotOrder
  - 1 ProductionLot → N ProductionPlan

#### 4. **ProductionLotOrder** (`production_lot_order`)
- **Mục đích**: Liên kết giữa Lot và Contract/QuotationDetail
- **Quan hệ**: 
  - N ProductionLotOrder → 1 ProductionLot
  - N ProductionLotOrder → 1 Contract
  - N ProductionLotOrder → 1 QuotationDetail

#### 5. **ProductionPlan** (`production_plan`)
- **Mục đích**: Kế hoạch sản xuất cho một Lot
- **Trạng thái**: 
  - `DRAFT` → đang soạn thảo
  - `PENDING_APPROVAL` → chờ phê duyệt
  - `APPROVED` → đã phê duyệt → trigger tạo ProductionOrder
  - `SUPERSEDED` → đã bị thay thế bởi version mới
- **Versioning**: 
  - `version_no`: số phiên bản
  - `is_current_version`: true nếu là version hiện tại
- **Quan hệ**: 
  - 1 ProductionPlan → 1 ProductionLot
  - 1 ProductionPlan → N ProductionPlanStage

#### 6. **ProductionPlanStage** (`production_plan_stage`)
- **Mục đích**: Chi tiết từng công đoạn trong kế hoạch (6 công đoạn: WARPING, WEAVING, DYEING, CUTTING, HEMMING, PACKAGING)
- **Thông tin**: 
  - `stage_type`: loại công đoạn
  - `sequence_no`: thứ tự (1-6)
  - `assigned_machine_id`: máy được phân công
  - `in_charge_user_id`: người phụ trách (Team Leader)
  - `qc_user_id`: người QC phụ trách
  - `planned_start_time`, `planned_end_time`: thời gian dự kiến
- **Quan hệ**: 
  - N ProductionPlanStage → 1 ProductionPlan
  - 1 ProductionPlanStage → 1 Machine (assigned_machine_id)
  - 1 ProductionPlanStage → 1 User (in_charge_user_id)
  - 1 ProductionPlanStage → 1 User (qc_user_id)

---

## PHẦN 2: TẠO PRODUCTION ORDER (Sau khi Plan được duyệt)

### Các bảng chính:

#### 7. **ProductionOrder** (`production_order`)
- **Mục đích**: Đơn hàng sản xuất (tự động tạo khi Plan được APPROVED)
- **Trạng thái**: 
  - `PENDING_APPROVAL` → chờ PM phê duyệt
  - `APPROVED` → đã phê duyệt
  - `IN_PROGRESS` → đang sản xuất
  - `COMPLETED` → hoàn thành
- **execution_status**: 
  - `WAITING_PRODUCTION` → chờ bắt đầu sản xuất
  - `IN_PROGRESS` → đang sản xuất
  - `COMPLETED` → hoàn thành
- **Quan hệ**: 
  - 1 ProductionOrder → 1 Contract
  - 1 ProductionOrder → N ProductionOrderDetail
  - 1 ProductionOrder → N WorkOrder

#### 8. **ProductionOrderDetail** (`production_order_detail`)
- **Mục đích**: Chi tiết sản phẩm trong ProductionOrder
- **Thông tin**: 
  - `product_id`: sản phẩm
  - `bom_id`, `bom_version`: BOM được lock
  - `quantity`: số lượng
- **Quan hệ**: 
  - N ProductionOrderDetail → 1 ProductionOrder
  - N ProductionOrderDetail → 1 Product
  - N ProductionOrderDetail → 1 Bom
  - N ProductionOrderDetail → N WorkOrderDetail

---

## PHẦN 3: TẠO WORK ORDER (Khi PM bắt đầu sản xuất)

### Các bảng chính:

#### 9. **WorkOrder** (`work_order`)
- **Mục đích**: Lệnh sản xuất (tạo từ ProductionPlanStage khi PM approve)
- **Trạng thái**: 
  - `DRAFT` → đang soạn thảo
  - `APPROVED` → đã phê duyệt (PM approve)
  - `REJECTED` → bị từ chối
- **Quan hệ**: 
  - N WorkOrder → 1 ProductionOrder
  - 1 WorkOrder → N WorkOrderDetail

#### 10. **WorkOrderDetail** (`work_order_detail`)
- **Mục đích**: Chi tiết WorkOrder (liên kết với ProductionOrderDetail)
- **Thông tin**: 
  - `stage_sequence`: thứ tự công đoạn (1, 2, 3...)
  - `planned_start_at`, `planned_end_at`: thời gian dự kiến
  - `work_status`: PENDING, IN_PROGRESS, COMPLETED
- **Quan hệ**: 
  - N WorkOrderDetail → 1 WorkOrder
  - N WorkOrderDetail → 1 ProductionOrderDetail
  - 1 WorkOrderDetail → N ProductionStage

---

## PHẦN 4: THỰC THI STAGES (Execution)

### Các bảng chính:

#### 11. **ProductionStage** (`production_stage`) ⭐ **BẢNG CHÍNH CHO EXECUTION**
- **Mục đích**: Công đoạn sản xuất thực tế (tạo từ ProductionPlanStage)
- **Thông tin chính**: 
  - `stage_type`: WARPING, WEAVING, DYEING, CUTTING, HEMMING, PACKAGING
  - `stage_sequence`: 1-6
  - `assigned_leader_id`: Team Leader phụ trách
  - `qc_assignee_id`: QC phụ trách
  - `machine_id`: máy được sử dụng
- **Trạng thái (`status`)**: 
  - `PENDING` → chờ làm
  - `IN_PROGRESS` → đang làm
  - `PAUSED` → tạm dừng
  - `COMPLETED` → hoàn thành
- **Trạng thái thực thi (`execution_status`)**: 
  - `WAITING` → chờ làm (stage đầu tiên sau khi start work order)
  - `PENDING` → đợi (các stage khác)
  - `IN_PROGRESS` → đang làm
  - `WAITING_QC` → chờ kiểm tra (sau khi 100%)
  - `QC_IN_PROGRESS` → đang kiểm tra
  - `QC_PASSED` → đạt QC
  - `QC_FAILED` → lỗi QC
  - `WAITING_REWORK` → chờ sửa
  - `REWORK_IN_PROGRESS` → đang sửa
  - `COMPLETED` → hoàn thành
- **Tiến độ**: 
  - `progress_percent`: 0-100%
- **QC**: 
  - `qc_last_result`: PASS/FAIL
  - `qc_last_checked_at`: thời gian kiểm tra cuối
- **Quan hệ**: 
  - N ProductionStage → 1 WorkOrderDetail
  - N ProductionStage → 1 User (assigned_leader_id)
  - N ProductionStage → 1 User (qc_assignee_id)
  - N ProductionStage → 1 Machine
  - 1 ProductionStage → N StageTracking
  - 1 ProductionStage → N QcSession
  - 1 ProductionStage → N QualityIssue

#### 12. **StageTracking** (`stage_tracking`)
- **Mục đích**: Lịch sử tracking tiến độ của stage
- **Thông tin**: 
  - `action`: START, PAUSE, RESUME, COMPLETE, REPORT_ISSUE
  - `quantity_completed`: số lượng hoàn thành
  - `timestamp`: thời gian
  - `operator_id`: người thực hiện
- **Quan hệ**: 
  - N StageTracking → 1 ProductionStage
  - N StageTracking → 1 User (operator_id)

#### 13. **StagePauseLog** (`stage_pause_log`)
- **Mục đích**: Log khi stage bị tạm dừng
- **Thông tin**: 
  - `pause_reason`: lý do tạm dừng
  - `pause_notes`: ghi chú
  - `paused_by`: người tạm dừng
- **Quan hệ**: 
  - N StagePauseLog → 1 ProductionStage

---

## PHẦN 5: KIỂM TRA CHẤT LƯỢNG (QC)

### Các bảng chính:

#### 14. **QcCheckpoint** (`qc_checkpoint`)
- **Mục đích**: Tiêu chí kiểm tra cho từng loại stage
- **Thông tin**: 
  - `stage_type`: WARPING, WEAVING, DYEING, etc.
  - `checkpoint_name`: tên tiêu chí
  - `inspection_criteria`: tiêu chuẩn kiểm tra
  - `sampling_plan`: kế hoạch lấy mẫu
  - `mandatory`: bắt buộc hay không
  - `display_order`: thứ tự hiển thị
- **Quan hệ**: Không có FK, chỉ là master data

#### 15. **QcSession** (`qc_session`)
- **Mục đích**: Phiên kiểm tra QC cho một stage
- **Thông tin**: 
  - `status`: IN_PROGRESS, SUBMITTED
  - `overall_result`: PASS/FAIL
  - `started_by_id`: người bắt đầu kiểm tra
  - `started_at`, `submitted_at`: thời gian
- **Quan hệ**: 
  - N QcSession → 1 ProductionStage
  - N QcSession → 1 User (started_by_id)

#### 16. **QcInspection** (`qc_inspection`)
- **Mục đích**: Kết quả kiểm tra từng checkpoint trong một session
- **Thông tin**: 
  - `checkpoint_id`: tiêu chí được kiểm tra
  - `result`: PASS/FAIL
  - `notes`: ghi chú
  - `photo_url`: ảnh minh chứng (nếu có lỗi)
- **Quan hệ**: 
  - N QcInspection → 1 QcSession
  - N QcInspection → 1 QcCheckpoint

#### 17. **QcPhoto** (`qc_photo`)
- **Mục đích**: Ảnh chụp lỗi trong QC
- **Quan hệ**: 
  - N QcPhoto → 1 QcInspection

#### 18. **QualityIssue** (`quality_issue`)
- **Mục đích**: Vấn đề chất lượng (tạo khi QC FAIL)
- **Thông tin**: 
  - `severity`: MINOR/MAJOR
  - `issue_type`: REWORK/MATERIAL_REQUEST
  - `status`: PENDING/PROCESSED
  - `description`: mô tả lỗi
  - `material_needed`: có cần vật liệu không
- **Quan hệ**: 
  - N QualityIssue → 1 ProductionStage
  - N QualityIssue → 1 ProductionOrder (optional)

---

## PHẦN 6: CÁC BẢNG HỖ TRỢ KHÁC

#### 19. **Notification** (`notification`)
- **Mục đích**: Thông báo cho users
- **Trigger**: 
  - WorkOrder approved → notify PM
  - Stage WAITING → notify Leader
  - Stage 100% → notify QC
  - QC PASS → notify next Leader/PM
  - QC FAIL → notify Technical

#### 20. **User** (`user`)
- **Mục đích**: Người dùng hệ thống
- **Roles**: 
  - Director (phê duyệt Plan)
  - Planning (tạo Plan)
  - Production Manager (quản lý sản xuất)
  - Team Leader (thực hiện stages)
  - QC (kiểm tra chất lượng)
  - Technical (xử lý lỗi)

#### 21. **Machine** (`machine`)
- **Mục đích**: Máy móc thiết bị
- **Quan hệ**: 
  - 1 Machine → N ProductionStage
  - 1 Machine → N ProductionPlanStage

#### 22. **Product** (`product`)
- **Mục đích**: Sản phẩm
- **Quan hệ**: 
  - 1 Product → N ProductionOrderDetail
  - 1 Product → N ProductionLot

#### 23. **Bom** (`bom`)
- **Mục đích**: Bill of Materials (định mức nguyên vật liệu)
- **Quan hệ**: 
  - 1 Bom → N ProductionOrderDetail

---

## LUỒNG DỮ LIỆU CHI TIẾT

### BƯỚC 1: Contract APPROVED
```
Contract (APPROVED)
  ↓
createOrMergeLotFromContract()
  ↓
ProductionLot (READY_FOR_PLANNING)
  ↓
ProductionLotOrder (liên kết Contract ↔ Lot)
```

### BƯỚC 2: Planner tạo Plan
```
ProductionLot (READY_FOR_PLANNING)
  ↓
createPlanFromLot()
  ↓
ProductionPlan (DRAFT, version_no=1, is_current_version=true)
  ↓
Tự động tạo 6 ProductionPlanStage
```

### BƯỚC 3: Planner chỉnh sửa Plan
```
ProductionPlanStage
  - assigned_machine_id
  - in_charge_user_id (Team Leader)
  - qc_user_id
  - planned_start_time, planned_end_time
```

### BƯỚC 4: Gửi duyệt
```
ProductionPlan (PENDING_APPROVAL)
  ↓
Notification → Director
```

### BƯỚC 5: Director phê duyệt
```
ProductionPlan (APPROVED)
  ↓
ProductionLot (PLAN_APPROVED)
  ↓
Tự động tạo ProductionOrder (PENDING_APPROVAL)
  ↓
Tự động tạo ProductionOrderDetail
  ↓
Notification → Production Manager
```

### BƯỚC 6: PM tạo Work Order
```
ProductionOrder (APPROVED)
  ↓
createWorkOrderFromPlanStages()
  ↓
WorkOrder (DRAFT)
  ↓
WorkOrderDetail (cho mỗi ProductionOrderDetail)
  ↓
ProductionStage (tạo từ ProductionPlanStage)
  - stage_type, stage_sequence
  - assigned_leader_id, qc_assignee_id, machine_id
  - execution_status: WAITING (stage đầu) / PENDING (các stage khác)
```

### BƯỚC 7: PM approve Work Order
```
WorkOrder (APPROVED)
  ↓
ProductionStage (execution_status: WAITING cho stage đầu)
  ↓
Notification → Team Leader (stage đầu)
  ↓
Notification → QC staff
```

### BƯỚC 8: PM bắt đầu Work Order
```
startWorkOrder()
  ↓
ProductionStage (stage đầu: WAITING)
ProductionStage (các stage khác: PENDING)
  ↓
Notification → Team Leaders
Notification → QC staff
```

### BƯỚC 9: Team Leader bắt đầu Stage
```
startStage(stageId)
  ↓
ProductionStage (execution_status: IN_PROGRESS)
  ↓
StageTracking (action: START)
```

### BƯỚC 10: Team Leader cập nhật tiến độ
```
updateProgress(stageId, percent)
  ↓
ProductionStage (progress_percent: 0-100)
  ↓
StageTracking (action: UPDATE_PROGRESS)
```

### BƯỚC 11: Stage đạt 100%
```
ProductionStage (progress_percent: 100)
  ↓
ProductionStage (execution_status: WAITING_QC)
  ↓
Notification → QC staff
```

### BƯỚC 12: QC kiểm tra
```
startQcSession(stageId)
  ↓
QcSession (status: IN_PROGRESS)
  ↓
ProductionStage (execution_status: QC_IN_PROGRESS)
  ↓
QC đánh giá từng QcCheckpoint
  ↓
QcInspection (result: PASS/FAIL)
  ↓
QcPhoto (nếu có lỗi)
```

### BƯỚC 13: QC submit kết quả
```
submitQcSession(sessionId, overallResult)
  ↓
QcSession (overall_result: PASS/FAIL)
  ↓
ProductionStage (execution_status: QC_PASSED hoặc QC_FAILED)
  ↓
Nếu PASS:
  - ProductionStage (stage tiếp theo: WAITING)
  - Notification → Team Leader (stage tiếp theo)
Nếu FAIL:
  - QualityIssue (severity, description)
  - ProductionStage (execution_status: WAITING_REWORK)
  - Notification → Technical
```

### BƯỚC 14: Stage tiếp theo (nếu QC PASS)
```
Lặp lại BƯỚC 9-13 cho stage tiếp theo
```

### BƯỚC 15: Stage cuối cùng (PACKAGING) PASS
```
ProductionStage (PACKAGING, execution_status: QC_PASSED)
  ↓
ProductionOrder (execution_status: COMPLETED)
  ↓
Notification → Warehouse
```

---

## TÓM TẮT CÁC BẢNG THEO CHỨC NĂNG

### 📋 PLANNING
- `contract`
- `quotation_detail`
- `production_lot`
- `production_lot_order`
- `production_plan`
- `production_plan_stage`

### 📦 PRODUCTION ORDER
- `production_order`
- `production_order_detail`

### 🔧 WORK ORDER
- `work_order`
- `work_order_detail`

### ⚙️ EXECUTION
- `production_stage` ⭐ **BẢNG CHÍNH**
- `stage_tracking`
- `stage_pause_log`

### ✅ QUALITY CONTROL
- `qc_checkpoint` (master data)
- `qc_session`
- `qc_inspection`
- `qc_photo`
- `quality_issue`

### 🔔 SUPPORT
- `notification`
- `user`
- `machine`
- `product`
- `bom`

---

## QUAN HỆ CHÍNH GIỮA CÁC BẢNG

```
Contract
  ↓ 1:N
ProductionLotOrder
  ↓ N:1
ProductionLot
  ↓ 1:N
ProductionPlan
  ↓ 1:N
ProductionPlanStage
  ↓ [Khi APPROVED]
ProductionOrder
  ↓ 1:N
ProductionOrderDetail
  ↓ [PM tạo Work Order]
WorkOrder
  ↓ 1:N
WorkOrderDetail
  ↓ 1:N
ProductionStage ⭐ (BẢNG CHÍNH CHO EXECUTION)
  ↓ 1:N
StageTracking
  ↓ 1:N
QcSession
  ↓ 1:N
QcInspection
  ↓ 1:N
QcPhoto
```

---

## LƯU Ý QUAN TRỌNG

1. **ProductionStage** là bảng chính cho việc thực thi sản xuất
2. **execution_status** trong ProductionStage điều khiển workflow
3. **ProductionPlanStage** chỉ dùng cho planning, không dùng cho execution
4. Khi Plan được APPROVED, tự động tạo ProductionOrder
5. Khi PM tạo WorkOrder, ProductionStage được tạo từ ProductionPlanStage
6. QC sử dụng QcCheckpoint (master data) để kiểm tra
7. Mỗi stage có thể có nhiều QcSession (nếu rework)

