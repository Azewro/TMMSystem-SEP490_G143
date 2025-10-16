package tmmsystem.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tmmsystem.common.BaseEntity;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "customer")
@Getter
@Setter
public class Customer extends BaseEntity {

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "tax_code", length = 50)
    private String taxCode;

    @Column(name = "business_license", length = 100)
    private String businessLicense;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "contact_person", nullable = false, length = 150)
    private String contactPerson;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(length = 100)
    private String position;

    @JsonIgnore
    @Column(length = 255)
    private String password;

    @Column(name = "is_verified")
    private Boolean verified = false;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "password_reset_token", length = 255)
    private String passwordResetToken;

    @Column(name = "password_reset_expires_at")
    private Instant passwordResetExpiresAt;

    @Column(name = "additional_contacts", columnDefinition = "TEXT")
    private String additionalContacts;

    @Column(name = "customer_type", length = 20)
    private String customerType = "B2B";

    @Column(length = 100)
    private String industry;

    @Column(name = "credit_limit", precision = 15, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(name = "payment_terms", length = 100)
    private String paymentTerms;

    @Column(name = "is_active")
    private Boolean active = true;

    @Column(name = "registration_type", length = 20)
    private String registrationType = "SALES_CREATED";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}
