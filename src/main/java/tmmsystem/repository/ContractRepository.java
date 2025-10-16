// repository/ContractRepository.java
package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tmmsystem.entity.Contract;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    @Query("SELECT c FROM Contract c LEFT JOIN FETCH c.customer LEFT JOIN FETCH c.createdBy WHERE c.contractNumber = :contractNumber")
    Optional<Contract> findByContractNumber(@Param("contractNumber") String contractNumber);

    boolean existsByContractNumber(String contractNumber);

    List<Contract> findByCustomerId(Long customerId);

    List<Contract> findByStatus(String status);

    List<Contract> findByQuotationId(Long quotationId);
}
