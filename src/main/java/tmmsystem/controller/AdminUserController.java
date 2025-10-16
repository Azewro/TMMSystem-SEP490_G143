package tmmsystem.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tmmsystem.entity.User;
import tmmsystem.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/v1/admin/users")
public class AdminUserController {
    private final UserService userService;
    public AdminUserController(UserService s){ this.userService = s; }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<SimpleUserDTO> all(){
        return userService.findAll().stream()
                .map(u -> new SimpleUserDTO(
                        u.getId(),
                        u.getName(),
                        u.getEmail(),
                        u.getRole() != null ? u.getRole().getName() : "NO_ROLE",
                        u.getActive()
                ))
                .toList();
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> setActive(@PathVariable Long id, @RequestParam boolean value){
        userService.setActive(id, value);
        return ResponseEntity.ok().build();
    }

    record SimpleUserDTO(Long id, String name, String email, String roleName, Boolean active) {}
}
