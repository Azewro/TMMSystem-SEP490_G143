// repository/UserRepository.java
package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import tmmsystem.entity.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    List<User> findByRoleName(String roleName);

    List<User> findByRoleNameIgnoreCase(String roleName);

    Optional<User> findByEmployeeCode(String employeeCode);

    @Query(value = "SELECT MAX(CAST(SUBSTRING_INDEX(employee_code, '-', -1) AS UNSIGNED)) FROM `user` WHERE employee_code LIKE CONCAT('EMP-', DATE_FORMAT(UTC_TIMESTAMP(), '%Y%m'), '-%')", nativeQuery = true)
    Integer findMaxEmployeeSeqForCurrentMonth();
}
