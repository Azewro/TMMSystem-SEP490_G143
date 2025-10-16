// entity/Bom.java
package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tmmsystem.common.BaseEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bom", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"product_id", "version"})
})
@Getter
@Setter
public class Bom extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 20)
    private String version; // v1.0, v1.1, v2.0

    @Column(name = "version_notes", columnDefinition = "TEXT")
    private String versionNotes; // What changed in this version

    @Column(name = "is_active")
    private Boolean active = true; // Only one active BOM per product

    @Column(name = "effective_date")
    private LocalDate effectiveDate; // When this BOM became active

    @Column(name = "obsolete_date")
    private LocalDate obsoleteDate; // When this BOM was replaced

    // Audit fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy; // Technical Department

    // One-to-Many with BomDetail
    @OneToMany(mappedBy = "bom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BomDetail> details = new ArrayList<>();

    // Helper method
    public void addDetail(BomDetail detail) {
        details.add(detail);
        detail.setBom(this);
    }
}
