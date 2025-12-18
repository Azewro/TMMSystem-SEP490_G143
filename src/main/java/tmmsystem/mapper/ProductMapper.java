package tmmsystem.mapper;

import org.springframework.stereotype.Component;
import tmmsystem.dto.product.*;
import tmmsystem.entity.*;

@Component
public class ProductMapper {
    public ProductDto toDto(Product e) {
        if (e == null)
            return null;
        ProductDto dto = new ProductDto();
        dto.setId(e.getId());
        dto.setCode(e.getCode());
        dto.setName(e.getName());
        dto.setDescription(e.getDescription());
        dto.setCategoryId(e.getCategory() != null ? e.getCategory().getId() : null);
        dto.setCategoryName(e.getCategory() != null ? e.getCategory().getName() : null);
        dto.setUnit(e.getUnit());
        dto.setStandardWeight(e.getStandardWeight());
        dto.setStandardDimensions(e.getStandardDimensions());
        dto.setBasePrice(e.getBasePrice());
        dto.setIsActive(e.getActive());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        return dto;
    }

    public MaterialDto toDto(Material e) {
        if (e == null)
            return null;
        MaterialDto dto = new MaterialDto();
        dto.setId(e.getId());
        dto.setCode(e.getCode());
        dto.setName(e.getName());
        dto.setType(e.getType());
        dto.setUnit(e.getUnit());
        dto.setReorderPoint(e.getReorderPoint());
        dto.setStandardCost(e.getStandardCost());
        dto.setIsActive(e.getActive());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        return dto;
    }

    public BomDto toDto(Bom e) {
        if (e == null)
            return null;
        BomDto dto = new BomDto();
        dto.setId(e.getId());
        dto.setProductId(e.getProduct() != null ? e.getProduct().getId() : null);
        dto.setVersion(e.getVersion());
        dto.setVersionNotes(e.getVersionNotes());
        dto.setIsActive(e.getActive());
        dto.setEffectiveDate(e.getEffectiveDate());
        dto.setObsoleteDate(e.getObsoleteDate());
        dto.setCreatedById(e.getCreatedBy() != null ? e.getCreatedBy().getId() : null);
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        return dto;
    }

    public BomDetailDto toDto(BomDetail e) {
        if (e == null)
            return null;
        BomDetailDto dto = new BomDetailDto();
        dto.setId(e.getId());
        dto.setBomId(e.getBom() != null ? e.getBom().getId() : null);
        dto.setMaterialId(e.getMaterial() != null ? e.getMaterial().getId() : null);
        dto.setQuantity(e.getQuantity());
        dto.setUnit(e.getUnit());
        dto.setStage(e.getStage());
        dto.setIsOptional(e.getOptional());
        dto.setNotes(e.getNotes());
        return dto;
    }
}
