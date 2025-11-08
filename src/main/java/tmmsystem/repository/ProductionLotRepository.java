package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tmmsystem.entity.ProductionLot;
import java.util.List;

public interface ProductionLotRepository extends JpaRepository<ProductionLot, Long> {
    boolean existsByLotCode(String lotCode);
    List<ProductionLot> findByStatus(String status);
}

