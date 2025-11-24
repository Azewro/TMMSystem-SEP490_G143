package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "qc_inspection", indexes = {
        @Index(name = "idx_qc_inspection_stage", columnList = "production_stage_id"),
        @Index(name = "idx_qc_inspection_inspector_time", columnList = "inspector_id, inspected_at"),
        @Index(name = "idx_qc_inspection_result", columnList = "result")
})
@Getter
@Setter
public class QcInspection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_stage_id")
    private ProductionStage productionStage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "qc_checkpoint_id")
    private QcCheckpoint qcCheckpoint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspector_id")
    private User inspector;

    @Column(name = "sample_size")
    private Integer sampleSize;

    @Column(name = "pass_count")
    private Integer passCount;

    @Column(name = "fail_count")
    private Integer failCount;

    @Column(name = "result", length = 20, nullable = false)
    private String result; // PASS, FAIL, CONDITIONAL_PASS

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "photo_url")
    private String photoUrl;

    @CreationTimestamp
    @Column(name = "inspected_at", updatable = false)
    private Instant inspectedAt;
}
