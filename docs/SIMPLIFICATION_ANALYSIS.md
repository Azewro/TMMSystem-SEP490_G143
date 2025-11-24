# PHÂN TÍCH ĐỀ XUẤT ĐƠN GIẢN HÓA CÁC BẢNG

## HIỆN TRẠNG

### Luồng hiện tại:
```
ProductionPlan (APPROVED)
  ↓
ProductionOrder (tự động tạo)
  ↓
ProductionOrderDetail (sản phẩm, BOM, số lượng)
  ↓
WorkOrder (PM tạo)
  ↓
WorkOrderDetail (liên kết với ProductionOrderDetail)
  ↓
ProductionStage (thực thi)
```

### Các bảng đang dùng:
1. **ProductionOrder** - Đơn hàng sản xuất
2. **ProductionOrderDetail** - Chi tiết sản phẩm trong PO
3. **WorkOrder** - Lệnh sản xuất
4. **WorkOrderDetail** - Chi tiết lệnh sản xuất
5. **BOM** - Bill of Materials (lock version)

---

## PHÂN TÍCH TỪNG BẢNG

### 1. **BOM (Bill of Materials)**

#### ✅ **CẦN THIẾT** - Nếu hệ thống cần:
- Tính toán nguyên vật liệu cần thiết
- Dự trữ nguyên vật liệu
- Theo dõi consumption
- Lock BOM version để đảm bảo consistency

#### ❌ **KHÔNG CẦN THIẾT** - Nếu:
- Không cần tính toán nguyên vật liệu tự động
- Không cần lock BOM version
- Chỉ cần thông tin sản phẩm đơn giản

#### 📊 **Sử dụng hiện tại:**
- `MaterialConsumptionService` dùng BOM để tính toán nguyên vật liệu
- `ProductionOrderDetail` lock BOM version khi tạo PO
- Có thể thay thế bằng cách lưu BOM version trong ProductionPlan hoặc ProductionStage

---

### 2. **ProductionOrder**

#### ✅ **CẦN THIẾT** - Vì:
- **Quản lý đơn hàng**: PM cần xem danh sách orders
- **Phê duyệt**: Director approve/reject orders
- **Tracking**: Theo dõi execution status của toàn bộ đơn hàng
- **Liên kết với Contract**: Một Contract có thể có nhiều ProductionOrder
- **Thông báo**: Notify PM khi Plan được approve

#### ⚠️ **CÓ THỂ ĐƠN GIẢN HÓA** - Bằng cách:
- Gộp vào ProductionPlan (thêm status: APPROVED → IN_PRODUCTION)
- Nhưng sẽ mất khả năng một Plan có nhiều ProductionOrder

#### 📊 **Sử dụng hiện tại:**
- `getManagerOrders()` - PM xem danh sách
- `approvePO()` / `rejectPO()` - Director phê duyệt
- `startWorkOrder()` - PM bắt đầu sản xuất
- `enrichProductionOrderDto()` - Frontend hiển thị

---

### 3. **ProductionOrderDetail**

#### ✅ **CẦN THIẾT** - Vì:
- Một ProductionOrder có thể có nhiều sản phẩm khác nhau
- Mỗi sản phẩm có BOM riêng, số lượng riêng
- Lock BOM version cho từng sản phẩm

#### ⚠️ **CÓ THỂ ĐƠN GIẢN HÓA** - Nếu:
- Một ProductionOrder chỉ có 1 sản phẩm → có thể gộp vào ProductionOrder
- Không cần lock BOM → có thể lấy từ Product.activeBom

#### 📊 **Sử dụng hiện tại:**
- Lưu `product_id`, `bom_id`, `bom_version`, `quantity`
- Tạo WorkOrderDetail từ ProductionOrderDetail
- Frontend hiển thị chi tiết sản phẩm

---

### 4. **WorkOrder**

#### ❌ **KHÔNG CẦN THIẾT** - Vì:
- Chỉ là lớp trung gian giữa ProductionOrder và ProductionStage
- Không có logic nghiệp vụ riêng
- Một ProductionOrder chỉ có 1 WorkOrder (1:1 relationship)
- Có thể thay thế bằng:
  - Thêm field `work_order_number` vào ProductionOrder
  - Hoặc tạo ProductionStage trực tiếp từ ProductionOrder

