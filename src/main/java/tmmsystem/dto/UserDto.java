package tmmsystem.dto;

import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String email;
    private String name;
    private String phoneNumber;
    private String avatar;
    private Boolean isActive;
    private Boolean isVerified;
    private String roleName;
    private String employeeCode;

    // explicit accessors to avoid IDE inspection issues
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
}
