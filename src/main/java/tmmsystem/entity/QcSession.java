package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "qc_session", indexes = {
        @Index(name = "idx_qc_session_stage", columnList = "production_stage_id"),
        @Index(name = "idx_qc_session_status", columnList = "status"),
        @Index(name = "idx_qc_session_result", columnList = "overall_result")
})
@Getter @Setter
public class QcSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_stage_id")
    private ProductionStage productionStage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "started_by_id")
    private User startedBy;

    @Column(name = "status", length = 20)
    private String status = "IN_PROGRESS"; // IN_PROGRESS, SUBMITTED

    @Column(name = "overall_result", length = 20)
    private String overallResult; // PASS / FAIL

    @Column(columnDefinition = "text")
    private String notes; // mô tả chung của phiên kiểm tra

    @CreationTimestamp
    @Column(name = "started_at", updatable = false)
    private Instant startedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;
}

