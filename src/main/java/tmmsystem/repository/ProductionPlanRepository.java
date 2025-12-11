package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tmmsystem.entity.ProductionPlan;

import java.util.List;
import java.util.Optional;

public interface ProductionPlanRepository extends JpaRepository<ProductionPlan, Long> {

    // Kiểm tra mã kế hoạch đã tồn tại
    boolean existsByPlanCode(String planCode);

    // Tìm kế hoạch theo hợp đồng
    List<ProductionPlan> findByContractId(Long contractId);

    // Tìm kế hoạch theo trạng thái
    List<ProductionPlan> findByStatus(ProductionPlan.PlanStatus status);

    // Tìm kế hoạch theo người tạo
    List<ProductionPlan> findByCreatedById(Long createdById);

    // Tìm kế hoạch theo người phê duyệt
    List<ProductionPlan> findByApprovedById(Long approvedById);

    // Tìm kế hoạch chờ phê duyệt
    @Query("SELECT pp FROM ProductionPlan pp WHERE pp.status = 'PENDING_APPROVAL' ORDER BY pp.createdAt ASC")
    List<ProductionPlan> findPendingApprovalPlans();

    // Tìm kế hoạch đã phê duyệt chưa tạo Production Order
    @Query("SELECT pp FROM ProductionPlan pp WHERE pp.status = 'APPROVED' AND NOT EXISTS " +
            "(SELECT po FROM ProductionOrder po WHERE po.contract.id = pp.contract.id)")
    List<ProductionPlan> findApprovedPlansNotConverted();

    // Tìm kế hoạch theo mã hợp đồng
    @Query("SELECT pp FROM ProductionPlan pp WHERE pp.contract.contractNumber = :contractNumber")
    Optional<ProductionPlan> findByContractNumber(@Param("contractNumber") String contractNumber);

    // Tìm kế hoạch theo lot
    List<ProductionPlan> findByLotId(Long lotId);

    // Tìm kế hoạch theo lot và phiên bản hiện tại
    List<ProductionPlan> findByLotIdAndCurrentVersionTrue(Long lotId);

    // Tìm kế hoạch theo mã kế hoạch
    Optional<ProductionPlan> findByPlanCode(String planCode);
}
