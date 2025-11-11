package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "stage_risk_assessment",
        indexes = {
                @Index(name = "idx_risk_stage_status", columnList = "production_stage_id, status")
        }
)
@Getter @Setter
public class StageRiskAssessment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_stage_id")
    private ProductionStage productionStage;

    @Column(name = "severity", length = 10, nullable = false)
    private String severity; // MINOR / MAJOR

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "root_cause", columnDefinition = "text")
    private String rootCause;

    @Column(name = "solution_proposal", columnDefinition = "text")
    private String solutionProposal;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "OPEN"; // OPEN/IN_REVIEW/APPROVED/REJECTED/CLOSED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "impacted_delivery")
    private Boolean impactedDelivery;

    @Column(name = "proposed_new_date")
    private LocalDate proposedNewDate;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}

