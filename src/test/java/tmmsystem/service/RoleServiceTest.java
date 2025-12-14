package tmmsystem.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tmmsystem.dto.RoleDto;
import tmmsystem.entity.Role;
import tmmsystem.mapper.RoleMapper;
import tmmsystem.repository.RoleRepository;
import tmmsystem.service.RoleService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleMapper roleMapper;

    @InjectMocks
    private RoleService roleService;

    // =========================================================================
    //                       TEST CASE: getAllRoles()
    // =========================================================================
    @Nested
    @DisplayName("getAllRoles() Tests")
    class GetAllRolesTests {

        @Test
        @DisplayName("UTCID01 - Status 200 và trả về List<Role>")
        void getAllRoles_Normal_ListRole() {
            Role role = new Role();
            role.setId(1L);
            role.setName("ADMIN");

            RoleDto dto = new RoleDto();
            dto.setId(1L);
            dto.setName("ADMIN");

            when(roleRepository.findAll()).thenReturn(List.of(role));
            when(roleMapper.toDto(role)).thenReturn(dto);

            List<RoleDto> result = roleService.findAll();

            assertEquals(1, result.size());
            assertEquals("ADMIN", result.get(0).getName());

            verify(roleRepository).findAll();
        }

        @Test
        @DisplayName("UTCID02 - Status 200 nhưng trả về List rỗng")
        void getAllRoles_EmptyList() {
            when(roleRepository.findAll()).thenReturn(Collections.emptyList());

            List<RoleDto> result = roleService.findAll();

            assertTrue(result.isEmpty());
            verify(roleRepository).findAll();
        }
    }

    // =========================================================================
    //                       TEST CASE: getRoleById()
    // =========================================================================
    @Nested
    @DisplayName("getRoleById() Tests")
    class GetRoleByIdTests {

        @Test
        @DisplayName("UTCID01 - ID = 1 → Status 200 → có dữ liệu")
        void getRoleById_ValidId() {
            Role role = new Role();
            role.setId(1L);
            role.setName("ADMIN");

            RoleDto dto = new RoleDto();
            dto.setId(1L);
            dto.setName("ADMIN");

            when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
            when(roleMapper.toDto(role)).thenReturn(dto);

            Optional<RoleDto> result = roleService.findById(1L);

            assertTrue(result.isPresent());
            assertEquals("ADMIN", result.get().getName());
        }

        @Test
        @DisplayName("UTCID02 - ID = 999 → Không tồn tại → trả về Optional.empty")
        void getRoleById_NotFound() {
            when(roleRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<RoleDto> result = roleService.findById(999L);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("UTCID03 - ID âm → Không hợp lệ → trả về empty")
        void getRoleById_NegativeId() {
            when(roleRepository.findById(-1L)).thenReturn(Optional.empty());

            Optional<RoleDto> result = roleService.findById(-1L);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("UTCID04 - ID null → không gọi repository → trả về empty")
        void getRoleById_NullId() {
            Optional<RoleDto> result = roleService.findById(null);
            assertTrue(result.isEmpty());

            verify(roleRepository, never()).findById(any());
        }
    }
}

