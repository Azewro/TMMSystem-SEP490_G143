# LUỒNG DỮ LIỆU DATABASE - LẬP KẾ HOẠCH SẢN XUẤT

## SƠ ĐỒ TỔNG QUAN LUỒNG DỮ LIỆU

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         LUỒNG DỮ LIỆU DATABASE                              │
└─────────────────────────────────────────────────────────────────────────────┘

BƯỚC 1: CONTRACT ĐƯỢC DUYỆT
═══════════════════════════════════════════════════════════════════════════════

    ┌──────────────┐
    │   Contract   │ Status: APPROVED
    │   (id: 1)    │ contract_date: 2025-01-15
    │              │ delivery_date: 2025-02-20
    └──────┬───────┘ quotation_id: 10
           │
           │ [1 Contract có nhiều QuotationDetail]
           │
           ▼
    ┌──────────────────────┐
    │  QuotationDetail     │ product_id: 5
    │  (id: 100)           │ quantity: 1000
    │                      │ unit_price: 50000
    └──────────────────────┘
           │
           │ [Hệ thống tự động gọi createOrMergeLotFromContract]
           │
           ▼
    ┌─────────────────────────────────────────────────────────────────────┐
    │  LOGIC MERGE LOT:                                                    │
    │  1. Nhóm QuotationDetail theo product_id                            │
    │  2. Tìm ProductionLot có:                                           │
    │     - product_id = 5                                                │
    │     - delivery_date_target ±1 ngày với delivery_date                │
    │     - contract_date_min/max ±1 ngày với contract_date              │
    │     - status = FORMING hoặc READY_FOR_PLANNING                      │
    │  3. Nếu không tìm thấy → Tạo mới                                    │
    │  4. Nếu tìm thấy → Merge vào Lot hiện có                            │
    └─────────────────────────────────────────────────────────────────────┘
           │
           ▼
    ┌──────────────────────┐
    │   ProductionLot      │ lot_code: "LOT-20250115-001"
    │   (id: 50)           │ product_id: 5
    │                      │ total_quantity: 1000 (hoặc cộng thêm nếu merge)
    │                      │ delivery_date_target: 2025-02-20
    │                      │ contract_date_min: 2025-01-15
    │                      │ contract_date_max: 2025-01-15
    │                      │ status: READY_FOR_PLANNING
    └──────┬───────────────┘
           │
           │ [Tạo ProductionLotOrder để liên kết]
           │
           ▼
    ┌──────────────────────────────┐
    │   ProductionLotOrder         │ lot_id: 50
    │   (id: 200)                   │ contract_id: 1
    │                               │ quotation_detail_id: 100
    │                               │ allocated_quantity: 1000
    └──────────────────────────────┘
           │
           │ [Nếu Contract có nhiều sản phẩm khác nhau]
           │ [Mỗi sản phẩm sẽ tạo 1 ProductionLot riêng]
           │
           ▼
    ┌──────────────────────┐
    │   ProductionLot      │ lot_code: "LOT-20250115-002"
    │   (id: 51)            │ product_id: 7 (sản phẩm khác)
    │                      │ total_quantity: 500
    │                      │ status: READY_FOR_PLANNING
    └──────────────────────┘


