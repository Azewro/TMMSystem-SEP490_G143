# HƯỚNG DẪN TÍNH GIÁ THÀNH SẢN PHẨM (PRODUCT COSTING)

Tài liệu này mô tả chi tiết cách hệ thống tính giá thành và hình thành báo giá (Quotation) từ RFQ.

## 1. Mục tiêu
- Chuẩn hoá công thức tính giá cho từng sản phẩm dựa trên: trọng lượng chuẩn, giá nguyên liệu trung bình, chi phí chế biến, lợi nhuận.
- Hiển thị rõ các thành phần cấu thành trong API trả về (`PriceCalculationDto`).

## 2. Dữ liệu nguồn
| Thành phần | Nguồn dữ liệu | Mô tả |
|------------|---------------|------|
| Trọng lượng chuẩn sản phẩm (gram) | `Product.standardWeight` | Dùng để suy ra trọng lượng kg mỗi đơn vị sản phẩm. |
| Giá nguyên liệu trung bình (VND/kg) | Bảng `material` + `material_stock` | Lấy theo mã nguyên liệu (ví dụ "Ne 32/1CD", "Ne 30/1"). Nếu không có tồn kho thì fallback giá chuẩn. |
| Chi phí chế biến (process cost) | Hằng số trong `QuotationService` | 45,000 VND/kg (giá thao tác công đoạn: dệt, nhuộm, cắt, may, đóng gói). |
| Tỷ lệ lợi nhuận (profit margin) | Tham số đầu vào API | Thường > 1 (ví dụ 1.15 nghĩa là markup 15%). |
| Số lượng sản phẩm | `RfqDetail.quantity` | Dùng nhân đơn giá để ra tổng giá. |

## 3. Công thức chi tiết
### 3.1 Quy đổi trọng lượng
```
unitWeightKg = standardWeightGram / 1000
```
Chia 1000 để đổi gram sang kg, làm tròn 6 chữ số thập phân (RoundingMode.HALF_UP).

### 3.2 Giá nguyên liệu trung bình có trọng số
Duyệt tất cả các stock của nguyên liệu:
```
weightedAvgPrice = (Σ (stock.quantity * stock.unitPrice)) / (Σ stock.quantity)
```
Nếu không có stock hoặc không tìm thấy mã nguyên liệu:
```
materialPrice = fallbackPrice
// fallback: Ne 32/1CD = 68,000; Ne 30/1 = 78,155
```

### 3.3 Xác định loại nguyên liệu theo tên sản phẩm
Pseudocode:
```
name = product.name.toLowerCase()
if name chứa cả "cotton" và "bambo" => materialPrice = (avgPriceCotton + avgPriceBamboo) / 2
else if name chứa "bambo" => materialPrice = avgPriceBamboo
else => materialPrice = avgPriceCotton
```

### 3.4 Tính chi phí
```
materialCostPerUnit = unitWeightKg * materialPricePerKg
processCostPerUnit  = unitWeightKg * 45,000
baseCostPerUnit     = materialCostPerUnit + processCostPerUnit
unitPrice           = baseCostPerUnit * profitMargin   // profitMargin > 1
totalPrice          = unitPrice * quantity
```
Làm tròn theo yêu cầu: giá nguyên liệu trung bình làm tròn 2 chữ số; unitWeight 6 chữ số; các phép nhân giữ precision BigDecimal.

### 3.5 Tổng hợp toàn bộ báo giá
```
TotalMaterialCost = Σ(materialCostPerUnit * quantity)
TotalProcessCost  = Σ(processCostPerUnit  * quantity)
TotalBaseCost     = Σ(baseCostPerUnit     * quantity)
FinalTotalPrice   = Σ(totalPrice)
```

## 4. Flow tạo báo giá từ RFQ
1. RFQ phải ở trạng thái `RECEIVED_BY_PLANNING`.
2. Phải có đánh giá năng lực (`capacityStatus` trong RFQ) và đủ điều kiện:
   - `SUFFICIENT` hoặc
   - `INSUFFICIENT` nhưng đã cập nhật `expectedDeliveryDate` = `proposedNewDeliveryDate`.
3. Planning gọi hàm `createQuotationFromRfq(rfqId, planningUserId, profitMargin, notes)`.
4. Hệ thống duyệt từng `RfqDetail`, tính `QuotationDetail` theo công thức trên.
5. Gán `quotation.totalAmount = Σ(detail.totalPrice)` và chuyển RFQ sang trạng thái `QUOTED`.

## 5. Các API liên quan (dự kiến / ví dụ)
| Mục đích | Endpoint | Method | Ghi chú |
|----------|----------|--------|--------|
| Xem trước tính giá với margin | `/v1/quotations/price-preview?rfqId={rfqId}&profitMargin=1.15` | GET/POST | Trả về `PriceCalculationDto`. (Nếu chưa có, có thể bổ sung) |
| Tạo báo giá từ RFQ | `/v1/quotations/create-from-rfq` | POST | Body gồm rfqId, profitMargin, notes. |
| Gửi báo giá cho khách | `/v1/quotations/{id}/send-to-customer` | POST | Chuyển DRAFT → SENT. |
| Khách duyệt báo giá | `/v1/quotations/{id}/approve` | POST | SENT → ACCEPTED + tạo Contract. |
| Khách từ chối báo giá | `/v1/quotations/{id}/reject` | POST | SENT → REJECTED. |

(Điểm API nêu trên phản ánh logic trong `QuotationService`; nếu thiếu có thể cần bổ sung controller.)

## 6. Kiểm thử mẫu
Ví dụ file test `MaterialPriceCalculationTest`:
```
Average = (500000 * 71,400 + 499999 * 64,600) / (999999) ≈ 68,000 VND/kg
```
Cho thấy cách lấy giá trung bình có trọng số.

## 7. Lưu ý & Edge Cases
- Không tìm thấy nguyên liệu hoặc không có tồn kho: dùng fallback giá chuẩn.
- `profitMargin` phải > 1. Nếu người dùng nhập 0.15 cần chuyển đổi sang 1.15 trước khi tính.
- Làm tròn BigDecimal rõ ràng để tránh sai lệch hiển thị (ví dụ trong UI có thể format).
- Sản phẩm có tên hỗn hợp (cotton + bamboo) dùng trung bình cộng hai giá.
- Đảm bảo RFQ chưa bị hủy trước khi tạo báo giá.

## 8. Gợi ý mở rộng
- Thêm phân rã chi phí: điện năng, khấu hao máy, nhân công tách riêng.
- Thêm trường `processCostRatePerKg` cấu hình trong DB thay vì hằng số.
- Lưu snapshot giá nguyên liệu vào `QuotationDetail` để bảo toàn lịch sử.

Kết thúc.

