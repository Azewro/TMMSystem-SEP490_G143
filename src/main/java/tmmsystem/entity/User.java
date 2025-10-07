// entity/User.java
package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import tmmsystem.common.BaseEntity;

@Entity @Table(name = "user")
@Getter @Setter
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(length = 255)
    private String avatar;

    @Column(name = "is_active")
    private Boolean active = true;

    @Column(name = "is_verified")
    private Boolean verified = false;

    @Column(name = "reset_code", length = 12)
    private String resetCode;

    @Column(name = "reset_code_expires_at")
    private java.time.Instant resetCodeExpiresAt;

    @Column(name = "last_login_at")
    private java.time.Instant lastLoginAt;

    @Column(name = "deleted_at")
    private java.time.Instant deletedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

}
