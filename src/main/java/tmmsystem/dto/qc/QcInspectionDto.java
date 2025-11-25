package tmmsystem.dto.qc;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class QcInspectionDto {
    private Long id;
    private Long productionStageId;
    private Long qcCheckpointId;
    private Long inspectorId;
    private Integer sampleSize;
    private Integer passCount;
    private Integer failCount;
    private String result;
    private String notes;
    private String photoUrl;
    private String checkpointName;
    private Instant inspectedAt;
}
