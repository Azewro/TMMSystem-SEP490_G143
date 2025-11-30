package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "stage_tracking", indexes = {
        @Index(name = "idx_stage_tracking_stage_time", columnList = "production_stage_id, timestamp"),
        @Index(name = "idx_stage_tracking_operator_time", columnList = "operator_id, timestamp")
})
@Getter
@Setter
public class StageTracking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_stage_id")
    private ProductionStage productionStage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id")
    private User operator;

    @Column(name = "action", length = 20, nullable = false)
    private String action; // START, PAUSE, RESUME, COMPLETE, REPORT_ISSUE

    @Column(name = "quantity_completed", precision = 10, scale = 2)
    private BigDecimal quantityCompleted;

    @Column(columnDefinition = "text")
    private String notes;

    @CreationTimestamp
    @Column(name = "timestamp", updatable = false)
    private Instant timestamp;

    @Column(name = "evidence_photo_url", length = 500)
    private String evidencePhotoUrl;

    @Column(name = "is_rework")
    private Boolean isRework = false;
}
