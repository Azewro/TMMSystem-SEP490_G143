package tmmsystem.dto.execution;

import lombok.Getter; import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter
public class StageTrackingDto {
    private Long id;
    private Long productionStageId;
    private Long operatorId;
    private String operatorName;
    private String action;
    private BigDecimal quantityCompleted;
    private String notes;
    private Instant timestamp;
    private String evidencePhotoUrl;
}
