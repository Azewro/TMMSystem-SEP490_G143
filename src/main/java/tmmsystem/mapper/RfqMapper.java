package tmmsystem.mapper;

import org.springframework.stereotype.Component;
import tmmsystem.dto.sales.RfqDetailDto;
import tmmsystem.dto.sales.RfqDto;
import tmmsystem.entity.*;

import java.util.stream.Collectors;

@Component
public class RfqMapper {
    public RfqDto toDto(Rfq entity) {
        if (entity == null) return null;
        RfqDto dto = new RfqDto();
        dto.setId(entity.getId());
        dto.setRfqNumber(entity.getRfqNumber());
        dto.setCustomerId(entity.getCustomer() != null ? entity.getCustomer().getId() : null);
        dto.setSourceType(entity.getSourceType());
        dto.setExpectedDeliveryDate(entity.getExpectedDeliveryDate());
        dto.setStatus(entity.getStatus());
        dto.setIsSent(entity.getSent());
        dto.setNotes(entity.getNotes());
        dto.setCreatedById(entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null);
        dto.setAssignedSalesId(entity.getAssignedSales() != null ? entity.getAssignedSales().getId() : null);
        dto.setAssignedPlanningId(entity.getAssignedPlanning() != null ? entity.getAssignedPlanning().getId() : null);
        dto.setApprovedById(entity.getApprovedBy() != null ? entity.getApprovedBy().getId() : null);
        dto.setApprovalDate(entity.getApprovalDate());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        // Contact mapping: prefer snapshots then fallback to customer
        String cp = entity.getContactPersonSnapshot();
        String ce = entity.getContactEmailSnapshot();
        String cph = entity.getContactPhoneSnapshot();
        String ca = entity.getContactAddressSnapshot();
        if (cp == null || ce == null || cph == null || ca == null) {
            Customer cust = entity.getCustomer();
            if (cp == null && cust != null) cp = cust.getContactPerson();
            if (ce == null && cust != null) ce = cust.getEmail();
            if (cph == null && cust != null) cph = cust.getPhoneNumber();
            if (ca == null && cust != null) ca = cust.getAddress();
        }
        dto.setContactPerson(cp);
        dto.setContactEmail(ce);
        dto.setContactPhone(cph);
        dto.setContactAddress(ca);
        dto.setContactMethod(entity.getContactMethod());

        if (entity.getDetails() != null) {
            dto.setDetails(entity.getDetails().stream().map(this::toDetailDto).collect(Collectors.toList()));
        }
        return dto;
    }

    public RfqDetailDto toDetailDto(RfqDetail d) {
        if (d == null) return null;
        RfqDetailDto dto = new RfqDetailDto();
        dto.setId(d.getId());
        dto.setProductId(d.getProduct() != null ? d.getProduct().getId() : null);
        dto.setQuantity(d.getQuantity());
        dto.setUnit(d.getUnit());
        dto.setNoteColor(d.getNoteColor());
        dto.setNotes(d.getNotes());
        return dto;
    }
}
