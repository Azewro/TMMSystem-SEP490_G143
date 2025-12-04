package tmmsystem.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.entity.Customer;
import tmmsystem.entity.User;
import tmmsystem.entity.OtpToken;
import tmmsystem.repository.CustomerRepository;
import tmmsystem.repository.UserRepository;
import tmmsystem.repository.OtpTokenRepository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final tmmsystem.util.JwtService jwtService;
    private final MailService mailService;
    private final String appBaseUrl;
    private final OtpTokenRepository otpTokenRepository;

    public CustomerService(CustomerRepository customerRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           tmmsystem.util.JwtService jwtService,
                           MailService mailService,
                           @Value("${app.base-url}") String appBaseUrl,
                           OtpTokenRepository otpTokenRepository) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailService = mailService;
        this.appBaseUrl = appBaseUrl;
        this.otpTokenRepository = otpTokenRepository;
    }

    public List<Customer> findAll() { return customerRepository.findAll(); }
    
    public Page<Customer> findAll(Pageable pageable, String search, Boolean isActive) {
        if (search != null && !search.trim().isEmpty() || isActive != null) {
            String searchLower = search != null ? search.trim().toLowerCase() : "";
            Boolean finalIsActive = isActive;
            return customerRepository.findAll((root, query, cb) -> {
                var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                
                // Search predicate
                if (search != null && !search.trim().isEmpty()) {
                    var searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("companyName")), "%" + searchLower + "%"),
                        cb.like(cb.lower(root.get("contactPerson")), "%" + searchLower + "%"),
                        cb.like(cb.lower(root.get("email")), "%" + searchLower + "%"),
                        cb.like(cb.lower(root.get("phoneNumber")), "%" + searchLower + "%"),
                        cb.like(cb.lower(root.get("taxCode")), "%" + searchLower + "%")
                    );
                    predicates.add(searchPredicate);
                }
                
                // Status filter
                if (finalIsActive != null) {
                    predicates.add(cb.equal(root.get("active"), finalIsActive));
                }
                
                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            }, pageable);
        } else {
            return customerRepository.findAll(pageable);
        }
    }
    public Customer findById(Long id) { return customerRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Customer not found")); }
    public Customer findByEmailOrThrow(String email) { return customerRepository.findByEmail(email).orElseThrow(); }
    public boolean existsByEmail(String email) { return customerRepository.existsByEmail(email); }
    public boolean existsByPhoneNumber(String phoneNumber) { return customerRepository.existsByPhoneNumber(phoneNumber); }
    
    // Check if email exists for another customer (excluding current customer)
    public boolean isEmailTakenByOtherCustomer(Long excludeCustomerId, String email) {
        return customerRepository.findByEmail(email)
                .map(c -> !c.getId().equals(excludeCustomerId))
                .orElse(false);
    }
    
    // Check if phone number exists for another customer (excluding current customer)
    public boolean isPhoneNumberTakenByOtherCustomer(Long excludeCustomerId, String phoneNumber) {
        return customerRepository.findByPhoneNumber(phoneNumber)
                .map(c -> !c.getId().equals(excludeCustomerId))
                .orElse(false);
    }

    @Transactional
    public Customer create(Customer customer, Long createdByUserId) {
        // Check email duplicate
        if (customer.getEmail() != null && !customer.getEmail().isBlank()) {
            if (customerRepository.existsByEmail(customer.getEmail())) {
                throw new RuntimeException("Email đã được sử dụng");
            }
        }
        
        // Check phone number duplicate
        if (customer.getPhoneNumber() != null && !customer.getPhoneNumber().isBlank()) {
            if (customerRepository.existsByPhoneNumber(customer.getPhoneNumber())) {
                throw new RuntimeException("Số điện thoại đã được sử dụng");
            }
        }
        
        if (createdByUserId != null) {
            User createdBy = userRepository.findById(createdByUserId).orElseThrow();
            customer.setCreatedBy(createdBy);
        }
        if (customer.getCustomerCode() == null || customer.getCustomerCode().isBlank()) {
            customer.setCustomerCode(generateCustomerCode());
        }
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer update(Long id, Customer updated) {
        Customer existing = customerRepository.findById(id).orElseThrow();
        
        // Check email duplicate (if changed)
        if (updated.getEmail() != null && !updated.getEmail().equals(existing.getEmail())) {
            if (isEmailTakenByOtherCustomer(id, updated.getEmail())) {
                throw new RuntimeException("Email đã được sử dụng bởi khách hàng khác");
            }
        }
        
        // Check phone number duplicate (if changed)
        if (updated.getPhoneNumber() != null && !updated.getPhoneNumber().equals(existing.getPhoneNumber())) {
            if (isPhoneNumberTakenByOtherCustomer(id, updated.getPhoneNumber())) {
                throw new RuntimeException("Số điện thoại đã được sử dụng bởi khách hàng khác");
            }
        }
        
        existing.setCompanyName(updated.getCompanyName());
        existing.setTaxCode(updated.getTaxCode());
        existing.setBusinessLicense(updated.getBusinessLicense());
        existing.setAddress(updated.getAddress());
        existing.setContactPerson(updated.getContactPerson());
        existing.setEmail(updated.getEmail());
        existing.setPhoneNumber(updated.getPhoneNumber());
        existing.setPosition(updated.getPosition());
        existing.setIndustry(updated.getIndustry());
        existing.setCustomerType(updated.getCustomerType());
        existing.setCreditLimit(updated.getCreditLimit());
        existing.setPaymentTerms(updated.getPaymentTerms());
        
        // Only update isActive, isVerified, and registrationType if explicitly provided (not null)
        // This prevents customer self-updates from accidentally deactivating their account
        if (updated.getActive() != null) {
            existing.setActive(updated.getActive());
        }
        if (updated.getVerified() != null) {
            existing.setVerified(updated.getVerified());
        }
        if (updated.getRegistrationType() != null) {
            existing.setRegistrationType(updated.getRegistrationType());
        }
        
        return existing;
    }

    public void delete(Long id) { customerRepository.deleteById(id); }

    @Transactional
    public void setActive(Long id, boolean active) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        customer.setActive(active);
        customerRepository.save(customer);
    }

    // ===== Customer portal OTP auth =====
    @Transactional
    public void requestOtp(String emailOrPhone) {
        Customer customer = findByEmailOrPhoneOrThrow(emailOrPhone);
        String code = generateNumericCode(6);
        OtpToken otp = new OtpToken();
        otp.setCustomer(customer);
        otp.setOtpCode(code);
        otp.setExpiredAt(java.time.Instant.now().plus(java.time.Duration.ofMinutes(5)));
        otpTokenRepository.save(otp);
        // send via email; for phone/SMS integration can be added later
        if (customer.getEmail() != null) {
            mailService.send(customer.getEmail(), "Your OTP Code", "Your OTP is: " + code + "\nIt expires in 5 minutes.");
        }
    }

    @Transactional
    public tmmsystem.dto.auth.CustomerLoginResponse verifyOtpAndLogin(String emailOrPhone, String otpCode) {
        Customer customer = findByEmailOrPhoneOrThrow(emailOrPhone);
        OtpToken latest = otpTokenRepository.findTopByCustomerIdAndUsedFalseOrderByCreatedAtDesc(customer.getId())
                .orElseThrow(() -> new RuntimeException("No OTP requested"));
        if (Boolean.TRUE.equals(latest.getUsed())) { throw new RuntimeException("OTP already used"); }
        if (java.time.Instant.now().isAfter(latest.getExpiredAt())) { throw new RuntimeException("OTP expired"); }
        if (!latest.getOtpCode().equals(otpCode)) { throw new RuntimeException("Invalid OTP"); }
        latest.setUsed(true); otpTokenRepository.save(latest);
        String token = jwtService.generateToken(customer.getEmail(), java.util.Map.of(
                "cid", customer.getId(),
                "role", "CUSTOMER",
                "fpc", customer.getForcePasswordChange()
        ));
        long expiresIn = jwtService.getExpirationMillis();
        customer.setLastLoginAt(java.time.Instant.now());
        return new tmmsystem.dto.auth.CustomerLoginResponse(
                customer.getContactPerson(),
                customer.getEmail(),
                "CUSTOMER",
                Boolean.TRUE.equals(customer.getActive()),
                token,
                expiresIn,
                customer.getId(),
                customer.getCompanyName(),
                customer.getForcePasswordChange()
        );
    }

    // ===== Password-based flows for customers (optional) =====

    /** Cấp mật khẩu tạm cho khách hàng (hash lưu trong customer.password). */
    @Transactional
    public String provisionTemporaryPassword(Long customerId) {
        Customer customer = customerRepository.findById(customerId).orElseThrow();
        String raw = generateTempPassword();
        customer.setPassword(passwordEncoder.encode(raw));
        customer.setForcePasswordChange(true); // bắt buộc đổi lần đầu
        customerRepository.save(customer);
        return raw; // trả về để gửi email/SMS
    }

    /** Đăng nhập bằng email HOẶC số điện thoại + password cho customer. */
    public tmmsystem.dto.auth.CustomerLoginResponse customerPasswordLoginEmailOrPhone(String emailOrPhone, String password) {
        Customer customer = findByEmailOrPhoneOrThrow(emailOrPhone);
        if (customer.getPassword() == null || !passwordEncoder.matches(password, customer.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        customer.setLastLoginAt(java.time.Instant.now());
        return buildCustomerLoginResponse(customer);
    }

    /** Đăng nhập bằng email + password cho customer. */
    public tmmsystem.dto.auth.CustomerLoginResponse customerPasswordLogin(String email, String password) {
        Customer customer = customerRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        if (customer.getPassword() == null || !passwordEncoder.matches(password, customer.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        customer.setLastLoginAt(java.time.Instant.now());
        return buildCustomerLoginResponse(customer);
    }

    private tmmsystem.dto.auth.CustomerLoginResponse buildCustomerLoginResponse(Customer customer){
        String subject = customer.getEmail()!=null && !customer.getEmail().isBlank() ? customer.getEmail() : customer.getPhoneNumber();
        if (subject==null || subject.isBlank()) { subject = "customer-"+customer.getId(); }
        String token = jwtService.generateToken(subject, java.util.Map.of(
                "cid", customer.getId(),
                "role", "CUSTOMER",
                "fpc", customer.getForcePasswordChange()
        ));
        long expiresIn = jwtService.getExpirationMillis();
        return new tmmsystem.dto.auth.CustomerLoginResponse(
                customer.getContactPerson(),
                customer.getEmail(),
                "CUSTOMER",
                Boolean.TRUE.equals(customer.getActive()),
                token,
                expiresIn,
                customer.getId(),
                customer.getCompanyName(),
                customer.getForcePasswordChange()
        );
    }

    /** Đổi mật khẩu (lần đầu sau khi được cấp). */
    @Transactional
    public void changeCustomerPassword(Long customerId, String oldPassword, String newPassword) {
        Customer customer = customerRepository.findById(customerId).orElseThrow();
        if (customer.getPassword() != null && !passwordEncoder.matches(oldPassword, customer.getPassword())) {
            throw new RuntimeException("Sai mật khẩu hiện tại");
        }
        customer.setPassword(passwordEncoder.encode(newPassword));
        customer.setForcePasswordChange(false); // đã đổi xong lần đầu
        customerRepository.save(customer);
    }

    /** Yêu cầu reset mật khẩu cho customer (gửi code). */
    @Transactional
    public void requestCustomerPasswordReset(String email) {
        Customer customer = customerRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        if (!Boolean.TRUE.equals(customer.getActive())) {
            throw new RuntimeException("Customer inactive");
        }
        String code = generateNumericCode(6);
        OtpToken otp = new OtpToken();
        otp.setCustomer(customer);
        otp.setOtpCode(code);
        otp.setExpiredAt(java.time.Instant.now().plus(java.time.Duration.ofMinutes(10)));
        otpTokenRepository.save(otp);
        mailService.send(customer.getEmail(), "Customer Password Reset Code", "Your reset code: " + code + "\nExpires in 10 minutes.");
    }

    /** Xác thực mã reset & đặt mật khẩu mới ngẫu nhiên (gửi qua email). */
    @Transactional
    public void verifyCustomerResetCode(String email, String code) {
        Customer customer = customerRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        OtpToken latest = otpTokenRepository.findTopByCustomerIdAndUsedFalseOrderByCreatedAtDesc(customer.getId())
                .orElseThrow(() -> new RuntimeException("No reset code requested"));
        if (java.time.Instant.now().isAfter(latest.getExpiredAt())) {
            throw new RuntimeException("Code expired");
        }
        if (!latest.getOtpCode().equals(code)) {
            throw new RuntimeException("Invalid code");
        }
        latest.setUsed(true);
        otpTokenRepository.save(latest);
        String newPass = generateTempPassword();
        customer.setPassword(passwordEncoder.encode(newPass));
        customerRepository.save(customer);
        mailService.send(customer.getEmail(), "Your new password", "New password: " + newPass + "\nPlease change after login.");
    }

    /** Đổi mật khẩu qua email + currentPassword giống user flow. */
    @Transactional
    public void changeCustomerPasswordByEmail(String email, String currentPassword, String newPassword) {
        Customer customer = customerRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        if (customer.getPassword() == null || !passwordEncoder.matches(currentPassword, customer.getPassword())) {
            throw new RuntimeException("Current password incorrect");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new RuntimeException("New password must not be empty");
        }
        // Validate new password is not the same as current password
        if (customer.getPassword() != null && passwordEncoder.matches(newPassword, customer.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới không được trùng với mật khẩu hiện tại.");
        }
        customer.setPassword(passwordEncoder.encode(newPassword));
        customer.setForcePasswordChange(false); // đã đổi xong lần đầu
        customerRepository.save(customer);
    }

    private Customer findByEmailOrPhoneOrThrow(String emailOrPhone) {
        String key = emailOrPhone == null ? null : emailOrPhone.trim().toLowerCase();
        java.util.Optional<Customer> byEmail = (key != null && key.contains("@")) ? customerRepository.findByEmail(key) : java.util.Optional.empty();
        if (byEmail.isPresent()) return byEmail.get();
        return customerRepository.findByPhoneNumber(emailOrPhone)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    public Long getCustomerIdFromToken(String token) {
        try {
            io.jsonwebtoken.Claims claims = jwtService.parseToken(token);
            String email = claims.getSubject();
            return customerRepository.findByEmail(email)
                    .map(Customer::getId)
                    .orElseThrow(() -> new RuntimeException("Token không hợp lệ"));
        } catch (Exception ex) {
            throw new RuntimeException("Token không hợp lệ: " + ex.getMessage());
        }
    }

    public Customer getCustomerFromToken(String token) {
        try {
            io.jsonwebtoken.Claims claims = jwtService.parseToken(token);
            String email = claims.getSubject();
            return customerRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Customer not found for token"));
        } catch (Exception ex) {
            throw new RuntimeException("Token không hợp lệ: " + ex.getMessage());
        }
    }

    private String generateNumericCode(int length) {
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) { sb.append(random.nextInt(10)); }
        return sb.toString();
    }

    private String generateCustomerCode() {
        java.time.LocalDate now = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
        String yyyymm = String.format("%04d%02d", now.getYear(), now.getMonthValue());
        String suffix = generateNumericCode(3);
        return "CUS-" + yyyymm + "-" + suffix;
    }

    private String generateTempPassword() {
        // 10-ký tự: chữ + số, dễ đọc
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        java.security.SecureRandom r = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }
}