#### 📊 **Sử dụng hiện tại:**
- `wo_number`: có thể là `po_number` hoặc `plan_code`
- `status`: DRAFT → APPROVED (có thể là status của ProductionOrder)
- `created_by`: có thể lưu trong ProductionOrder
- `approved_by`: có thể lưu trong ProductionOrder

#### 💡 **Đề xuất:**
- **Loại bỏ WorkOrder**
- Thêm vào ProductionOrder:
  - `work_order_number` (nếu cần)
  - `work_status` (DRAFT, APPROVED, IN_PROGRESS, COMPLETED)
  - `work_created_by`, `work_approved_by`

---

### 5. **WorkOrderDetail**

#### ❌ **KHÔNG CẦN THIẾT** - Vì:
- Chỉ liên kết WorkOrder với ProductionOrderDetail
- Một WorkOrder chỉ có 1 WorkOrderDetail (1:1 với ProductionOrderDetail)
- Không có logic nghiệp vụ riêng
- Có thể thay thế bằng:
  - ProductionStage trực tiếp link với ProductionOrderDetail
  - Hoặc ProductionStage link với ProductionOrder (nếu chỉ có 1 sản phẩm)

#### 📊 **Sử dụng hiện tại:**
- `stage_sequence`: có thể lưu trong ProductionStage
- `planned_start_at`, `planned_end_at`: có thể lưu trong ProductionStage
- `work_status`: có thể là `execution_status` của ProductionStage

#### 💡 **Đề xuất:**
- **Loại bỏ WorkOrderDetail**
- ProductionStage trực tiếp link với ProductionOrderDetail:
  ```java
  @ManyToOne
  private ProductionOrderDetail productionOrderDetail;
  ```

---

## ĐỀ XUẤT ĐƠN GIẢN HÓA

### Option 1: Loại bỏ WorkOrder và WorkOrderDetail (Đề xuất)

#### Luồng mới:
```
ProductionPlan (APPROVED)
  ↓
ProductionOrder (tự động tạo)
  ↓
ProductionOrderDetail (sản phẩm, BOM, số lượng)
  ↓
ProductionStage (tạo trực tiếp từ ProductionPlanStage)
```

#### Thay đổi:
1. **Loại bỏ**: `WorkOrder`, `WorkOrderDetail`
2. **ProductionStage** thay đổi:
   ```java
   // Thay vì:
   @ManyToOne
   private WorkOrderDetail workOrderDetail;
   
   // Thành:
   @ManyToOne
   private ProductionOrderDetail productionOrderDetail;
   ```
3. **ProductionOrder** thêm fields:
   ```java
   @Column(name = "work_status")
   private String workStatus; // DRAFT, APPROVED, IN_PROGRESS, COMPLETED
   
   @ManyToOne
   @JoinColumn(name = "work_created_by")
   private User workCreatedBy;
   
   @ManyToOne
   @JoinColumn(name = "work_approved_by")
   private User workApprovedBy;
   ```

#### Lợi ích:
- ✅ Giảm 2 bảng không cần thiết
- ✅ Đơn giản hóa luồng dữ liệu
- ✅ Giảm số lượng JOIN khi query
- ✅ Dễ hiểu hơn cho developers

#### Nhược điểm:
- ⚠️ Cần refactor code (nhưng không nhiều)
- ⚠️ Cần migration data (nếu có data cũ)

---

### Option 2: Loại bỏ ProductionOrder (Không khuyến nghị)

#### Luồng mới:
```
ProductionPlan (APPROVED)
  ↓
ProductionStage (tạo trực tiếp từ ProductionPlanStage)
```

#### Thay đổi:
1. **Loại bỏ**: `ProductionOrder`, `ProductionOrderDetail`
2. **ProductionStage** link trực tiếp với ProductionPlan:
   ```java
   @ManyToOne
   private ProductionPlan productionPlan;
   ```

#### Nhược điểm:
- ❌ Mất khả năng quản lý đơn hàng độc lập
- ❌ Một Plan chỉ có thể có 1 lần sản xuất
- ❌ Khó track execution status của đơn hàng
- ❌ Mất khả năng Director approve/reject orders

