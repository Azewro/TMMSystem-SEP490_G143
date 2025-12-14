package tmmsystem.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import tmmsystem.entity.Customer;
import tmmsystem.entity.User;
import tmmsystem.repository.CustomerRepository;
import tmmsystem.repository.UserRepository;
import tmmsystem.service.CustomerService;
import tmmsystem.service.MailService;
import tmmsystem.util.JwtService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private MailService mailService;

    @InjectMocks
    private CustomerService customerService;

    @BeforeEach
    void init() {}

    // =====================================================================
    //  TEST CREATE CUSTOMER — Based on 23 test cases in your Excel sheet
    // =====================================================================

    @Nested
    @DisplayName("create() Customer Tests")
    class CreateCustomerTests {

        @Test
        @DisplayName("UTCID01 - Normal Case: Full valid fields, createdById exists")
        void create_Normal_FullValid() {
            Customer input = new Customer();
            input.setCompanyName("Công ty TNHH ABC");
            input.setContactPerson("Nguyễn Văn A");
            input.setEmail("abc@example.com");
            input.setPhoneNumber("0901234567");
            input.setAddress("123 Đường ABC");
            input.setTaxCode("0312345678");
            input.setActive(true);
            input.setVerified(false);
            input.setRegistrationType("IMPORT");
            input.setPassword("rawpass");

            User creator = new User();
            creator.setId(1L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
            when(passwordEncoder.encode("rawpass")).thenReturn("encoded123");
            when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));

            Customer result = customerService.create(input, 1L);

            assertEquals("encoded123", result.getPassword());
            assertEquals(creator, result.getCreatedBy());
            assertEquals("IMPORT", result.getRegistrationType());
        }

        @Test
        @DisplayName("UTCID02 - Null companyName should still save (business accepts null)")
        void create_NullCompanyName() {
            Customer input = new Customer();
            input.setCompanyName(null);

            when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));

            Customer result = customerService.create(input, null);
            assertNull(result.getCompanyName());
        }

        @Test
        @DisplayName("UTCID03 - Null contactPerson")
        void create_NullContactPerson() {
            Customer input = new Customer();
            input.setContactPerson(null);

            when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));

            Customer res = customerService.create(input, null);
            assertNull(res.getContactPerson());
        }

        @Test
        @DisplayName("UTCID04 - Null email")
        void create_NullEmail() {
            Customer input = new Customer();
            input.setEmail(null);

            when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));

            Customer res = customerService.create(input, null);
            assertNull(res.getEmail());
        }

        @Test
        @DisplayName("UTCID05 - Null phoneNumber")
        void create_NullPhone() {
            Customer input = new Customer();
            input.setPhoneNumber(null);

            when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));
            Customer res = customerService.create(input, null);

            assertNull(res.getPhoneNumber());
        }

        @Test
        @DisplayName("UTCID06 - Null position should not throw")
        void create_NullPosition() {
            Customer input = new Customer();
            input.setPosition(null);

            when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));
            Customer res = customerService.create(input, null);

            assertNull(res.getPosition());
        }

        @Test
        @DisplayName("UTCID07 - Null address")
        void create_NullAddress() {
            Customer input = new Customer();
            input.setAddress(null);

            when(customerRepository.save(any(Customer.class))).thenReturn(input);

            Customer res = customerService.create(input, null);
            assertNull(res.getAddress());
        }

        @Test
        @DisplayName("UTCID08 - Null taxCode")
        void create_NullTaxCode() {
            Customer input = new Customer();
            input.setTaxCode(null);

            when(customerRepository.save(any(Customer.class))).thenReturn(input);
            Customer res = customerService.create(input, null);

            assertNull(res.getTaxCode());
        }

        @Test
        @DisplayName("UTCID09 - Null isVerified should default to FALSE")
        void create_NullIsVerified() {
            Customer input = new Customer();
            input.setVerified(null);

            when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));

            Customer result = customerService.create(input, null);
            assertFalse(result.getVerified());
        }

        @Test
        @DisplayName("UTCID10 - Null isActive should default to TRUE")
        void create_NullIsActive() {
            Customer input = new Customer();
            input.setActive(null);

            when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));

            Customer result = customerService.create(input, null);
            assertTrue(result.getActive());
        }

        @Test
        @DisplayName("UTCID11 - Null registrationType defaults to SALES_CREATED")
        void create_NullRegistrationType() {
            Customer input = new Customer();
            input.setRegistrationType(null);

            when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));

            Customer result = customerService.create(input, null);
            assertEquals("SALES_CREATED", result.getRegistrationType());
        }

        @Test
        @DisplayName("UTCID12 - createdById exists → set CreatedBy")
        void create_CreatedByExists() {
            Customer cust = new Customer();
            User u = new User(); u.setId(999L);

            when(userRepository.findById(999L)).thenReturn(Optional.of(u));
            when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));

            Customer result = customerService.create(cust, 999L);
            assertEquals(u, result.getCreatedBy());
        }

        @Test
        @DisplayName("UTCID13 - createdById not exists → throw RuntimeException")
        void create_CreatedByNotExists() {
            Customer cust = new Customer();
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> customerService.create(cust, 999L));
        }

        @Test
        @DisplayName("UTCID14 - With password → should encode")
        void create_WithPassword() {
            Customer cust = new Customer();
            cust.setPassword("mypassword");

            when(passwordEncoder.encode("mypassword")).thenReturn("encodedPass");
            when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));

            Customer result = customerService.create(cust, null);
            assertEquals("encodedPass", result.getPassword());
        }

        @Test
        @DisplayName("UTCID15 - Null password → no encoding")
        void create_NullPassword() {
            Customer cust = new Customer();
            cust.setPassword(null);

            when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));

            Customer result = customerService.create(cust, null);
            assertNull(result.getPassword());
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("UTCID16 - Empty companyName still OK")
        void create_EmptyCompanyName() {
            Customer cust = new Customer();
            cust.setCompanyName("");

            when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));
            Customer result = customerService.create(cust, null);

            assertEquals("", result.getCompanyName());
        }

        @Test
        @DisplayName("UTCID17 - Empty address")
        void create_EmptyAddress() {
            Customer cust = new Customer();
            cust.setAddress("");

            when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));
            Customer result = customerService.create(cust, null);

            assertEquals("", result.getAddress());
        }

        @Test
        @DisplayName("UTCID18 - Empty taxCode")
        void create_EmptyTaxCode() {
            Customer cust = new Customer();
            cust.setTaxCode("");

            when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));
            Customer result = customerService.create(cust, null);

            assertEquals("", result.getTaxCode());
        }

        @Test
        @DisplayName("UTCID19 - Empty email")
        void create_EmptyEmail() {
            Customer cust = new Customer();
            cust.setEmail("");

            when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));
            Customer result = customerService.create(cust, null);

            assertEquals("", result.getEmail());
        }

        @Test
        @DisplayName("UTCID20 - Empty phoneNumber")
        void create_EmptyPhone() {
            Customer cust = new Customer();
            cust.setPhoneNumber("");

            when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));
            Customer result = customerService.create(cust, null);

            assertEquals("", result.getPhoneNumber());
        }

        @Test
        @DisplayName("UTCID21 - createdById = null → no creator")
        void create_NoCreatedBy() {
            Customer cust = new Customer();

            when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));
            Customer result = customerService.create(cust, null);

            assertNull(result.getCreatedBy());
        }

        @Test
        @DisplayName("UTCID22 - createdById negative")
        void create_NegativeCreatedById() {
            Customer cust = new Customer();

            when(userRepository.findById(-1L)).thenReturn(Optional.empty());
            assertThrows(RuntimeException.class, () -> customerService.create(cust, -1L));
        }

        @Test
        @DisplayName("UTCID23 - createdById = 0")
        void create_ZeroCreatedById() {
            Customer cust = new Customer();

            when(userRepository.findById(0L)).thenReturn(Optional.empty());
            assertThrows(RuntimeException.class, () -> customerService.create(cust, 0L));
        }
    }
    // =====================================================================
