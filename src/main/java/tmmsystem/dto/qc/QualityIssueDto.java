package tmmsystem.dto.qc;

public class QualityIssueDto {
    private Long id;
    private String severity;
    private String issueType;
    private String description;
    private String status;
    private Long stageId;
    private String stageType;
    private Long orderId;
    private String poNumber;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(Long stageId) {
        this.stageId = stageId;
    }

    public String getStageType() {
        return stageType;
    }

    public void setStageType(String stageType) {
        this.stageType = stageType;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    private java.time.Instant createdAt;

    public java.time.Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.Instant createdAt) {
        this.createdAt = createdAt;
    }

    private String productName;
    private String size;

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    private String stageName;
    private String batchNumber;
    private String reportedBy;
    private String issueDescription;

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public String getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(String reportedBy) {
        this.reportedBy = reportedBy;
    }

    public String getIssueDescription() {
        return issueDescription;
    }

    public void setIssueDescription(String issueDescription) {
        this.issueDescription = issueDescription;
    }

    private String evidencePhoto;

    public String getEvidencePhoto() {
        return evidencePhoto;
    }

    public void setEvidencePhoto(String evidencePhoto) {
        this.evidencePhoto = evidencePhoto;
    }

    private java.util.List<QcInspectionDto> inspections;

    public java.util.List<QcInspectionDto> getInspections() {
        return inspections;
    }

    public void setInspections(java.util.List<QcInspectionDto> inspections) {
        this.inspections = inspections;
    }

    private Integer reworkProgress;
    private java.util.List<tmmsystem.dto.production.StageTrackingDto> reworkHistory;

    public Integer getReworkProgress() {
        return reworkProgress;
    }

    public void setReworkProgress(Integer reworkProgress) {
        this.reworkProgress = reworkProgress;
    }

    public java.util.List<tmmsystem.dto.production.StageTrackingDto> getReworkHistory() {
        return reworkHistory;
    }

    public void setReworkHistory(java.util.List<tmmsystem.dto.production.StageTrackingDto> reworkHistory) {
        this.reworkHistory = reworkHistory;
    }
}
