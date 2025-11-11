package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "stage_risk_attachment",
        indexes = { @Index(name = "idx_risk_attachment", columnList = "risk_assessment_id") }
)
@Getter @Setter
public class StageRiskAttachment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "risk_assessment_id")
    private StageRiskAssessment riskAssessment;

    @Column(name = "file_url", length = 500, nullable = false)
    private String fileUrl;

    @Column(name = "caption", length = 255)
    private String caption;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}

