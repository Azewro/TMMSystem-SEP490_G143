// dto/auth/LoginResponse.java
package tmmsystem.dto.auth;
public record LoginResponse(Long userId, String name, String email, String role, boolean active, String accessToken, long expiresIn, String employeeCode) {}
