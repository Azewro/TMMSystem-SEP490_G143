// service/UserService.java
package tmmsystem.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.dto.UserDto;
import tmmsystem.dto.auth.LoginResponse;
import tmmsystem.dto.auth.ForgotPasswordRequest;
import tmmsystem.dto.auth.VerifyResetCodeRequest;
import tmmsystem.dto.auth.ChangePasswordRequest;
import tmmsystem.entity.Role;
import tmmsystem.entity.User;
import tmmsystem.mapper.UserMapper;
import tmmsystem.repository.RoleRepository;
import tmmsystem.repository.UserRepository;
import tmmsystem.util.JwtService;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class UserService {
    private final UserRepository userRepo;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;
    
    public UserService(UserRepository userRepo, RoleRepository roleRepository, PasswordEncoder passwordEncoder, JwtService jwtService, MailService mailService) {
        this.userRepo = userRepo;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailService = mailService;
    }

    public LoginResponse authenticate(String email, String password){
        return userRepo.findByEmail(email)
                .filter(u -> passwordEncoder.matches(password, u.getPassword()) && Boolean.TRUE.equals(u.getActive()))
                .map(u -> {
                    String token = jwtService.generateToken(u.getEmail(), java.util.Map.of(
                            "uid", u.getId(),
                            "role", u.getRole().getName()
                    ));
                    long expiresIn = jwtService.getExpirationMillis();
                    return new LoginResponse(u.getId(), u.getName(), u.getEmail(), u.getRole().getName(), u.getActive(), token, expiresIn);
                })
                .orElse(null);
    }

    public List<User> findAll(){ return userRepo.findAll(); }
    public boolean existsByEmail(String email) { return userRepo.existsByEmail(email); }

    @Transactional
    public void setActive(Long id, boolean active){
        User u = userRepo.findById(id).orElseThrow();
        u.setActive(active);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = userRepo.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new RuntimeException("User is inactive");
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        if (request.newPassword() == null || request.newPassword().isBlank()) {
            throw new RuntimeException("New password must not be empty");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        // save is optional due to transactional dirty checking but keep explicit
        userRepo.save(user);
    }

    // ===== Forgot password flow =====
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        User user = userRepo.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new RuntimeException("User is inactive");
        }
        // Generate and store reset code inline on user
        String code = generateNumericCode(6);
        user.setResetCode(code);
        user.setResetCodeExpiresAt(java.time.Instant.now().plus(java.time.Duration.ofMinutes(10)));
        userRepo.save(user);

        String subject = "Password Reset Code";
        String body = "Your verification code is: " + code + "\nThis code expires in 10 minutes.";
        mailService.send(user.getEmail(), subject, body);
    }

    @Transactional
    public void verifyCodeAndResetPassword(VerifyResetCodeRequest request) {
        User user = userRepo.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getResetCode() == null || !user.getResetCode().equals(request.code())) {
            throw new RuntimeException("Invalid or used code");
        }

        if (user.getResetCodeExpiresAt() == null || java.time.Instant.now().isAfter(user.getResetCodeExpiresAt())) {
            throw new RuntimeException("Reset code expired");
        }

        String newPasswordPlain = generateRandomPassword(10);
        user.setPassword(passwordEncoder.encode(newPasswordPlain));
        // clear reset code
        user.setResetCode(null);
        user.setResetCodeExpiresAt(null);
        userRepo.save(user);

        mailService.send(user.getEmail(), "Your new password", "Your new password is: " + newPasswordPlain);
    }

    private String generateNumericCode(int length) {
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String generateRandomPassword(int length) {
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#$%";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    public List<UserDto> getAllUsers() {
        return userRepo.findAll()
                .stream()
                .map(UserMapper::toDto)
                .collect(Collectors.toList());
    }
    
    public Page<UserDto> getAllUsers(Pageable pageable) {
        return userRepo.findAll(pageable)
                .map(UserMapper::toDto);
    }
    
    public Page<UserDto> getAllUsers(Pageable pageable, String search, String roleName, Boolean isActive) {
        Page<User> userPage;
        
        // Build query conditions
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.trim().toLowerCase();
            // Search in name, email, phoneNumber, or role name
            userPage = userRepo.findAll((root, query, cb) -> {
                var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                
                // Search predicate
                var searchPredicate = cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + searchLower + "%"),
                    cb.like(cb.lower(root.get("email")), "%" + searchLower + "%"),
                    cb.like(cb.lower(root.get("phoneNumber")), "%" + searchLower + "%"),
                    cb.like(cb.lower(root.get("role").get("name")), "%" + searchLower + "%")
                );
                predicates.add(searchPredicate);
                
                // Role filter
                if (roleName != null && !roleName.trim().isEmpty()) {
                    predicates.add(cb.equal(root.get("role").get("name"), roleName));
                }
                
                // Status filter
                if (isActive != null) {
                    predicates.add(cb.equal(root.get("active"), isActive));
                }
                
                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            }, pageable);
        } else {
            // No search, just filters
            if (roleName != null || isActive != null) {
                userPage = userRepo.findAll((root, query, cb) -> {
                    var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                    
                    if (roleName != null && !roleName.trim().isEmpty()) {
                        predicates.add(cb.equal(root.get("role").get("name"), roleName));
                    }
                    
                    if (isActive != null) {
                        predicates.add(cb.equal(root.get("active"), isActive));
                    }
                    
                    return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
                }, pageable);
            } else {
                userPage = userRepo.findAll(pageable);
            }
        }
        
        return userPage.map(UserMapper::toDto);
    }

    public UserDto getUserById(Long id) {
        return userRepo.findById(id)
                .map(UserMapper::toDto)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public UserDto createUser(User user) {
        // Check email duplicate
        if (userRepo.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }
        
        // Check phone number duplicate (if provided)
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank()) {
            if (userRepo.existsByPhoneNumber(user.getPhoneNumber())) {
                throw new RuntimeException("Số điện thoại đã được sử dụng");
            }
        }
        
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        Role role = roleRepository.findById(user.getRole().getId())
                .orElseThrow(() -> new RuntimeException("Role not found"));
        user.setRole(role);

        // Auto-generate employee code if not provided
        if (user.getEmployeeCode() == null || user.getEmployeeCode().isBlank()) {
            user.setEmployeeCode(generateNextEmployeeCode());
        }
        return UserMapper.toDto(userRepo.save(user));
    }

    private String generateNextEmployeeCode() {
        // Pattern: EMP-YYYYMM-XXX
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC);
        String yyyymm = String.format("%04d%02d", now.getYear(), now.getMonthValue());
        // In case of race condition, we retry a few times
        for (int attempt = 0; attempt < 3; attempt++) {
            Integer maxSeq = userRepo.findMaxEmployeeSeqForCurrentMonth();
            int next = (maxSeq == null ? 1 : maxSeq + 1);
            String code = String.format("EMP-%s-%03d", yyyymm, next);
            // Optimistic: try to use it; unique constraint will protect duplicates
            try {
                // very small window, but we just return and let caller save
                return code;
            } catch (Exception ignore) {
                // retry if unique violation happened elsewhere
            }
        }
        // Fallback with random suffix to avoid infinite loop
        int rand = new java.security.SecureRandom().nextInt(900) + 100;
        return String.format("EMP-%s-%03d", yyyymm, rand);
    }

    public UserDto updateUser(Long id, User updated) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check phone number duplicate (if changed)
        if (updated.getPhoneNumber() != null && !updated.getPhoneNumber().isBlank()) {
            if (!updated.getPhoneNumber().equals(user.getPhoneNumber())) {
                if (userRepo.existsByPhoneNumber(updated.getPhoneNumber())) {
                    throw new RuntimeException("Số điện thoại đã được sử dụng bởi user khác");
                }
            }
        }

        user.setName(updated.getName());
        user.setPhoneNumber(updated.getPhoneNumber());
        user.setAvatar(updated.getAvatar());
        // Only update active and verified if explicitly provided
        if (updated.getActive() != null) {
            user.setActive(updated.getActive());
        }
        if (updated.getVerified() != null) {
            user.setVerified(updated.getVerified());
        }

        if (updated.getPassword() != null && !updated.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(updated.getPassword()));
        }

        if (updated.getRole() != null) {
            Role role = roleRepository.findById(updated.getRole().getId())
                    .orElseThrow(() -> new RuntimeException("Role not found"));
            user.setRole(role);
        }

        return UserMapper.toDto(userRepo.save(user));
    }

    public void deleteUser(Long id) {
        userRepo.deleteById(id);
    }
}
