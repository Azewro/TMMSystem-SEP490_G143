package tmmsystem.mapper;

import org.springframework.stereotype.Component;
import tmmsystem.dto.inventory.*;
import tmmsystem.entity.*;

@Component
public class InventoryMapper {
    public MaterialStockDto toDto(MaterialStock e) {
        if (e == null) return null;
        MaterialStockDto dto = new MaterialStockDto();
        dto.setId(e.getId());
        if (e.getMaterial() != null) {
            dto.setMaterialId(e.getMaterial().getId());
            dto.setMaterialCode(e.getMaterial().getCode());
            dto.setMaterialName(e.getMaterial().getName());
        }
        dto.setQuantity(e.getQuantity());
        dto.setUnit(e.getUnit());
        dto.setUnitPrice(e.getUnitPrice());
        dto.setLocation(e.getLocation());
        dto.setBatchNumber(e.getBatchNumber());
        dto.setReceivedDate(e.getReceivedDate());
        dto.setExpiryDate(e.getExpiryDate());
        dto.setLastUpdatedAt(e.getLastUpdatedAt());
        return dto;
    }

    public MaterialTransactionDto toDto(MaterialTransaction e) {
        if (e == null) return null;
        MaterialTransactionDto dto = new MaterialTransactionDto();
        dto.setId(e.getId());
        dto.setMaterialId(e.getMaterial() != null ? e.getMaterial().getId() : null);
        dto.setTransactionType(e.getTransactionType());
        dto.setQuantity(e.getQuantity());
        dto.setUnit(e.getUnit());
        dto.setReferenceType(e.getReferenceType());
        dto.setReferenceId(e.getReferenceId());
        dto.setBatchNumber(e.getBatchNumber());
        dto.setLocation(e.getLocation());
        dto.setNotes(e.getNotes());
        dto.setCreatedById(e.getCreatedBy() != null ? e.getCreatedBy().getId() : null);
        dto.setCreatedAt(e.getCreatedAt());
        return dto;
    }

    public FinishedGoodsStockDto toDto(FinishedGoodsStock e) {
        if (e == null) return null;
        FinishedGoodsStockDto dto = new FinishedGoodsStockDto();
        dto.setId(e.getId());
        dto.setProductId(e.getProduct() != null ? e.getProduct().getId() : null);
        dto.setQuantity(e.getQuantity());
        dto.setUnit(e.getUnit());
        dto.setLocation(e.getLocation());
        dto.setBatchNumber(e.getBatchNumber());
        dto.setProductionDate(e.getProductionDate());
        dto.setQualityGrade(e.getQualityGrade());
        dto.setQcInspectionId(e.getQcInspection() != null ? e.getQcInspection().getId() : null);
        dto.setLastUpdatedAt(e.getLastUpdatedAt());
        return dto;
    }

    public FinishedGoodsTransactionDto toDto(FinishedGoodsTransaction e) {
        if (e == null) return null;
        FinishedGoodsTransactionDto dto = new FinishedGoodsTransactionDto();
        dto.setId(e.getId());
        dto.setProductId(e.getProduct() != null ? e.getProduct().getId() : null);
        dto.setTransactionType(e.getTransactionType());
        dto.setQuantity(e.getQuantity());
        dto.setUnit(e.getUnit());
        dto.setReferenceType(e.getReferenceType());
        dto.setReferenceId(e.getReferenceId());
        dto.setBatchNumber(e.getBatchNumber());
        dto.setLocation(e.getLocation());
        dto.setQualityGrade(e.getQualityGrade());
        dto.setNotes(e.getNotes());
        dto.setCreatedById(e.getCreatedBy() != null ? e.getCreatedBy().getId() : null);
        dto.setCreatedAt(e.getCreatedAt());
        return dto;
    }
}


