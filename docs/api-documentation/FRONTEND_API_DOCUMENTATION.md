# 📚 **API DOCUMENTATION CHO FRONTEND DEVELOPER**

## 🎯 **TỔNG QUAN**

Tài liệu này cung cấp thông tin chi tiết về các API endpoints để Frontend Developer có thể tích hợp với hệ thống Production Plan và Machine Selection.

---

## 📋 **MAPPING UI VỚI BACKEND**

### **✅ UI "Tạo kế hoạch sản xuất chi tiết" ↔ Backend APIs**

| **UI Field** | **Backend Field** | **API Endpoint** | **Method** |
|--------------|-------------------|------------------|------------|
| **Mã đơn hàng** | `contractNumber` | `/v1/production-plans/{id}` | GET |
| **Tên sản phẩm** | `productName` | `/v1/production-plans/{id}` | GET |
| **Kích thước sản phẩm** | `productCode` | `/v1/production-plans/{id}` | GET |
| **Số lượng** | `plannedQuantity` | `/v1/production-plans/{id}` | GET |
| **Nguyên vật liệu** | `notes` | `/v1/production-plans/{id}` | GET |
| **Ngày bắt đầu** | `proposedStartDate` | `/v1/production-plans/{id}` | GET |
| **Ngày kết thúc** | `proposedEndDate` | `/v1/production-plans/{id}` | GET |
| **Người lập kế hoạch** | `createdByName` | `/v1/production-plans/{id}` | GET |
| **Ngày lập** | `createdAt` | `/v1/production-plans/{id}` | GET |

### **✅ UI "Chi tiết công đoạn sản xuất" ↔ Backend APIs**

| **UI Field** | **Backend Field** | **API Endpoint** | **Method** |
|--------------|-------------------|------------------|------------|
| **Máy móc/thiết bị** | `assignedMachineName` | `/v1/production-plans/stages/{stageId}/machine-suggestions` | GET |
| **Người phụ trách** | `inChargeUserName` | `/v1/production-plans/stages/{stageId}` | GET |
| **Thời gian bắt đầu** | `plannedStartTime` | `/v1/production-plans/stages/{stageId}` | GET |
| **Thời gian kết thúc** | `plannedEndTime` | `/v1/production-plans/stages/{stageId}` | GET |
| **Thời lượng (giờ)** | `minRequiredDurationMinutes` | `/v1/production-plans/stages/{stageId}` | GET |
| **Ghi chú** | `notes` | `/v1/production-plans/stages/{stageId}` | GET |

### **✅ UI "Phê duyệt kế hoạch" ↔ Backend APIs**

| **UI Field** | **Backend Field** | **API Endpoint** | **Method** |
|--------------|-------------------|------------------|------------|
| **Trạng thái** | `status` | `/v1/production-plans/{id}` | GET |
| **Ghi chú của Giám đốc** | `approvalNotes` | `/v1/production-plans/{id}/approve` | PUT |
| **Nút Phê duyệt** | - | `/v1/production-plans/{id}/approve` | PUT |
| **Nút Từ chối** | - | `/v1/production-plans/{id}/reject` | PUT |

---

## 🚀 **API ENDPOINTS CHI TIẾT**

### **1. PRODUCTION PLAN MANAGEMENT**

#### **📋 Lấy danh sách kế hoạch sản xuất**
```http
GET /v1/production-plans
```
**Response:**
```json
[
  {
    "id": 1,
    "contractId": 1,
    "contractNumber": "ORD-101",
    "planCode": "PP-2025-001",
    "status": "DRAFT",
    "createdById": 1,
    "createdByName": "Nguyễn Văn A",
    "approvedById": null,
    "approvedByName": null,
    "approvedAt": null,
    "approvalNotes": null,
    "createdAt": "2025-10-26T09:45:00Z",
    "updatedAt": "2025-10-26T09:45:00Z",
    "customerName": "Công ty ABC",
    "customerCode": "ABC123",
    "contractCreatedAt": "2025-10-25T10:00:00Z",
    "contractApprovedAt": "2025-10-25T15:00:00Z",
    "details": [
      {
        "id": 1,
        "planId": 1,
        "productId": 1,
        "productCode": "KM-001",
        "productName": "Khăn mặt Bamboo cao cấp",
        "productDescription": "Khăn mặt 35x50cm",
        "plannedQuantity": 1000,
        "requiredDeliveryDate": "2025-11-02",
        "proposedStartDate": "2025-10-28",
        "proposedEndDate": "2025-11-02",
        "workCenterId": 1,
        "workCenterName": "Dây chuyền A",
        "workCenterCode": "DC-A",
        "expectedCapacityPerDay": 200,
        "leadTimeDays": 5,
        "notes": "Sợi bamboo 50kg, chỉ cotton 5kg, bao bì nilon 1000 cái",
        "totalStages": 6,
        "totalDurationDays": 5,
        "stages": [...]
      }
    ]
  }
]
```

