# HƯỚNG DẪN MIGRATION: BỎ WORK ORDER VÀ WORK ORDER DETAIL

## TỔNG QUAN

Migration này sẽ:
1. **Thêm field `production_order_id`** vào bảng `production_stage`
2. **Migrate dữ liệu**: Link ProductionStage trực tiếp với ProductionOrder (không qua WorkOrderDetail)
3. **Sửa code**: Tất cả logic sẽ query ProductionStage theo ProductionOrder thay vì WorkOrderDetail
4. **KHÔNG XÓA BẢNG**: WorkOrder và WorkOrderDetail vẫn tồn tại nhưng không được sử dụng nữa

---

## BƯỚC 1: DEPLOY CODE MỚI

### 1.1. Backup Database
```sql
-- Backup các bảng quan trọng
CREATE TABLE production_stage_backup AS SELECT * FROM production_stage;
CREATE TABLE work_order_backup AS SELECT * FROM work_order;
CREATE TABLE work_order_detail_backup AS SELECT * FROM work_order_detail;
```

### 1.2. Deploy Code
- Deploy code mới có:
  - `ProductionStage` entity với field `productionOrder` (nullable)
  - `WorkOrderMigrationService`
  - `MigrationController`
  - Các service đã được sửa để hỗ trợ cả 2 cách (backward compatible)

### 1.3. Kiểm tra Database Schema
Sau khi deploy, kiểm tra xem column `production_order_id` đã được thêm vào bảng `production_stage` chưa:
```sql
DESCRIBE production_stage;
-- Hoặc
SHOW COLUMNS FROM production_stage LIKE 'production_order_id';
```

Nếu chưa có, cần thêm thủ công:
```sql
ALTER TABLE production_stage 
ADD COLUMN production_order_id BIGINT NULL,
ADD INDEX idx_stage_po_sequence (production_order_id, stage_sequence), -- KHÔNG unique vì một PO có thể có nhiều sets stages
ADD FOREIGN KEY (production_order_id) REFERENCES production_order(id);
```

**QUAN TRỌNG**: Index `idx_stage_po_sequence` KHÔNG được là UNIQUE vì:
- Một ProductionOrder có thể có nhiều ProductionOrderDetail
- Mỗi ProductionOrderDetail tạo ra các stages với cùng sequence (1-6)
- Khi migrate, tất cả stages này đều link với cùng ProductionOrder → duplicate (production_order_id, stage_sequence) là hợp lệ

Nếu index đã được tạo với UNIQUE, cần sửa lại:
```sql
-- Xóa index cũ nếu có unique
ALTER TABLE production_stage DROP INDEX idx_stage_po_sequence;

-- Tạo lại index không unique
CREATE INDEX idx_stage_po_sequence ON production_stage(production_order_id, stage_sequence);
```

---

## BƯỚC 2: CHẠY MIGRATION

### 2.1. Chạy Migration Service

**Cách 1: Qua API (Khuyến nghị)**
```bash
# Chạy migration
curl -X POST http://localhost:8080/v1/migration/work-order-to-production-order

# Verify migration
curl -X GET http://localhost:8080/v1/migration/verify
```

**Cách 2: Qua Code (Nếu cần)**
```java
@Autowired
private WorkOrderMigrationService migrationService;

// Chạy migration
migrationService.migrateProductionStagesToProductionOrder();

// Verify
migrationService.verifyMigration();
```

### 2.2. Kiểm tra Kết quả

Sau khi chạy migration, kiểm tra:
```sql
-- Kiểm tra số lượng stages đã được migrate
SELECT 
    COUNT(*) as total_stages,
    COUNT(production_order_id) as migrated_stages,
    COUNT(work_order_detail_id) as old_linked_stages
FROM production_stage;

-- Kiểm tra stages chưa được migrate (nếu có)
SELECT id, stage_type, stage_sequence, work_order_detail_id, production_order_id
FROM production_stage
WHERE production_order_id IS NULL AND work_order_detail_id IS NOT NULL;
```

---

## BƯỚC 3: VERIFY VÀ TEST

### 3.1. Verify Migration
```bash
curl -X GET http://localhost:8080/v1/migration/verify
```

Kiểm tra console/log để xem:
- Tổng số stages
- Số stages đã migrate
- Số stages còn link với WorkOrderDetail (có thể giữ lại để backup)

### 3.2. Test API Endpoints

