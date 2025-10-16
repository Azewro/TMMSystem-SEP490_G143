package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tmmsystem.common.BaseEntity;

import java.time.Instant;

@Entity
@Table(name = "machine")
@Getter
@Setter
public class Machine extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(length = 20)
    private String status = "AVAILABLE";

    @Column(length = 100)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String specifications;

    @Column(name = "last_maintenance_at")
    private Instant lastMaintenanceAt;

    @Column(name = "next_maintenance_at")
    private Instant nextMaintenanceAt;

    @Column(name = "maintenance_interval_days")
    private Integer maintenanceIntervalDays = 90;
}