//  TEST GET CUSTOMER BY ID — 4 test cases theo Excel bạn gửi
// =====================================================================

    @Nested
    @DisplayName("get() Customer Tests")
    class GetCustomerTests {

        @Test
        @DisplayName("UTCID01 - id = 1 (Normal) → should return customer")
        void getCustomer_Normal() {
            Customer c = new Customer();
            c.setId(1L);
            c.setCompanyName("ABC");

            when(customerRepository.findById(1L)).thenReturn(Optional.of(c));

            Customer result = customerService.findById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("ABC", result.getCompanyName());
        }

        @Test
        @DisplayName("UTCID02 - id = 999 (Not Found) → should throw RuntimeException")
        void getCustomer_NotFound() {
            when(customerRepository.findById(999L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> customerService.findById(999L));

            assertEquals("Customer not found", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID03 - id = -1 (Invalid) → should throw RuntimeException")
        void getCustomer_InvalidId_Negative() {
            when(customerRepository.findById(-1L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> customerService.findById(-1L));
        }

        @Test
        @DisplayName("UTCID04 - id = null → should throw NullPointerException")
        void getCustomer_NullId() {
            assertThrows(NullPointerException.class,
                    () -> customerService.findById(null));
        }
    }
    // =====================================================================
//  TEST LIST<CustomerDto> list() — 2 test cases
// =====================================================================
    @Nested
    @DisplayName("list() Customer Tests")
    class ListCustomerTests {

        @Test
        @DisplayName("UTCID01 - Return status 200 and list<Customer> NOT empty")
        void list_ReturnsNonEmptyList() {
            Customer c1 = new Customer();
            c1.setId(1L);
            c1.setCompanyName("Công ty ABC");

            Customer c2 = new Customer();
            c2.setId(2L);
            c2.setCompanyName("Công ty XYZ");

            when(customerRepository.findAll()).thenReturn(java.util.List.of(c1, c2));

            var result = customerService.findAll();

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("Công ty ABC", result.get(0).getCompanyName());
            assertEquals("Công ty XYZ", result.get(1).getCompanyName());
            verify(customerRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("UTCID02 - Return status 200 and EMPTY list")
        void list_ReturnsEmptyList() {
            when(customerRepository.findAll()).thenReturn(java.util.List.of());

            var result = customerService.findAll();

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(customerRepository, times(1)).findAll();
        }
    }

}

