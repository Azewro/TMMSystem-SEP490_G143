package tmmsystem.mapper;

import org.springframework.stereotype.Component;
import tmmsystem.dto.sales.QuotationDetailDto;
import tmmsystem.dto.sales.QuotationDto;
import tmmsystem.entity.*;

@Component
public class QuotationMapper {
    public QuotationDto toDto(Quotation entity) {
        if (entity == null) return null;
        QuotationDto dto = new QuotationDto();
        dto.setId(entity.getId());
        dto.setQuotationNumber(entity.getQuotationNumber());
        dto.setRfqId(entity.getRfq() != null ? entity.getRfq().getId() : null);
        dto.setCustomerId(entity.getCustomer() != null ? entity.getCustomer().getId() : null);
        dto.setValidUntil(entity.getValidUntil());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setStatus(entity.getStatus());
        dto.setIsAccepted(entity.getAccepted());
        dto.setIsCanceled(entity.getCanceled());
        dto.setCapacityCheckedById(entity.getCapacityCheckedBy() != null ? entity.getCapacityCheckedBy().getId() : null);
        dto.setCapacityCheckedAt(entity.getCapacityCheckedAt());
        dto.setCapacityCheckNotes(entity.getCapacityCheckNotes());
        dto.setAssignedSalesId(entity.getAssignedSales() != null ? entity.getAssignedSales().getId() : null);
        dto.setAssignedPlanningId(entity.getAssignedPlanning() != null ? entity.getAssignedPlanning().getId() : null);
        dto.setCreatedById(entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null);
        dto.setApprovedById(entity.getApprovedBy() != null ? entity.getApprovedBy().getId() : null);
        dto.setSentAt(entity.getSentAt());
        dto.setFilePath(entity.getFilePath());
        // map contact snapshots
        dto.setContactPersonSnapshot(entity.getContactPersonSnapshot());
        dto.setContactEmailSnapshot(entity.getContactEmailSnapshot());
        dto.setContactPhoneSnapshot(entity.getContactPhoneSnapshot());
        dto.setContactAddressSnapshot(entity.getContactAddressSnapshot());
        dto.setContactMethod(entity.getContactMethod());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        if (entity.getDetails() != null) {
            dto.setDetails(entity.getDetails().stream().map(this::toDetailDto).collect(java.util.stream.Collectors.toList()));
        }
        return dto;
    }

    public QuotationDetailDto toDetailDto(QuotationDetail d) {
        if (d == null) return null;
        QuotationDetailDto dto = new QuotationDetailDto();
        dto.setId(d.getId());
        dto.setProductId(d.getProduct() != null ? d.getProduct().getId() : null);
        dto.setQuantity(d.getQuantity());
        dto.setUnit(d.getUnit());
        dto.setUnitPrice(d.getUnitPrice());
        dto.setTotalPrice(d.getTotalPrice());
        dto.setNoteColor(d.getNoteColor());
        dto.setDiscountPercentage(d.getDiscountPercentage());
        return dto;
    }
}