#### **📋 Lấy chi tiết kế hoạch sản xuất**
```http
GET /v1/production-plans/{id}
```
**Response:** Tương tự như trên nhưng chỉ 1 object

#### **📋 Lấy kế hoạch theo trạng thái**
```http
GET /v1/production-plans/status/{status}
```
**Status values:** `DRAFT`, `PENDING_APPROVAL`, `APPROVED`, `REJECTED`

#### **📋 Lấy kế hoạch chờ duyệt**
```http
GET /v1/production-plans/pending-approval
```

#### **📋 Tạo kế hoạch sản xuất mới**
```http
POST /v1/production-plans
```
**Request Body:**
```json
{
  "contractId": 1,
  "planCode": "PP-2025-002",
  "notes": "Kế hoạch sản xuất cho hợp đồng ORD-101",
  "details": [
    {
      "productId": 1,
      "plannedQuantity": 1000,
      "requiredDeliveryDate": "2025-11-02",
      "proposedStartDate": "2025-10-28",
      "proposedEndDate": "2025-11-02",
      "workCenterId": 1,
      "expectedCapacityPerDay": 200,
      "leadTimeDays": 5,
      "notes": "Sợi bamboo 50kg, chỉ cotton 5kg",
      "stages": [
        {
          "stageType": "WARPING",
          "sequenceNo": 1,
          "assignedMachineId": 1,
          "inChargeUserId": 2,
          "plannedStartTime": "2025-10-28T08:00:00",
          "plannedEndTime": "2025-10-28T12:00:00",
          "minRequiredDurationMinutes": 240,
          "transferBatchQuantity": 100,
          "capacityPerHour": 200,
          "notes": "Kiểm tra độ căng sợi trước khi chạy máy"
        }
      ]
    }
  ]
}
```

#### **📋 Gửi kế hoạch để duyệt**
```http
PUT /v1/production-plans/{id}/submit
```
**Request Body:**
```json
{
  "notes": "Kế hoạch đã hoàn thiện, xin gửi duyệt"
}
```

#### **📋 Phê duyệt kế hoạch**
```http
PUT /v1/production-plans/{id}/approve
```
**Request Body:**
```json
{
  "approvalNotes": "Kế hoạch sản xuất đã được phê duyệt"
}
```

#### **📋 Từ chối kế hoạch**
```http
PUT /v1/production-plans/{id}/reject
```
**Request Body:**
```json
{
  "rejectionReason": "Cần điều chỉnh lại thời gian sản xuất"
}
```

---

### **2. MACHINE SELECTION APIS**

#### **🤖 Lấy gợi ý máy móc cho công đoạn**
```http
GET /v1/production-plans/machine-suggestions
```
**Parameters:**
- `stageType`: `WARPING`, `WEAVING`, `DYEING`, `CUTTING`, `SEWING`, `PACKAGING`
- `productId`: ID sản phẩm
- `requiredQuantity`: Số lượng cần sản xuất
- `preferredStartTime`: Thời gian bắt đầu mong muốn (optional)
- `preferredEndTime`: Thời gian kết thúc mong muốn (optional)

**Response:**
```json
[
  {
    "machineId": 1,
    "machineCode": "CM-01",
    "machineName": "Máy cuộn mắc 01",
    "machineType": "WARPING",
    "location": "Khu A",
    "capacityPerHour": 200.0,
    "estimatedDurationHours": 4.0,
    "canHandleQuantity": true,
    "available": true,
    "availabilityScore": 100.0,
    "conflicts": [],
    "suggestedStartTime": "2025-10-28T08:00:00",
    "suggestedEndTime": "2025-10-28T12:00:00",
    "priorityScore": 95.5
  },
  {
    "machineId": 2,
    "machineCode": "CM-02",
    "machineName": "Máy cuộn mắc 02",
    "machineType": "WARPING",
    "location": "Khu B",
    "capacityPerHour": 150.0,
    "estimatedDurationHours": 5.33,
    "canHandleQuantity": true,
    "available": false,
    "availabilityScore": 50.0,
    "conflicts": [
      "Máy đang được sử dụng cho Work Order từ 2025-10-28T08:00:00"
    ],
    "suggestedStartTime": "2025-10-29T08:00:00",
    "suggestedEndTime": "2025-10-29T13:20:00",
    "priorityScore": 70.0
  }
]
```