BƯỚC 2: PLANNER TẠO PRODUCTION PLAN
═══════════════════════════════════════════════════════════════════════════════

    Planner chọn Lot (id: 50) từ danh sách READY_FOR_PLANNING
           │
           │ [Gọi createPlanFromLot(50)]
           │
           ▼
    ┌─────────────────────────────────────────────────────────────────────┐
    │  LOGIC TẠO PLAN:                                                     │
    │  1. Lock Lot: status → PLANNING                                     │
    │  2. Tìm các Plan cũ của Lot này:                                    │
    │     - Set is_current_version = false                                │
    │     - Set status = SUPERSEDED                                       │
    │  3. Tạo ProductionPlan mới:                                         │
    │     - version_no = max(version_no) + 1                              │
    │     - is_current_version = true                                      │
    │     - status = DRAFT                                                │
    │  4. Tự động tạo 6 ProductionPlanStage                              │
    └─────────────────────────────────────────────────────────────────────┘
           │
           ▼
    ┌──────────────────────┐
    │   ProductionPlan     │ plan_code: "PP-20250115-001"
    │   (id: 100)          │ lot_id: 50
    │                      │ contract_id: 1 (từ ProductionLotOrder đầu tiên)
    │                      │ version_no: 1
    │                      │ is_current_version: true
    │                      │ status: DRAFT
    │                      │ created_by: user_id 10 (Planner)
    └──────┬───────────────┘
           │
           │ [Tự động tạo 6 stages]
           │
           ├──────────────────────────────────────────────────────────────┐
           │                                                              │
           ▼                                                              ▼
    ┌──────────────────────┐                                    ┌──────────────────────┐
    │ ProductionPlanStage  │                                    │ ProductionPlanStage  │
    │ (id: 1001)           │                                    │ (id: 1002)           │
    │ plan_id: 100         │                                    │ plan_id: 100         │
    │ stage_type: WARPING  │                                    │ stage_type: WEAVING  │
    │ sequence_no: 1       │                                    │ sequence_no: 2       │
    │ planned_start_time:  │                                    │ planned_start_time:  │
    │   2025-01-20 08:00   │                                    │   2025-01-20 12:00   │
    │ planned_end_time:    │                                    │ planned_end_time:    │
    │   2025-01-20 12:00   │                                    │   2025-01-20 16:00   │
    │ stage_status: PENDING│                                    │ stage_status: PENDING│
    └──────────────────────┘                                    └──────────────────────┘
           │                                                              │
           │                                                              │
           ▼                                                              ▼
    ┌──────────────────────┐                                    ┌──────────────────────┐
    │ ProductionPlanStage  │                                    │ ProductionPlanStage  │
    │ (id: 1003)           │                                    │ (id: 1004)           │
    │ stage_type: DYEING   │                                    │ stage_type: CUTTING   │
    │ sequence_no: 3       │                                    │ sequence_no: 4        │
    └──────────────────────┘                                    └──────────────────────┘
           │                                                              │
           │                                                              │
           ▼                                                              ▼
    ┌──────────────────────┐                                    ┌──────────────────────┐
    │ ProductionPlanStage  │                                    │ ProductionPlanStage  │
    │ (id: 1005)           │                                    │ (id: 1006)           │
    │ stage_type: HEMMING  │                                    │ stage_type: PACKAGING │
    │ sequence_no: 5       │                                    │ sequence_no: 6        │
    └──────────────────────┘                                    └──────────────────────┘


BƯỚC 3: PLANNER CHỈNH SỬA PLAN
═══════════════════════════════════════════════════════════════════════════════

    Planner cập nhật từng Stage:
           │
           │ [Gọi updateStage(stageId, stageData)]
           │
           ▼
    ┌─────────────────────────────────────────────────────────────────────┐
    │  CẬP NHẬT STAGE:                                                      │
    │  - assigned_machine_id: 25 (Máy dệt số 1)                           │
    │  - in_charge_user_id: 15 (Công nhân A)                               │
    │  - qc_user_id: 20 (QC B)                                             │
    │  - planned_start_time: 2025-01-20 08:00                              │
    │  - planned_end_time: 2025-01-20 14:00                                │
    │  - notes: "Chú ý chất lượng sợi"                                      │
    └─────────────────────────────────────────────────────────────────────┘
           │
           ▼
    ┌──────────────────────┐
    │ ProductionPlanStage  │ plan_id: 100
    │ (id: 1001)           │ stage_type: WARPING
    │                      │ assigned_machine_id: 25 → Machine (id: 25)
    │                      │ in_charge_user_id: 15 → User (id: 15)
    │                      │ qc_user_id: 20 → User (id: 20)
    │                      │ planned_start_time: 2025-01-20 08:00
    │                      │ planned_end_time: 2025-01-20 14:00
    │                      │ stage_status: PENDING
    └──────────────────────┘
           │
           │ [Lặp lại cho các stage khác]
           │
           ▼
    ProductionPlan (id: 100) vẫn ở status: DRAFT


