# Luồng sản xuất khăn bông (Happy Case) 

## 1. Khởi tạo & Phân công
1. PM mở danh sách Production Order (PO) ở trạng thái DRAFT / APPROVED và chọn PO cần triển khai.
2. PM phân công kỹ thuật viên: `POST /v1/production/orders/{id}/assign-technician?technicianUserId=...`
   - Hệ thống cập nhật `assignedTechnicianId`, `assignedAt` trên PO.
   - Gửi notification INFO category=PRODUCTION tới kỹ thuật viên.
3. Kỹ thuật viên review PO & chi tiết, tạo Work Order chuẩn: `POST /v1/production/orders/{poId}/work-orders/create-standard`
   - Sinh Work Order (WO) với 6 công đoạn mặc định: Warping → Weaving → Dyeing (outsourced) → Cutting → Hemming → Packaging.
   - Mỗi ProductionStage ở trạng thái PENDING.

## 2. Phân công Leader & QC
1. PM/Kỹ thuật bulk phân công leader cho từng stage (nếu đã biết): `POST /v1/production/work-orders/{woId}/assign-leaders` body: `{ "WARPING": leaderId1, "WEAVING": leaderId2, ... }`
2. Tuỳ chọn: phân công QC riêng cho stage: `POST /v1/production/stages/{stageId}/assign-kcs?kcsUserId=...` (nếu không dùng thì QC STAFF sẽ nhận chung).

## 3. Phê duyệt Work Order
1. Kỹ thuật viên gửi WO để PM duyệt: `POST /v1/production/work-orders/{id}/submit-approval` → status=PENDING_APPROVAL.
2. PM duyệt: `POST /v1/production/work-orders/{id}/approve?pmId=...`
   - Sinh QR token cho từng stage (nếu chưa có).
   - Gửi notification tới PRODUCTION_STAFF + nếu Warping stage có leader → gửi riêng SUCCESS cho leader Warping: "Công đoạn đầu sẵn sàng".
3. Nếu PM từ chối: `POST /v1/production/work-orders/{id}/reject?pmId=...&reason=...` → status=REJECTED + notification WARNING.

## 4. Thực thi chuỗi công đoạn
### 4.1 Warping
1. Leader Warping thấy stage ở danh sách leader: `GET /v1/production/stages/for-leader?userId=...`.
2. Bắt đầu: `POST /v1/production/stages/{id}/start?leaderUserId=...` → status=IN_PROGRESS.
3. Hoàn thành: `POST /v1/production/stages/{id}/complete?leaderUserId=...` → status=COMPLETED.
4. Hệ thống gửi notification cho QC (qcAssignee hoặc QC_STAFF role): "Chờ kiểm tra QC".
5. QC kiểm tra: tạo inspection PASS `POST /v1/qc/inspections` (result=PASS). Hệ thống:
   - cập nhật qcLastResult=PASS, qcLastCheckedAt.
   - tìm stage kế tiếp (Weaving) → notify leader Weaving: "Có thể bắt đầu Weaving".

### 4.2 Weaving
- Tương tự Warping. PASS → nếu stage DYEING kế tiếp:
  - Notify PRODUCTION_MANAGER: "Chuẩn bị Dyeing" (điều phối vendor).

### 4.3 Dyeing (outsourced)
1. PM/leader Start Dyeing: `POST /v1/production/stages/{id}/start?leaderUserId=...`.
   - Nếu `is_outsourced=true`, hệ thống tự tạo OutsourcingTask (status=SENT, sentAt=now, vendorName dùng outsourceVendor).
2. Nhận hàng về & hoàn thành: `POST /v1/production/stages/{id}/complete?leaderUserId=...`.
3. QC tạo inspection PASS → notify leader Cutting.

### 4.4 Cutting / 4.5 Hemming
- Mỗi công đoạn: start → complete → QC PASS → notify leader công đoạn kế tiếp.

### 4.6 Packaging
1. Leader start/complete.
2. QC PASS Packaging → hệ thống kiểm tra tất cả stages của mọi WorkOrderDetail thuộc PO đều PASS.
3. Nếu thỏa mãn:
   - Update PO.status = ORDER_COMPLETED.
   - Notify các role: SALE_STAFF, PLANNING_STAFF, DIRECTOR, TECHNICAL_STAFF, PRODUCTION_MANAGER.
   - Notify INVENTORY_STAFF: "Đã sẵn sàng nhập kho".

## 5. Các endpoint mới & mở rộng
- Phân công kỹ thuật viên PO: `POST /v1/production/orders/{id}/assign-technician` (param: technicianUserId)
- Phân công leader cho stage: `POST /v1/production/stages/{id}/assign-leader` (param: leaderUserId)
- Bulk phân công leaders: `POST /v1/production/work-orders/{woId}/assign-leaders` (body map stageType→userId)
- Phân công QC: `POST /v1/production/stages/{id}/assign-kcs` (param: kcsUserId)

## 6. Dữ liệu & fields mới
- `production_order`: assigned_technician_id, assigned_at
- `production_stage`: qc_assignee_id
- DTO mở rộng: ProductionOrderDto (assignedTechnicianId, assignedAt), ProductionStageDto (qcAssigneeId)
- Mapper cập nhật tương ứng.

## 7. Notifications chính
| Sự kiện | Người nhận | Loại |
|---------|------------|------|
| Assign technician | Technician | INFO |
| Assign leader | Leader | INFO |
| Assign QC | QC user | INFO |
| WO approved | Production Staff + Leader Warping | SUCCESS/INFO |
| Stage completed | QC assignee hoặc QC_STAFF | INFO |
| QC PASS (chuyển tiếp) | Leader kế / PM (cho Dyeing) | SUCCESS/INFO |
| Packaging PASS toàn bộ | Sales, Planning, Director, Technical, PM | SUCCESS |
| Packaging PASS toàn bộ | Inventory Staff | SUCCESS |

## 8. Dyeing OutsourcingTask hook
- Tự động tạo khi start Dyeing nếu chưa có.
- Chứa vendorName = outsourceVendor.
- Có thể cập nhật sau bằng API `/v1/execution/outsourcing` nếu cần thêm weightReturned, chứng từ.

## 9. Bảo vệ & phân quyền
- Leader thao tác start/pause/resume/complete → kiểm tra `assignedLeader` khớp userId.
- QC thao tác inspection không bị giới hạn ở đây (có thể thêm bộ lọc role QC ở layer bảo mật).

## 10. Mở rộng tương lai (Gợi ý)
- Thêm rejectionReason riêng cho WorkOrder.
- Thêm audit bảng ApprovalHistory.
- Tối ưu truy vấn tổng hợp hoàn tất PO (repo chuyên biệt thay vì filter toàn bộ wod).

Kết thúc: File này mô tả Happy Case end-to-end dựa trên các thay đổi đã triển khai.

