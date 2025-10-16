// entity/Rfq.java
package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tmmsystem.common.BaseEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rfq")
@Getter
@Setter
public class Rfq extends BaseEntity {

    @Column(name = "rfq_number", nullable = false, unique = true, length = 50)
    private String rfqNumber; // RFQ-YYYYMMDD-XXX

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(length = 20)
    private String status = "DRAFT"; // DRAFT, SENT, QUOTED, CANCELED

    @Column(name = "is_sent")
    private Boolean sent = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Audit fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy; // Sales person

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    // One-to-Many with RfqDetail
    @OneToMany(mappedBy = "rfq", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RfqDetail> details = new ArrayList<>();

    // Helper method
    public void addDetail(RfqDetail detail) {
        details.add(detail);
        detail.setRfq(this);
    }
}
