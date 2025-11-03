// controller/AuthController.java
package tmmsystem.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import tmmsystem.dto.auth.LoginRequest;
import tmmsystem.dto.auth.LoginResponse;
import tmmsystem.dto.auth.ChangePasswordRequest;
import tmmsystem.dto.auth.ForgotPasswordRequest;
import tmmsystem.dto.auth.VerifyResetCodeRequest;
import tmmsystem.dto.auth.CustomerOtpRequest;
import tmmsystem.dto.auth.CustomerOtpVerifyRequest;
import tmmsystem.service.UserService;
import tmmsystem.service.CustomerService;
import tmmsystem.dto.auth.CustomerRegisterRequest;
import tmmsystem.dto.CustomerCreateRequest;
import tmmsystem.entity.Customer;

@RestController
@RequestMapping("/v1/auth")
@Validated
public class AuthController {
    private final UserService userService;
    private final CustomerService customerService;
    public AuthController(UserService s, CustomerService cs){ 
        this.userService = s; 
        this.customerService = cs;
    }

    @PostMapping("/user/login")
    public ResponseEntity<?> userLogin(@RequestBody LoginRequest req){
        LoginResponse userRes = userService.authenticate(req.email(), req.password());
        if (userRes != null) {
            return ResponseEntity.ok(userRes);
        }
        return ResponseEntity.status(401).body("Invalid credentials or inactive");
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest req){
        try {
            userService.changePassword(req);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Error: " + ex.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest req){
        try {
            userService.requestPasswordReset(req);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Error: " + ex.getMessage());
        }
    }

    @PostMapping("/verify-reset-code")
    public ResponseEntity<?> verifyResetCode(@RequestBody VerifyResetCodeRequest req){
        try {
            userService.verifyCodeAndResetPassword(req);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Error: " + ex.getMessage());
        }
    }

    // ===== Customer OTP login =====
    @PostMapping("/customer/customer-login")
    public ResponseEntity<?> customerRequestOtp(@RequestBody CustomerOtpRequest req) {
        try {
            customerService.requestOtp(req.emailOrPhone());
            return ResponseEntity.ok().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/customer/verify-login-otp")
    public ResponseEntity<?> customerVerifyOtp(@RequestBody CustomerOtpVerifyRequest req) {
        try {
            var res = customerService.verifyOtpAndLogin(req.emailOrPhone(), req.otp());
            return ResponseEntity.ok(res);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(401).body(ex.getMessage());
        }
    }

    @PostMapping("/customer/register")
    public ResponseEntity<?> registerCustomerUser(@Valid @RequestBody CustomerRegisterRequest req){
        try {
            // Kiểm tra email đã tồn tại chưa trong Customer table
            if (customerService.existsByEmail(req.email())) {
                return ResponseEntity.badRequest().body("Email đã được sử dụng");
            }

            Customer customer = new Customer();
            customer.setCompanyName("");
            customer.setContactPerson(null);
            customer.setEmail(req.email().toLowerCase().trim());
            customer.setPhoneNumber(null);
            customer.setAddress(null);
            customer.setTaxCode(null);
            customer.setActive(true);
            customer.setVerified(false);
            customer.setRegistrationType("SELF_REGISTERED");

            customerService.create(customer, null);
            return ResponseEntity.ok("Đăng ký thành công. Vui lòng cập nhật thông tin công ty sau khi đăng nhập.");
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body("Lỗi đăng ký: " + ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("Lỗi hệ thống: " + ex.getMessage());
        }
    }

    @PostMapping("/customer/create-company")
    public ResponseEntity<?> createCompany(@Valid @RequestBody CustomerCreateRequest req, 
                                         @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Extract user ID from JWT token
            String token = null;
            if (authHeader != null && !authHeader.isBlank()) {
                token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader.trim();
            } else {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getName() != null) {
                    // derive by email from principal when header missing
                    Customer c = customerService.findByEmailOrThrow(auth.getName());
                    if (c != null) {
                        return ResponseEntity.ok("Tạo/cập nhật công ty yêu cầu header Authorization (Bearer token)");
                    }
                }
                return ResponseEntity.badRequest().body("Thiếu header Authorization");
            }
            Long customerId = customerService.getCustomerIdFromToken(token);

            // Cập nhật/ghi đè company info cho customer đã đăng ký
            Customer customer = customerService.findById(customerId);
            if (customer.getCompanyName() != null && !customer.getCompanyName().isBlank()) {
                return ResponseEntity.badRequest().body("Bạn đã có công ty rồi");
            }

            customer.setCompanyName(req.companyName().trim());
            customer.setContactPerson(req.contactPerson() != null ? req.contactPerson().trim() : null);
            customer.setPhoneNumber(req.phoneNumber());
            customer.setAddress(req.address());
            customer.setTaxCode(req.taxCode());
            customerService.update(customer.getId(), customer);

            return ResponseEntity.ok("Tạo/cập nhật công ty thành công");
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body("Lỗi tạo công ty: " + ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("Lỗi hệ thống: " + ex.getMessage());
        }
    }

    @GetMapping("/customer/verify")
    public ResponseEntity<?> verifyCustomerEmail(@RequestParam("token") String token) {
        try {
            // In merged model, optional: auto-verified after register or future email flow
            return ResponseEntity.ok("OK");
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping("/customer/profile")
    public ResponseEntity<?> getCustomerProfile(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String token = null;
            if (authHeader != null && !authHeader.isBlank()) {
                token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader.trim();
            }

            Customer customer = null;
            if (token != null && !token.isBlank()) {
                customer = customerService.getCustomerFromToken(token);
            } else {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getName() != null) {
                    customer = customerService.findByEmailOrThrow(auth.getName());
                }
            }

            if (customer == null) {
                return ResponseEntity.badRequest().body("Thiếu hoặc sai token. Hãy dùng header Authorization: Bearer <JWT>.");
            }

            return ResponseEntity.ok(java.util.Map.of(
                "customerId", customer.getId(),
                "companyName", customer.getCompanyName(),
                "contactPerson", customer.getContactPerson(),
                "email", customer.getEmail(),
                "phoneNumber", customer.getPhoneNumber(),
                "address", customer.getAddress(),
                "taxCode", customer.getTaxCode(),
                "active", customer.getActive(),
                "verified", customer.getVerified(),
                "registrationType", customer.getRegistrationType()
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body("Lỗi: " + ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("Lỗi hệ thống: " + ex.getMessage());
        }
    }
}