BƯỚC 4: GỬI DUYỆT
═══════════════════════════════════════════════════════════════════════════════

    Planner nhấn "Gửi phê duyệt"
           │
           │ [Gọi submitForApproval(100, notes)]
           │
           ▼
    ┌──────────────────────┐
    │   ProductionPlan     │ plan_code: "PP-20250115-001"
    │   (id: 100)          │ status: DRAFT → PENDING_APPROVAL
    │                      │ approval_notes: "Đã cập nhật chi tiết kế hoạch"
    └──────────────────────┘
           │
           │ [Gửi notification cho Director]


BƯỚC 5: DIRECTOR PHÊ DUYỆT
═══════════════════════════════════════════════════════════════════════════════

    Director phê duyệt
           │
           │ [Gọi approvePlan(100, approvalNotes)]
           │
           ▼
    ┌─────────────────────────────────────────────────────────────────────┐
    │  LOGIC PHÊ DUYỆT:                                                   │
    │  1. Cập nhật ProductionPlan:                                       │
    │     - status: PENDING_APPROVAL → APPROVED                          │
    │     - approved_by: user_id 5 (Director)                           │
    │     - approved_at: 2025-01-16 10:30:00                             │
    │     - approval_notes: "Đồng ý"                                     │
    │  2. Cập nhật ProductionLot:                                        │
    │     - status: PLANNING → PLAN_APPROVED                             │
    │  3. Tự động tạo ProductionOrder                                    │
    └─────────────────────────────────────────────────────────────────────┘
           │
           ▼
    ┌──────────────────────┐
    │   ProductionPlan     │ status: APPROVED
    │   (id: 100)          │ approved_by: 5
    │                      │ approved_at: 2025-01-16 10:30:00
    └──────┬───────────────┘
           │
           │ [Cập nhật Lot]
           │
           ▼
    ┌──────────────────────┐
    │   ProductionLot      │ status: PLAN_APPROVED
    │   (id: 50)           │
    └──────────────────────┘
           │
           │ [Tạo ProductionOrder]
           │
           ▼
    ┌─────────────────────────────────────────────────────────────────────┐
    │  LOGIC TẠO PRODUCTION ORDER:                                        │
    │  1. Tạo ProductionOrder:                                           │
    │     - po_number: "PO-1737000000000"                                │
    │     - contract_id: 1                                               │
    │     - total_quantity: từ Lot.totalQuantity (1000)                  │
    │     - status: PENDING_APPROVAL                                     │
    │     - notes: "Auto-generated from Production Plan: PP-20250115-001"│
    │  2. Tạo ProductionOrderDetail cho mỗi ProductionLotOrder:          │
    │     - Lấy product từ ProductionLotOrder.quotationDetail.product    │
    │     - Lấy quantity từ ProductionLotOrder.allocatedQuantity        │
    │     - Tìm BOM active của product                                    │
    │     - Gán bom_id và bom_version                                     │
    └─────────────────────────────────────────────────────────────────────┘
           │
           ▼
    ┌──────────────────────┐
    │   ProductionOrder    │ po_number: "PO-1737000000000"
    │   (id: 500)          │ contract_id: 1
    │                      │ total_quantity: 1000
    │                      │ status: PENDING_APPROVAL
    │                      │ created_by: 10 (Planner)
    │                      │ planned_start_date: 2025-01-20
    │                      │ planned_end_date: 2025-02-03
    └──────┬───────────────┘
           │
           │ [Tạo ProductionOrderDetail]
           │
           ▼
    ┌──────────────────────────────┐
    │   ProductionOrderDetail      │ production_order_id: 500
    │   (id: 5001)                 │ product_id: 5
    │                              │ bom_id: 30 (từ Product)
    │                              │ bom_version: "v1.0"
    │                              │ quantity: 1000
    │                              │ unit: "UNIT"
    │                              │ note_color: (từ QuotationDetail)
    └──────────────────────────────┘