#### **🤖 Lấy gợi ý máy cho stage hiện có**
```http
GET /v1/production-plans/stages/{stageId}/machine-suggestions
```

#### **🤖 Tự động gán máy cho stage**
```http
POST /v1/production-plans/stages/{stageId}/auto-assign-machine
```
**Response:**
```json
{
  "id": 1,
  "planDetailId": 1,
  "stageType": "WARPING",
  "stageTypeName": "Cuộn mắc",
  "sequenceNo": 1,
  "assignedMachineId": 1,
  "assignedMachineName": "Máy cuộn mắc 01",
  "assignedMachineCode": "CM-01",
  "inChargeUserId": 2,
  "inChargeUserName": "Trần Hùng",
  "plannedStartTime": "2025-10-28T08:00:00",
  "plannedEndTime": "2025-10-28T12:00:00",
  "minRequiredDurationMinutes": 240,
  "transferBatchQuantity": 100,
  "capacityPerHour": 200,
  "notes": "Kiểm tra độ căng sợi trước khi chạy máy",
  "durationMinutes": 240,
  "estimatedOutput": 800
}
```

#### **🤖 Kiểm tra xung đột lịch trình**
```http
GET /v1/production-plans/stages/{stageId}/check-conflicts
```
**Response:**
```json
[
  "Máy đang được sử dụng cho Work Order từ 2025-10-28T08:00:00",
  "Máy đang bảo trì từ 2025-10-28 08:00 đến 2025-10-28 12:00"
]
```

#### **🤖 Gợi ý máy móc (POST)**
```http
POST /v1/machine-selection/suggest-machines
```
**Request Body:**
```json
{
  "stageType": "WARPING",
  "productId": 1,
  "requiredQuantity": 1000,
  "preferredStartTime": "2025-10-28T08:00:00",
  "preferredEndTime": "2025-10-28T17:00:00"
}
```

#### **🤖 Kiểm tra khả năng sẵn sàng máy**
```http
GET /v1/machine-selection/check-availability
```
**Parameters:**
- `machineId`: ID máy
- `startTime`: Thời gian bắt đầu
- `endTime`: Thời gian kết thúc

**Response:**
```json
{
  "available": false,
  "message": "Machine is not available during the requested time period",
  "conflicts": [
    "Máy đang được sử dụng cho Work Order từ 2025-10-28T08:00:00"
  ],
  "suggestedStartTime": "2025-10-29T08:00:00",
  "suggestedEndTime": "2025-10-29T17:00:00"
}
```

---

## 🎨 **FRONTEND INTEGRATION EXAMPLES**

### **1. Tạo Production Plan với Machine Selection**

```javascript
// 1. Lấy gợi ý máy móc cho từng công đoạn
async function getMachineSuggestions(stageType, productId, quantity) {
  const response = await fetch(`/v1/production-plans/machine-suggestions?stageType=${stageType}&productId=${productId}&requiredQuantity=${quantity}`);
  const suggestions = await response.json();
  
  // Hiển thị dropdown với máy được sắp xếp theo priorityScore
  suggestions.sort((a, b) => b.priorityScore - a.priorityScore);
  
  return suggestions;
}

// 2. Tạo Production Plan
async function createProductionPlan(planData) {
  const response = await fetch('/v1/production-plans', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(planData)
  });
  
  return await response.json();
}

// 3. Tự động gán máy cho stage
async function autoAssignMachine(stageId) {
  const response = await fetch(`/v1/production-plans/stages/${stageId}/auto-assign-machine`, {
    method: 'POST'
  });
  
  return await response.json();
}
```

### **2. UI Component cho Machine Selection**

