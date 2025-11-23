package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "quality_issue", indexes = {
        @Index(name = "idx_quality_issue_stage", columnList = "production_stage_id"),
        @Index(name = "idx_quality_issue_order", columnList = "production_order_id"),
        @Index(name = "idx_quality_issue_status", columnList = "status"),
        @Index(name = "idx_quality_issue_severity", columnList = "severity")
})
@Getter @Setter
public class QualityIssue {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_stage_id")
    private ProductionStage productionStage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_order_id")
    private ProductionOrder productionOrder;

    @Column(name = "severity", length = 20)
    private String severity; // MINOR / MAJOR

    @Column(name = "issue_type", length = 30)
    private String issueType; // REWORK / MATERIAL_REQUEST

    @Column(name = "status", length = 20)
    private String status = "PENDING"; // PENDING / PROCESSED

    @Column(columnDefinition = "text")
    private String description; // mô tả lỗi, lý do

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_id")
    private User processedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "material_needed")
    private Boolean materialNeeded = false; // NEW: lỗi nặng cần cấp sợi
}
