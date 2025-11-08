package tmmsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.entity.*;
import tmmsystem.repository.*;

import java.math.BigDecimal;
import java.util.*;

@Service
public class MaterialConsumptionService {

    private final BomRepository bomRepository;
    private final BomDetailRepository bomDetailRepository;
    private final MaterialRepository materialRepository;
    private final MaterialStockRepository materialStockRepository;
    private final MaterialTransactionRepository materialTransactionRepository;
    private final ProductionPlanRepository productionPlanRepository;
    private final ProductRepository productRepository;
    private final MaterialRequisitionRepository materialRequisitionRepository;
    private final MaterialRequisitionDetailRepository materialRequisitionDetailRepository;
    private final UserRepository userRepository;
    private final ProductionLotRepository productionLotRepository;
    private final ProductionLotOrderRepository productionLotOrderRepository;

    public MaterialConsumptionService(BomRepository bomRepository,
                                     BomDetailRepository bomDetailRepository,
                                     MaterialRepository materialRepository,
                                     MaterialStockRepository materialStockRepository,
                                     MaterialTransactionRepository materialTransactionRepository,
                                     ProductionPlanRepository productionPlanRepository,
                                     ProductRepository productRepository,
                                     MaterialRequisitionRepository materialRequisitionRepository,
                                     MaterialRequisitionDetailRepository materialRequisitionDetailRepository,
                                     UserRepository userRepository,
                                     ProductionLotRepository productionLotRepository,
                                     ProductionLotOrderRepository productionLotOrderRepository) {
        this.bomRepository = bomRepository;
        this.bomDetailRepository = bomDetailRepository;
        this.materialRepository = materialRepository;
        this.materialStockRepository = materialStockRepository;
        this.materialTransactionRepository = materialTransactionRepository;
        this.productionPlanRepository = productionPlanRepository;
        this.productRepository = productRepository;
        this.materialRequisitionRepository = materialRequisitionRepository;
        this.materialRequisitionDetailRepository = materialRequisitionDetailRepository;
        this.userRepository = userRepository;
        this.productionLotRepository = productionLotRepository;
        this.productionLotOrderRepository = productionLotOrderRepository;
    }

    /**
     * Tính toán nguyên vật liệu tiêu hao cho một Production Plan
     */
    public MaterialConsumptionResult calculateMaterialConsumption(Long planId) {
        return calculateMaterialConsumption(planId, BigDecimal.valueOf(0.10)); // Mặc định 10% hao hụt
    }

