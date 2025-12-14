package tmmsystem.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import tmmsystem.entity.*;
import tmmsystem.repository.*;
import tmmsystem.service.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock private ContractRepository contractRepo;
    @Mock private UserRepository userRepo;
    @Mock private NotificationService notificationService;
    @Mock private FileStorageService fileStorageService;
    @Mock private ProductionPlanService productionPlanService;
    private Contract mockContractWithDetails() {

        // ----- Customer -----
        Customer customer = new Customer();
        customer.setId(100L);
        customer.setContactPerson("John");
        customer.setPhoneNumber("0123456789");
        customer.setCompanyName("ABC Corp");
        customer.setTaxCode("TAX123");
        customer.setAddress("HN");

        // ----- Product 1 -----
        Product p1 = new Product();
        p1.setId(1L);
        p1.setName("Product A");
        p1.setStandardDimensions("Size A");

        // ----- Product 2 -----
        Product p2 = new Product();
        p2.setId(2L);
        p2.setName("Product B");
        p2.setStandardDimensions("Size B");

        // ----- Quotation Detail 1 -----
        QuotationDetail d1 = new QuotationDetail();
        d1.setProduct(p1);
        d1.setQuantity(new BigDecimal("10"));
        d1.setUnit("PCS");
        d1.setUnitPrice(new BigDecimal("100"));
        d1.setTotalPrice(new BigDecimal("1000"));
        d1.setNoteColor("Red");

        // ----- Quotation Detail 2 -----
        QuotationDetail d2 = new QuotationDetail();
        d2.setProduct(p2);
        d2.setQuantity(new BigDecimal("5"));
        d2.setUnit("PCS");
        d2.setUnitPrice(new BigDecimal("200"));
        d2.setTotalPrice(new BigDecimal("1000"));
        d2.setNoteColor("Blue");

        // ----- Quotation -----
        Quotation quotation = new Quotation();
        quotation.setId(10L);
        quotation.setDetails(List.of(d1, d2));

        // ----- Contract -----
        Contract c = new Contract();
        c.setId(1L);
        c.setContractNumber("CON-01");
        c.setStatus("NEW");
        c.setContractDate(java.time.LocalDate.now());
        c.setDeliveryDate(java.time.LocalDate.now().plusDays(7));
        c.setTotalAmount(new BigDecimal("2000"));
        c.setFilePath("abc.pdf");

        c.setCustomer(customer);
        c.setQuotation(quotation);

        return c;
    }
    @InjectMocks
    private ContractService service;

    @BeforeEach
    void setup() {}

    // =========================================================
    // findById
    // =========================================================
    @Test
    void findById_Normal() {
        Contract c = new Contract();
        c.setId(1L);

        when(contractRepo.findById(1L)).thenReturn(Optional.of(c));

        Contract result = service.findById(1L);
        assertNotNull(result);
    }

    @Test
    void findById_NotFound() {
        when(contractRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.findById(99L));
    }

    // =========================================================
    // uploadSignedContract
    // =========================================================
    @Test
    void uploadSignedContract_Normal() throws Exception {

        MultipartFile file = mock(MultipartFile.class);

        Contract c = new Contract();
        c.setId(10L);

        when(contractRepo.findById(10L)).thenReturn(Optional.of(c));
        when(fileStorageService.uploadContractFile(file, 10L))
                .thenReturn("contract10.pdf");

        when(contractRepo.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        Contract result = service.uploadSignedContract(10L, file, "notes", 1L);

        assertNotNull(result);
        assertEquals("PENDING_APPROVAL", result.getStatus());

        verify(notificationService, times(1))
                .notifyContractUploaded(any());
    }


    @Test
    void uploadSignedContract_NotFound() throws Exception {
        MultipartFile file = mock(MultipartFile.class);

        when(contractRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.uploadSignedContract(999L, file, "n", 1L));
    }

    // =========================================================
    // approveContract
    // =========================================================
     @Test
    void approveContract_Normal() {
        // Arrange
        Contract c = new Contract();
        c.setId(20L);
        // Không set quotation => nhánh createOrMergeLotFromContractAndProduct sẽ bỏ qua
        // c.setQuotation(null); // mặc định null

        User director = new User();
        director.setId(7L);

        // map vào các mock đang được inject vào service
        when(contractRepo.findById(20L)).thenReturn(Optional.of(c));
        when(userRepo.findById(7L)).thenReturn(Optional.of(director));
        when(contractRepo.save(any(Contract.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        Contract r = service.approveContract(20L, 7L, "OK");

        // Assert
        assertEquals("APPROVED", r.getStatus());
        assertEquals(director, r.getDirectorApprovedBy());
        assertEquals("OK", r.getDirectorApprovalNotes());
        assertNotNull(r.getDirectorApprovedAt());
        assertNotNull(r.getUpdatedAt());

        // verify repository + user
        verify(contractRepo).findById(20L);
        verify(userRepo).findById(7L);
        verify(contractRepo).save(any(Contract.class));

        // verify gửi thông báo
        verify(notificationService, times(1))
                .notifyContractApproved(r);

        // vì không có quotation nên service sẽ không gọi merge lot
        verifyNoInteractions(productionPlanService);
    }


    @Test
    void approveContract_ContractNotFound() {
        when(contractRepo.findById(100L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
                () -> service.approveContract(100L, 1L, "n"));
    }

    @Test
    void approveContract_DirectorNotFound() {
        Contract c = new Contract();
        c.setId(1L);

        when(contractRepo.findById(1L)).thenReturn(Optional.of(c));
        when(userRepo.findById(5L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.approveContract(1L, 5L, "n"));
    }

    // =========================================================
    // rejectContract
    // =========================================================
    @Test
    void rejectContract_Normal() {

        Contract c = new Contract();
        c.setId(30L);

        User d = new User();
        d.setId(10L);

        when(contractRepo.findById(30L)).thenReturn(Optional.of(c));
        when(userRepo.findById(10L)).thenReturn(Optional.of(d));
        when(contractRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Contract r = service.rejectContract(30L, 10L, "Bad");

        assertEquals("REJECTED", r.getStatus());
        verify(notificationService, times(1)).notifyContractRejected(any());
    }

    // =========================================================
    // getContractsPendingApproval
    // =========================================================
    @Test
    void getContractsPendingApproval_Normal() {
        when(contractRepo.findByStatus("PENDING_APPROVAL"))
                .thenReturn(List.of(new Contract(), new Contract()));

        List<Contract> list = service.getContractsPendingApproval();
        assertEquals(2, list.size());
    }

    // =========================================================
    // getContractFileUrl
    // =========================================================
    @Test
    void getContractFileUrl_Normal() {
        when(fileStorageService.getContractFileUrl(5L))
                .thenReturn("url.pdf");

        assertEquals("url.pdf", service.getContractFileUrl(5L));
    }

    @Test
    void getContractFileUrl_Exception() {
        when(fileStorageService.getContractFileUrl(5L))
                .thenThrow(new RuntimeException("x"));

        assertNull(service.getContractFileUrl(5L));
    }
    // =====================================================================
// list()
// =====================================================================
    @Test
    @DisplayName("list() - Normal (3 contracts)")
    void list_Normal() {
        when(contractRepo.findAll())
                .thenReturn(List.of(new Contract(), new Contract(), new Contract()));

        List<Contract> out = service.findAll();

        assertEquals(3, out.size());
    }

    @Test
    @DisplayName("list() - Empty list")
    void list_Empty() {
        when(contractRepo.findAll()).thenReturn(List.of());

        List<Contract> out = service.findAll();

        assertTrue(out.isEmpty());
    }

    @Test
    @DisplayName("list() - Large list 5000")
    void list_LargeList() {
        List<Contract> bigList = new ArrayList<>();
        for (int i = 0; i < 5000; i++) bigList.add(new Contract());

        when(contractRepo.findAll()).thenReturn(bigList);

        List<Contract> out = service.findAll();
        assertEquals(5000, out.size());
    }

    @Test
    @DisplayName("list() - repository throws exception")
    void list_Exception() {
        when(contractRepo.findAll())
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> service.findAll());
    }
    // =========================================================
// create()
// =========================================================
    @Test
    void create_Normal() {
        Contract input = new Contract();
        input.setContractNumber("CON-20251112-001");
        input.setTotalAmount(BigDecimal.valueOf(1000000));
        input.setFilePath("contracts/2025/001.pdf");

        when(contractRepo.save(input)).thenReturn(input);

        Contract result = service.create(input);

        assertNotNull(result);
        assertEquals("CON-20251112-001", result.getContractNumber());
        verify(contractRepo, times(1)).save(input);
    }

    @Test
    void create_NullContractNumber() {
        Contract input = new Contract();
        input.setContractNumber(null);

        when(contractRepo.save(input)).thenReturn(input);

        Contract result = service.create(input);

        assertNull(result.getContractNumber());
    }


    // =========================================================
// update()
// =========================================================
    @Test
    void update_Normal() {

        Contract existing = new Contract();
        existing.setId(1L);

        Contract upd = new Contract();
        upd.setContractNumber("CON-20251112-001");
        upd.setTotalAmount(BigDecimal.valueOf(1000000));
        upd.setFilePath("contracts/2025/001.pdf");

        when(contractRepo.findById(1L)).thenReturn(Optional.of(existing));

        Contract result = service.update(1L, upd);

        assertEquals("CON-20251112-001", result.getContractNumber());
        assertEquals(BigDecimal.valueOf(1000000), result.getTotalAmount());
        assertEquals("contracts/2025/001.pdf", result.getFilePath());
    }

    @Test
    void update_NotFound() {
        when(contractRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
                () -> service.update(99L, new Contract()));
    }

    @Test
    void update_NullContractNumber() {

        Contract existing = new Contract();
        existing.setId(1L);

        Contract upd = new Contract();
        upd.setContractNumber(null);

        when(contractRepo.findById(1L)).thenReturn(Optional.of(existing));

        Contract result = service.update(1L, upd);

        assertNull(result.getContractNumber());
    }
    // =========================================================
// getOrderDetails()
// =========================================================
    @Test
    void getOrderDetails_Normal() {
        Contract c = mockContractWithDetails();

        when(contractRepo.findById(1L)).thenReturn(Optional.of(c));

        var result = service.getOrderDetails(1L);

        assertNotNull(result);
        assertEquals(1L, result.getContractId());
        assertEquals("CON-01", result.getContractNumber());
        assertEquals(2, result.getOrderItems().size());
    }

    @Test
    void getOrderDetails_ContractNotFound() {
        when(contractRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
                () -> service.getOrderDetails(99L));
    }

    @Test
    void getOrderDetails_NoQuotation() {
        Contract c = mockContractWithDetails();
        c.setQuotation(null);

        when(contractRepo.findById(1L)).thenReturn(Optional.of(c));

        var result = service.getOrderDetails(1L);

        assertEquals(0, result.getOrderItems().size());
    }

    @Test
    void getOrderDetails_NoOrderItems() {
        Contract c = mockContractWithDetails();
        c.getQuotation().setDetails(null);

        when(contractRepo.findById(1L)).thenReturn(Optional.of(c));

        var result = service.getOrderDetails(1L);

        assertEquals(0, result.getOrderItems().size());
    }

    @Test
    void getOrderDetails_CustomerNull_ShouldThrow() {
        Contract c = mockContractWithDetails();
        c.setCustomer(null);

        when(contractRepo.findById(1L)).thenReturn(Optional.of(c));

        assertThrows(NullPointerException.class,
                () -> service.getOrderDetails(1L));
    }


}