═══════════════════════════════════════════════════════════════════════════════
                            KẾT THÚC LUỒNG
═══════════════════════════════════════════════════════════════════════════════
```

## CHI TIẾT CÁC BẢNG DATABASE

### 1. CONTRACT → PRODUCTION_LOT

```
Contract Table:
┌────┬─────────────────┬──────────┬──────────────┬───────────────┐
│ id │ contract_number │ status   │ contract_date│ delivery_date │
├────┼─────────────────┼──────────┼──────────────┼───────────────┤
│ 1  │ CON-20250115-001│ APPROVED │ 2025-01-15   │ 2025-02-20    │
└────┴─────────────────┴──────────┴──────────────┴───────────────┘
         │
         │ FK: quotation_id → Quotation
         │
         ▼
QuotationDetail Table:
┌────┬──────────────┬────────────┬──────────┐
│ id │ quotation_id │ product_id │ quantity │
├────┼──────────────┼────────────┼──────────┤
│100 │      10      │     5      │   1000   │
└────┴──────────────┴────────────┴──────────┘
         │
         │ [createOrMergeLotFromContract]
         │
         ▼
ProductionLot Table:
┌────┬──────────────────┬────────────┬───────────────┬───────────────┬──────────┐
│ id │ lot_code         │ product_id │ total_quantity│ delivery_date │ status   │
├────┼──────────────────┼────────────┼───────────────┼───────────────┼──────────┤
│ 50 │ LOT-20250115-001 │     5      │     1000     │  2025-02-20   │READY_FOR_│
│    │                  │            │               │               │PLANNING  │
└────┴──────────────────┴────────────┴───────────────┴───────────────┴──────────┘
         │
         │ [Tạo ProductionLotOrder để liên kết]
         │
         ▼
ProductionLotOrder Table:
┌────┬─────────┬─────────────┬─────────────────────┬──────────────────┐
│ id │ lot_id  │ contract_id │ quotation_detail_id  │ allocated_quantity│
├────┼─────────┼─────────────┼─────────────────────┼──────────────────┤
│200 │   50    │      1      │        100          │       1000       │
└────┴─────────┴─────────────┴─────────────────────┴──────────────────┘
```

### 2. PRODUCTION_LOT → PRODUCTION_PLAN

```
ProductionLot Table:
┌────┬──────────────────┬────────────┬──────────┐
│ id │ lot_code         │ product_id │ status   │
├────┼──────────────────┼────────────┼──────────┤
│ 50 │ LOT-20250115-001 │     5      │READY_FOR_│
│    │                  │            │PLANNING  │
└────┴──────────────────┴────────────┴──────────┘
         │
         │ [createPlanFromLot(50)]
         │ Lock: status → PLANNING
         │
         ▼
ProductionPlan Table:
┌────┬──────────────┬─────────┬──────────┬─────────────┬──────────────┬──────────┐
│ id │ plan_code    │ lot_id │ version_no│is_current_ │ contract_id  │ status   │
│    │              │        │           │ version     │              │          │
├────┼──────────────┼─────────┼──────────┼─────────────┼──────────────┼──────────┤
│100 │PP-20250115-001│  50   │     1     │    true     │      1       │  DRAFT   │
└────┴──────────────┴─────────┴──────────┴─────────────┴──────────────┴──────────┘
         │
         │ [Tự động tạo 6 stages]
         │
         ▼
