package tmmsystem.service;

import org.springframework.stereotype.Service;
import tmmsystem.entity.*;
import tmmsystem.repository.*;

import java.math.BigDecimal;
import java.util.*;

@Service
public class MaterialConsumptionService {

    private final BomRepository bomRepository;
    private final BomDetailRepository bomDetailRepository;
    private final ProductionPlanRepository productionPlanRepository;

    public MaterialConsumptionService(BomRepository bomRepository,
                                     BomDetailRepository bomDetailRepository,
                                     ProductionPlanRepository productionPlanRepository) {
        this.bomRepository = bomRepository;
        this.bomDetailRepository = bomDetailRepository;
        this.productionPlanRepository = productionPlanRepository;
    }

    /**
     * Tính toán nguyên vật liệu tiêu hao cho một Production Plan
     */
    public MaterialConsumptionResult calculateMaterialConsumption(Long planId) {
        return calculateMaterialConsumption(planId, BigDecimal.valueOf(0.10)); // Mặc định 10% hao hụt
    }

    /**
     * Tính toán nguyên vật liệu tiêu hao cho một Production Plan với tỷ lệ hao hụt mặc định 10%
     * Logic: Tính theo Lot.totalQuantity (đã gộp các orders lại) thay vì tính từng order riêng lẻ
     */
    public MaterialConsumptionResult calculateMaterialConsumption(Long planId, BigDecimal wastePercentage) {
        ProductionPlan plan = productionPlanRepository.findById(planId)
            .orElseThrow(() -> new RuntimeException("Production plan not found"));

        // Lấy Lot từ Plan
        ProductionLot lot = plan.getLot();
        if (lot == null) {
            MaterialConsumptionResult empty = new MaterialConsumptionResult();
            empty.setPlanId(planId); 
            empty.setPlanCode(plan.getPlanCode()); 
            empty.setTotalProducts(0); 
            empty.setWastePercentage(wastePercentage);
            empty.setProductConsumptions(java.util.List.of()); 
            empty.setMaterialSummaries(java.util.List.of());
            empty.setTotalMaterialValue(BigDecimal.ZERO); 
            empty.setTotalBasicQuantity(BigDecimal.ZERO); 
            empty.setTotalWasteAmount(BigDecimal.ZERO);
            return empty;
        }

        // Lấy Product từ Lot (1 Lot = 1 Product sau khi gộp)
        Product product = lot.getProduct();
        if (product == null) {
            throw new RuntimeException("Lot does not have a product assigned");
        }

        // Sử dụng totalQuantity của Lot (đã gộp tất cả orders lại)
        BigDecimal lotTotalQuantity = lot.getTotalQuantity();
        if (lotTotalQuantity == null || lotTotalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            MaterialConsumptionResult empty = new MaterialConsumptionResult();
            empty.setPlanId(planId); 
            empty.setPlanCode(plan.getPlanCode()); 
            empty.setTotalProducts(1); 
            empty.setWastePercentage(wastePercentage);
            empty.setProductConsumptions(java.util.List.of()); 
            empty.setMaterialSummaries(java.util.List.of());
            empty.setTotalMaterialValue(BigDecimal.ZERO); 
            empty.setTotalBasicQuantity(BigDecimal.ZERO); 
            empty.setTotalWasteAmount(BigDecimal.ZERO);
            return empty;
        }

        // Lấy BOM active của product
        Bom activeBom = bomRepository.findActiveBomByProductId(product.getId())
            .orElseThrow(() -> new RuntimeException("No active BOM found for product: " + product.getName()));
        java.util.List<BomDetail> bomDetails = bomDetailRepository.findByBomId(activeBom.getId());

        MaterialConsumptionResult result = new MaterialConsumptionResult();
        result.setPlanId(planId);
        result.setPlanCode(plan.getPlanCode());
        result.setTotalProducts(1); // 1 Lot = 1 Product
        result.setWastePercentage(wastePercentage);

        // Tạo ProductConsumption với totalQuantity của Lot
        MaterialConsumptionResult.ProductMaterialConsumption productConsumption = new MaterialConsumptionResult.ProductMaterialConsumption();
        productConsumption.setProductId(product.getId());
        productConsumption.setProductCode(product.getCode());
        productConsumption.setProductName(product.getName());
        productConsumption.setPlannedQuantity(lotTotalQuantity); // Sử dụng totalQuantity của Lot
        productConsumption.setBomVersion(activeBom.getVersion());
        java.util.List<MaterialConsumptionResult.MaterialConsumptionDetail> materialDetails = new java.util.ArrayList<>();

        // Map để tổng hợp nguyên vật liệu
        java.util.Map<Long, MaterialConsumptionResult.MaterialSummary> materialSummaryMap = new java.util.HashMap<>();

        // Tính toán nguyên vật liệu dựa trên BOM và totalQuantity của Lot
        for (BomDetail bomDetail : bomDetails) {
            Material material = bomDetail.getMaterial();
            BigDecimal materialQuantityPerUnit = bomDetail.getQuantity();
            
            // Tính theo totalQuantity của Lot (đã gộp)
            BigDecimal basicMaterialQuantity = materialQuantityPerUnit.multiply(lotTotalQuantity);
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
            
            // Tổng hợp vào MaterialSummary
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
        result.setProductConsumptions(java.util.List.of(productConsumption)); // Chỉ có 1 product trong Lot
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

}
