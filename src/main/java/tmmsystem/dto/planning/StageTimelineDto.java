package tmmsystem.dto.planning;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class StageTimelineDto {
    private String stageType;
    private LocalDateTime start;
    private LocalDateTime end;
}