ProductionPlanStage Table:
┌────┬─────────┬─────────────┬──────────────┬──────────────┬─────────────────────┐
│ id │ plan_id │ stage_type   │ sequence_no │planned_start │ planned_end_time     │
│    │         │              │             │ _time         │                      │
├────┼─────────┼─────────────┼──────────────┼──────────────┼─────────────────────┤
│1001│  100    │ WARPING      │      1      │2025-01-20    │ 2025-01-20 12:00:00 │
│    │         │              │             │ 08:00:00      │                      │
├────┼─────────┼─────────────┼──────────────┼──────────────┼─────────────────────┤
│1002│  100    │ WEAVING      │      2      │2025-01-20    │ 2025-01-20 16:00:00 │
│    │         │              │             │ 12:00:00      │                      │
├────┼─────────┼─────────────┼──────────────┼──────────────┼─────────────────────┤
│1003│  100    │ DYEING       │      3      │2025-01-20    │ 2025-01-21 08:00:00 │
│    │         │              │             │ 16:00:00      │                      │
├────┼─────────┼─────────────┼──────────────┼──────────────┼─────────────────────┤
│1004│  100    │ CUTTING      │      4      │2025-01-21    │ 2025-01-21 12:00:00 │
│    │         │              │             │ 08:00:00      │                      │
├────┼─────────┼─────────────┼──────────────┼──────────────┼─────────────────────┤
│1005│  100    │ HEMMING      │      5      │2025-01-21    │ 2025-01-21 16:00:00 │
│    │         │              │             │ 12:00:00      │                      │
├────┼─────────┼─────────────┼──────────────┼──────────────┼─────────────────────┤
│1006│  100    │ PACKAGING    │      6      │2025-01-21    │ 2025-01-22 08:00:00 │
│    │         │              │             │ 16:00:00      │                      │
└────┴─────────┴─────────────┴──────────────┴──────────────┴─────────────────────┘
```

### 3. CẬP NHẬT STAGE (Planner chỉnh sửa)

```
ProductionPlanStage (id: 1001) - TRƯỚC:
┌────┬─────────┬─────────────┬──────────────────┬──────────────┬──────────────┐
│ id │ plan_id │ stage_type   │assigned_machine_ │in_charge_user│ qc_user_id   │
│    │         │              │ id               │ _id          │              │
├────┼─────────┼─────────────┼──────────────────┼──────────────┼──────────────┤
│1001│  100    │ WARPING      │    NULL          │    NULL     │    NULL      │
└────┴─────────┴─────────────┴──────────────────┴──────────────┴──────────────┘

         │
         │ [updateStage(1001, {assignedMachineId: 25, inChargeUserId: 15, ...})]
         │
         ▼

ProductionPlanStage (id: 1001) - SAU:
┌────┬─────────┬─────────────┬──────────────────┬──────────────┬──────────────┐
│ id │ plan_id │ stage_type   │assigned_machine_ │in_charge_user│ qc_user_id   │
│    │         │              │ id               │ _id          │              │
├────┼─────────┼─────────────┼──────────────────┼──────────────┼──────────────┤
│1001│  100    │ WARPING      │      25          │      15      │      20      │
└────┴─────────┴─────────────┴──────────────────┴──────────────┴──────────────┘
         │                    │                  │              │
         │                    │                  │              │
         ├────────────────────┼──────────────────┼──────────────┤
         │                    │                  │              │
         ▼                    ▼                  ▼              ▼
    Machine(id:25)      User(id:15)        User(id:20)
    "Máy cuộn mắc 01"   "Nguyễn Văn A"     "Trần Thị B (QC)"
```

### 4. PHÊ DUYỆT VÀ TẠO PRODUCTION ORDER

```
ProductionPlan (id: 100) - TRƯỚC:
┌────┬──────────────┬──────────┬─────────────┬──────────────┐
│ id │ plan_code    │ status   │ approved_by │ approved_at   │
├────┼──────────────┼──────────┼─────────────┼──────────────┤
│100 │PP-20250115-001│PENDING_  │    NULL     │    NULL       │
│    │              │APPROVAL  │             │               │
└────┴──────────────┴──────────┴─────────────┴──────────────┘

         │
         │ [approvePlan(100, notes)]
         │
         ▼

