package tmmsystem.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for verify reset code requests.
 * Validation matches frontend: CustomerForgotPassword.jsx and InternalForgotPassword.jsx
 */
public record VerifyResetCodeRequest(
        @NotBlank(message = "Email không được để trống.")
        @Email(message = "Email không hợp lệ.")
        @Size(max = 150, message = "Email không được quá 150 ký tự")
        String email,
        
        @NotBlank(message = "Mã xác minh không được để trống.")
        String code
) {}


