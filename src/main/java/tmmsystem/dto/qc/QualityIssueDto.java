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

    private String stageStatus;

    public String getStageStatus() {
        return stageStatus;
    }

    public void setStageStatus(String stageStatus) {
        this.stageStatus = stageStatus;
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

    private String technicalNotes;

    public String getTechnicalNotes() {
        return technicalNotes;
    }

    public void setTechnicalNotes(String technicalNotes) {
        this.technicalNotes = technicalNotes;
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

    private tmmsystem.dto.execution.MaterialRequisitionDto materialRequisition;

    public tmmsystem.dto.execution.MaterialRequisitionDto getMaterialRequisition() {
        return materialRequisition;
    }

    public void setMaterialRequisition(tmmsystem.dto.execution.MaterialRequisitionDto materialRequisition) {
        this.materialRequisition = materialRequisition;
    }

    // NEW: Attempt tracking - calculated dynamically based on createdAt order
    private Integer attemptNumber; // 1, 2, 3...
    private String attemptLabel; // "Lỗi lần 1", "Lỗi lần 2"...

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public String getAttemptLabel() {
        return attemptLabel;
    }

    public void setAttemptLabel(String attemptLabel) {
        this.attemptLabel = attemptLabel;
    }

    // Leader assigned to the stage
    private String leaderName;

    public String getLeaderName() {
        return leaderName;
    }

    public void setLeaderName(String leaderName) {
        this.leaderName = leaderName;
    }
}
