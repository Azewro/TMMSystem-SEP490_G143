package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import tmmsystem.dto.UserDto;
import tmmsystem.entity.Role;
import tmmsystem.entity.User;
import tmmsystem.repository.RoleRepository;
import tmmsystem.repository.UserRepository;
import tmmsystem.service.UserService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

    private User user;
    private Role role;

    @BeforeEach
    void setUp() {
        role = new Role();
        role.setId(1L);
        role.setName("USER");

        user = new User();
        user.setEmail("test@example.com");
        user.setName("Test User");
        user.setPassword("ValidPassword123");
        user.setRole(role);
    }

    @Nested
    @DisplayName("Create User Tests: Input Validation")
    class CreateUserTests {

        @Test
        @DisplayName("Normal Case: Should create user successfully with valid inputs")
        void createUser_Normal_Success() {
            // Given
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(roleRepository.findById(anyLong())).thenReturn(Optional.of(role));
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(user);

            // When
            UserDto createdUser = userService.createUser(user);

            // Then
            assertNotNull(createdUser, "Created user should not be null.");
            assertEquals("test@example.com", createdUser.getEmail(), "Email should match.");
            verify(userRepository).save(any(User.class));
            System.out.println("[SUCCESS] createUser_Normal_Success: User created successfully.");
        }

        @Test
        @DisplayName("Abnormal Case: Should fail when email already exists")
        void createUser_Abnormal_EmailExists() {
            // Given
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.createUser(user));
            assertEquals("Email already exists", exception.getMessage());
            System.out.println("[SUCCESS] createUser_Abnormal_EmailExists: Failed as expected. Email already exists.");
        }

        @Test
        @DisplayName("Boundary Case: Should fail when email is null")
        void createUser_Boundary_NullEmail() {
            // Given
            user.setEmail(null);
            when(userRepository.existsByEmail(null)).thenThrow(new IllegalArgumentException("Email cannot be null"));

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.createUser(user), "Expected an IllegalArgumentException for null email");
            System.out.println("[SUCCESS] createUser_Boundary_NullEmail: Failed as expected. Email is null.");
        }

        @Test
        @DisplayName("Boundary Case: Should create user even with invalid email format (current behavior)")
        void createUser_Boundary_InvalidEmailFormat() {
            // Given
            user.setEmail("invalid-email");
            when(userRepository.existsByEmail("invalid-email")).thenReturn(false);
            when(roleRepository.findById(anyLong())).thenReturn(Optional.of(role));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // When
            UserDto createdUser = userService.createUser(user);

            // Then
            assertNotNull(createdUser);
            assertEquals("invalid-email", createdUser.getEmail());
            System.out.println("[SUCCESS] createUser_Boundary_InvalidEmailFormat: User created with invalid email format.");
        }
        @Test
        @DisplayName("Boundary Case: Should create user with null password")
        void createUser_Boundary_NullPassword() {
            // Given
            user.setPassword(null);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(roleRepository.findById(anyLong())).thenReturn(Optional.of(role));
            when(userRepository.save(any(User.class))).thenReturn(user);
            // When
            UserDto createdUser = userService.createUser(user);
            // Then
            assertNotNull(createdUser);
            verify(passwordEncoder, never()).encode(any());
            System.out.println("[SUCCESS] createUser_Boundary_NullPassword: User created with null password.");
        }
    }

    @Nested
    @DisplayName("Update User Tests")
    class UpdateUserTests {

        private User existingUser;
        private Role userRole;
        private Role adminRole;

        @BeforeEach
        void setUpUpdate() {
            userRole = new Role();
            userRole.setId(1L);
            userRole.setName("USER");

            adminRole = new Role();
            adminRole.setId(2L);
            adminRole.setName("ADMIN");

            existingUser = new User();
            existingUser.setId(1L);
            existingUser.setEmail("test@example.com");
            existingUser.setName("Test User");
            existingUser.setPassword("encodedOldPassword");
            existingUser.setPhoneNumber("0123456789");
            existingUser.setRole(userRole);
        }

        @Test
        @DisplayName("Normal Case: Update basic info successfully")
        void updateUser_Normal_Success() {
            // Given
            User updatedInfo = new User();
            updatedInfo.setName("Updated Name");
            updatedInfo.setPhoneNumber("0987654321");

            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            UserDto resultDto = userService.updateUser(1L, updatedInfo);

            // Then
            assertEquals("Updated Name", resultDto.getName());
            assertEquals("0987654321", resultDto.getPhoneNumber());
            verify(passwordEncoder, never()).encode(any());
            System.out.println("[SUCCESS] updateUser_Normal_Success: User info updated successfully.");
        }

        @Test
        @DisplayName("Normal Case: Update with new valid password")
        void updateUser_Normal_WithNewPassword() {
            // Given
            User updatedInfo = new User();
            updatedInfo.setPassword("newPassword123");

            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            UserDto resultDto = userService.updateUser(1L, updatedInfo);

            // Then
            verify(passwordEncoder, times(1)).encode("newPassword123");
            assertEquals("encodedNewPassword", existingUser.getPassword());
            System.out.println("[SUCCESS] updateUser_Normal_WithNewPassword: User password updated successfully.");
        }

        @Test
        @DisplayName("Abnormal Case: User not found")
        void updateUser_Abnormal_UserNotFound() {
            // Given
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            // When & Then
            Exception e = assertThrows(RuntimeException.class, () -> userService.updateUser(99L, new User()));
            assertEquals("User not found", e.getMessage());
            System.out.println("[SUCCESS] updateUser_Abnormal_UserNotFound: Failed as expected. User not found.");
        }


        @Test
        @DisplayName("Boundary Case: Password is null")
        void updateUser_Boundary_NullPassword() {
            // Given
            User updatedInfo = new User();
            updatedInfo.setPassword(null);

            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            UserDto resultDto = userService.updateUser(1L, updatedInfo);

            // Then
            verify(passwordEncoder, never()).encode(any());
            assertEquals("encodedOldPassword", existingUser.getPassword());
            System.out.println("[SUCCESS] updateUser_Boundary_NullPassword: User password remains unchanged with null input.");
        }

        @Test
        @DisplayName("Boundary Case: Password is blank")
        void updateUser_Boundary_BlankPassword() {
            // Given
            User updatedInfo = new User();
            updatedInfo.setPassword("   ");

            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            UserDto resultDto = userService.updateUser(1L, updatedInfo);

            // Then
            verify(passwordEncoder, never()).encode(any());
            assertEquals("encodedOldPassword", existingUser.getPassword());
            System.out.println("[SUCCESS] updateUser_Boundary_BlankPassword: User password remains unchanged with blank input.");
        }

        @Test
        @DisplayName("Boundary Case: Phone number is invalid format")
        void updateUser_Boundary_InvalidPhoneNumber() {
            // Given
            User updatedInfo = new User();
            updatedInfo.setPhoneNumber("not-a-number");

            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            UserDto resultDto = userService.updateUser(1L, updatedInfo);

            // Then
            assertEquals("not-a-number", resultDto.getPhoneNumber());
            System.out.println("[SUCCESS] updateUser_Boundary_InvalidPhoneNumber: User phone number updated with invalid format.");
        }

        @Test
        @DisplayName("Boundary Case: Phone number is blank")
        void updateUser_Boundary_BlankPhoneNumber() {
            // Given
            User updatedInfo = new User();
            updatedInfo.setPhoneNumber("   ");

            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            UserDto resultDto = userService.updateUser(1L, updatedInfo);

            // Then
            assertEquals("   ", resultDto.getPhoneNumber());
            System.out.println("[SUCCESS] updateUser_Boundary_BlankPhoneNumber: User phone number updated with blank input.");
        }
    }
}
