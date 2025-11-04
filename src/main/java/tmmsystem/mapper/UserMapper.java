package tmmsystem.mapper;

import tmmsystem.dto.UserDto;
import tmmsystem.entity.User;

public class UserMapper {
    public static UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setAvatar(user.getAvatar());
        dto.setIsActive(user.getActive());
        dto.setIsVerified(user.getVerified());
        dto.setEmployeeCode(user.getEmployeeCode());
        if (user.getRole() != null) {
            dto.setRoleName(user.getRole().getName());
        }
        return dto;
    }
}
