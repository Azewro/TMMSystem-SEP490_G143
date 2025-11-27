package tmmsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.entity.Machine;
import tmmsystem.repository.MachineRepository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Map;

@Service
public class MachineService {
    private final MachineRepository repository;
    private final tmmsystem.repository.ProductionStageRepository stageRepository;
    private final tmmsystem.repository.MachineAssignmentRepository assignmentRepository;
    private static final Map<String, String> STAGE_TYPE_ALIASES = Map.ofEntries(
            Map.entry("WARPING", "CUONG_MAC"),
            Map.entry("CUONG_MAC", "WARPING"),
            Map.entry("WEAVING", "DET"),
            Map.entry("DET", "WEAVING"),
            Map.entry("DYEING", "NHUOM"),
            Map.entry("NHUOM", "DYEING"),
            Map.entry("CUTTING", "CAT"),
            Map.entry("CAT", "CUTTING"),
            Map.entry("HEMMING", "MAY"),
            Map.entry("MAY", "HEMMING"),
            Map.entry("PACKAGING", "DONG_GOI"),
            Map.entry("DONG_GOI", "PACKAGING"));

    public MachineService(MachineRepository repository,
            tmmsystem.repository.ProductionStageRepository stageRepository,
            tmmsystem.repository.MachineAssignmentRepository assignmentRepository) {
        this.repository = repository;
        this.stageRepository = stageRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public List<Machine> findAll() {
        return repository.findAll();
    }

    public Page<Machine> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Machine> findAll(Pageable pageable, String search, String type, String status) {
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.trim().toLowerCase();
            return repository.findAll((root, query, cb) -> {
                var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

                // Search predicate
                var searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("code")), "%" + searchLower + "%"),
                        cb.like(cb.lower(root.get("name")), "%" + searchLower + "%"),
                        cb.like(cb.lower(root.get("location")), "%" + searchLower + "%"));
                predicates.add(searchPredicate);

                // Type filter
                if (type != null && !type.trim().isEmpty()) {
                    predicates.add(cb.equal(root.get("type"), type));
                }

                // Status filter
                if (status != null && !status.trim().isEmpty()) {
                    predicates.add(cb.equal(root.get("status"), status));
                }

                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            }, pageable);
        } else {
            // No search, just filters
            if (type != null || status != null) {
                return repository.findAll((root, query, cb) -> {
                    var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

                    if (type != null && !type.trim().isEmpty()) {
                        predicates.add(cb.equal(root.get("type"), type));
                    }

                    if (status != null && !status.trim().isEmpty()) {
                        predicates.add(cb.equal(root.get("status"), status));
                    }

                    return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
                }, pageable);
            } else {
                return repository.findAll(pageable);
            }
        }
    }

    public Machine findById(Long id) {
        return repository.findById(id).orElseThrow();
    }

    @Transactional
    public Machine create(Machine m) {
        return repository.save(m);
    }

    @Transactional
    public Machine update(Long id, Machine updated) {
        Machine existing = repository.findById(id).orElseThrow();
        existing.setCode(updated.getCode());
        existing.setName(updated.getName());
        existing.setType(updated.getType());
        existing.setStatus(updated.getStatus());
        existing.setSpecifications(updated.getSpecifications());
        existing.setLastMaintenanceAt(updated.getLastMaintenanceAt());
        existing.setNextMaintenanceAt(updated.getNextMaintenanceAt());
        return existing;
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * Migration/Sync function:
     * Resets all machines to AVAILABLE, then sets machines to IN_USE
     * if there are any active stages of that type.
     */
    @Transactional
    public void syncMachineStatuses() {
        // 1. Reset all to AVAILABLE
        repository.resetAllMachineStatuses();

        // 2. Find all IN_PROGRESS stages
        List<String> activeStatuses = List.of("IN_PROGRESS");
        List<tmmsystem.entity.ProductionStage> activeStages = stageRepository.findByExecutionStatusIn(activeStatuses);

        // 3. Collect active stage types
        java.util.Set<String> activeTypes = activeStages.stream()
                .map(tmmsystem.entity.ProductionStage::getStageType)
                .collect(java.util.stream.Collectors.toSet());

        // 4. Update machines for active types
        for (String type : activeTypes) {
            repository.updateStatusByType(type, "IN_USE");
            // Also update for alias if exists
            if (STAGE_TYPE_ALIASES.containsKey(type)) {
                repository.updateStatusByType(STAGE_TYPE_ALIASES.get(type), "IN_USE");
            }
        }
    }

    public org.springframework.data.domain.Page<tmmsystem.entity.MachineAssignment> getAssignments(Long machineId,
            org.springframework.data.domain.Pageable pageable) {
        return assignmentRepository.findByMachineIdOrderByAssignedAtDesc(machineId, pageable);
    }

    @Transactional
    public void syncPastAssignments() {
        List<tmmsystem.entity.ProductionStage> allStages = stageRepository.findAll();
        for (tmmsystem.entity.ProductionStage stage : allStages) {
            // Skip if stage hasn't started or type is null
            if (stage.getStartAt() == null || stage.getStageType() == null) {
                continue;
            }

            // Check if assignments already exist
            List<tmmsystem.entity.MachineAssignment> existing = assignmentRepository
                    .findByProductionStageId(stage.getId());
            if (!existing.isEmpty()) {
                continue;
            }

            // Find machines by type
            List<Machine> machines = repository.findByType(stage.getStageType());
            // Also check aliases
            if (STAGE_TYPE_ALIASES.containsKey(stage.getStageType())) {
                machines.addAll(repository.findByType(STAGE_TYPE_ALIASES.get(stage.getStageType())));
            }

            for (Machine m : machines) {
                tmmsystem.entity.MachineAssignment ma = new tmmsystem.entity.MachineAssignment();
                ma.setMachine(m);
                ma.setProductionStage(stage);
                ma.setAssignedAt(stage.getStartAt());
                ma.setReservationType("PRODUCTION");

                // Determine status
                boolean isReleased = stage.getCompleteAt() != null ||
                        "COMPLETED".equals(stage.getExecutionStatus()) ||
                        "QC_PASSED".equals(stage.getExecutionStatus());

                if (isReleased) {
                    ma.setReservationStatus("RELEASED");
                    ma.setReleasedAt(stage.getCompleteAt() != null ? stage.getCompleteAt() : stage.getUpdatedAt());
                } else {
                    ma.setReservationStatus("ACTIVE");
                }

                assignmentRepository.save(ma);
            }
        }
    }
}
