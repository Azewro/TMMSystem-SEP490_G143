# 📊 **VÍ DỤ TÍNH TOÁN NGUYÊN VẬT LIỆU VỚI HAO HỤT**

## 🎯 **VÍ DỤ CỦA BẠN**

### **Đề bài:**
- **500 khăn A** → cần **100kg** nguyên liệu A
- **500 khăn B** → cần **100kg** nguyên liệu B  
- **Hao hụt 5-10%** qua các công đoạn
- **Cần lấy dư** để bù hao hụt

### **Tính toán với hệ thống mới:**

---

## 🔧 **API CALLS**

### **1. Tính toán với hao hụt mặc định (10%):**
```http
GET /v1/material-consumption/production-plan/1
```

### **2. Tính toán với hao hụt tùy chỉnh (5%):**
```http
GET /v1/material-consumption/production-plan/1/with-waste?wastePercentage=0.05
```

### **3. Tính toán với hao hụt tùy chỉnh (10%):**
```http
GET /v1/material-consumption/production-plan/1/with-waste?wastePercentage=0.10
```

---

## 📋 **RESPONSE EXAMPLES**

### **Với hao hụt 10% (wastePercentage = 0.10):**

```json
{
  "planId": 1,
  "planCode": "PP-2025-001",
  "totalProducts": 2,
  "wastePercentage": 0.10,
  "totalMaterialValue": 2750000.00,
  "totalBasicQuantity": 200.0,
  "totalWasteAmount": 20.0,
  "productConsumptions": [
    {
      "productId": 1,
      "productCode": "KM-A-001",
      "productName": "Khăn mặt A",
      "plannedQuantity": 500,
      "bomVersion": "1.0",
      "materialDetails": [
        {
          "materialId": 1,
          "materialCode": "MAT-A-001",
          "materialName": "Nguyên liệu A",
          "materialType": "RAW_MATERIAL",
          "unit": "KG",
          "quantityPerUnit": 0.2,
          "basicQuantityRequired": 100.0,
          "wasteAmount": 10.0,
          "totalQuantityRequired": 110.0,
          "wastePercentage": 0.10,
          "stage": "WARPING",
          "optional": false,
          "notes": "Nguyên liệu chính cho khăn A",
          "unitPrice": 25000.00,
          "totalValue": 2750000.00
        }
      ]
    },
    {
      "productId": 2,
      "productCode": "KM-B-001", 
      "productName": "Khăn mặt B",
      "plannedQuantity": 500,
      "bomVersion": "1.0",
      "materialDetails": [
        {
          "materialId": 2,
          "materialCode": "MAT-B-001",
          "materialName": "Nguyên liệu B",
          "materialType": "RAW_MATERIAL",
          "unit": "KG",
          "quantityPerUnit": 0.2,
          "basicQuantityRequired": 100.0,
          "wasteAmount": 10.0,
          "totalQuantityRequired": 110.0,
          "wastePercentage": 0.10,
          "stage": "WARPING",
          "optional": false,
          "notes": "Nguyên liệu chính cho khăn B",
          "unitPrice": 25000.00,
          "totalValue": 2750000.00
        }
      ]
    }
  ],
  "materialSummaries": [
    {
      "materialId": 1,
      "materialCode": "MAT-A-001",
      "materialName": "Nguyên liệu A",
      "materialType": "RAW_MATERIAL",
      "unit": "KG",
      "basicQuantityRequired": 100.0,
      "wasteAmount": 10.0,
      "totalQuantityRequired": 110.0,
      "totalValue": 2750000.00,
      "unitPrice": 25000.00
    },
    {
      "materialId": 2,
      "materialCode": "MAT-B-001",
      "materialName": "Nguyên liệu B",
      "materialType": "RAW_MATERIAL",
      "unit": "KG",
      "basicQuantityRequired": 100.0,
      "wasteAmount": 10.0,
      "totalQuantityRequired": 110.0,
      "totalValue": 2750000.00,
      "unitPrice": 25000.00
    }
  ]
}
```

---

## 🧮 **LOGIC TÍNH TOÁN CHI TIẾT**

### **Công thức tính toán:**

```java
// 1. Tính nguyên liệu cơ bản (theo BOM)
BigDecimal basicQuantity = quantityPerUnit * plannedQuantity;
// Khăn A: 0.2 * 500 = 100kg
// Khăn B: 0.2 * 500 = 100kg

// 2. Tính lượng hao hụt
BigDecimal wasteAmount = basicQuantity * wastePercentage;
// Nguyên liệu A: 100 * 0.10 = 10kg
// Nguyên liệu B: 100 * 0.10 = 10kg

// 3. Tính tổng nguyên liệu cần lấy (bao gồm dư)
BigDecimal totalQuantity = basicQuantity + wasteAmount;
// Nguyên liệu A: 100 + 10 = 110kg
// Nguyên liệu B: 100 + 10 = 110kg
```

