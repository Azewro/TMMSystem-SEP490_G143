package tmmsystem.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.entity.ProductionOrder;
import tmmsystem.entity.ProductionStage;
import tmmsystem.entity.WorkOrderDetail;
import tmmsystem.repository.ProductionOrderRepository;
import tmmsystem.repository.ProductionStageRepository;
import tmmsystem.repository.WorkOrderDetailRepository;

import java.util.List;

/**
 * Service để migrate ProductionStage từ WorkOrderDetail sang ProductionOrder
 * Chạy 1 lần để migrate dữ liệu hiện có
 * 
 * Logic migration:
 * ProductionStage.workOrderDetailId → WorkOrderDetail → WorkOrder → ProductionOrder.id
 */
@Service
public class WorkOrderMigrationService {

    private final ProductionStageRepository stageRepo;
    private final WorkOrderDetailRepository wodRepo;
    private final ProductionOrderRepository poRepo;
    private final JdbcTemplate jdbcTemplate;

    public WorkOrderMigrationService(ProductionStageRepository stageRepo,
                                     WorkOrderDetailRepository wodRepo,
                                     ProductionOrderRepository poRepo,
                                     JdbcTemplate jdbcTemplate) {
        this.stageRepo = stageRepo;
        this.wodRepo = wodRepo;
        this.poRepo = poRepo;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Sửa unique constraint trên index idx_stage_po_sequence
     * Nếu index đã tồn tại với unique, sẽ drop foreign key (nếu có), drop index và tạo lại không unique
     */
    @Transactional
    public void fixUniqueConstraint() {
        System.out.println("Đang kiểm tra và sửa unique constraint trên idx_stage_po_sequence...");
        
        try {
            // Bước 1: Tìm foreign key constraint nào đang sử dụng index này
            String findFkSql = "SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE " +
                    "WHERE TABLE_SCHEMA = DATABASE() " +
                    "AND TABLE_NAME = 'production_stage' " +
                    "AND COLUMN_NAME = 'production_order_id' " +
                    "AND REFERENCED_TABLE_NAME IS NOT NULL";
            
            List<String> fkNames = jdbcTemplate.query(findFkSql, (rs, rowNum) -> rs.getString("CONSTRAINT_NAME"));
            
            // Bước 2: Drop foreign key nếu có
            for (String fkName : fkNames) {
                if (fkName != null && !fkName.isEmpty()) {
                    try {
                        System.out.println("  ℹ️ Đang xóa foreign key: " + fkName);
                        jdbcTemplate.execute("ALTER TABLE production_stage DROP FOREIGN KEY " + fkName);
                        System.out.println("  ✅ Đã xóa foreign key: " + fkName);
                    } catch (Exception e) {
                        System.out.println("  ⚠️ Không thể xóa foreign key " + fkName + ": " + e.getMessage());
                    }
                }
            }
            
            // Bước 3: Kiểm tra xem index có unique không
            String checkUniqueSql = "SELECT COUNT(*) FROM information_schema.statistics " +
                    "WHERE table_schema = DATABASE() " +
                    "AND table_name = 'production_stage' " +
                    "AND index_name = 'idx_stage_po_sequence' " +
                    "AND non_unique = 0";
            
            Integer uniqueIndexCount = jdbcTemplate.queryForObject(checkUniqueSql, Integer.class);
            
            if (uniqueIndexCount != null && uniqueIndexCount > 0) {
                System.out.println("  ⚠️ Tìm thấy unique constraint trên idx_stage_po_sequence, đang xóa...");
                
                // Drop index cũ
                try {
                    jdbcTemplate.execute("ALTER TABLE production_stage DROP INDEX idx_stage_po_sequence");
                    System.out.println("  ✅ Đã xóa index cũ");
                } catch (Exception e) {
                    System.out.println("  ⚠️ Không thể xóa index: " + e.getMessage());
                }
            } else {
                // Kiểm tra xem index có tồn tại không
                String checkIndexSql = "SELECT COUNT(*) FROM information_schema.statistics " +
                        "WHERE table_schema = DATABASE() " +
                        "AND table_name = 'production_stage' " +
                        "AND index_name = 'idx_stage_po_sequence'";
                
                Integer indexCount = jdbcTemplate.queryForObject(checkIndexSql, Integer.class);
                
                if (indexCount != null && indexCount > 0) {
                    // Index tồn tại, thử drop để tạo lại
                    try {
                        jdbcTemplate.execute("ALTER TABLE production_stage DROP INDEX idx_stage_po_sequence");
                        System.out.println("  ✅ Đã xóa index hiện tại");
                    } catch (Exception e) {
                        System.out.println("  ⚠️ Không thể xóa index: " + e.getMessage());
                    }
                }
            }
            
            // Bước 4: Tạo lại index không unique
            try {
                jdbcTemplate.execute("CREATE INDEX idx_stage_po_sequence ON production_stage(production_order_id, stage_sequence)");
                System.out.println("  ✅ Đã tạo lại index không unique");
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("Duplicate key name")) {
                    System.out.println("  ℹ️ Index đã tồn tại: " + e.getMessage());
                } else {
                    System.out.println("  ⚠️ Không thể tạo index: " + e.getMessage());
                }
            }
            
            // Bước 5: Tạo lại foreign key nếu đã drop
            if (!fkNames.isEmpty()) {
                try {
                    System.out.println("  ℹ️ Đang tạo lại foreign key...");
                    jdbcTemplate.execute("ALTER TABLE production_stage " +
                            "ADD CONSTRAINT fk_production_stage_production_order " +
                            "FOREIGN KEY (production_order_id) REFERENCES production_order(id)");
                    System.out.println("  ✅ Đã tạo lại foreign key");
                } catch (Exception e) {
                    System.out.println("  ⚠️ Không thể tạo lại foreign key: " + e.getMessage());
                    System.out.println("  ℹ️ Foreign key sẽ được tạo tự động khi cần");
                }
            }
            
            System.out.println("✅ Hoàn thành kiểm tra và sửa unique constraint\n");
        } catch (Exception e) {
            System.err.println("  ❌ Lỗi khi sửa unique constraint: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Không thể sửa unique constraint tự động", e);
        }
    }

