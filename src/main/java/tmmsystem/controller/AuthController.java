package tmmsystem.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import tmmsystem.dto.auth.*;
import tmmsystem.entity.Customer;
import tmmsystem.service.CustomerService;
import tmmsystem.service.UserService;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/auth")
@Validated
public class AuthController {
    private final UserService userService;
    private final CustomerService customerService;  // ← Changed

    public AuthController(UserService userService, CustomerService customerService) {
        this.userService = userService;
        this.customerService = customerService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        // Try internal user first
        LoginResponse res = userService.authenticate(req.email(), req.password());
        if (res == null) {
            // Try customer login
            res = customerService.authenticate(req.email(), req.password());
        }
        if (res == null) {
            return ResponseEntity.status(401).body("Invalid credentials or inactive");
        }
        return ResponseEntity.ok(res);
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest req) {
        try {
            userService.changePassword(req);
            return ResponseEntity.ok().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        try {
            userService.requestPasswordReset(req);
            return ResponseEntity.ok().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/verify-reset-code")
    public ResponseEntity<?> verifyResetCode(@RequestBody VerifyResetCodeRequest req) {
        try {
            userService.verifyCodeAndResetPassword(req);
            return ResponseEntity.ok().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/customer/register")
    public ResponseEntity<?> registerCustomer(@Valid @RequestBody CustomerUserRegisterRequest req) {
        try {
            // Check if email exists
            if (customerService.existsByEmail(req.email())) {
                return ResponseEntity.badRequest().body("Email đã được sử dụng");
            }

            Customer customer = new Customer();
            customer.setEmail(req.email().toLowerCase().trim());
            customer.setPassword(req.password());
            customer.setContactPerson(req.name().trim());
            customer.setPhoneNumber(req.phoneNumber());
            customer.setPosition(req.position() != null ? req.position().trim() : null);
            customer.setRegistrationType("SELF_REGISTERED");

            Customer savedCustomer = customerService.create(customer);
            return ResponseEntity.ok("Vui lòng kiểm tra email để xác minh tài khoản");
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body("Lỗi đăng ký: " + ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("Lỗi hệ thống: " + ex.getMessage());
        }
    }

    @GetMapping("/customer/verify")
    public ResponseEntity<?> verifyCustomerEmail(@RequestParam("token") String token) {
        try {
            customerService.verifyEmail(token);
            return ResponseEntity.ok("Email đã được xác minh thành công");
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("Not authenticated");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("email", authentication.getName());
        response.put("authorities", authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

}
