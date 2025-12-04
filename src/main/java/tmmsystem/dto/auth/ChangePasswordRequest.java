package tmmsystem.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import tmmsystem.validation.PasswordStrength;

/**
 * DTO for change password requests.
 * Validation matches frontend: ChangePasswordModal.jsx
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Email không được để trống.")
        @Email(message = "Email không hợp lệ.")
        @Size(max = 150, message = "Email không được quá 150 ký tự")
        String email,
        
        @NotBlank(message = "Mật khẩu hiện tại không được để trống.")
        String currentPassword,
        
        @NotBlank(message = "Mật khẩu mới không được để trống.")
        @Size(min = 8, message = "Mật khẩu mới phải có ít nhất 8 ký tự.")
        @PasswordStrength
        String newPassword
) {}


