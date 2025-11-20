package tmmsystem.dto.machine;

import lombok.Getter; import lombok.Setter;

import java.time.Instant;

@Getter @Setter
public class MachineAssignmentDto {
    private Long id;
    private Long machineId;
    private Long productionStageId;
    private Long planStageId;
    private Instant assignedAt;
    private Instant releasedAt;
    private String reservationType;
    private String reservationStatus;
}


