package tmmsystem.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import tmmsystem.validation.OptionalPasswordStrength;
import tmmsystem.validation.ValidName;
import tmmsystem.validation.VietnamesePhoneNumber;

@Schema(name = "UpdateUserRequest")
public class UpdateUserRequest {
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

    @Schema(description = "Trạng thái kích hoạt")
    private Boolean active;

    @Schema(description = "Trạng thái xác minh")
    private Boolean verified;

    @Size(min = 8, max = 100, message = "Mật khẩu phải có ít nhất 8 ký tự.")
    @OptionalPasswordStrength
    @Schema(description = "Mật khẩu mới (tùy chọn)", example = "NewPass1")
    private String password;

    @Schema(description = "Role ID mới (tùy chọn)", example = "2")
    private Long roleId;

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
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
}


