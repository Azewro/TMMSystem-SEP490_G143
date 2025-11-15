package tmmsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tmmsystem.dto.UserDto;
import tmmsystem.dto.user.CreateUserRequest;
import tmmsystem.dto.user.UpdateUserRequest;
import tmmsystem.entity.Role;
import tmmsystem.entity.User;
import tmmsystem.service.UserService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import tmmsystem.dto.PageResponse;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public PageResponse<UserDto> getAll(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String search,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String roleName,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Boolean isActive) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<UserDto> userPage = userService.getAllUsers(pageable, search, roleName, isActive);
        return new PageResponse<>(userPage.getContent(), userPage.getNumber(), userPage.getSize(), 
                userPage.getTotalElements(), userPage.getTotalPages(), userPage.isFirst(), userPage.isLast());
    }

    @GetMapping("/{id}")
    public UserDto getOne(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @Operation(summary = "Tạo user mới")
    @PostMapping
    public UserDto create(
            @RequestBody(description = "Payload tạo user", required = true,
                    content = @Content(schema = @Schema(implementation = CreateUserRequest.class)))
            @org.springframework.web.bind.annotation.RequestBody CreateUserRequest req) {
        User user = new User();
        user.setEmail(req.getEmail());
        user.setPassword(req.getPassword());
        user.setName(req.getName());
        user.setPhoneNumber(req.getPhoneNumber());
        user.setAvatar(req.getAvatar());
        user.setActive(req.getActive() != null ? req.getActive() : true);
        user.setVerified(req.getVerified() != null ? req.getVerified() : false);
        Role role = new Role(); role.setId(req.getRoleId());
        user.setRole(role);
        return userService.createUser(user);
    }

    @Operation(summary = "Cập nhật user")
    @PutMapping("/{id}")
    public UserDto update(
            @PathVariable Long id,
            @RequestBody(description = "Payload cập nhật user", required = true,
                    content = @Content(schema = @Schema(implementation = UpdateUserRequest.class)))
            @org.springframework.web.bind.annotation.RequestBody UpdateUserRequest req) {
        User updated = new User();
        updated.setName(req.getName());
        updated.setPhoneNumber(req.getPhoneNumber());
        updated.setAvatar(req.getAvatar());
        updated.setActive(req.getActive());
        updated.setVerified(req.getVerified());
        updated.setPassword(req.getPassword());
        if (req.getRoleId() != null) {
            Role role = new Role(); role.setId(req.getRoleId());
            updated.setRole(role);
        }
        return userService.updateUser(id, updated);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        userService.deleteUser(id);
    }
}
