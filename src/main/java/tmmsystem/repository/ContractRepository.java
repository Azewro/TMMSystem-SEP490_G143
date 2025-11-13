package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tmmsystem.entity.Contract;

import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    boolean existsByContractNumber(String contractNumber);
    List<Contract> findByStatus(String status);

    @Query("SELECT c FROM Contract c WHERE c.status='APPROVED' AND NOT EXISTS (SELECT p FROM ProductionPlan p WHERE p.contract.id = c.id)")
    List<Contract> findApprovedWithoutPlan();

    // NEW: approved contracts not yet linked to any production lot order
    @Query("SELECT c FROM Contract c WHERE c.status='APPROVED' AND NOT EXISTS (SELECT lo FROM ProductionLotOrder lo WHERE lo.contract.id = c.id)")
    List<Contract> findApprovedWithoutLot();

    // NEW: find contract by quotation id
    Contract findFirstByQuotation_Id(Long quotationId);

    // existing approvals queries (kept for future use)
    List<Contract> findBySalesApprovedBy_Id(Long salesUserId);
    List<Contract> findByPlanningApprovedBy_Id(Long planningUserId);

    // NEW: assignment queries as requested
    List<Contract> findByAssignedSales_Id(Long salesUserId);
    List<Contract> findByAssignedPlanning_Id(Long planningUserId);
}
