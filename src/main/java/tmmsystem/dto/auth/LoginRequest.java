package tmmsystem.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for login requests (both customer and internal user).
 * Validation matches frontend: LoginPage.jsx and InternalLoginPage.jsx
 */
public record LoginRequest(
        @NotBlank(message = "Email không được để trống.")
        @Email(message = "Email không hợp lệ.")
        @Size(max = 150, message = "Email không được quá 150 ký tự")
        String email,
        
        @NotBlank(message = "Mật khẩu không được để trống.")
        String password
) {}
