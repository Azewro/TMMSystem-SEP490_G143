// repository/PaymentRepository.java
package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tmmsystem.entity.Payment;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByContractIdOrderByPaymentDateDesc(Long contractId);

    List<Payment> findByStatus(String status);

    @Query("SELECT p FROM Payment p WHERE p.contract.id = :contractId AND p.status = :status")
    List<Payment> findByContractIdAndStatus(@Param("contractId") Long contractId, @Param("status") String status);
}