    /**
     * Migrate tất cả ProductionStage hiện có từ WorkOrderDetail sang ProductionOrder
     * Logic: ProductionStage.workOrderDetailId → WorkOrderDetail → WorkOrder → ProductionOrder.id
     * 
     * LƯU Ý: Một ProductionOrder có thể có nhiều ProductionOrderDetail, mỗi detail tạo ra các stages
     * với cùng sequence (1-6). Do đó, sẽ có duplicate (production_order_id, stage_sequence).
     * Unique constraint đã được bỏ để cho phép điều này.
     */
    @Transactional
    public void migrateProductionStagesToProductionOrder() {
        System.out.println("Bắt đầu migration ProductionStage → ProductionOrder...");
        System.out.println("LƯU Ý: Một ProductionOrder có thể có nhiều sets stages (từ nhiều ProductionOrderDetail)");
        
        // Bước 1: Sửa unique constraint trước khi migrate
        fixUniqueConstraint();
        
        // Lấy tất cả ProductionStage có workOrderDetailId
        List<ProductionStage> allStages = stageRepo.findAll();
        // REMOVED: Filter by workOrderDetail - field đã bị xóa
        // Migration này chỉ chạy một lần, nếu đã migrate xong thì không cần chạy nữa
        List<ProductionStage> stagesToMigrate = allStages.stream()
                .filter(s -> s.getProductionOrder() == null)
                .toList();

        System.out.println("Tìm thấy " + stagesToMigrate.size() + " stages cần migrate");

        int migratedCount = 0;
        int errorCount = 0;
        int duplicateCount = 0;

        // Group stages by ProductionOrder để detect duplicates
        java.util.Map<Long, java.util.Map<Integer, Integer>> poSequenceCount = new java.util.HashMap<>();

        // REMOVED: Migration logic qua WorkOrderDetail - field đã bị xóa khỏi database
        // Nếu cần migrate lại, phải dùng cách khác (query trực tiếp từ database)
        System.out.println("⚠️ Migration service không thể chạy vì work_order_detail_id đã bị xóa khỏi database");
        System.out.println("Nếu cần migrate lại, phải dùng SQL query trực tiếp");
        
        // REMOVED: Toàn bộ logic migration - không thể chạy được vì work_order_detail_id đã bị xóa
        // Nếu cần migrate lại, phải dùng SQL query trực tiếp từ database
        System.out.println("⚠️ Không thể migrate vì work_order_detail_id đã bị xóa khỏi database");
        System.out.println("Tất cả stages trong danh sách sẽ bị bỏ qua");
        
        for (ProductionStage stage : stagesToMigrate) {
            System.out.println("  ⚠️ Stage " + stage.getId() + " không có ProductionOrder, bỏ qua");
            errorCount++;
        }

        // Report duplicates
        for (java.util.Map.Entry<Long, java.util.Map<Integer, Integer>> entry : poSequenceCount.entrySet()) {
            for (java.util.Map.Entry<Integer, Integer> seqEntry : entry.getValue().entrySet()) {
                if (seqEntry.getValue() > 1) {
                    duplicateCount += seqEntry.getValue() - 1;
                    System.out.println("  ℹ️ ProductionOrder " + entry.getKey() + " có " + seqEntry.getValue() + 
                            " stages với sequence " + seqEntry.getKey() + " (duplicate hợp lệ)");
                }
            }
        }

        // Save tất cả stages đã migrate (batch save để tránh constraint issues)
        if (migratedCount > 0) {
            System.out.println("Đang lưu " + migratedCount + " stages đã migrate...");
            // Save từng batch để tránh lỗi
            int batchSize = 50;
            for (int i = 0; i < stagesToMigrate.size(); i += batchSize) {
                int end = Math.min(i + batchSize, stagesToMigrate.size());
                List<ProductionStage> batch = stagesToMigrate.subList(i, end);
                try {
                    stageRepo.saveAll(batch);
                    System.out.println("  Đã lưu batch " + (i / batchSize + 1) + " (" + batch.size() + " stages)");
                } catch (Exception e) {
                    System.err.println("  ❌ Lỗi khi lưu batch " + (i / batchSize + 1) + ": " + e.getMessage());
                    // Try saving individually
                    for (ProductionStage s : batch) {
                        try {
                            stageRepo.save(s);
                        } catch (Exception ex) {
                            System.err.println("    ❌ Không thể lưu stage " + s.getId() + ": " + ex.getMessage());
                        }
                    }
                }
            }
            System.out.println("✅ Đã lưu thành công!");
        }

        System.out.println("\n=== KẾT QUẢ MIGRATION ===");
        System.out.println("✅ Đã migrate: " + migratedCount + " stages");
        if (duplicateCount > 0) {
            System.out.println("ℹ️  Có " + duplicateCount + " duplicate (production_order_id, stage_sequence) - hợp lệ");
        }
        if (errorCount > 0) {
            System.out.println("❌ Lỗi: " + errorCount + " stages");
        }
        System.out.println("========================\n");
    }