ProductionPlan (id: 100) - SAU:
┌────┬──────────────┬──────────┬─────────────┬──────────────────────┐
│ id │ plan_code    │ status   │ approved_by │ approved_at          │
├────┼──────────────┼──────────┼─────────────┼──────────────────────┤
│100 │PP-20250115-001│APPROVED │      5      │ 2025-01-16 10:30:00  │
└────┴──────────────┴──────────┴─────────────┴──────────────────────┘
         │
         │ [Tự động tạo ProductionOrder]
         │
         ▼
ProductionOrder Table:
┌────┬──────────────────┬─────────────┬──────────────┬──────────┐
│ id │ po_number        │ contract_id │total_quantity│ status   │
├────┼──────────────────┼─────────────┼──────────────┼──────────┤
│500 │PO-1737000000000  │      1      │     1000     │PENDING_  │
│    │                  │             │              │APPROVAL  │
└────┴──────────────────┴─────────────┴──────────────┴──────────┘
         │
         │ [Tạo ProductionOrderDetail từ ProductionLotOrder]
         │
         ▼
ProductionOrderDetail Table:
┌────┬──────────────────┬────────────┬──────────┬────────────┐
│ id │production_order_ │ product_id │ bom_id   │ quantity   │
│    │ id               │            │          │            │
├────┼──────────────────┼────────────┼──────────┼────────────┤
│5001│       500        │     5      │    30    │    1000    │
└────┴──────────────────┴────────────┴──────────┴────────────┘
```

## QUAN HỆ GIỮA CÁC BẢNG (ER DIAGRAM ĐƠN GIẢN)

```
┌─────────────┐
│  Contract   │
│  (id: PK)   │
└──────┬──────┘
       │ 1
       │
       │ N
       ▼
┌─────────────────────┐
│ ProductionLotOrder  │──────┐
│ (id: PK)            │      │
│ contract_id: FK     │      │ N
│ lot_id: FK          │      │
│ quotation_detail_id:│      │
│   FK                │      │
└─────────────────────┘      │
       │                     │
       │ N                   │
       │                     │
       ▼                     │
┌─────────────┐              │
│ProductionLot│◄──────────────┘
│ (id: PK)    │
│ product_id: │
│   FK        │
└──────┬──────┘
       │ 1
       │
       │ N
       ▼
┌──────────────────┐
│ ProductionPlan   │
│ (id: PK)         │
│ lot_id: FK       │
│ contract_id: FK  │
└──────┬───────────┘
       │ 1
       │
       │ N
       ▼
┌──────────────────────┐
│ ProductionPlanStage │
│ (id: PK)            │
│ plan_id: FK         │
│ assigned_machine_id:│
│   FK                │
│ in_charge_user_id:  │
│   FK                │
│ qc_user_id: FK      │
└──────────────────────┘
       │
       │ [Khi Plan APPROVED]
       │
       ▼
┌──────────────────┐
│ ProductionOrder  │
│ (id: PK)         │
│ contract_id: FK  │
└──────┬───────────┘
       │ 1
       │
       │ N
       ▼
┌──────────────────────┐
│ProductionOrderDetail │
│ (id: PK)            │
│ production_order_id:│
│   FK                │
│ product_id: FK      │
│ bom_id: FK          │
└──────────────────────┘
```

## TÓM TẮT LUỒNG DỮ LIỆU

1. **Contract APPROVED** 
   → Tạo/merge vào `production_lot`
   → Tạo `production_lot_order` để liên kết

2. **Planner chọn Lot**
   → Tạo `production_plan` (version mới)
   → Tự động tạo 6 `production_plan_stage`

3. **Planner chỉnh sửa**
   → Cập nhật `production_plan_stage` (máy, người, thời gian)

4. **Gửi duyệt**
   → `production_plan.status` = PENDING_APPROVAL

5. **Director duyệt**
   → `production_plan.status` = APPROVED
   → `production_lot.status` = PLAN_APPROVED
   → Tự động tạo `production_order` và `production_order_detail`

