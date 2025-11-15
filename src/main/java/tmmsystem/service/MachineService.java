package tmmsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.entity.Machine;
import tmmsystem.repository.MachineRepository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class MachineService {
    private final MachineRepository repository;

    public MachineService(MachineRepository repository) { this.repository = repository; }

    public List<Machine> findAll() { return repository.findAll(); }
    public Page<Machine> findAll(Pageable pageable) { return repository.findAll(pageable); }
    
    public Page<Machine> findAll(Pageable pageable, String search, String type, String status) {
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.trim().toLowerCase();
            return repository.findAll((root, query, cb) -> {
                var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                
                // Search predicate
                var searchPredicate = cb.or(
                    cb.like(cb.lower(root.get("code")), "%" + searchLower + "%"),
                    cb.like(cb.lower(root.get("name")), "%" + searchLower + "%"),
                    cb.like(cb.lower(root.get("location")), "%" + searchLower + "%")
                );
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
    
    public Machine findById(Long id) { return repository.findById(id).orElseThrow(); }

    @Transactional
    public Machine create(Machine m) { return repository.save(m); }

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

    public void delete(Long id) { repository.deleteById(id); }
}


