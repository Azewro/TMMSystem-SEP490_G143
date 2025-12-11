package tmmsystem.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tmmsystem.dto.UserDto;
import tmmsystem.dto.response.ApiResponse;
import tmmsystem.entity.User;
import tmmsystem.service.user.FindLeaderHaveMinLOTService;

@RestController
@RequestMapping("/v1/leader")
public class LeaderController {
    private final FindLeaderHaveMinLOTService findLeaderHaveMinLOTService;
    private LeaderController (FindLeaderHaveMinLOTService findLeaderHaveMinLOTService) {
        this.findLeaderHaveMinLOTService = findLeaderHaveMinLOTService;
    }

    @GetMapping("/min/lot")
    @Operation(summary = "Lấy Leader có LOT nhỏ nhất")
    public ResponseEntity<ApiResponse<UserDto>> getLeaderHaveMinLOT () {
        UserDto leader = findLeaderHaveMinLOTService.getLeaderWithMinLOT();
        if (leader != null) {
            // 3. Tạo đối tượng ApiResponse thành công
            ApiResponse<UserDto> response = ApiResponse.success(
                    "Tìm thấy Leader có LOT nhỏ nhất thành công.",
                    leader
            );

            return ResponseEntity.ok(response);
        } else {
            ApiResponse<UserDto> response = new ApiResponse<>(
                    false,
                    "Không tìm thấy Leader nào.",
                    null // data là null
            );
            return ResponseEntity.status(404).body(response);
        }
    }
}
