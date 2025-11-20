# HƯỚNG DẪN TÍNH NĂNG LỰC SẢN XUẤT (PRODUCTION CAPACITY)

Tài liệu này ghi rõ công thức, dữ liệu đầu vào, luồng và API kiểm tra năng lực theo mô hình tuần tự đã được implement trong `CapacityCheckService`. Kể từ bản cập nhật 2025-11-20, cùng mô hình này cũng được dùng để tự động sinh timeline cho màn Lập Kế Hoạch. Các stage được phân bổ theo **giờ hành chính** (08:00–17:00, tối đa 8h/ngày) và có thời gian chờ cố định giữa các công đoạn như mô tả dưới đây.

## 1. Tổng quan mô hình
Quy trình tuần tự 5 công đoạn:
1) Mắc cuồng (WARPING) → 2) Dệt vải (WEAVING) → 3) Nhuộm (DYEING, vendor) → 4) Cắt (CUTTING) → 5) May (SEWING)

Có thời gian chờ giữa các công đoạn:
- Mắc → Dệt: 0.5 ngày
- Dệt → Nhuộm: 0.5 ngày
- Nhuộm → Cắt: 1.0 ngày
- Cắt → May: 0.2 ngày
- Sau May: 0.3 ngày
- Vendor nhuộm cố định: 2.0 ngày

## 2. Dữ liệu nguồn
| Dữ liệu | Nguồn | Ghi chú |
|--------|-------|--------|
| RFQ + RFQ Details | `Rfq`, `RfqDetail` | Từ `rfqId`, lấy danh sách sản phẩm và số lượng.
| Trọng lượng sản phẩm | `Product.standardWeight (gram)` | Đổi sang kg: chia 1000.
| Máy móc & công suất | `Machine`, field `type`, `status`, `specifications` | - WARPING/WEAVING dùng capacityPerDay (kg/ngày) trong JSON.
| | | - CUTTING/SEWING dùng capacityPerHour theo từng loại sản phẩm trong JSON; quy đổi ngày = × 8 giờ.
| Lịch sản xuất | `ProductionStage`, `WorkOrder` | Kiểm tra xung đột theo ngày và loại máy.
| Bảo trì máy | `MachineMaintenance` | Trừ công suất máy đang bảo trì theo ngày.

## 3. Công thức
### 3.1 Tổng khối lượng và số lượng
```
unitWeightKg = product.standardWeight / 1000
totalWeightKg = Σ (unitWeightKg * quantity)
```
Phân loại số lượng theo tên sản phẩm ("khăn mặt", "khăn tắm", "khăn thể thao") để dùng cho CUTTING/SEWING.

### 3.2 Công suất máy
- WARPING/WEAVING:
```
capacity(type=WARPING|WEAVING) = Σ capacityPerDay của các máy AVAILABLE đúng loại
// lấy từ JSON `{"capacityPerDay": 200}` trong Machine.specifications
```
- CUTTING/SEWING:
```
capacityPerDay = avg(capacityPerHour.faceTowels, bathTowels, sportsTowels) * 8h
// cho tổng công suất; riêng khi tính ngày theo từng loại sẽ lấy đúng capacity của loại đó * 8h
```

### 3.3 Thời gian xử lý từng công đoạn
```
warpingDays = totalWeightKg / warpingCapacityKgPerDay
weavingDays = totalWeightKg / weavingCapacityKgPerDay
dyeingDays  = 2.0 (vendor cố định)

cuttingDays = max(
  faceTowels / totalFaceCapacityPerDay,
  bathTowels / totalBathCapacityPerDay,
  sportsTowels / totalSportsCapacityPerDay
)

sewingDays  = max(
  faceTowels / totalFaceCapacityPerDay,
  bathTowels / totalBathCapacityPerDay,
  sportsTowels / totalSportsCapacityPerDay
)

// RoundingMode.HALF_UP với scale phù hợp (2).
```

### 3.4 Tổng thời gian tuần tự + chờ
```
sequentialDays = warpingDays + weavingDays + dyeingDays + cuttingDays + sewingDays
waitTimeDays   = 0.5 + 0.5 + 1.0 + 0.2 + 0.3
TOTAL_DAYS     = sequentialDays + waitTimeDays
```

### 3.5 Bottleneck
```
maxProcessTime = max(warpingDays, weavingDays, dyeingDays, cuttingDays, sewingDays)
```
Đặt tên công đoạn có giá trị lớn nhất làm bottleneck.

## 4. Chọn khung thời gian sản xuất
- productionStartDate = `rfq.createdAt` + 7 ngày
- productionEndDate   = `rfq.expectedDeliveryDate` − 7 ngày
- availableDays       = số ngày giữa start và end
- Kết luận đủ/thiếu: `TOTAL_DAYS <= availableDays` và không có conflicts.

## 5. Kiểm tra xung đột theo lịch thực tế
Duyệt từng công đoạn theo dải ngày tương ứng, mỗi ngày:
1) Lấy tổng công suất loại máy (`getMachineCapacity(stageType)`).
2) Tính usedCapacity:
   - Cộng công suất các `ProductionStage` đang chạy ngày đó (máy chưa released).
   - Cộng công suất bị mất do bảo trì (`MachineMaintenance`).
3) available = totalCapacity − usedCapacity. Nếu available < totalCapacity cần cho "đơn mới" trong ngày đó, đánh dấu conflict.

Trả về `CapacityCheckResultDto.ConflictDto` gồm: date, machineType, required, available, used, conflictMessage.

## 6. Phân bổ lịch tuần tự (stage timeline)
Hệ thống sinh `SequentialStageDto` cho 5 công đoạn, gồm:
- stageName, stageType
- processingDays, waitTime
- startDate, endDate (tính nối tiếp nhau theo waitTime)
- capacity và description
- totalWaitTime được tổng hợp vào kết quả.

## 7. API và dòng chảy
- Kiểm tra máy móc: `POST /v1/rfqs/{id}/check-machine-capacity` → trả `CapacityCheckResultDto`.
- Kiểm tra kho: `POST /v1/rfqs/{id}/check-warehouse-capacity` (luôn đủ theo giả định).
- Ghi kết quả đánh giá: `POST /v1/rfqs/{id}/capacity-evaluate?status=SUFFICIENT|INSUFFICIENT&reason=...&proposedNewDate=...`.
- Sau khi đủ năng lực: tạo báo giá (xem COSTING_GUIDE.md) rồi gửi cho khách.

## 8. Ví dụ tham khảo
Xem các test:
- `CapacityCheckTest` và `SequentialCapacityApiTest` mô tả cụ thể input và cách tính, kết quả in ra.

## 9. Lưu ý & Edge cases
- Không có máy AVAILABLE của 1 loại: thời gian công đoạn đó tính 0 (cần xử lý UI cảnh báo thiếu máy).
- Công suất CUTTING/SEWING đọc từ JSON trong `Machine.specifications`; thiếu key sẽ coi như 0.
- Lịch xung đột dày đặc: API trả `mergeSuggestion` gợi ý dời lịch hoặc gộp đơn.
- Vendor nhuộm có công suất "vô hạn" nhưng thời gian cố định 2 ngày; năng lực chỗ này không phải bottleneck do capacity.

Kết thúc.

