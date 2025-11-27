package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "machine_assignment", indexes = {
        @Index(name = "idx_machine_assignment_unique", columnList = "machine_id, production_stage_id", unique = true),
        @Index(name = "idx_machine_assignment_plan_stage", columnList = "plan_stage_id"),
        @Index(name = "idx_machine_assignment_time", columnList = "machine_id, assigned_at")
})
@Getter
@Setter
public class MachineAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id")
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_stage_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private ProductionStage productionStage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_stage_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private ProductionPlanStage planStage;

    @Column(name = "reservation_type", length = 20, nullable = false)
    private String reservationType = "PRODUCTION"; // PRODUCTION, PLAN

    @Column(name = "reservation_status", length = 20, nullable = false)
    private String reservationStatus = "ACTIVE"; // ACTIVE, RELEASED

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "released_at")
    private Instant releasedAt;
}
