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
import tmmsystem.entity.User;
import tmmsystem.entity.Customer;
import tmmsystem.repository.UserRepository;
import tmmsystem.repository.CustomerRepository;
import tmmsystem.util.JwtService;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository,
            CustomerRepository customerRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String email = jwtService.parseToken(token).getSubject();
                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    User user = userRepository.findByEmail(email).orElse(null);
                    if (user != null && Boolean.TRUE.equals(user.getActive())
                            && jwtService.isTokenValid(token, email)) {
                        String roleName = user.getRole() != null ? user.getRole().getName() : "USER";
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleName)));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        Customer cu = customerRepository.findByEmail(email).orElse(null);
                        if (cu != null && Boolean.TRUE.equals(cu.getActive())
                                && jwtService.isTokenValid(token, email)) {
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    email,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                    }
                }
            } catch (Exception ignored) {
                // invalid token -> continue without authentication
            }
        }

        filterChain.doFilter(request, response);
    }
}
