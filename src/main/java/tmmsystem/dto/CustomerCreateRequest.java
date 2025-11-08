package tmmsystem.dto;

import jakarta.validation.constraints.*;

public record CustomerCreateRequest(
        @NotBlank(message = "Tên công ty không được để trống")
        @Size(max = 255, message = "Tên công ty không được quá 255 ký tự")
        String companyName,
        
        @Size(max = 150, message = "Người liên hệ không được quá 150 ký tự")
        String contactPerson,
        
        @Email(message = "Email công ty không hợp lệ")
        @Size(max = 150, message = "Email không được quá 150 ký tự")
        String email,
        
        @Size(max = 30, message = "Số điện thoại không được quá 30 ký tự")
        String phoneNumber,
        
        @Size(max = 100, message = "Vị trí không được quá 100 ký tự")
        String position,
        
        @Size(max = 1000, message = "Địa chỉ không được quá 1000 ký tự")
        String address,
        
        @Size(max = 50, message = "Mã số thuế không được quá 50 ký tự")
        String taxCode,
        
        Boolean isActive,
        Boolean isVerified,
        @Size(max = 20, message = "registrationType không được quá 20 ký tự")
        String registrationType,
        Long createdById
) {}