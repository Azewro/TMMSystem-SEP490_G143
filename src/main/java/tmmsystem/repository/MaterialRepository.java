package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tmmsystem.entity.Material;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    boolean existsByCode(String code);

    java.util.Optional<Material> findByCode(String code);
}
