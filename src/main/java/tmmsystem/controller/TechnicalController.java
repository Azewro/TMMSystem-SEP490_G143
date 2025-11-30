
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
    public void handleDefect(@RequestBody tmmsystem.dto.technical.TechnicalDefectActionDto body) {
        service.handleDefect(body.getStageId(), body.getDecision(), body.getNotes(), body.getTechnicalUserId(),
                body.getQuantity(), body.getDetails());
    }
}
