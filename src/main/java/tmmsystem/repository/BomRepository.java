// repository/BomRepository.java
package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tmmsystem.entity.Bom;

import java.util.List;
import java.util.Optional;

@Repository
public interface BomRepository extends JpaRepository<Bom, Long> {

    List<Bom> findByProductId(Long productId);

    @Query("SELECT b FROM Bom b WHERE b.product.id = :productId AND b.active = true")
    Optional<Bom> findActiveByProductId(@Param("productId") Long productId);

    Optional<Bom> findByProductIdAndVersion(Long productId, String version);

    boolean existsByProductIdAndVersion(Long productId, String version);
}