    /**
     * Tính toán nguyên vật liệu tiêu hao cho một Production Plan với tỷ lệ hao hụt tùy chỉnh
     */
    public MaterialConsumptionResult calculateMaterialConsumption(Long planId, BigDecimal wastePercentage) {
        ProductionPlan plan = productionPlanRepository.findById(planId)
            .orElseThrow(() -> new RuntimeException("Production plan not found"));

        // NEW: derive product & quantity list from lot orders instead of ProductionPlanDetail
        java.util.List<ProductionLotOrder> lotOrders = plan.getLot()==null? java.util.List.of() : productionLotOrderRepository.findByLotId(plan.getLot().getId());
        if (lotOrders.isEmpty()) {
            MaterialConsumptionResult empty = new MaterialConsumptionResult();
            empty.setPlanId(planId); empty.setPlanCode(plan.getPlanCode()); empty.setTotalProducts(0); empty.setWastePercentage(wastePercentage);
            empty.setProductConsumptions(java.util.List.of()); empty.setMaterialSummaries(java.util.List.of());
            empty.setTotalMaterialValue(BigDecimal.ZERO); empty.setTotalBasicQuantity(BigDecimal.ZERO); empty.setTotalWasteAmount(BigDecimal.ZERO);
            return empty;
        }

        MaterialConsumptionResult result = new MaterialConsumptionResult();
        result.setPlanId(planId);
        result.setPlanCode(plan.getPlanCode());
        result.setTotalProducts(lotOrders.size());
        result.setWastePercentage(wastePercentage);

        java.util.List<MaterialConsumptionResult.ProductMaterialConsumption> productConsumptions = new java.util.ArrayList<>();
        java.util.Map<Long, MaterialConsumptionResult.MaterialSummary> materialSummaryMap = new java.util.HashMap<>();

        for (ProductionLotOrder lo : lotOrders) {
            QuotationDetail qd = lo.getQuotationDetail();
            if (qd==null) continue;
            Product product = qd.getProduct();
            BigDecimal quantity = qd.getQuantity();

            Bom activeBom = bomRepository.findActiveBomByProductId(product.getId())
                .orElseThrow(() -> new RuntimeException("No active BOM found for product: " + product.getName()));
            java.util.List<BomDetail> bomDetails = bomDetailRepository.findByBomId(activeBom.getId());

            MaterialConsumptionResult.ProductMaterialConsumption productConsumption = new MaterialConsumptionResult.ProductMaterialConsumption();
            productConsumption.setProductId(product.getId());
            productConsumption.setProductCode(product.getCode());
            productConsumption.setProductName(product.getName());
            productConsumption.setPlannedQuantity(quantity);
            productConsumption.setBomVersion(activeBom.getVersion());
            java.util.List<MaterialConsumptionResult.MaterialConsumptionDetail> materialDetails = new java.util.ArrayList<>();

            for (BomDetail bomDetail : bomDetails) {
                Material material = bomDetail.getMaterial();
                BigDecimal materialQuantityPerUnit = bomDetail.getQuantity();
                BigDecimal basicMaterialQuantity = materialQuantityPerUnit.multiply(quantity);
                BigDecimal wasteAmount = basicMaterialQuantity.multiply(wastePercentage);
                BigDecimal totalMaterialQuantity = basicMaterialQuantity.add(wasteAmount);
                MaterialConsumptionResult.MaterialConsumptionDetail materialDetail = new MaterialConsumptionResult.MaterialConsumptionDetail();
                materialDetail.setMaterialId(material.getId());
                materialDetail.setMaterialCode(material.getCode());
                materialDetail.setMaterialName(material.getName());
                materialDetail.setMaterialType(material.getType());
                materialDetail.setUnit(material.getUnit());
                materialDetail.setQuantityPerUnit(materialQuantityPerUnit);
                materialDetail.setBasicQuantityRequired(basicMaterialQuantity);
                materialDetail.setWasteAmount(wasteAmount);
                materialDetail.setTotalQuantityRequired(totalMaterialQuantity);
                materialDetail.setWastePercentage(wastePercentage);
                materialDetail.setStage(bomDetail.getStage());
                materialDetail.setOptional(bomDetail.getOptional());
                materialDetail.setNotes(bomDetail.getNotes());
                BigDecimal unitPrice = material.getStandardCost() != null ? material.getStandardCost() : BigDecimal.ZERO;
                materialDetail.setUnitPrice(unitPrice);
                materialDetail.setTotalValue(totalMaterialQuantity.multiply(unitPrice));
                materialDetails.add(materialDetail);
                MaterialConsumptionResult.MaterialSummary summary = materialSummaryMap.get(material.getId());
                if (summary == null) {
                    summary = new MaterialConsumptionResult.MaterialSummary();
                    summary.setMaterialId(material.getId());
                    summary.setMaterialCode(material.getCode());
                    summary.setMaterialName(material.getName());
                    summary.setMaterialType(material.getType());
                    summary.setUnit(material.getUnit());
                    summary.setBasicQuantityRequired(BigDecimal.ZERO);
                    summary.setWasteAmount(BigDecimal.ZERO);
                    summary.setTotalQuantityRequired(BigDecimal.ZERO);
                    summary.setTotalValue(BigDecimal.ZERO);
                    summary.setUnitPrice(unitPrice);
                    materialSummaryMap.put(material.getId(), summary);
                }
                summary.setBasicQuantityRequired(summary.getBasicQuantityRequired().add(basicMaterialQuantity));
                summary.setWasteAmount(summary.getWasteAmount().add(wasteAmount));
                summary.setTotalQuantityRequired(summary.getTotalQuantityRequired().add(totalMaterialQuantity));
                summary.setTotalValue(summary.getTotalValue().add(totalMaterialQuantity.multiply(unitPrice)));
            }
            productConsumption.setMaterialDetails(materialDetails);
            productConsumptions.add(productConsumption);
        }
        result.setProductConsumptions(productConsumptions);
        result.setMaterialSummaries(new java.util.ArrayList<>(materialSummaryMap.values()));
        BigDecimal totalMaterialValue = materialSummaryMap.values().stream()
            .map(MaterialConsumptionResult.MaterialSummary::getTotalValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.setTotalMaterialValue(totalMaterialValue);
        BigDecimal totalBasicQuantity = materialSummaryMap.values().stream()
            .map(MaterialConsumptionResult.MaterialSummary::getBasicQuantityRequired)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalWasteAmount = materialSummaryMap.values().stream()
            .map(MaterialConsumptionResult.MaterialSummary::getWasteAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.setTotalBasicQuantity(totalBasicQuantity);
        result.setTotalWasteAmount(totalWasteAmount);
        return result;
    }

    /**
     * Kiểm tra khả năng cung ứng nguyên vật liệu cho Production Plan
     */
    public MaterialAvailabilityResult checkMaterialAvailability(Long planId) {
        MaterialConsumptionResult consumption = calculateMaterialConsumption(planId);
        
        MaterialAvailabilityResult result = new MaterialAvailabilityResult();
        result.setPlanId(planId);
        result.setPlanCode(consumption.getPlanCode());
        
        List<MaterialAvailabilityResult.MaterialAvailability> availabilities = new ArrayList<>();
        boolean allMaterialsAvailable = true;
        
        for (MaterialConsumptionResult.MaterialSummary summary : consumption.getMaterialSummaries()) {
            MaterialAvailabilityResult.MaterialAvailability availability = 
                new MaterialAvailabilityResult.MaterialAvailability();
            availability.setMaterialId(summary.getMaterialId());
            availability.setMaterialCode(summary.getMaterialCode());
            availability.setMaterialName(summary.getMaterialName());
            availability.setMaterialType(summary.getMaterialType());
            availability.setUnit(summary.getUnit());
            availability.setRequiredQuantity(summary.getTotalQuantityRequired());
            
            // Tính tổng tồn kho hiện tại
            BigDecimal currentStock = calculateCurrentStock(summary.getMaterialId());
            availability.setCurrentStock(currentStock);
            
            // Tính tổng tồn kho sau khi trừ đi các Production Plan đã được phê duyệt
            BigDecimal reservedQuantity = calculateReservedQuantity(summary.getMaterialId());
            BigDecimal availableStock = currentStock.subtract(reservedQuantity);
            availability.setReservedQuantity(reservedQuantity);
            availability.setAvailableStock(availableStock);
            
            // Kiểm tra khả năng cung ứng
            boolean isAvailable = availableStock.compareTo(summary.getTotalQuantityRequired()) >= 0;
            availability.setAvailable(isAvailable);
            
            if (!isAvailable) {
                allMaterialsAvailable = false;
                BigDecimal shortage = summary.getTotalQuantityRequired().subtract(availableStock);
                availability.setShortage(shortage);
            } else {
                availability.setShortage(BigDecimal.ZERO);
            }
            
            availabilities.add(availability);
        }
        
        result.setMaterialAvailabilities(availabilities);
        result.setAllMaterialsAvailable(allMaterialsAvailable);
        
        return result;
    }

    /**
     * Tính tổng tồn kho hiện tại của một nguyên vật liệu
     */
    private BigDecimal calculateCurrentStock(Long materialId) {
        return materialStockRepository.findByMaterialId(materialId).stream()
            .map(MaterialStock::getQuantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Tính tổng số lượng nguyên vật liệu đã được đặt trước (reserved) cho các Production Plan đã phê duyệt
     */
    private BigDecimal calculateReservedQuantity(Long materialId) {
        List<ProductionPlan> approvedPlans = productionPlanRepository.findByStatus(ProductionPlan.PlanStatus.APPROVED);
        BigDecimal totalReserved = BigDecimal.ZERO;
        for (ProductionPlan plan : approvedPlans) {
            if (plan.getLot()==null) continue;
            var lotOrders = productionLotOrderRepository.findByLotId(plan.getLot().getId());
            for (ProductionLotOrder lo : lotOrders) {
                QuotationDetail qd = lo.getQuotationDetail();
                if (qd==null) continue;
                Product product = qd.getProduct();
                Bom activeBom = bomRepository.findActiveBomByProductId(product.getId()).orElse(null);
                if (activeBom == null) continue;
                List<BomDetail> bomDetails = bomDetailRepository.findByBomId(activeBom.getId());
                for (BomDetail bomDetail : bomDetails) {
                    if (bomDetail.getMaterial().getId().equals(materialId)) {
                        BigDecimal reservedQuantity = bomDetail.getQuantity().multiply(qd.getQuantity());
                        totalReserved = totalReserved.add(reservedQuantity);
                    }
                }
            }
        }
        return totalReserved;
    }

    /**
     * Tạo Material Requisition từ Production Plan
     */
    @Transactional
    public MaterialRequisition createMaterialRequisitionFromPlan(Long planId, Long createdById) {
        MaterialConsumptionResult consumption = calculateMaterialConsumption(planId);
        MaterialAvailabilityResult availability = checkMaterialAvailability(planId);
        
        if (!availability.getAllMaterialsAvailable()) {
            throw new RuntimeException("Cannot create material requisition: insufficient material availability");
        }
        
        ProductionPlan plan = productionPlanRepository.findById(planId)
            .orElseThrow(() -> new RuntimeException("Production plan not found"));
        
        // Tạo Material Requisition
        MaterialRequisition requisition = new MaterialRequisition();
        requisition.setRequisitionNumber("MR-" + System.currentTimeMillis());
        // Note: MaterialRequisition entity hiện tại không có ProductionPlan field
        // Có thể cần thêm field này hoặc sử dụng ProductionStage
        requisition.setStatus("PENDING");
        requisition.setNotes("Auto-generated from Production Plan: " + plan.getPlanCode());
        requisition.setRequestedBy(userRepository.findById(createdById)
            .orElseThrow(() -> new RuntimeException("User not found")));
        
        MaterialRequisition savedRequisition = materialRequisitionRepository.save(requisition);
        
        // Tạo Material Requisition Details
        for (MaterialConsumptionResult.MaterialSummary summary : consumption.getMaterialSummaries()) {
            MaterialRequisitionDetail detail = new MaterialRequisitionDetail();
            detail.setRequisition(savedRequisition);
            detail.setMaterial(materialRepository.findById(summary.getMaterialId())
                .orElseThrow(() -> new RuntimeException("Material not found")));
            detail.setQuantityRequested(summary.getTotalQuantityRequired());
            detail.setUnit(summary.getUnit());
            detail.setNotes("Required for " + summary.getMaterialName());
            
            materialRequisitionDetailRepository.save(detail);
        }
        
        return savedRequisition;
    }

    // DTOs for API responses
    @lombok.Getter @lombok.Setter
    public static class MaterialConsumptionResult {
        private Long planId;
        private String planCode;
        private Integer totalProducts;
        private BigDecimal wastePercentage;
        private BigDecimal totalMaterialValue;
        private BigDecimal totalBasicQuantity;
        private BigDecimal totalWasteAmount;
        private List<ProductMaterialConsumption> productConsumptions;
        private List<MaterialSummary> materialSummaries;

        @lombok.Getter @lombok.Setter
        public static class ProductMaterialConsumption {
            private Long productId;
            private String productCode;
            private String productName;
            private BigDecimal plannedQuantity;
            private String bomVersion;
            private List<MaterialConsumptionDetail> materialDetails;
        }

        @lombok.Getter @lombok.Setter
        public static class MaterialConsumptionDetail {
            private Long materialId;
            private String materialCode;
            private String materialName;
            private String materialType;
            private String unit;
            private BigDecimal quantityPerUnit;
            private BigDecimal basicQuantityRequired;
            private BigDecimal wasteAmount;
            private BigDecimal totalQuantityRequired;
            private BigDecimal wastePercentage;
            private String stage;
            private Boolean optional;
            private String notes;
            private BigDecimal unitPrice;
            private BigDecimal totalValue;
        }

        @lombok.Getter @lombok.Setter
        public static class MaterialSummary {
            private Long materialId;
            private String materialCode;
            private String materialName;
            private String materialType;
            private String unit;
            private BigDecimal basicQuantityRequired;
            private BigDecimal wasteAmount;
            private BigDecimal totalQuantityRequired;
            private BigDecimal totalValue;
            private BigDecimal unitPrice;
        }
    }

    @lombok.Getter @lombok.Setter
    public static class MaterialAvailabilityResult {
        private Long planId;
        private String planCode;
        private Boolean allMaterialsAvailable;
        private List<MaterialAvailability> materialAvailabilities;

        @lombok.Getter @lombok.Setter
        public static class MaterialAvailability {
            private Long materialId;
            private String materialCode;
            private String materialName;
            private String materialType;
            private String unit;
            private BigDecimal requiredQuantity;
            private BigDecimal currentStock;
            private BigDecimal reservedQuantity;
            private BigDecimal availableStock;
            private Boolean available;
            private BigDecimal shortage;
        }
    }
}
