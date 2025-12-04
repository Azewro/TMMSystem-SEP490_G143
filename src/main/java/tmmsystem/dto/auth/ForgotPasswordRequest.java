package tmmsystem.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for forgot password requests.
 * Validation matches frontend: CustomerForgotPassword.jsx and InternalForgotPassword.jsx
 */
public record ForgotPasswordRequest(
        @NotBlank(message = "Email không được để trống.")
        @Email(message = "Vui lòng nhập đúng định dạng Email")
        @Size(max = 150, message = "Email không được quá 150 ký tự")
        String email
) {}


