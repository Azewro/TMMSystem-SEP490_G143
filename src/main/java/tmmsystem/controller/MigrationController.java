package tmmsystem.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tmmsystem.service.BomBackfillService;
import tmmsystem.service.MachineService;

@RestController
@RequestMapping("/api/migration")
public class MigrationController {

    private final MachineService machineService;
    private final BomBackfillService bomBackfillService;

    public MigrationController(MachineService machineService, BomBackfillService bomBackfillService) {
        this.machineService = machineService;
        this.bomBackfillService = bomBackfillService;
    }

    @PostMapping("/sync-machine-statuses")
    public ResponseEntity<String> syncMachineStatuses() {
        machineService.syncMachineStatuses();
        return ResponseEntity.ok("Machine statuses synchronized successfully based on active production stages.");
    }

    @PostMapping("/backfill-boms")
    public ResponseEntity<String> backfillBoms() {
        bomBackfillService.backfillMissingBoms();
        return ResponseEntity.ok("Missing BOMs have been backfilled successfully.");
    }
}
