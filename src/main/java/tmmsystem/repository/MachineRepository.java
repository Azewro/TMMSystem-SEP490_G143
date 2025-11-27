package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import tmmsystem.entity.Machine;

import java.util.List;

public interface MachineRepository extends JpaRepository<Machine, Long>, JpaSpecificationExecutor<Machine> {
    boolean existsByCode(String code);

    List<Machine> findByTypeAndStatus(String type, String status);

    @Modifying
    @Query("UPDATE Machine m SET m.status = :status WHERE m.type = :type")
    void updateStatusByType(String type, String status);

    List<Machine> findByType(String type);

    @Modifying
    @Query("UPDATE Machine m SET m.status = 'AVAILABLE'")
    void resetAllMachineStatuses();
}
