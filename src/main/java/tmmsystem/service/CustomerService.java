package tmmsystem.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.dto.auth.LoginResponse;
import tmmsystem.entity.Customer;
import tmmsystem.entity.User;
import tmmsystem.repository.CustomerRepository;
import tmmsystem.repository.UserRepository;
import tmmsystem.util.JwtService;

import java.util.List;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public CustomerService(CustomerRepository customerRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public Customer findById(Long id) {
        return customerRepository.findById(id).orElseThrow();
    }

    @Transactional
    public Customer create(Customer customer, Long createdByUserId) {
        if (createdByUserId != null) {
            User createdBy = userRepository.findById(createdByUserId).orElseThrow();
            customer.setCreatedBy(createdBy);
        }
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer create(Customer customer) {
        // For self-registration (no createdBy)
        if (customer.getPassword() != null) {
            customer.setPassword(passwordEncoder.encode(customer.getPassword()));
        }
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer update(Long id, Customer updated) {
        Customer existing = customerRepository.findById(id).orElseThrow();
        existing.setCompanyName(updated.getCompanyName());
        existing.setContactPerson(updated.getContactPerson());
        existing.setEmail(updated.getEmail());
        existing.setPhoneNumber(updated.getPhoneNumber());
        existing.setAddress(updated.getAddress());
        existing.setTaxCode(updated.getTaxCode());
        existing.setActive(updated.getActive());
        existing.setVerified(updated.getVerified());
        existing.setRegistrationType(updated.getRegistrationType());
        return existing;
    }

    public void delete(Long id) {
        customerRepository.deleteById(id);
    }

    // ========== Authentication Methods ==========

    public LoginResponse authenticate(String email, String password) {
        Customer customer = customerRepository.findByEmail(email).orElse(null);
        if (customer == null || !Boolean.TRUE.equals(customer.getActive())) {
            return null;
        }
        if (!passwordEncoder.matches(password, customer.getPassword())) {
            return null;
        }
        String token = jwtService.generateToken(email, java.util.Map.of("role", "CUSTOMER"));
        return new LoginResponse(
                customer.getId(),           // userId
                customer.getContactPerson(), // name
                customer.getEmail(),         // email
                "CUSTOMER",                  // role
                customer.getActive(),        // active
                token,                       // accessToken
                jwtService.getExpirationMillis()  // expiresIn
        );
    }

    public boolean existsByEmail(String email) {
        return customerRepository.existsByEmail(email);
    }

    @Transactional
    public void verifyEmail(String token) {
        // Implement email verification logic
        // For now, just throw not implemented
        throw new RuntimeException("Email verification not implemented yet");
    }
}
