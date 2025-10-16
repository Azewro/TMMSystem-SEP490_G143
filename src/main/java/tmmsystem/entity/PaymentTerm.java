// entity/PaymentTerm.java
package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tmmsystem.common.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payment_term", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"contract_id", "term_sequence"})
})
@Getter
@Setter
public class PaymentTerm extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(name = "term_sequence", nullable = false)
    private Integer termSequence; // 1=Deposit, 2=Progress, 3=Final

    @Column(name = "term_name", nullable = false, length = 100)
    private String termName; // 30% Deposit, 50% After Production, 20% After Delivery

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage; // 30.00, 50.00, 20.00

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount; // contract.total_amount * percentage / 100

    @Column(name = "due_date")
    private LocalDate dueDate; // When payment is due

    @Column(columnDefinition = "TEXT")
    private String description;
}
