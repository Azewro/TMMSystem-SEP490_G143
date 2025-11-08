package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tmmsystem.dto.sales.PriceCalculationDto;
import tmmsystem.entity.*;
import tmmsystem.repository.*;
import tmmsystem.service.EmailService;
import tmmsystem.service.NotificationService;
import tmmsystem.service.QuotationService;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuotationServiceTest {

    @Mock
    private QuotationRepository quotationRepository;
    @Mock
    private QuotationDetailRepository quotationDetailRepository;
    @Mock
    private RfqRepository rfqRepository;
    @Mock
    private RfqDetailRepository rfqDetailRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private MaterialStockRepository materialStockRepository;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private QuotationService quotationService;

    @Nested
    @DisplayName("Create Quotation Tests")
    class CreateQuotationTests {

        @Test
        @DisplayName("Normal Case: Should create quotation successfully")
        void createQuotation_Normal_Success() {
            // Given
            Rfq rfq = new Rfq();
            rfq.setId(1L);
            Customer customer = new Customer();
            customer.setId(1L);

            Quotation quotation = new Quotation();
            quotation.setId(1L);
            quotation.setQuotationNumber("QUO-20240101-001");
            quotation.setRfq(rfq);
            quotation.setCustomer(customer);
            quotation.setValidUntil(LocalDate.now().plusDays(30));
            quotation.setTotalAmount(new BigDecimal("1000.00"));
            quotation.setStatus("DRAFT");

            when(quotationRepository.save(any(Quotation.class))).thenReturn(quotation);

            // When
            Quotation createdQuotation = quotationService.create(quotation);

            // Then
            assertNotNull(createdQuotation, "The created quotation should not be null.");
            assertEquals("QUO-20240101-001", createdQuotation.getQuotationNumber(), "Quotation number should match.");
            System.out.println("[SUCCESS] createQuotation_Normal_Success: Quotation created successfully.");
            System.out.println(">> Returned Quotation: " + createdQuotation);
        }

        @Test
        @DisplayName("Abnormal Case: Should throw exception when quotation is null")
        void createQuotation_Abnormal_NullQuotation() {
            // When & Then
            when(quotationRepository.save(null)).thenThrow(new IllegalArgumentException("Entity must not be null."));

            Exception exception = assertThrows(IllegalArgumentException.class, () -> {
                quotationService.create(null);
            });

            assertEquals("Entity must not be null.", exception.getMessage());
            System.out.println("[SUCCESS] createQuotation_Abnormal_NullQuotation: Failed as expected. Quotation is null.");
            System.out.println(">> Exception Message: " + exception.getMessage());
        }

        @Test
        @DisplayName("Abnormal Case: Quotation number is empty")
        void createQuotation_Abnormal_EmptyQuotationNumber() {
            // Given
            Rfq rfq = new Rfq();
            rfq.setId(1L);
            Customer customer = new Customer();
            customer.setId(1L);

            Quotation quotation = new Quotation();
            quotation.setId(1L);
            quotation.setQuotationNumber("");
            quotation.setRfq(rfq);
            quotation.setCustomer(customer);
            quotation.setValidUntil(LocalDate.now().plusDays(30));
            quotation.setTotalAmount(new BigDecimal("1000.00"));
            quotation.setStatus("DRAFT");

            when(quotationRepository.save(any(Quotation.class))).thenReturn(quotation);

            // When
            Quotation createdQuotation = quotationService.create(quotation);

            // Then
            assertNotNull(createdQuotation);
            assertEquals("", createdQuotation.getQuotationNumber());
            System.out.println("[SUCCESS] createQuotation_Abnormal_EmptyQuotationNumber: Quotation created with empty quotation number.");
            System.out.println(">> Returned Quotation: " + createdQuotation);
        }

        @Test
        @DisplayName("Abnormal Case: Total amount is negative")
        void createQuotation_Abnormal_NegativeTotalAmount() {
            // Given
            Rfq rfq = new Rfq();
            rfq.setId(1L);
            Customer customer = new Customer();
            customer.setId(1L);

            Quotation quotation = new Quotation();
            quotation.setId(1L);
            quotation.setQuotationNumber("QUO-20240101-001");
            quotation.setRfq(rfq);
            quotation.setCustomer(customer);
            quotation.setValidUntil(LocalDate.now().plusDays(30));
            quotation.setTotalAmount(new BigDecimal("-100.00"));
            quotation.setStatus("DRAFT");

            when(quotationRepository.save(any(Quotation.class))).thenReturn(quotation);

            // When
            Quotation createdQuotation = quotationService.create(quotation);

            // Then
            assertNotNull(createdQuotation);
            assertTrue(createdQuotation.getTotalAmount().compareTo(BigDecimal.ZERO) < 0);
            System.out.println("[SUCCESS] createQuotation_Abnormal_NegativeTotalAmount: Quotation created with negative total amount.");
            System.out.println(">> Returned Quotation: " + createdQuotation);
        }

        @Test
        @DisplayName("Abnormal Case: Valid until date is in the past")
        void createQuotation_Abnormal_PastValidUntilDate() {
            // Given
            Rfq rfq = new Rfq();
            rfq.setId(1L);
            Customer customer = new Customer();
            customer.setId(1L);

            Quotation quotation = new Quotation();
            quotation.setId(1L);
            quotation.setQuotationNumber("QUO-20240101-001");
            quotation.setRfq(rfq);
            quotation.setCustomer(customer);
            quotation.setValidUntil(LocalDate.now().minusDays(1));
            quotation.setTotalAmount(new BigDecimal("1000.00"));
            quotation.setStatus("DRAFT");

            when(quotationRepository.save(any(Quotation.class))).thenReturn(quotation);

            // When
            Quotation createdQuotation = quotationService.create(quotation);

            // Then
            assertNotNull(createdQuotation);
            assertTrue(createdQuotation.getValidUntil().isBefore(LocalDate.now()));
            System.out.println("[SUCCESS] createQuotation_Abnormal_PastValidUntilDate: Quotation created with a past validation date.");
            System.out.println(">> Returned Quotation: " + createdQuotation);
        }

        @Test
        @DisplayName("Boundary Case: Quotation number is null")
        void createQuotation_Boundary_NullQuotationNumber() {
            // Given
            Rfq rfq = new Rfq();
            rfq.setId(1L);
            Customer customer = new Customer();
            customer.setId(1L);

            Quotation quotation = new Quotation();
            quotation.setId(1L);
            quotation.setQuotationNumber(null);
            quotation.setRfq(rfq);
            quotation.setCustomer(customer);
            quotation.setValidUntil(LocalDate.now().plusDays(30));
            quotation.setTotalAmount(new BigDecimal("1000.00"));
            quotation.setStatus("DRAFT");

            when(quotationRepository.save(any(Quotation.class))).thenReturn(quotation);

            // When
            Quotation createdQuotation = quotationService.create(quotation);

            // Then
            assertNotNull(createdQuotation, "The created quotation should not be null.");
            assertNull(createdQuotation.getQuotationNumber(), "Quotation number should be null.");
            System.out.println("[SUCCESS] createQuotation_Boundary_NullQuotationNumber: Quotation created with null quotation number.");
            System.out.println(">> Returned Quotation: " + createdQuotation);
        }

        @Test
        @DisplayName("Boundary Case: RFQ is null")
        void createQuotation_Boundary_NullRfq() {
            // Given
            Customer customer = new Customer();
            customer.setId(1L);

            Quotation quotation = new Quotation();
            quotation.setId(1L);
            quotation.setQuotationNumber("QUO-20240101-001");
            quotation.setRfq(null);
            quotation.setCustomer(customer);
            quotation.setValidUntil(LocalDate.now().plusDays(30));
            quotation.setTotalAmount(new BigDecimal("1000.00"));
            quotation.setStatus("DRAFT");

            when(quotationRepository.save(any(Quotation.class))).thenReturn(quotation);

            // When
            Quotation createdQuotation = quotationService.create(quotation);

            // Then
            assertNotNull(createdQuotation, "The created quotation should not be null.");
            assertNull(createdQuotation.getRfq(), "RFQ should be null.");
            System.out.println("[SUCCESS] createQuotation_Boundary_NullRfq: Quotation created with null RFQ.");
            System.out.println(">> Returned Quotation: " + createdQuotation);
        }

        @Test
        @DisplayName("Boundary Case: Customer is null")
        void createQuotation_Boundary_NullCustomer() {
            // Given
            Rfq rfq = new Rfq();
            rfq.setId(1L);

            Quotation quotation = new Quotation();
            quotation.setId(1L);
            quotation.setQuotationNumber("QUO-20240101-001");
            quotation.setRfq(rfq);
            quotation.setCustomer(null);
            quotation.setValidUntil(LocalDate.now().plusDays(30));
            quotation.setTotalAmount(new BigDecimal("1000.00"));
            quotation.setStatus("DRAFT");

            when(quotationRepository.save(any(Quotation.class))).thenReturn(quotation);

            // When
            Quotation createdQuotation = quotationService.create(quotation);

            // Then
            assertNotNull(createdQuotation, "The created quotation should not be null.");
            assertNull(createdQuotation.getCustomer(), "Customer should be null.");
            System.out.println("[SUCCESS] createQuotation_Boundary_NullCustomer: Quotation created with null customer.");
            System.out.println(">> Returned Quotation: " + createdQuotation);
        }

        @Test
        @DisplayName("Boundary Case: Total amount is null")
        void createQuotation_Boundary_NullTotalAmount() {
            // Given
            Rfq rfq = new Rfq();
            rfq.setId(1L);
            Customer customer = new Customer();
            customer.setId(1L);

            Quotation quotation = new Quotation();
            quotation.setId(1L);
            quotation.setQuotationNumber("QUO-20240101-001");
            quotation.setRfq(rfq);
            quotation.setCustomer(customer);
            quotation.setValidUntil(LocalDate.now().plusDays(30));
            quotation.setTotalAmount(null);
            quotation.setStatus("DRAFT");

            when(quotationRepository.save(any(Quotation.class))).thenReturn(quotation);

            // When
            Quotation createdQuotation = quotationService.create(quotation);

            // Then
            assertNotNull(createdQuotation, "The created quotation should not be null.");
            assertNull(createdQuotation.getTotalAmount(), "Total amount should be null.");
            System.out.println("[SUCCESS] createQuotation_Boundary_NullTotalAmount: Quotation created with null total amount.");
            System.out.println(">> Returned Quotation: " + createdQuotation);
        }

        @Test
        @DisplayName("Boundary Case: Status is null")
        void createQuotation_Boundary_NullStatus() {
            // Given
            Rfq rfq = new Rfq();
            rfq.setId(1L);
            Customer customer = new Customer();
            customer.setId(1L);

            Quotation quotation = new Quotation();
            quotation.setId(1L);
            quotation.setQuotationNumber("QUO-20240101-001");
            quotation.setRfq(rfq);
            quotation.setCustomer(customer);
            quotation.setValidUntil(LocalDate.now().plusDays(30));
            quotation.setTotalAmount(new BigDecimal("1000.00"));
            quotation.setStatus(null);

            when(quotationRepository.save(any(Quotation.class))).thenReturn(quotation);

            // When
            Quotation createdQuotation = quotationService.create(quotation);

            // Then
            assertNotNull(createdQuotation, "The created quotation should not be null.");
            assertNull(createdQuotation.getStatus(), "Status should be null.");
            System.out.println("[SUCCESS] createQuotation_Boundary_NullStatus: Quotation created with null status.");
            System.out.println(">> Returned Quotation: " + createdQuotation);
        }
    }

    @Nested
    @DisplayName("Update Quotation Tests")
    class UpdateQuotationTests {

        private Quotation existingQuotation;
        private Quotation updatedInfo;

        @BeforeEach
        void setUp() {
            Rfq rfq = new Rfq();
            rfq.setId(1L);
            Customer customer = new Customer();
            customer.setId(1L);

            existingQuotation = new Quotation();
            existingQuotation.setId(1L);
            existingQuotation.setQuotationNumber("QUO-EXISTING-001");
            existingQuotation.setRfq(rfq);
            existingQuotation.setCustomer(customer);
            existingQuotation.setValidUntil(LocalDate.now().plusDays(15));
            existingQuotation.setTotalAmount(new BigDecimal("500.00"));
            existingQuotation.setStatus("DRAFT");

            updatedInfo = new Quotation();
        }

        @Test
        @DisplayName("Abnormal Case: Quotation not found")
        void updateQuotation_Abnormal_NotFound() {
            // Given
            Long nonExistentId = 99L;
            when(quotationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            Exception exception = assertThrows(NoSuchElementException.class, () -> {
                quotationService.update(nonExistentId, new Quotation());
            });

            System.out.println("[SUCCESS] updateQuotation_Abnormal_NotFound: Failed as expected. Quotation not found.");
            System.out.println(">> Exception: " + exception.getClass().getSimpleName());
        }

        @Test
        @DisplayName("Abnormal Case: Update with empty quotation number")
        void updateQuotation_Abnormal_EmptyQuotationNumber() {
            // Given
            updatedInfo.setQuotationNumber("");
            when(quotationRepository.findById(1L)).thenReturn(Optional.of(existingQuotation));

            // When
            Quotation result = quotationService.update(1L, updatedInfo);

            // Then
            assertEquals("", result.getQuotationNumber(), "Quotation number should be updated to empty.");
            System.out.println("[SUCCESS] updateQuotation_Abnormal_EmptyQuotationNumber: Quotation updated with empty number.");
            System.out.println(">> Updated Quotation: " + result);
        }

        @Test
        @DisplayName("Abnormal Case: Update with negative total amount")
        void updateQuotation_Abnormal_NegativeTotalAmount() {
            // Given
            updatedInfo.setTotalAmount(new BigDecimal("-200.00"));
            when(quotationRepository.findById(1L)).thenReturn(Optional.of(existingQuotation));

            // When
            Quotation result = quotationService.update(1L, updatedInfo);

            // Then
            assertTrue(result.getTotalAmount().compareTo(BigDecimal.ZERO) < 0, "Total amount should be negative.");
            System.out.println("[SUCCESS] updateQuotation_Abnormal_NegativeTotalAmount: Quotation updated with negative total amount.");
            System.out.println(">> Updated Quotation: " + result);
        }

        @Test
        @DisplayName("Abnormal Case: Update with past valid until date")
        void updateQuotation_Abnormal_PastValidUntilDate() {
            // Given
            updatedInfo.setValidUntil(LocalDate.now().minusDays(5));
            when(quotationRepository.findById(1L)).thenReturn(Optional.of(existingQuotation));

            // When
            Quotation result = quotationService.update(1L, updatedInfo);

            // Then
            assertTrue(result.getValidUntil().isBefore(LocalDate.now()), "Valid until date should be in the past.");
            System.out.println("[SUCCESS] updateQuotation_Abnormal_PastValidUntilDate: Quotation updated with past validation date.");
            System.out.println(">> Updated Quotation: " + result);
        }

        @Test
        @DisplayName("Normal Case: Should update quotation successfully")
        void updateQuotation_Normal_Success() {
            // Given
            updatedInfo.setQuotationNumber("QUO-UPDATED-002");
            updatedInfo.setTotalAmount(new BigDecimal("1500.00"));
            updatedInfo.setStatus("SENT");
            when(quotationRepository.findById(1L)).thenReturn(Optional.of(existingQuotation));

            // When
            Quotation result = quotationService.update(1L, updatedInfo);

            // Then
            assertEquals("QUO-UPDATED-002", result.getQuotationNumber());
            assertEquals(0, new BigDecimal("1500.00").compareTo(result.getTotalAmount()));
            assertEquals("SENT", result.getStatus());
            System.out.println("[SUCCESS] updateQuotation_Normal_Success: Quotation updated successfully.");
            System.out.println(">> Updated Quotation: " + result);
        }

        @Test
        @DisplayName("Boundary Case: Update with null quotation number")
        void updateQuotation_Boundary_NullQuotationNumber() {
            // Given
            updatedInfo.setQuotationNumber(null);
            when(quotationRepository.findById(1L)).thenReturn(Optional.of(existingQuotation));

            // When
            Quotation result = quotationService.update(1L, updatedInfo);

            // Then
            assertNull(result.getQuotationNumber(), "Quotation number should be updated to null.");
            System.out.println("[SUCCESS] updateQuotation_Boundary_NullQuotationNumber: Quotation updated with null number.");
            System.out.println(">> Updated Quotation: " + result);
        }

        @Test
        @DisplayName("Boundary Case: Update with null total amount")
        void updateQuotation_Boundary_NullTotalAmount() {
            // Given
            updatedInfo.setTotalAmount(null);
            when(quotationRepository.findById(1L)).thenReturn(Optional.of(existingQuotation));

            // When
            Quotation result = quotationService.update(1L, updatedInfo);

            // Then
            assertNull(result.getTotalAmount(), "Total amount should be updated to null.");
            System.out.println("[SUCCESS] updateQuotation_Boundary_NullTotalAmount: Quotation updated with null total amount.");
            System.out.println(">> Updated Quotation: " + result);
        }
    }

    @Nested
    @DisplayName("Calculate Quotation Price Parameter Tests")
    class CalculateQuotationPriceTests {

        @Test
        @DisplayName("Normal Case: Should calculate price with valid rfqId and profitMargin")
        void calculateQuotationPrice_Normal_ValidInputs() {
            // Given
            Long rfqId = 1L;
            BigDecimal profitMargin = new BigDecimal("1.10");

            Rfq rfq = new Rfq();
            rfq.setId(rfqId);
            rfq.setStatus("RECEIVED_BY_PLANNING");

            Product product = new Product();
            product.setId(101L);
            product.setName("Test Product");
            product.setStandardWeight(new BigDecimal("500"));

            RfqDetail rfqDetail = new RfqDetail();
            rfqDetail.setProduct(product);
            rfqDetail.setQuantity(new BigDecimal("10"));

            when(rfqRepository.findById(rfqId)).thenReturn(Optional.of(rfq));
            when(rfqDetailRepository.findByRfqId(rfqId)).thenReturn(Collections.singletonList(rfqDetail));
            when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
            when(materialRepository.findAll()).thenReturn(Collections.emptyList()); // Use fallback prices

            // When
            PriceCalculationDto result = quotationService.calculateQuotationPrice(rfqId, profitMargin);

            // Then
            assertNotNull(result);
            assertFalse(result.getProductDetails().isEmpty());
            assertTrue(result.getFinalTotalPrice().compareTo(BigDecimal.ZERO) > 0);
            System.out.println("[SUCCESS] calculateQuotationPrice_Normal_ValidInputs: Price calculated successfully.");
        }

        @Test
        @DisplayName("Abnormal Case: rfqId does not exist")
        void calculateQuotationPrice_Abnormal_RfqNotFound() {
            // Given
            Long nonExistentRfqId = 99L;
            BigDecimal profitMargin = new BigDecimal("1.10");
            when(rfqRepository.findById(nonExistentRfqId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NoSuchElementException.class, () -> {
                quotationService.calculateQuotationPrice(nonExistentRfqId, profitMargin);
            });
            System.out.println("[SUCCESS] calculateQuotationPrice_Abnormal_RfqNotFound: Threw exception for non-existent rfqId.");
        }

        @Test
        @DisplayName("Abnormal Case: profitMargin is null")
        void calculateQuotationPrice_Abnormal_NullProfitMargin() {
            // Given
            Long rfqId = 1L;
            
            Rfq rfq = new Rfq();
            rfq.setId(rfqId);
            rfq.setStatus("RECEIVED_BY_PLANNING");

            Product product = new Product();
            product.setId(101L);
            product.setName("Test Product");
            product.setStandardWeight(new BigDecimal("500"));

            RfqDetail rfqDetail = new RfqDetail();
            rfqDetail.setProduct(product);
            rfqDetail.setQuantity(new BigDecimal("10"));

            when(rfqRepository.findById(rfqId)).thenReturn(Optional.of(rfq));
            when(rfqDetailRepository.findByRfqId(rfqId)).thenReturn(Collections.singletonList(rfqDetail));
            when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
            when(materialRepository.findAll()).thenReturn(Collections.emptyList());

            // When & Then
            assertThrows(NullPointerException.class, () -> {
                quotationService.calculateQuotationPrice(rfqId, null);
            });
            System.out.println("[SUCCESS] calculateQuotationPrice_Abnormal_NullProfitMargin: Threw exception for null profitMargin.");
        }
        
        @Test
        @DisplayName("Abnormal Case: rfqId is null")
        void calculateQuotationPrice_Abnormal_NullRfqId() {
            // Given
            BigDecimal profitMargin = new BigDecimal("1.10");
            when(rfqRepository.findById(null)).thenThrow(new IllegalArgumentException("ID must not be null"));

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                quotationService.calculateQuotationPrice(null, profitMargin);
            });
            System.out.println("[SUCCESS] calculateQuotationPrice_Abnormal_NullRfqId: Threw exception for null rfqId.");
        }

        @Test
        @DisplayName("Boundary Case: profitMargin is zero")
        void calculateQuotationPrice_Boundary_ZeroProfitMargin() {
            // Given
            Long rfqId = 1L;
            BigDecimal profitMargin = BigDecimal.ZERO;

            Rfq rfq = new Rfq();
            rfq.setId(rfqId);
            rfq.setStatus("RECEIVED_BY_PLANNING");

            Product product = new Product();
            product.setId(101L);
            product.setName("Test Product");
            product.setStandardWeight(new BigDecimal("500"));

            RfqDetail rfqDetail = new RfqDetail();
            rfqDetail.setProduct(product);
            rfqDetail.setQuantity(new BigDecimal("10"));

            when(rfqRepository.findById(rfqId)).thenReturn(Optional.of(rfq));
            when(rfqDetailRepository.findByRfqId(rfqId)).thenReturn(Collections.singletonList(rfqDetail));
            when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
            when(materialRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            PriceCalculationDto result = quotationService.calculateQuotationPrice(rfqId, profitMargin);

            // Then
            assertNotNull(result);
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getFinalTotalPrice()));
            System.out.println("[SUCCESS] calculateQuotationPrice_Boundary_ZeroProfitMargin: Final price is zero for zero profit margin.");
        }

        @Test
        @DisplayName("Boundary Case: profitMargin is negative")
        void calculateQuotationPrice_Boundary_NegativeProfitMargin() {
            // Given
            Long rfqId = 1L;
            BigDecimal profitMargin = new BigDecimal("-1.0");

            Rfq rfq = new Rfq();
            rfq.setId(rfqId);
            rfq.setStatus("RECEIVED_BY_PLANNING");

            Product product = new Product();
            product.setId(101L);
            product.setName("Test Product");
            product.setStandardWeight(new BigDecimal("500"));

            RfqDetail rfqDetail = new RfqDetail();
            rfqDetail.setProduct(product);
            rfqDetail.setQuantity(new BigDecimal("10"));

            when(rfqRepository.findById(rfqId)).thenReturn(Optional.of(rfq));
            when(rfqDetailRepository.findByRfqId(rfqId)).thenReturn(Collections.singletonList(rfqDetail));
            when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
            when(materialRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            PriceCalculationDto result = quotationService.calculateQuotationPrice(rfqId, profitMargin);

            // Then
            assertNotNull(result);
            assertTrue(result.getFinalTotalPrice().compareTo(BigDecimal.ZERO) < 0);
            System.out.println("[SUCCESS] calculateQuotationPrice_Boundary_NegativeProfitMargin: Final price is negative for negative profit margin.");
        }

        @Test
        @DisplayName("Boundary Case: rfqId is zero")
        void calculateQuotationPrice_Boundary_ZeroRfqId() {
            // Given
            Long rfqId = 0L;
            BigDecimal profitMargin = new BigDecimal("1.10");
            when(rfqRepository.findById(rfqId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NoSuchElementException.class, () -> {
                quotationService.calculateQuotationPrice(rfqId, profitMargin);
            });
            System.out.println("[SUCCESS] calculateQuotationPrice_Boundary_ZeroRfqId: Threw exception for rfqId = 0.");
        }

        @Test
        @DisplayName("Boundary Case: rfqId is negative")
        void calculateQuotationPrice_Boundary_NegativeRfqId() {
            // Given
            Long rfqId = -1L;
            BigDecimal profitMargin = new BigDecimal("1.10");
            when(rfqRepository.findById(rfqId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NoSuchElementException.class, () -> {
                quotationService.calculateQuotationPrice(rfqId, profitMargin);
            });
            System.out.println("[SUCCESS] calculateQuotationPrice_Boundary_NegativeRfqId: Threw exception for negative rfqId.");
        }

        @Test
        @DisplayName("Boundary Case: profitMargin has high precision")
        void calculateQuotationPrice_Boundary_HighPrecisionProfitMargin() {
            // Given
            Long rfqId = 1L;
            BigDecimal profitMargin = new BigDecimal("1.123456789");

            Rfq rfq = new Rfq();
            rfq.setId(rfqId);
            rfq.setStatus("RECEIVED_BY_PLANNING");

            Product product = new Product();
            product.setId(101L);
            product.setName("Test Product");
            product.setStandardWeight(new BigDecimal("500"));

            RfqDetail rfqDetail = new RfqDetail();
            rfqDetail.setProduct(product);
            rfqDetail.setQuantity(new BigDecimal("10"));

            when(rfqRepository.findById(rfqId)).thenReturn(Optional.of(rfq));
            when(rfqDetailRepository.findByRfqId(rfqId)).thenReturn(Collections.singletonList(rfqDetail));
            when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
            when(materialRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            PriceCalculationDto result = quotationService.calculateQuotationPrice(rfqId, profitMargin);

            // Then
            assertNotNull(result);
            assertTrue(result.getFinalTotalPrice().compareTo(BigDecimal.ZERO) > 0);
            System.out.println("[SUCCESS] calculateQuotationPrice_Boundary_HighPrecisionProfitMargin: Price calculated with high precision profit margin.");
        }

        @Test
        @DisplayName("Boundary Case: profitMargin is very large")
        void calculateQuotationPrice_Boundary_LargeProfitMargin() {
            // Given
            Long rfqId = 1L;
            BigDecimal profitMargin = new BigDecimal("999999999999999999.99");

            Rfq rfq = new Rfq();
            rfq.setId(rfqId);
            rfq.setStatus("RECEIVED_BY_PLANNING");

            Product product = new Product();
            product.setId(101L);
            product.setName("Test Product");
            product.setStandardWeight(new BigDecimal("500"));

            RfqDetail rfqDetail = new RfqDetail();
            rfqDetail.setProduct(product);
            rfqDetail.setQuantity(new BigDecimal("10"));

            when(rfqRepository.findById(rfqId)).thenReturn(Optional.of(rfq));
            when(rfqDetailRepository.findByRfqId(rfqId)).thenReturn(Collections.singletonList(rfqDetail));
            when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
            when(materialRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            PriceCalculationDto result = quotationService.calculateQuotationPrice(rfqId, profitMargin);

            // Then
            assertNotNull(result);
            assertTrue(result.getFinalTotalPrice().compareTo(BigDecimal.ZERO) > 0);
            System.out.println("[SUCCESS] calculateQuotationPrice_Boundary_LargeProfitMargin: Price calculated with a very large profit margin.");
        }
    }
    
    @Nested
    @DisplayName("Calculate Product Price Detail Parameter Tests")
    class CalculateProductPriceDetailTests {

        private Method method;

        @BeforeEach
        void setUp() throws NoSuchMethodException {
            method = QuotationService.class.getDeclaredMethod("calculateProductPriceDetail", Product.class, BigDecimal.class, BigDecimal.class);
            method.setAccessible(true);
        }

        @Test
        @DisplayName("Normal Case: Valid inputs")
        void calculateProductPriceDetail_Normal_ValidInputs() throws Exception {
            // Given
            Product product = new Product();
            product.setId(1L);
            product.setName("100% cotton");
            product.setStandardWeight(new BigDecimal("500"));
            BigDecimal quantity = new BigDecimal("100");
            BigDecimal profitMargin = new BigDecimal("1.10");

            when(materialRepository.findAll()).thenReturn(Collections.emptyList()); // Use fallback price

            // When
            PriceCalculationDto.ProductPriceDetailDto result = (PriceCalculationDto.ProductPriceDetailDto) method.invoke(quotationService, product, quantity, profitMargin);

            // Then
            assertNotNull(result);
            assertEquals(product.getId(), result.getProductId());
            assertTrue(result.getTotalPrice().compareTo(BigDecimal.ZERO) > 0);
            System.out.println("[SUCCESS] calculateProductPriceDetail_Normal_ValidInputs: Correctly calculated with valid inputs.");
        }

        @Test
        @DisplayName("Abnormal Case: Null product")
        void calculateProductPriceDetail_Abnormal_NullProduct() {
            // Given
            BigDecimal quantity = new BigDecimal("100");
            BigDecimal profitMargin = new BigDecimal("1.10");

            // When & Then
            Exception exception = assertThrows(Exception.class, () -> {
                method.invoke(quotationService, null, quantity, profitMargin);
            });
            assertInstanceOf(NullPointerException.class, exception.getCause());
            System.out.println("[SUCCESS] calculateProductPriceDetail_Abnormal_NullProduct: Threw exception for null product.");
        }

        @Test
        @DisplayName("Abnormal Case: Null quantity")
        void calculateProductPriceDetail_Abnormal_NullQuantity() {
            // Given
            Product product = new Product();
            product.setId(1L);
            product.setName("100% cotton");
            product.setStandardWeight(new BigDecimal("500"));
            BigDecimal profitMargin = new BigDecimal("1.10");

            // When & Then
            Exception exception = assertThrows(Exception.class, () -> {
                method.invoke(quotationService, product, null, profitMargin);
            });
            assertInstanceOf(NullPointerException.class, exception.getCause());
            System.out.println("[SUCCESS] calculateProductPriceDetail_Abnormal_NullQuantity: Threw exception for null quantity.");
        }

        @Test
        @DisplayName("Abnormal Case: Null profit margin")
        void calculateProductPriceDetail_Abnormal_NullProfitMargin() {
            // Given
            Product product = new Product();
            product.setId(1L);
            product.setName("100% cotton");
            product.setStandardWeight(new BigDecimal("500"));
            BigDecimal quantity = new BigDecimal("100");

            // When & Then
            Exception exception = assertThrows(Exception.class, () -> {
                method.invoke(quotationService, product, quantity, null);
            });
            assertInstanceOf(NullPointerException.class, exception.getCause());
            System.out.println("[SUCCESS] calculateProductPriceDetail_Abnormal_NullProfitMargin: Threw exception for null profit margin.");
        }

        @Test
        @DisplayName("Boundary Case: Zero quantity")
        void calculateProductPriceDetail_Boundary_ZeroQuantity() throws Exception {
            // Given
            Product product = new Product();
            product.setId(1L);
            product.setName("100% cotton");
            product.setStandardWeight(new BigDecimal("500"));
            BigDecimal quantity = BigDecimal.ZERO;
            BigDecimal profitMargin = new BigDecimal("1.10");

            when(materialRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            PriceCalculationDto.ProductPriceDetailDto result = (PriceCalculationDto.ProductPriceDetailDto) method.invoke(quotationService, product, quantity, profitMargin);

            // Then
            assertNotNull(result);
            assertEquals(0, result.getTotalPrice().compareTo(BigDecimal.ZERO));
            System.out.println("[SUCCESS] calculateProductPriceDetail_Boundary_ZeroQuantity: Total price is zero for zero quantity.");
        }

        @Test
        @DisplayName("Boundary Case: Zero profit margin")
        void calculateProductPriceDetail_Boundary_ZeroProfitMargin() throws Exception {
            // Given
            Product product = new Product();
            product.setId(1L);
            product.setName("100% cotton");
            product.setStandardWeight(new BigDecimal("500"));
            BigDecimal quantity = new BigDecimal("100");
            BigDecimal profitMargin = BigDecimal.ZERO;

            when(materialRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            PriceCalculationDto.ProductPriceDetailDto result = (PriceCalculationDto.ProductPriceDetailDto) method.invoke(quotationService, product, quantity, profitMargin);

            // Then
            assertNotNull(result);
            assertEquals(0, result.getUnitPrice().compareTo(BigDecimal.ZERO));
            assertEquals(0, result.getTotalPrice().compareTo(BigDecimal.ZERO));
            System.out.println("[SUCCESS] calculateProductPriceDetail_Boundary_ZeroProfitMargin: Unit price and total price are zero.");
        }

        @Test
        @DisplayName("Boundary Case: Zero product weight")
        void calculateProductPriceDetail_Boundary_ZeroProductWeight() throws Exception {
            // Given
            Product product = new Product();
            product.setId(1L);
            product.setName("100% cotton");
            product.setStandardWeight(BigDecimal.ZERO);
            BigDecimal quantity = new BigDecimal("100");
            BigDecimal profitMargin = new BigDecimal("1.10");

            when(materialRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            PriceCalculationDto.ProductPriceDetailDto result = (PriceCalculationDto.ProductPriceDetailDto) method.invoke(quotationService, product, quantity, profitMargin);

            // Then
            assertNotNull(result);
            assertEquals(0, result.getBaseCostPerUnit().compareTo(BigDecimal.ZERO));
            assertEquals(0, result.getTotalPrice().compareTo(BigDecimal.ZERO));
            System.out.println("[SUCCESS] calculateProductPriceDetail_Boundary_ZeroProductWeight: All costs are zero for zero weight product.");
        }
    }
}
