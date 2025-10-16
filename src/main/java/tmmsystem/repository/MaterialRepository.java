// repository/MaterialRepository.java
package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tmmsystem.entity.Material;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {

    Optional<Material> findByCode(String code);

    boolean existsByCode(String code);

    List<Material> findByType(String type);

    List<Material> findByActiveTrue();
}
