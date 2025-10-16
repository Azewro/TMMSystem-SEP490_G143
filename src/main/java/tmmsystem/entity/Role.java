package tmmsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tmmsystem.common.BaseEntity;

@Entity
@Table(name = "role")
@Getter
@Setter
public class Role extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String name;  // ADMIN, SALES, PLANNING, DIRECTOR, TECHNICAL, PRODUCTION, QA, WAREHOUSE

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active")
    private Boolean active = true;
}
