package tmmsystem.dto.production;

import java.math.BigDecimal;
import java.time.Instant;

public class StageTrackingDto {
    private Long id;
    private String action;
    private BigDecimal quantityCompleted;
    private String notes;
    private Instant timestamp;
    private String operatorName;
    private Boolean isRework;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public BigDecimal getQuantityCompleted() {
        return quantityCompleted;
    }

    public void setQuantityCompleted(BigDecimal quantityCompleted) {
        this.quantityCompleted = quantityCompleted;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public Boolean getIsRework() {
        return isRework;
    }

    public void setIsRework(Boolean isRework) {
        this.isRework = isRework;
    }
}
