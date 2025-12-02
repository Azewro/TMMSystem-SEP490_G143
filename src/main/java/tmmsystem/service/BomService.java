package tmmsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.entity.Bom;
import tmmsystem.entity.BomDetail;
import tmmsystem.entity.Material;
import tmmsystem.entity.Product;
import tmmsystem.repository.BomRepository;
import tmmsystem.repository.BomDetailRepository;
import tmmsystem.repository.MaterialRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class BomService {

    private final BomRepository bomRepo;
    private final BomDetailRepository bomDetailRepo;
    private final MaterialRepository materialRepo;

    public BomService(BomRepository bomRepo, BomDetailRepository bomDetailRepo, MaterialRepository materialRepo) {
        this.bomRepo = bomRepo;
        this.bomDetailRepo = bomDetailRepo;
        this.materialRepo = materialRepo;
    }

    @Transactional
    public Bom ensureBomExists(Product product) {
        Optional<Bom> existing = bomRepo.findActiveBomByProductId(product.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create default BOM
        Bom bom = new Bom();
        bom.setProduct(product);
        bom.setVersion("v1.0");
        bom.setEffectiveDate(LocalDate.now());
        bom.setActive(true);
        bom.setVersionNotes("Auto-generated default BOM");
        bom = bomRepo.save(bom);

        // Determine materials based on product name
        String name = product.getName() != null ? product.getName().toLowerCase() : "";
        List<String> materialCodes = new java.util.ArrayList<>();
        if (name.contains("cotton")) {
            materialCodes.add("Ne 32/1CD"); // Cotton yarn
        }
        if (name.contains("bambo") || name.contains("bamboo")) {
            materialCodes.add("Ne 30/1"); // Bamboo yarn
        }
        if (materialCodes.isEmpty()) {
            materialCodes.add("Ne 32/1CD"); // Default to Cotton
        }

        // Add BOM details
        for (String code : materialCodes) {
            Material material = materialRepo.findByCode(code).orElse(null);
            if (material == null) {
                // Try to find any material if specific code not found (fallback)
                List<Material> all = materialRepo.findAll();
                if (!all.isEmpty())
                    material = all.get(0);
            }

            if (material != null) {
                BomDetail detail = new BomDetail();
                detail.setBom(bom);
                detail.setMaterial(material);
                // Estimate quantity: standard weight / number of materials
                BigDecimal weightKg = (product.getStandardWeight() != null ? product.getStandardWeight()
                        : BigDecimal.ZERO)
                        .divide(new BigDecimal("1000"), 4, java.math.RoundingMode.HALF_UP);
                BigDecimal qty = weightKg.divide(new BigDecimal(materialCodes.size()), 4,
                        java.math.RoundingMode.HALF_UP);
                detail.setQuantity(qty);
                detail.setStage("WEAVING"); // Default stage
                detail.setOptional(false);
                bomDetailRepo.save(detail);
            }
        }
        return bom;
    }
}
