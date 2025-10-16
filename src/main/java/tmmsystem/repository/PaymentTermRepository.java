// repository/PaymentTermRepository.java
package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tmmsystem.entity.PaymentTerm;

import java.util.List;

@Repository
public interface PaymentTermRepository extends JpaRepository<PaymentTerm, Long> {

    List<PaymentTerm> findByContractIdOrderByTermSequence(Long contractId);

    boolean existsByContractIdAndTermSequence(Long contractId, Integer termSequence);
}
