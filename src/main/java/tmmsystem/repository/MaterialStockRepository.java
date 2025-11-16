package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tmmsystem.entity.MaterialStock;

import java.util.List;

public interface MaterialStockRepository extends JpaRepository<MaterialStock, Long>, JpaSpecificationExecutor<MaterialStock> {
    List<MaterialStock> findByMaterialId(Long materialId);
    List<MaterialStock> findByMaterialIdAndLocation(Long materialId, String location);
}