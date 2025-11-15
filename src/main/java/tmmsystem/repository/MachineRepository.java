package tmmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tmmsystem.entity.Machine;

public interface MachineRepository extends JpaRepository<Machine, Long>, JpaSpecificationExecutor<Machine> {
    boolean existsByCode(String code);
}