Test các API liên quan đến ProductionStage:
```bash
# Lấy stages của một ProductionOrder
GET /v1/production-orders/{orderId}/stages

# Bắt đầu work order
POST /v1/production-orders/{orderId}/start-work-order

# Lấy stage detail
GET /v1/production-stages/{stageId}
```

### 3.3. Test Workflow

Test toàn bộ workflow:
1. Production Manager: Bắt đầu lệnh làm việc
2. Team Leader: Bắt đầu stage, cập nhật tiến độ
3. QC: Kiểm tra stage
4. Verify notifications được gửi đúng

---

## BƯỚC 4: CLEANUP (TÙY CHỌN)

Sau khi verify mọi thứ hoạt động tốt, có thể:

### 4.1. Đánh dấu WorkOrder/WorkOrderDetail là deprecated
- Thêm comment vào code
- Thêm annotation `@Deprecated` vào các methods liên quan

### 4.2. Xóa Foreign Key Constraints (Nếu muốn)
```sql
-- Xóa foreign key từ production_stage đến work_order_detail
ALTER TABLE production_stage 
DROP FOREIGN KEY fk_production_stage_work_order_detail;

-- Xóa index cũ
ALTER TABLE production_stage 
DROP INDEX idx_stage_wodetail_sequence;
```

**LƯU Ý**: Không xóa bảng `work_order` và `work_order_detail` để giữ lại dữ liệu backup.

---

## ROLLBACK PLAN (Nếu cần)

Nếu migration gặp vấn đề:

### 1. Restore từ Backup
```sql
-- Restore production_stage
DROP TABLE production_stage;
CREATE TABLE production_stage AS SELECT * FROM production_stage_backup;
```

### 2. Revert Code
- Deploy lại code cũ
- Code mới có backward compatibility nên vẫn hoạt động với WorkOrderDetail

### 3. Verify
```sql
-- Kiểm tra dữ liệu đã được restore
SELECT COUNT(*) FROM production_stage;
```

---

## CÁC THAY ĐỔI CHÍNH

### Entity Changes
- `ProductionStage`: Thêm field `productionOrder` (nullable)
- Giữ lại field `workOrderDetail` (nullable) để backward compatibility

### Repository Changes
- `ProductionStageRepository`: 
  - Thêm `findByProductionOrderIdOrderByStageSequenceAsc()`
  - Sửa `findStagesByOrderId()` để query trực tiếp theo ProductionOrder
  - Giữ lại `findByWorkOrderDetailIdOrderByStageSequenceAsc()` (deprecated)

### Service Changes
- `ProductionService`:
  - `startWorkOrder()`: Query trực tiếp theo ProductionOrder
  - `createStagesFromPlan()`: Tạo stages trực tiếp với ProductionOrder
  - Các methods khác: Hỗ trợ cả 2 cách (fallback nếu chưa migrate)

- `ExecutionOrchestrationService`:
  - `openNextStage()`: Query trực tiếp theo ProductionOrder
  - Các methods khác: Hỗ trợ cả 2 cách

- `QcService`:
  - Sửa để lấy ProductionOrder trực tiếp từ ProductionStage

---

## LƯU Ý QUAN TRỌNG

1. **Migration chỉ chạy 1 lần**: Sau khi migrate xong, không cần chạy lại
2. **Backward Compatibility**: Code mới vẫn hoạt động với dữ liệu cũ (chưa migrate)
3. **Không xóa bảng**: WorkOrder và WorkOrderDetail vẫn tồn tại để backup
4. **Test kỹ**: Test toàn bộ workflow trước khi deploy production
5. **Monitor**: Theo dõi logs và errors sau khi deploy

---

## TROUBLESHOOTING

### Lỗi: "Column 'production_order_id' does not exist"
**Giải pháp**: Thêm column thủ công (xem Bước 1.3)

### Lỗi: "Foreign key constraint fails"
**Giải pháp**: Kiểm tra xem ProductionOrder có tồn tại không:
```sql
SELECT DISTINCT wod.work_order_id, wo.production_order_id
FROM work_order_detail wod
JOIN work_order wo ON wod.work_order_id = wo.id
WHERE wo.production_order_id IS NULL;
```

### Migration chạy nhưng không có stages nào được migrate
**Giải pháp**: 
- Kiểm tra xem có ProductionStage nào có WorkOrderDetail không
- Kiểm tra logs để xem lỗi cụ thể

---

## LIÊN HỆ

Nếu gặp vấn đề, liên hệ team development hoặc xem logs tại:
- Application logs: `logs/application.log`
- Migration logs: Console output khi chạy migration

