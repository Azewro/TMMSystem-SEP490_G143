package tmmsystem.dto.auth;

public record CustomerOtpVerifyRequest(String emailOrPhone, String otp) {}


