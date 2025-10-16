// entity/Payment.java
package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tmmsystem.common.BaseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "payment")
@Getter
@Setter
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_term_id")
    private PaymentTerm paymentTerm; // Which term does this fulfill?

    @Column(name = "payment_type", nullable = false, length = 20)
    private String paymentType; // DEPOSIT, MILESTONE, FINAL, EXTRA

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod; // BANK_TRANSFER, CASH, CHECK, CREDIT_CARD

    @Column(name = "payment_reference", length = 100)
    private String paymentReference; // Bank transaction ID / Check number

    @Column(length = 20)
    private String status = "PENDING"; // PENDING, COMPLETED, FAILED, REFUNDED

    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    @Column(name = "receipt_file_path", length = 500)
    private String receiptFilePath; // S3/Firebase path to receipt PDF

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Audit fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy; // Who recorded this payment

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy; // Accountant verification

    @Column(name = "verified_at")
    private Instant verifiedAt;
}
