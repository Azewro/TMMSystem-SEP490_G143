package tmmsystem.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class MaterialRequisitionResponseDto {
    private Long id;
    private String requisitionNumber;
    private String status;
    private Instant requestedAt;
    private Instant approvedAt;
    private String notes;
    private BigDecimal quantityRequested;
    private BigDecimal quantityApproved;

    // Enhanced display fields
    private String stageName;
    private String requesterName;
    private String approverName;

    // Defect info
    private SourceIssueDto sourceIssue;
    private List<DefectDetailDto> defectDetails;

    // Line items
    private List<MaterialRequisitionDetailDto> details;

    @Data
    public static class SourceIssueDto {
        private String description;
        private String severity; // minor, major
        private String evidencePhoto;
    }

    @Data
    public static class DefectDetailDto {
        private String criteriaName; // Checkpoint name
        private String description; // Notes/Defect desc
        private String photoUrl;
    }
}