    /**
     * Verify migration: Kiểm tra tất cả ProductionStage đã có productionOrderId
     */
    public void verifyMigration() {
        System.out.println("Đang verify migration...");
        
        List<ProductionStage> allStages = stageRepo.findAll();
        List<ProductionStage> stagesWithoutProductionOrder = allStages.stream()
                .filter(s -> s.getProductionOrder() == null || s.getProductionOrder().getId() == null)
                .toList();

        // REMOVED: Không thể check workOrderDetail nữa vì field đã bị xóa
        List<ProductionStage> stagesStillLinkedToWOD = java.util.Collections.emptyList();

        System.out.println("\n=== KẾT QUẢ VERIFY ===");
        System.out.println("Tổng số stages: " + allStages.size());
        System.out.println("Stages đã có ProductionOrder: " + (allStages.size() - stagesWithoutProductionOrder.size()));
        
        if (stagesWithoutProductionOrder.isEmpty()) {
            System.out.println("✅ Tất cả ProductionStage đã được migrate thành công!");
        } else {
            System.out.println("⚠️ Còn " + stagesWithoutProductionOrder.size() + " stages chưa có ProductionOrder:");
            stagesWithoutProductionOrder.forEach(s -> 
                System.out.println("  - Stage ID: " + s.getId() + ", không có ProductionOrder")
            );
        }
        
        System.out.println("\nStages vẫn còn link với WorkOrderDetail: " + stagesStillLinkedToWOD.size());
        System.out.println("(Có thể giữ lại để backup, nhưng code sẽ không dùng nữa)");
        System.out.println("======================\n");
    }
}

