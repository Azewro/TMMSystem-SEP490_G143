package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity @Table(name = "customer",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"email"}),
                @UniqueConstraint(columnNames = {"phone_number"}),
                @UniqueConstraint(columnNames = {"customer_code"})
        }
)
@Getter @Setter
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
    @Column(name = "customer_code", length = 30, nullable = false)
    private String customerCode; // CUS-YYYYMM-XXX
    @Column(name = "company_name", nullable = true, length = 255)
    private String companyName;
    @Column(name = "tax_code", length = 50)
    private String taxCode;
    @Column(name = "business_license", length = 100)
    private String businessLicense;
    @Column(columnDefinition = "text")
    private String address;

    // Primary Contact
    @Column(name = "contact_person", length = 150)
    private String contactPerson;
    @Column(nullable = false, length = 150)
    private String email; // Login email for customer portal
    @Column(name = "phone_number", length = 30)
    private String phoneNumber;
    @Column(length = 100)
    private String position; // Manager, Buyer, Director, etc.

    // Portal Access
    @Column(name = "is_verified")
    private Boolean verified = false; // Email verified?
    @Column(name = "last_login_at")
    private java.time.Instant lastLoginAt;

    // NEW: password (nullable, cho khách hàng chưa từng được cấp mật khẩu)
    @Column(name = "password", length = 255, nullable = true)
    private String password; // BCrypt hash hoặc null nếu chưa cấp
    // NEW: flag bắt buộc đổi mật khẩu lần đầu sau khi cấp mật khẩu tạm
    @Column(name = "force_password_change")
    private Boolean forcePasswordChange = false;

    // Additional contacts (JSON)
    @Column(name = "additional_contacts", columnDefinition = "json")
    private String additionalContacts;

    // Business Information
    @Column(name = "customer_type", length = 20)
    private String customerType = "B2B"; // B2B, B2C
    @Column(length = 100)
    private String industry; // Hotel, Hospital, Spa, Retail
    @Column(name = "credit_limit", precision = 15, scale = 2)
    private java.math.BigDecimal creditLimit = java.math.BigDecimal.ZERO;
    @Column(name = "payment_terms", length = 100)
    private String paymentTerms; // 30% deposit, 70% upon delivery

    // Status & registration
    @Column(name = "is_active")
    private Boolean active = true;
    @Column(name = "registration_type", length = 20)
    private String registrationType = "SALES_CREATED";

    // Sales in charge
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_in_charge_id")
    private User salesInCharge;

    // Audit
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}
