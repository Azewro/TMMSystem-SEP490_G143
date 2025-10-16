// repository/TechnicalSheetRepository.java
package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tmmsystem.entity.TechnicalSheet;

import java.util.Optional;

@Repository
public interface TechnicalSheetRepository extends JpaRepository<TechnicalSheet, Long> {

    Optional<TechnicalSheet> findByProductionOrderId(Long productionOrderId);

    Optional<TechnicalSheet> findBySheetNumber(String sheetNumber);

    boolean existsByProductionOrderId(Long productionOrderId);

    boolean existsBySheetNumber(String sheetNumber);
}
