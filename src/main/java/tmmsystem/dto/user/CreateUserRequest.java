package tmmsystem.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import tmmsystem.validation.ValidName;
import tmmsystem.validation.VietnamesePhoneNumber;

@Schema(name = "CreateUserRequest")
public class CreateUserRequest {
    @NotBlank(message = "Email là bắt buộc")
    @Email(message = "Email không hợp lệ.")
    @Size(max = 150, message = "Email không được quá 150 ký tự")
    @Schema(description = "Email đăng nhập", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Mật khẩu là bắt buộc")
    @Size(min = 8, max = 100, message = "Mật khẩu phải có ít nhất 8 ký tự.")
    @Schema(description = "Mật khẩu", example = "Passw0rd", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "Họ và tên là bắt buộc")
    @ValidName
    @Size(max = 255, message = "Họ và tên không được quá 255 ký tự")
    @Schema(description = "Tên hiển thị", example = "User Name")
    private String name;

    @NotBlank(message = "Số điện thoại là bắt buộc")
    @VietnamesePhoneNumber
    @Size(max = 30, message = "Số điện thoại không được quá 30 ký tự")
    @Schema(description = "SĐT", example = "0123456789")
    private String phoneNumber;

    @Schema(description = "Ảnh đại diện URL", example = "https://...")
    private String avatar;

    @Schema(description = "Trạng thái kích hoạt", defaultValue = "true")
    private Boolean active;

    @Schema(description = "Trạng thái xác minh", defaultValue = "false")
    private Boolean verified;

    @NotNull
    @Schema(description = "Role ID", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long roleId;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
}