### **Tổng kết:**
- **Nguyên liệu cơ bản**: 200kg (100kg A + 100kg B)
- **Lượng hao hụt**: 20kg (10kg A + 10kg B)
- **Tổng cần lấy**: 220kg (110kg A + 110kg B)
- **Tỷ lệ hao hụt**: 10%

---

## 📊 **SO SÁNH VỚI CÁC TỶ LỆ HAO HỤT KHÁC**

### **Với hao hụt 5% (wastePercentage = 0.05):**
```json
{
  "wastePercentage": 0.05,
  "totalBasicQuantity": 200.0,
  "totalWasteAmount": 10.0,
  "materialSummaries": [
    {
      "materialName": "Nguyên liệu A",
      "basicQuantityRequired": 100.0,
      "wasteAmount": 5.0,
      "totalQuantityRequired": 105.0
    },
    {
      "materialName": "Nguyên liệu B", 
      "basicQuantityRequired": 100.0,
      "wasteAmount": 5.0,
      "totalQuantityRequired": 105.0
    }
  ]
}
```

### **Với hao hụt 15% (wastePercentage = 0.15):**
```json
{
  "wastePercentage": 0.15,
  "totalBasicQuantity": 200.0,
  "totalWasteAmount": 30.0,
  "materialSummaries": [
    {
      "materialName": "Nguyên liệu A",
      "basicQuantityRequired": 100.0,
      "wasteAmount": 15.0,
      "totalQuantityRequired": 115.0
    },
    {
      "materialName": "Nguyên liệu B",
      "basicQuantityRequired": 100.0,
      "wasteAmount": 15.0,
      "totalQuantityRequired": 115.0
    }
  ]
}
```

---

## 🎯 **TẠI SAO CẦN LẤY DƯ?**

### **1. Hao hụt trong quá trình sản xuất:**
- **Cuộn mắc**: Sợi bị đứt, cuộn không đều
- **Dệt**: Sợi bị rối, lỗi kỹ thuật
- **Nhuộm**: Màu không đều, bị phai
- **Cắt**: Cắt sai, bị lỗi
- **May**: Chỉ bị đứt, đường may lỗi
- **Đóng gói**: Bao bì bị rách, lỗi đóng gói

### **2. Lợi ích của việc lấy dư:**
- ✅ **Đảm bảo đủ nguyên liệu** cho sản xuất
- ✅ **Tránh gián đoạn** sản xuất do thiếu nguyên liệu
- ✅ **Tính toán chính xác** chi phí nguyên liệu
- ✅ **Quản lý tồn kho** hiệu quả

### **3. Quản lý lượng dư:**
- **Theo dõi hao hụt thực tế** vs dự kiến
- **Điều chỉnh tỷ lệ hao hụt** cho lần sau
- **Tối ưu hóa** quy trình sản xuất
- **Giảm thiểu** lãng phí

---

## 🔄 **WORKFLOW HOÀN CHỈNH**

### **1. Tạo Production Plan:**
```
Contract → Production Plan → BOM → Material Calculation
```

### **2. Tính toán nguyên liệu:**
```
BOM Quantity × Planned Quantity = Basic Quantity
Basic Quantity × Waste Percentage = Waste Amount  
Basic Quantity + Waste Amount = Total Required
```

### **3. Kiểm tra tồn kho:**
```
Current Stock - Reserved Quantity = Available Stock
Available Stock ≥ Total Required = Can Produce
```

### **4. Tạo Material Requisition:**
```
Total Required → Material Requisition → Inventory Issue
```

---

## ✅ **KẾT LUẬN**

**Đúng vậy! Logic của bạn hoàn toàn chính xác:**

1. **500 khăn A + 500 khăn B** = 100kg A + 100kg B (cơ bản)
2. **Hao hụt 5-10%** = 5-20kg dư để bù hao hụt
3. **Tổng cần lấy** = 105-220kg (tùy tỷ lệ hao hụt)
4. **Lượng dư** sẽ bù cho hao hụt trong các công đoạn

**Hệ thống đã được cải thiện để hỗ trợ tính toán này một cách chính xác và linh hoạt!** 🎯✨

---

**Version:** 1.0.0  
**Last Updated:** 2025-10-26  
**Status:** ✅ IMPLEMENTED & READY FOR USE