---

### Option 3: Giữ nguyên (Không khuyến nghị)

#### Lý do:
- Nếu hệ thống cần:
  - Một ProductionOrder có nhiều WorkOrder (batch production)
  - Một WorkOrder có nhiều WorkOrderDetail (multiple products)
  - Tracking riêng biệt giữa Order và Work Order

#### Nhưng hiện tại:
- Một ProductionOrder chỉ có 1 WorkOrder (1:1)
- Một WorkOrder chỉ có 1 WorkOrderDetail (1:1 với ProductionOrderDetail)
- → Không cần thiết

---

## KẾT LUẬN VÀ KHUYẾN NGHỊ

### ✅ **NÊN LOẠI BỎ:**
1. **WorkOrder** - Lớp trung gian không cần thiết
2. **WorkOrderDetail** - Lớp trung gian không cần thiết

### ✅ **NÊN GIỮ LẠI:**
1. **ProductionOrder** - Cần cho quản lý đơn hàng
2. **ProductionOrderDetail** - Cần cho nhiều sản phẩm trong 1 order
3. **BOM** - Cần nếu hệ thống tính toán nguyên vật liệu

### 📋 **KẾ HOẠCH REFACTOR:**

#### Bước 1: Thêm fields vào ProductionOrder
```java
@Column(name = "work_status")
private String workStatus = "DRAFT";

@ManyToOne
@JoinColumn(name = "work_created_by")
private User workCreatedBy;

@ManyToOne
@JoinColumn(name = "work_approved_by")
private User workApprovedBy;
```

#### Bước 2: Thay đổi ProductionStage
```java
// Xóa:
@ManyToOne
private WorkOrderDetail workOrderDetail;

// Thêm:
@ManyToOne
private ProductionOrderDetail productionOrderDetail;
```

#### Bước 3: Refactor code
- `createWorkOrderFromPlanStages()` → `createStagesFromPlan()`
- `startWorkOrder()` → `startProductionOrder()`
- `approveWorkOrder()` → `approveProductionOrder()`
- Query: `findStagesByOrderId()` thay vì `findStagesByWorkOrderDetailId()`

#### Bước 4: Migration data (nếu có)
```sql
-- Copy work_status từ WorkOrder vào ProductionOrder
UPDATE production_order po
SET work_status = wo.status
FROM work_order wo
WHERE wo.production_order_id = po.id;

-- Update ProductionStage.work_order_detail_id → production_order_detail_id
UPDATE production_stage ps
SET production_order_detail_id = wod.production_order_detail_id
FROM work_order_detail wod
WHERE ps.work_order_detail_id = wod.id;
```

#### Bước 5: Xóa bảng
```sql
DROP TABLE work_order_detail;
DROP TABLE work_order;
```

---

## TÁC ĐỘNG ĐẾN FRONTEND

### Thay đổi API:
- `GET /v1/production/orders/{id}` - Không đổi
- `POST /v1/production/orders/{id}/start` - Thay vì `/work-orders/{id}/start`
- `GET /v1/production/stages?orderId={id}` - Thay vì `?workOrderId={id}`

### Thay đổi DTO:
- `ProductionOrderDto` thêm `workStatus`, `workCreatedBy`, `workApprovedBy`
- `ProductionStageDto` thay `workOrderDetailId` → `productionOrderDetailId`

---

## TỔNG KẾT

| Bảng | Cần thiết? | Lý do |
|------|-----------|-------|
| **BOM** | ✅ Có | Nếu cần tính toán nguyên vật liệu |
| **ProductionOrder** | ✅ Có | Quản lý đơn hàng, phê duyệt |
| **ProductionOrderDetail** | ✅ Có | Nhiều sản phẩm trong 1 order |
| **WorkOrder** | ❌ Không | Lớp trung gian 1:1 với ProductionOrder |
| **WorkOrderDetail** | ❌ Không | Lớp trung gian 1:1 với ProductionOrderDetail |

### Đề xuất cuối cùng:
**Loại bỏ WorkOrder và WorkOrderDetail**, giữ lại ProductionOrder, ProductionOrderDetail, và BOM.