```javascript
// Component MachineSelector
function MachineSelector({ stageType, productId, quantity, onMachineSelect }) {
  const [suggestions, setSuggestions] = useState([]);
  const [loading, setLoading] = useState(false);
  
  useEffect(() => {
    if (stageType && productId && quantity) {
      loadMachineSuggestions();
    }
  }, [stageType, productId, quantity]);
  
  const loadMachineSuggestions = async () => {
    setLoading(true);
    try {
      const suggestions = await getMachineSuggestions(stageType, productId, quantity);
      setSuggestions(suggestions);
    } catch (error) {
      console.error('Error loading machine suggestions:', error);
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <div className="machine-selector">
      <label>Máy móc/thiết bị phụ trách:</label>
      <select onChange={(e) => onMachineSelect(JSON.parse(e.target.value))}>
        <option value="">Chọn máy...</option>
        {suggestions.map(machine => (
          <option key={machine.machineId} value={JSON.stringify(machine)}>
            {machine.machineName} ({machine.machineCode}) 
            {machine.available ? ' ✅' : ' ❌'} 
            - Điểm: {machine.priorityScore.toFixed(1)}
          </option>
        ))}
      </select>
      
      {/* Hiển thị thông tin chi tiết máy được chọn */}
      {selectedMachine && (
        <div className="machine-details">
          <p><strong>Năng suất:</strong> {selectedMachine.capacityPerHour} đơn vị/giờ</p>
          <p><strong>Thời lượng ước tính:</strong> {selectedMachine.estimatedDurationHours} giờ</p>
          <p><strong>Vị trí:</strong> {selectedMachine.location}</p>
          {selectedMachine.conflicts.length > 0 && (
            <div className="conflicts">
              <strong>⚠️ Xung đột:</strong>
              <ul>
                {selectedMachine.conflicts.map((conflict, index) => (
                  <li key={index}>{conflict}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
```

### **3. Workflow hoàn chỉnh**

```javascript
// Workflow tạo Production Plan
async function createProductionPlanWorkflow(contractId) {
  try {
    // 1. Tạo Production Plan cơ bản
    const plan = await createProductionPlan({
      contractId: contractId,
      planCode: generatePlanCode(),
      notes: "Kế hoạch sản xuất tự động"
    });
    
    // 2. Tự động gán máy cho từng stage
    for (const detail of plan.details) {
      for (const stage of detail.stages) {
        try {
          const updatedStage = await autoAssignMachine(stage.id);
          console.log(`Auto-assigned machine for stage ${stage.stageType}`);
        } catch (error) {
          console.warn(`Failed to auto-assign machine for stage ${stage.stageType}:`, error);
        }
      }
    }
    
    // 3. Kiểm tra xung đột
    for (const detail of plan.details) {
      for (const stage of detail.stages) {
        const conflicts = await checkConflicts(stage.id);
        if (conflicts.length > 0) {
          console.warn(`Conflicts found for stage ${stage.stageType}:`, conflicts);
        }
      }
    }
    
    return plan;
  } catch (error) {
    console.error('Error creating production plan:', error);
    throw error;
  }
}
```

---

## 📊 **ERROR HANDLING**

### **Common Error Responses:**

```json
// 400 Bad Request
{
  "timestamp": "2025-10-26T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid stage type: INVALID_TYPE",
  "path": "/v1/production-plans/machine-suggestions"
}

// 404 Not Found
{
  "timestamp": "2025-10-26T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Production plan not found",
  "path": "/v1/production-plans/999"
}

// 500 Internal Server Error
{
  "timestamp": "2025-10-26T10:00:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "No suitable machines found for stage: WARPING",
  "path": "/v1/production-plans/stages/1/auto-assign-machine"
}
```

---

## 🔧 **SWAGGER DOCUMENTATION**

### **Swagger UI sẽ hiển thị:**

1. **Production Plan Management** tag với 15+ endpoints
2. **Machine Selection** tag với 4 endpoints
3. **Chi tiết request/response schemas**
4. **Example values** cho tất cả parameters
5. **Error response schemas**

### **Truy cập Swagger UI:**
```
http://localhost:8080/swagger-ui.html
```

---

## ✅ **CHECKLIST CHO FRONTEND DEVELOPER**

- [ ] **API Base URL**: `http://localhost:8080/v1`
- [ ] **Authentication**: Bearer token trong header
- [ ] **Content-Type**: `application/json`
- [ ] **Error Handling**: Xử lý 400, 404, 500 errors
- [ ] **Loading States**: Hiển thị loading khi gọi API
- [ ] **Machine Selection**: Implement dropdown với gợi ý thông minh
- [ ] **Conflict Detection**: Hiển thị cảnh báo xung đột
- [ ] **Auto-assignment**: Nút tự động gán máy
- [ ] **Real-time Updates**: Refresh data sau khi thay đổi
- [ ] **Validation**: Validate input trước khi gửi API

---

## 📞 **SUPPORT**

**Nếu gặp vấn đề:**
1. Kiểm tra Swagger UI: `http://localhost:8080/swagger-ui.html`
2. Xem logs trong console
3. Kiểm tra network tab trong DevTools
4. Contact Backend Team

**Version:** 1.0.0  
**Last Updated:** 2025-10-26  
**Contact:** Development Team
