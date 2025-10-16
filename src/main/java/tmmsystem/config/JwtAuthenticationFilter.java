package tmmsystem.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tmmsystem.entity.Customer;
import tmmsystem.entity.User;
import tmmsystem.repository.CustomerRepository;
import tmmsystem.repository.UserRepository;
import tmmsystem.util.JwtService;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository, CustomerRepository customerRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                System.out.println("🔍 JWT Filter - Token received: " + token.substring(0, 20) + "...");

                String email = jwtService.parseToken(token).getSubject();
                System.out.println("✅ JWT Filter - Email extracted: " + email);

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // Try internal user first
                    User user = userRepository.findByEmail(email).orElse(null);

                    if (user != null && Boolean.TRUE.equals(user.getActive()) && jwtService.isTokenValid(token, email)) {
                        // EAGER FETCH role name to avoid LazyInitializationException
                        String roleName = "USER";  // Default
                        if (user.getRole() != null && user.getRole().getName() != null) {
                            roleName = user.getRole().getName();
                        }
                        System.out.println("✅ JWT Filter - User found: " + email + ", Role: " + roleName);

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleName))
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        System.out.println("✅ JWT Filter - Authentication set with authority: ROLE_" + roleName);
                    } else {
                        System.out.println("⚠️ JWT Filter - User not found or inactive: " + email);

                        // Try customer
                        Customer customer = customerRepository.findByEmail(email).orElse(null);
                        if (customer != null && Boolean.TRUE.equals(customer.getActive()) && jwtService.isTokenValid(token, email)) {
                            System.out.println("✅ JWT Filter - Customer found: " + email);
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    email,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                            );
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            System.out.println("✅ JWT Filter - Authentication set with authority: ROLE_CUSTOMER");
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ JWT Filter ERROR: " + e.getClass().getName() + " - " + e.getMessage());
                e.printStackTrace();
            }
        }

        filterChain.doFilter(request, response);
    }
}
