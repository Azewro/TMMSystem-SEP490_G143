# Kiểm tra Pagination cho các List Endpoints trong Backend

## ✅ ĐÃ CÓ PAGINATION (PageResponse)

### RfqController
- ✅ `GET /v1/rfqs` - PageResponse<RfqDto>
- ✅ `GET /v1/rfqs/for-sales` - PageResponse<RfqDto>
- ✅ `GET /v1/rfqs/for-planning` - PageResponse<RfqDto>
- ✅ `GET /v1/rfqs/drafts/unassigned` - PageResponse<RfqDto>
- ❌ `GET /v1/rfqs/{rfqId}/details` - List<RfqDetailDto> (chi tiết của 1 RFQ, không cần pagination)

### QuotationController
- ✅ `GET /v1/quotations` - PageResponse<QuotationDto>
- ✅ `GET /v1/quotations/for-sales` - PageResponse<QuotationDto>
- ✅ `GET /v1/quotations/for-planning` - PageResponse<QuotationDto>
- ✅ `GET /v1/quotations/customer/{customerId}` - PageResponse<QuotationDto>
- ❌ `GET /v1/quotations/pending` - List<QuotationDto> (CHƯA CÓ PAGINATION)

### ContractController
- ✅ `GET /v1/contracts` - PageResponse<ContractDto>
- ✅ `GET /v1/contracts/pending-approval` - PageResponse<ContractDto>
- ✅ `GET /v1/contracts/director/pending` - PageResponse<ContractDto>
- ❌ `GET /v1/contracts/assigned/sales/{userId}` - List<ContractDto> (CHƯA CÓ PAGINATION)
- ❌ `GET /v1/contracts/assigned/planning/{userId}` - List<ContractDto> (CHƯA CÓ PAGINATION)

### UserController
- ✅ `GET /api/admin/users` - PageResponse<UserDto>

### CustomerController
- ✅ `GET /v1/customers` - PageResponse<CustomerDto>

### MachineController
- ✅ `GET /v1/machines` - PageResponse<MachineDto>

## ❌ CHƯA CÓ PAGINATION (List)

### ProductionLotController
- ❌ `GET /v1/production-lots` - List<ProductionLotDto>
- ❌ `GET /v1/production-lots/{id}/contracts` - List<ProductionLotContractDto> (chi tiết của 1 lot)

### ProductController
- ❌ `GET /v1/products` - List<ProductDto>
- ❌ `GET /v1/products/materials` - List<MaterialDto>
- ❌ `GET /v1/products/{productId}/boms` - List<BomDto> (chi tiết của 1 product)
- ❌ `GET /v1/products/boms/{bomId}/details` - List<BomDetailDto> (chi tiết của 1 bom)

### ProductCategoryController
- ❌ `GET /v1/product-categories` - List<ProductCategoryDto>

### RoleController
- ❌ `GET /v1/roles` - List<RoleDto>

### AdminUserController
- ❌ `GET /v1/admin/users` - List<User> (DUPLICATE với UserController, có thể deprecated)

### ProductionPlanController
- ❌ `GET /v1/production-plans` - List<ProductionPlanDto>
- ❌ `GET /v1/production-plans/status/{status}` - List<ProductionPlanDto>
- ❌ `GET /v1/production-plans/pending-approval` - List<ProductionPlanDto>
- ❌ `GET /v1/production-plans/contract/{contractId}` - List<ProductionPlanDto>
- ❌ `GET /v1/production-plans/creator/{userId}` - List<ProductionPlanDto>
- ❌ `GET /v1/production-plans/approved-not-converted` - List<ProductionPlanDto>
- ❌ `GET /v1/production-plans/{planId}/stages` - List<ProductionPlanStageDto> (chi tiết của 1 plan)

