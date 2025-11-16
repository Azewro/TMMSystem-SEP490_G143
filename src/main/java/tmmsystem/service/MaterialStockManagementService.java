package tmmsystem.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.entity.MaterialStock;
import tmmsystem.repository.MaterialStockRepository;

import java.time.LocalDate;

@Service
public class MaterialStockManagementService {
    private final MaterialStockRepository repository;

    public MaterialStockManagementService(MaterialStockRepository repository) {
        this.repository = repository;
    }

    public Page<MaterialStock> findAll(Pageable pageable, String search, LocalDate receivedDate) {
        // Build specification for search and filter
        Specification<MaterialStock> spec = (root, query, cb) -> cb.conjunction();

        // Search: by material name and material code only
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.trim().toLowerCase();
            Specification<MaterialStock> searchSpec = (root, query, cb) -> {
                var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                
                // Search in material name
                predicates.add(cb.like(cb.lower(root.get("material").get("name")), "%" + searchLower + "%"));
                
                // Search in material code
                predicates.add(cb.like(cb.lower(root.get("material").get("code")), "%" + searchLower + "%"));
                
                return cb.or(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            };
            spec = spec.and(searchSpec);
        }

        // Filter by received date
        if (receivedDate != null) {
            Specification<MaterialStock> dateSpec = (root, query, cb) -> 
                cb.equal(root.get("receivedDate"), receivedDate);
            spec = spec.and(dateSpec);
        }

        return repository.findAll(spec, pageable);
    }

    public MaterialStock findById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Material stock not found with id: " + id));
    }

    @Transactional
    public MaterialStock create(MaterialStock materialStock) {
        return repository.save(materialStock);
    }

    @Transactional
    public MaterialStock update(Long id, MaterialStock materialStock) {
        MaterialStock existing = findById(id);
        
        // Update fields
        if (materialStock.getMaterial() != null) {
            existing.setMaterial(materialStock.getMaterial());
        }
        if (materialStock.getQuantity() != null) {
            existing.setQuantity(materialStock.getQuantity());
        }
        if (materialStock.getUnit() != null) {
            existing.setUnit(materialStock.getUnit());
        }
        if (materialStock.getUnitPrice() != null) {
            existing.setUnitPrice(materialStock.getUnitPrice());
        }
        if (materialStock.getLocation() != null) {
            existing.setLocation(materialStock.getLocation());
        }
        if (materialStock.getBatchNumber() != null) {
            existing.setBatchNumber(materialStock.getBatchNumber());
        }
        if (materialStock.getReceivedDate() != null) {
            existing.setReceivedDate(materialStock.getReceivedDate());
        }
        if (materialStock.getExpiryDate() != null) {
            existing.setExpiryDate(materialStock.getExpiryDate());
        }
        
        return repository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}

