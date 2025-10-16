// entity/TechnicalSheet.java
package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tmmsystem.common.BaseEntity;

import java.time.Instant;

@Entity
@Table(name = "technical_sheet")
@Getter
@Setter
public class TechnicalSheet extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_order_id", nullable = false, unique = true)
    private ProductionOrder productionOrder;

    @Column(name = "sheet_number", nullable = false, unique = true, length = 50)
    private String sheetNumber; // TECH-YYYYMMDD-XXX

    // Yarn specifications (JSON format)
    @Column(name = "yarn_specifications", columnDefinition = "TEXT")
    private String yarnSpecifications; // JSON: {"yarn_count": "20s, 40s", "yarn_type": "100% Cotton", ...}

    // Machine settings per stage (JSON format)
    @Column(name = "machine_settings", columnDefinition = "TEXT")
    private String machineSettings; // JSON: {"warping": {...}, "weaving": {...}, "dyeing": {...}}

    // Quality standards (JSON format)
    @Column(name = "quality_standards", columnDefinition = "TEXT")
    private String qualityStandards; // JSON: {"weight_tolerance": "±5%", "color_fastness": "Grade 4", ...}

    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions; // Any special handling requirements

    // Audit fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy; // Technical Department

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;
}
