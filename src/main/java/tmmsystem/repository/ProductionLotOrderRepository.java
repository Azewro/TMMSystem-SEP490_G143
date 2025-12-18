package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tmmsystem.entity.ProductionLotOrder;
import java.util.List;

public interface ProductionLotOrderRepository extends JpaRepository<ProductionLotOrder, Long> {
    List<ProductionLotOrder> findByLotId(Long lotId);

    List<ProductionLotOrder> findByContractId(Long contractId);

    boolean existsByQuotationDetail_Id(Long quotationDetailId);
}