### ProductionController
- ❌ `GET /v1/production/orders` - List<ProductionOrderDto>
- ❌ `GET /v1/production/orders/{poId}/details` - List<ProductionOrderDetailDto> (chi tiết của 1 PO)
- ❌ `GET /v1/production/orders/{poId}/work-orders` - List<WorkOrderDto> (chi tiết của 1 PO)
- ❌ `GET /v1/production/work-orders/{woId}/details` - List<WorkOrderDetailDto> (chi tiết của 1 WO)
- ❌ `GET /v1/production/work-order-details/{woDetailId}/stages` - List<ProductionStageDto> (chi tiết của 1 WO detail)
- ❌ `GET /v1/production/orders/pending-approval` - List<ProductionOrderDto>
- ❌ `GET /v1/production/orders/director/pending` - List<ProductionOrderDto>
- ❌ `GET /v1/production/contracts/{contractId}/plans` - List<ProductionPlanDto>
- ❌ `GET /v1/production/plans/pending-approval` - List<ProductionPlanDto>
- ❌ `GET /v1/production/stages/for-leader` - List<ProductionStageDto>
- ❌ `GET /v1/production/stages/for-kcs` - List<ProductionStageDto>

### SystemController
- ❌ `GET /v1/system/users/{userId}/notifications` - List<NotificationDto>
- ❌ `GET /v1/system/report-templates` - List<ReportTemplateDto>
- ❌ `GET /v1/system/audit-logs` - List<AuditLogDto>

### ExecutionController
- ❌ `GET /v1/execution/stages/{stageId}/trackings` - List<StageTrackingDto> (chi tiết của 1 stage)
- ❌ `GET /v1/execution/stages/{stageId}/pauses` - List<StagePauseLogDto> (chi tiết của 1 stage)
- ❌ `GET /v1/execution/stages/{stageId}/outsourcing` - List<OutsourcingTaskDto> (chi tiết của 1 stage)
- ❌ `GET /v1/execution/orders/{poId}/losses` - List<ProductionLossDto> (chi tiết của 1 PO)
- ❌ `GET /v1/execution/requisitions/{reqId}/details` - List<MaterialRequisitionDetailDto> (chi tiết của 1 requisition)

### QcController
- ❌ `GET /v1/qc/checkpoints` - List<QcCheckpointDto>
- ❌ `GET /v1/qc/stages/{stageId}/inspections` - List<QcInspectionDto> (chi tiết của 1 stage)
- ❌ `GET /v1/qc/inspections/{inspectionId}/defects` - List<QcDefectDto> (chi tiết của 1 inspection)
- ❌ `GET /v1/qc/inspections/{inspectionId}/photos` - List<QcPhotoDto> (chi tiết của 1 inspection)
- ❌ `GET /v1/qc/standards` - List<QcStandardDto>

### MachineSelectionController
- ❌ `GET /v1/machine-selection/suitable-machines` - List<MachineSuggestionDto>
- ❌ `GET /v1/machine-selection/check-availability` - List<MachineSuggestionDto>

### InventoryController
- ❌ `GET /v1/inventory/materials/{materialId}/stock` - List<MaterialStockDto> (chi tiết của 1 material)
- ❌ `GET /v1/inventory/materials/{materialId}/transactions` - List<MaterialTransactionDto> (chi tiết của 1 material)
- ❌ `GET /v1/inventory/products/{productId}/stock` - List<FinishedGoodsStockDto> (chi tiết của 1 product)
- ❌ `GET /v1/inventory/products/{productId}/transactions` - List<FinishedGoodsTransactionDto> (chi tiết của 1 product)

### MachineOpsController
- ❌ `GET /v1/machine-ops/{machineId}/assignments` - List<MachineAssignmentDto> (chi tiết của 1 machine)
- ❌ `GET /v1/machine-ops/stages/{stageId}/assignments` - List<MachineAssignmentDto> (chi tiết của 1 stage)
- ❌ `GET /v1/machine-ops/{machineId}/maintenances` - List<MachineMaintenanceDto> (chi tiết của 1 machine)

### PaymentController
- ❌ `GET /v1/payments/contracts/{contractId}/terms` - List<PaymentTermDto> (chi tiết của 1 contract)
- ❌ `GET /v1/payments/contracts/{contractId}` - List<PaymentDto> (chi tiết của 1 contract)

## 📝 GHI CHÚ

- Các endpoint trả về chi tiết của một entity cụ thể (ví dụ: `/rfqs/{id}/details`, `/products/{id}/boms`) thường không cần pagination vì số lượng item thường nhỏ.
- Các endpoint trả về danh sách chính (main list) nên có pagination để tối ưu performance.

