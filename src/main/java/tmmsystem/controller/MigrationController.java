package tmmsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tmmsystem.service.WorkOrderMigrationService;

/**
 * Controller để chạy migration từ WorkOrder/WorkOrderDetail sang ProductionOrder
 * CHỈ CHẠY 1 LẦN sau khi deploy code mới
 */
@RestController
@RequestMapping("/v1/migration")
@Tag(name = "Migration", description = "API để migrate dữ liệu từ WorkOrder sang ProductionOrder")
public class MigrationController {

    private final WorkOrderMigrationService migrationService;

    public MigrationController(WorkOrderMigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Operation(summary = "Migrate ProductionStage từ WorkOrderDetail sang ProductionOrder",
               description = "Chạy migration để link ProductionStage trực tiếp với ProductionOrder. " +
                           "CHỈ CHẠY 1 LẦN sau khi deploy code mới. " +
                           "Sau khi migrate xong, code sẽ không dùng WorkOrder/WorkOrderDetail nữa.")
    @PostMapping("/work-order-to-production-order")
    public ResponseEntity<String> migrateWorkOrderToProductionOrder() {
        try {
            migrationService.migrateProductionStagesToProductionOrder();
            return ResponseEntity.ok("Migration hoàn thành thành công!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi migration: " + e.getMessage());
        }
    }

    @Operation(summary = "Verify migration",
               description = "Kiểm tra xem tất cả ProductionStage đã được migrate chưa")
    @GetMapping("/verify")
    public ResponseEntity<String> verifyMigration() {
        try {
            migrationService.verifyMigration();
            return ResponseEntity.ok("Verify hoàn thành. Xem console/log để xem chi tiết.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi khi verify: " + e.getMessage());
        }
    }
}

