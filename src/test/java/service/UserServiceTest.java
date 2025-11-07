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
            String testName = "Normal Case: Create User Successfully";
            try {
                when(userRepository.existsByEmail(anyString())).thenReturn(false);
                when(roleRepository.findById(anyLong())).thenReturn(Optional.of(role));
                when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
                when(userRepository.save(any(User.class))).thenReturn(user);

                UserDto createdUser = userService.createUser(user);

                assertNotNull(createdUser, "User should have been created, but was null.");
                assertEquals("test@example.com", createdUser.getEmail(), "The email of the created user is incorrect.");

                System.out.println("Log: Test Case '" + testName + "' -> PASS");
            } catch (Throwable e) {
                System.out.println("Log: Test Case '" + testName + "' -> FAIL: " + e.getMessage());
                throw e;
            }
        }

        @Test
        @DisplayName("Abnormal Case: Should fail when email already exists")
        void createUser_Abnormal_EmailExists() {
            String testName = "Abnormal Case: Email Already Exists";
            try {
                when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

                RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                    userService.createUser(user);
                });

                assertEquals("Email already exists", exception.getMessage(), "Expected error message for existing email was not found.");

                System.out.println("Log: Test Case '" + testName + "' -> PASS (Correctly failed to create user)");
            } catch (Throwable e) {
                System.out.println("Log: Test Case '" + testName + "' -> FAIL: " + e.getMessage());
                throw e;
            }
        }

        @Test
        @DisplayName("Boundary Case: Should fail when email is null")
        void createUser_Boundary_NullEmail() {
            String testName = "Boundary Case: Null Email";
            try {
                user.setEmail(null);
                when(userRepository.existsByEmail(null)).thenThrow(new IllegalArgumentException("Email cannot be null"));

                assertThrows(IllegalArgumentException.class, () -> {
                    userService.createUser(user);
                });

                System.out.println("Log: Test Case '" + testName + "' -> PASS (Correctly failed with null email)");
            } catch (Throwable e) {
                System.out.println("Log: Test Case '" + testName + "' -> FAIL: " + e.getMessage());
                throw e;
            }
        }

        @Test
        @DisplayName("Boundary Case: Should create user even with invalid email format (current behavior)")
        void createUser_Boundary_InvalidEmailFormat() {
            String testName = "Boundary Case: Invalid Email Format";
            try {
                user.setEmail("invalid-email");
                when(userRepository.existsByEmail("invalid-email")).thenReturn(false);
                when(roleRepository.findById(anyLong())).thenReturn(Optional.of(role));
                when(userRepository.save(any(User.class))).thenReturn(user);

                UserDto createdUser = userService.createUser(user);

                assertNotNull(createdUser);
                assertEquals("invalid-email", createdUser.getEmail());

                System.out.println("Log: Test Case '" + testName + "' -> PASS (Confirmed user can be created with invalid email)");
            } catch (Throwable e) {
                System.out.println("Log: Test Case '" + testName + "' -> FAIL: " + e.getMessage());
                throw e;
            }
        }

        @Test
        @DisplayName("Boundary Case: Should create user with null password")
        void createUser_Boundary_NullPassword() {
            String testName = "Boundary Case: Null Password";
            try {
                user.setPassword(null);
                when(userRepository.existsByEmail(anyString())).thenReturn(false);
                when(roleRepository.findById(anyLong())).thenReturn(Optional.of(role));
                when(userRepository.save(any(User.class))).thenReturn(user);

                UserDto createdUser = userService.createUser(user);

                assertNotNull(createdUser);
                verify(passwordEncoder, never()).encode(any());

                System.out.println("Log: Test Case '" + testName + "' -> PASS (User created with null password)");
            } catch (Throwable e) {
                System.out.println("Log: Test Case '" + testName + "' -> FAIL: " + e.getMessage());
                throw e;
            }
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
            String testName = "Normal Case: Update basic info";
            try {
                User updatedInfo = new User();
                updatedInfo.setName("Updated Name");
                updatedInfo.setPhoneNumber("0987654321");

                when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
                when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

                UserDto resultDto = userService.updateUser(1L, updatedInfo);

                assertEquals("Updated Name", resultDto.getName());
                assertEquals("0987654321", resultDto.getPhoneNumber());
                verify(passwordEncoder, never()).encode(any());

                System.out.println("Log: Test Case '" + testName + "' -> PASS");
            } catch (Throwable e) {
                System.out.println("Log: Test Case '" + testName + "' -> FAIL: " + e.getMessage());
                throw e;
            }
        }

        @Test
        @DisplayName("Normal Case: Update with new valid password")
        void updateUser_Normal_WithNewPassword() {
            String testName = "Normal Case: Update with new password";
            try {
                User updatedInfo = new User();
                updatedInfo.setPassword("newPassword123");

                when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
                when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
                when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

                userService.updateUser(1L, updatedInfo);

                verify(passwordEncoder, times(1)).encode("newPassword123");
                assertEquals("encodedNewPassword", existingUser.getPassword());

                System.out.println("Log: Test Case '" + testName + "' -> PASS");
            } catch (Throwable e) {
                System.out.println("Log: Test Case '" + testName + "' -> FAIL: " + e.getMessage());
                throw e;
            }
        }

        @Test
        @DisplayName("Normal Case: Update with new valid role")
        void updateUser_Normal_WithNewRole() {
            String testName = "Normal Case: Update with new role";
            try {
                User updatedInfo = new User();
                updatedInfo.setRole(adminRole);

                when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
                when(roleRepository.findById(2L)).thenReturn(Optional.of(adminRole));
                when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

                UserDto resultDto = userService.updateUser(1L, updatedInfo);

                assertEquals("ADMIN", resultDto.getRoleName());

                System.out.println("Log: Test Case '" + testName + "' -> PASS");
            } catch (Throwable e) {
                System.out.println("Log: Test Case '" + testName + "' -> FAIL: " + e.getMessage());
                throw e;
            }
        }

        @Test
        @DisplayName("Abnormal Case: User not found")
        void updateUser_Abnormal_UserNotFound() {
            String testName = "Abnormal Case: User not found";
            try {
                when(userRepository.findById(99L)).thenReturn(Optional.empty());

                Exception e = assertThrows(RuntimeException.class, () -> userService.updateUser(99L, new User()));
                assertEquals("User not found", e.getMessage());

                System.out.println("Log: Test Case '" + testName + "' -> PASS (Correctly threw exception)");
            } catch (Throwable e) {
                System.out.println("Log: Test Case '" + testName + "' -> FAIL: " + e.getMessage());
                throw e;
            }
        }

        @Test
        @DisplayName("Abnormal Case: Role not found")
        void updateUser_Abnormal_RoleNotFound() {
            String testName = "Abnormal Case: Role not found";
            try {
                User updatedInfo = new User();
                updatedInfo.setRole(adminRole);

                when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
                when(roleRepository.findById(2L)).thenReturn(Optional.empty());

                Exception e = assertThrows(RuntimeException.class, () -> userService.updateUser(1L, updatedInfo));
                assertEquals("Role not found", e.getMessage());

                System.out.println("Log: Test Case '" + testName + "' -> PASS (Correctly threw exception)");
            } catch (Throwable e) {
                System.out.println("Log: Test Case '" + testName + "' -> FAIL: " + e.getMessage());
                throw e;
            }
        }

        @Test
        @DisplayName("Boundary Case: Password is null")
        void updateUser_Boundary_NullPassword() {
            String testName = "Boundary Case: Password is null";
            try {
                User updatedInfo = new User();
                updatedInfo.setPassword(null);

                when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
                when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

                userService.updateUser(1L, updatedInfo);

                verify(passwordEncoder, never()).encode(any());
                assertEquals("encodedOldPassword", existingUser.getPassword());

                System.out.println("Log: Test Case '" + testName + "' -> PASS");
            } catch (Throwable e) {
                System.out.println("Log: Test Case '" + testName + "' -> FAIL: " + e.getMessage());
                throw e;
            }
        }

        @Test
        @DisplayName("Boundary Case: Password is blank")
        void updateUser_Boundary_BlankPassword() {
            String testName = "Boundary Case: Password is blank";
            try {
                User updatedInfo = new User();
                updatedInfo.setPassword("   ");

                when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
                when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

                userService.updateUser(1L, updatedInfo);

                verify(passwordEncoder, never()).encode(any());
                assertEquals("encodedOldPassword", existingUser.getPassword());

                System.out.println("Log: Test Case '" + testName + "' -> PASS");
            } catch (Throwable e) {
                System.out.println("Log: Test Case '" + testName + "' -> FAIL: " + e.getMessage());
                throw e;
            }
        }

        @Test
        @DisplayName("Boundary Case: Phone number is invalid format")
        void updateUser_Boundary_InvalidPhoneNumber() {
            String testName = "Boundary Case: Phone number is invalid";
            try {
                User updatedInfo = new User();
                updatedInfo.setPhoneNumber("not-a-number");

                when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
                when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

                UserDto resultDto = userService.updateUser(1L, updatedInfo);

                assertEquals("not-a-number", resultDto.getPhoneNumber());

                System.out.println("Log: Test Case '" + testName + "' -> PASS (Confirmed invalid phone is saved)");
            } catch (Throwable e) {
                System.out.println("Log: Test Case '" + testName + "' -> FAIL: " + e.getMessage());
                throw e;
            }
        }
    }
}
