
package tmmsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import tmmsystem.service.TechnicalService;

@RestController
@RequestMapping("/v1/technical")
@Validated
@Tag(name = "Technical")
public class TechnicalController {
    private final TechnicalService service;

    public TechnicalController(TechnicalService service) {
        this.service = service;
    }

    @Operation(summary = "Xử lý lỗi (Technical decision)")
    @PostMapping("/defects/handle")
    public void handleDefect(@RequestParam Long stageId,
            @RequestParam String decision, // REWORK, MATERIAL_REQUEST, ACCEPT
            @RequestParam(required = false) String notes,
            @RequestParam Long technicalUserId,
            @RequestParam(required = false) java.math.BigDecimal quantity) {
        service.handleDefect(stageId, decision, notes, technicalUserId, quantity);
    }
}
