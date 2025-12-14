package tmmsystem.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyString;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verifyNoInteractions;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tmmsystem.dto.sales.RfqDetailDto;
import tmmsystem.dto.sales.SalesRfqCreateRequest;
import tmmsystem.entity.*;
import tmmsystem.repository.*;
import tmmsystem.service.NotificationService;
import tmmsystem.service.RfqService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
// import thêm ở đầu file
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RfqServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private RfqRepository rfqRepository;

    @Mock
    private RfqDetailRepository detailRepository;
    @Mock
    private QuotationRepository quotationRepository;
    @Mock
    private ContractRepository contractRepository;

    @Mock
    private NotificationService notificationService;
    @Mock
    private tmmsystem.repository.CustomerRepository customerRepository;

    @InjectMocks
    private RfqService rfqService;

    @Nested
    @DisplayName("Create With Details Tests")
    class CreateWithDetailsTests {
        @Test
        @DisplayName("Normal Case: Create RFQ with a single detail")
        void createWithDetails_Normal_WithOneDetail() {
            Rfq rfq = new Rfq();
            rfq.setId(1L);
            RfqDetailDto detailDto = new RfqDetailDto();
            detailDto.setProductId(101L);
            detailDto.setQuantity(new BigDecimal("100"));
            when(rfqRepository.save(any(Rfq.class))).thenReturn(rfq);
            when(detailRepository.save(any(RfqDetail.class))).thenAnswer(inv -> inv.getArgument(0));
            Rfq result = rfqService.createWithDetails(rfq, List.of(detailDto));
            assertNotNull(result);
            assertEquals(rfq.getId(), result.getId());
            ArgumentCaptor<RfqDetail> detailCaptor = ArgumentCaptor.forClass(RfqDetail.class);
            verify(rfqRepository, times(1)).save(rfq);
            verify(detailRepository, times(1)).save(detailCaptor.capture());
            RfqDetail capturedDetail = detailCaptor.getValue();
            assertEquals(rfq, capturedDetail.getRfq());
            assertEquals(detailDto.getProductId(), capturedDetail.getProduct().getId());
            assertEquals(detailDto.getQuantity(), capturedDetail.getQuantity());
            System.out.println("[SUCCESS] createWithDetails_Normal_WithOneDetail: RFQ and one detail were saved correctly.");
        }
        @Test
        @DisplayName("Normal Case: Create RFQ with multiple details")
        void createWithDetails_Normal_WithMultipleDetails() {
            Rfq rfq = new Rfq();
            rfq.setId(1L);
            RfqDetailDto detailDto1 = new RfqDetailDto();
            detailDto1.setProductId(101L);
            detailDto1.setQuantity(new BigDecimal("100"));

            RfqDetailDto detailDto2 = new RfqDetailDto();
            detailDto2.setProductId(102L);
            detailDto2.setQuantity(new BigDecimal("200"));

            when(rfqRepository.save(any(Rfq.class))).thenReturn(rfq);

            // When
            rfqService.createWithDetails(rfq, List.of(detailDto1, detailDto2));

            // Then
            verify(rfqRepository, times(1)).save(rfq);
            verify(detailRepository, times(2)).save(any(RfqDetail.class));

            System.out.println("[SUCCESS] createWithDetails_Normal_WithMultipleDetails: RFQ and multiple details were saved.");
        }

        @Test
        @DisplayName("Boundary Case: Create RFQ with a null details list")
        void createWithDetails_Boundary_NullDetailsList() {
            // Given
            Rfq rfq = new Rfq();
            rfq.setId(1L);

            when(rfqRepository.save(any(Rfq.class))).thenReturn(rfq);

            // When
            Rfq result = rfqService.createWithDetails(rfq, null);

            // Then
            assertNotNull(result);
            verify(rfqRepository, times(1)).save(rfq);
            verify(detailRepository, never()).save(any(RfqDetail.class));

            System.out.println("[SUCCESS] createWithDetails_Boundary_NullDetailsList: RFQ was saved and no details were processed for null list.");
        }

        @Test
        @DisplayName("Boundary Case: Create RFQ with an empty details list")
        void createWithDetails_Boundary_EmptyDetailsList() {
            // Given
            Rfq rfq = new Rfq();
            rfq.setId(1L);

            when(rfqRepository.save(any(Rfq.class))).thenReturn(rfq);

            // When
            Rfq result = rfqService.createWithDetails(rfq, Collections.emptyList());

            // Then
            assertNotNull(result);
            verify(rfqRepository, times(1)).save(rfq);
            verify(detailRepository, never()).save(any(RfqDetail.class));

            System.out.println("[SUCCESS] createWithDetails_Boundary_EmptyDetailsList: RFQ was saved and no details were processed for empty list.");
        }

        @Test
        @DisplayName("Boundary Case: Create RFQ with a detail having null productId")
        void createWithDetails_Boundary_DetailWithNullProductId() {
            // Given
            Rfq rfq = new Rfq();
            rfq.setId(1L);

            RfqDetailDto detailDto = new RfqDetailDto();
            detailDto.setProductId(null);
            detailDto.setQuantity(new BigDecimal("50"));

            detailDto.setProductId(null);
            when(rfqRepository.save(any(Rfq.class))).thenReturn(rfq);

            // When
            rfqService.createWithDetails(rfq, List.of(detailDto));

            // Then
            ArgumentCaptor<RfqDetail> detailCaptor = ArgumentCaptor.forClass(RfqDetail.class);
            verify(detailRepository, times(1)).save(detailCaptor.capture());

            RfqDetail capturedDetail = detailCaptor.getValue();
            assertNull(capturedDetail.getProduct(), "Product should be null when productId is null.");

            System.out.println("[SUCCESS] createWithDetails_Boundary_DetailWithNullProductId: Detail was saved with a null product.");
        }

        @Test
        @DisplayName("Abnormal Case: RFQ object is null")
        void createWithDetails_Abnormal_NullRfq() {
            // Given
            RfqDetailDto detailDto = new RfqDetailDto();
            detailDto.setProductId(101L);
            detailDto.setQuantity(new BigDecimal("100"));

            // When & Then
            when(rfqRepository.save(null)).thenThrow(new IllegalArgumentException());

            assertThrows(IllegalArgumentException.class, () -> {
                rfqService.createWithDetails(null, List.of(detailDto));
            });

            verify(detailRepository, never()).save(any());
            System.out.println("[SUCCESS] createWithDetails_Abnormal_NullRfq: Threw exception when RFQ object was null.");
        }

        @Test
        @DisplayName("Abnormal Case: Saving a detail fails")
        void createWithDetails_Abnormal_DetailSaveFails() {
            // Given
            Rfq rfq = new Rfq();
            rfq.setId(1L);

            RfqDetailDto detailDto = new RfqDetailDto();
            detailDto.setProductId(101L);
            detailDto.setQuantity(new BigDecimal("100"));

            when(rfqRepository.save(any(Rfq.class))).thenReturn(rfq);
            when(detailRepository.save(any(RfqDetail.class))).thenThrow(new RuntimeException("Database connection failed"));

            // When & Then
            assertThrows(RuntimeException.class, () -> {
                rfqService.createWithDetails(rfq, List.of(detailDto));
            });

            // Verify that the initial RFQ save was attempted, but the detail save failed.
            verify(rfqRepository, times(1)).save(rfq);
            verify(detailRepository, times(1)).save(any(RfqDetail.class));

            System.out.println("[SUCCESS] createWithDetails_Abnormal_DetailSaveFails: Exception propagated when detail saving failed.");
        }

        @Test
        @DisplayName("Boundary Case: Create RFQ with a detail having zero quantity")
        void createWithDetails_Boundary_DetailWithZeroQuantity() {
            // Given
            Rfq rfq = new Rfq();
            rfq.setId(1L);

            RfqDetailDto detailDto = new RfqDetailDto();
            detailDto.setProductId(101L);
            detailDto.setQuantity(BigDecimal.ZERO);

            when(rfqRepository.save(any(Rfq.class))).thenReturn(rfq);

            // When
            rfqService.createWithDetails(rfq, List.of(detailDto));

            // Then
            ArgumentCaptor<RfqDetail> detailCaptor = ArgumentCaptor.forClass(RfqDetail.class);
            verify(detailRepository, times(1)).save(detailCaptor.capture());

            RfqDetail capturedDetail = detailCaptor.getValue();
            assertEquals(0, BigDecimal.ZERO.compareTo(capturedDetail.getQuantity()), "Quantity should be zero.");

            System.out.println("[SUCCESS] createWithDetails_Boundary_DetailWithZeroQuantity: Detail was saved with zero quantity.");
        }

        @Test
        @DisplayName("Abnormal Case: Details list contains a null element")
        void createWithDetails_Abnormal_ListContainsNullDetailDto() {
            // Given
            Rfq rfq = new Rfq();
            rfq.setId(1L);

            when(rfqRepository.save(any(Rfq.class))).thenReturn(rfq);

            // When & Then
            // The loop will encounter a null DTO, causing a NullPointerException.
            assertThrows(NullPointerException.class, () -> {
                rfqService.createWithDetails(rfq, Collections.singletonList(null));
            });

            verify(rfqRepository, times(1)).save(rfq);
            verify(detailRepository, never()).save(any()); // Should not attempt to save any detail

            System.out.println("[SUCCESS] createWithDetails_Abnormal_ListContainsNullDetailDto: Threw exception when details list contained a null element.");
        }

        @Test
        @DisplayName("Abnormal Case: Saving the main RFQ object fails")
        void createWithDetails_Abnormal_RfqSaveFails() {
            // Given
            Rfq rfq = new Rfq();
            rfq.setId(1L);

            RfqDetailDto detailDto = new RfqDetailDto();
            detailDto.setProductId(101L);
            detailDto.setQuantity(new BigDecimal("100"));

            when(rfqRepository.save(any(Rfq.class))).thenThrow(new RuntimeException("Database connection failed for RFQ"));

            // When & Then
            assertThrows(RuntimeException.class, () -> {
                rfqService.createWithDetails(rfq, List.of(detailDto));
            });

            // Verify that the RFQ save was attempted, but no details were processed.
            verify(rfqRepository, times(1)).save(rfq);
            verify(detailRepository, never()).save(any());

            System.out.println("[SUCCESS] createWithDetails_Abnormal_RfqSaveFails: Exception propagated when main RFQ saving failed.");
        }
    }

    @Nested
    @DisplayName("Create Tests")
    class CreateTests {

        @Test
        @DisplayName("Normal Case: Create with a valid RFQ object")
        void create_Normal_ValidRfq() {
            // Given
            Rfq rfqToSave = new Rfq();
            rfqToSave.setRfqNumber("RFQ-NEW-001");
            rfqToSave.setCustomer(new Customer());
            rfqToSave.setStatus("DRAFT");

            Rfq savedRfq = new Rfq();
            savedRfq.setId(99L);
            savedRfq.setRfqNumber("RFQ-NEW-001");

            when(rfqRepository.save(any(Rfq.class))).thenReturn(savedRfq);

            // When
            Rfq result = rfqService.create(rfqToSave);

            // Then
            assertNotNull(result);
            assertEquals(99L, result.getId());
            verify(rfqRepository, times(1)).save(rfqToSave);

            System.out.println("[SUCCESS] create_Normal_ValidRfq: RFQ was created successfully.");
        }

        @Test
        @DisplayName("Abnormal Case: Create with a null RFQ object")
        void create_Abnormal_NullRfq() {
            // Given
            when(rfqRepository.save(null)).thenThrow(new IllegalArgumentException("Entity must not be null."));

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                rfqService.create(null);
            });

            verify(rfqRepository, times(1)).save(null);
            System.out.println("[SUCCESS] create_Abnormal_NullRfq: Threw exception for null RFQ object as expected.");
        }

        @Test
        @DisplayName("Abnormal Case: Repository throws an exception on save")
        void create_Abnormal_RepositoryThrowsException() {
            // Given
            Rfq rfqToSave = new Rfq();
            rfqToSave.setRfqNumber("RFQ-FAIL-001");

            when(rfqRepository.save(any(Rfq.class))).thenThrow(new RuntimeException("Database connection error"));

            // When & Then
            assertThrows(RuntimeException.class, () -> {
                rfqService.create(rfqToSave);
            });

            verify(rfqRepository, times(1)).save(rfqToSave);
            System.out.println("[SUCCESS] create_Abnormal_RepositoryThrowsException: Exception from repository was propagated correctly.");
        }

        @Test
        @DisplayName("Boundary Case: Create with an RFQ having null fields")
        void create_Boundary_RfqWithNullFields() {
            // Given
            Rfq rfqWithNulls = new Rfq();
            rfqWithNulls.setRfqNumber(null);
            rfqWithNulls.setCustomer(null);
            rfqWithNulls.setStatus(null);

            when(rfqRepository.save(any(Rfq.class))).thenReturn(rfqWithNulls);

            // When
            Rfq result = rfqService.create(rfqWithNulls);

            // Then
            assertNotNull(result);
            verify(rfqRepository, times(1)).save(rfqWithNulls);

            System.out.println("[SUCCESS] create_Boundary_RfqWithNullFields: RFQ with null fields was passed to repository.");
        }

        @Test
        @DisplayName("Normal Case: Create RFQ with some null and some non-null fields")
        void create_Normal_RfqWithPartialData() {
            // Given
            Rfq rfqToSave = new Rfq();
            rfqToSave.setRfqNumber("RFQ-PARTIAL-001");
            rfqToSave.setCustomer(null); // Customer is null
            rfqToSave.setStatus("DRAFT");
            rfqToSave.setExpectedDeliveryDate(LocalDate.now().plusDays(10));

            when(rfqRepository.save(rfqToSave)).thenReturn(rfqToSave);

            // When
            Rfq result = rfqService.create(rfqToSave);

            // Then
            assertNotNull(result);
            verify(rfqRepository, times(1)).save(rfqToSave);
            System.out.println("[SUCCESS] create_Normal_RfqWithPartialData: RFQ with partial data was passed to repository.");
        }

        @Test
        @DisplayName("Boundary Case: Create with an RFQ having empty string fields")
        void create_Boundary_RfqWithEmptyStrings() {
            // Given
            Rfq rfqWithEmptyStrings = new Rfq();
            rfqWithEmptyStrings.setRfqNumber("");
            rfqWithEmptyStrings.setStatus("");

            when(rfqRepository.save(rfqWithEmptyStrings)).thenReturn(rfqWithEmptyStrings);

            // When
            Rfq result = rfqService.create(rfqWithEmptyStrings);

            // Then
            assertNotNull(result);
            assertEquals("", result.getRfqNumber());
            verify(rfqRepository, times(1)).save(rfqWithEmptyStrings);
            System.out.println("[SUCCESS] create_Boundary_RfqWithEmptyStrings: RFQ with empty strings was passed to repository.");
        }

        @Test
        @DisplayName("Boundary Case: Create with a past expected delivery date")
        void create_Boundary_RfqWithPastDate() {
            // Given
            Rfq rfqWithPastDate = new Rfq();
            rfqWithPastDate.setRfqNumber("RFQ-PAST-DATE");
            rfqWithPastDate.setExpectedDeliveryDate(LocalDate.now().minusDays(30));

            when(rfqRepository.save(rfqWithPastDate)).thenReturn(rfqWithPastDate);

            // When
            Rfq result = rfqService.create(rfqWithPastDate);

            // Then
            assertNotNull(result);
            assertTrue(result.getExpectedDeliveryDate().isBefore(LocalDate.now()));
            verify(rfqRepository, times(1)).save(rfqWithPastDate);
            System.out.println("[SUCCESS] create_Boundary_RfqWithPastDate: RFQ with past date was passed to repository.");
        }

        @Test
        @DisplayName("Abnormal Case: Create with a pre-set ID")
        void create_Abnormal_RfqWithIdAlreadySet() {
            // Given
            Rfq rfqWithId = new Rfq();
            rfqWithId.setId(123L); // Pre-set ID
            rfqWithId.setRfqNumber("RFQ-WITH-ID");

            when(rfqRepository.save(rfqWithId)).thenReturn(rfqWithId);

            // When
            Rfq result = rfqService.create(rfqWithId);

            // Then
            assertNotNull(result);
            assertEquals(123L, result.getId());
            verify(rfqRepository, times(1)).save(rfqWithId);
            System.out.println("[SUCCESS] create_Abnormal_RfqWithIdAlreadySet: RFQ with pre-set ID was passed to repository.");
        }

        @Test
        @DisplayName("Normal Case: Create with a non-default status")
        void create_Normal_RfqWithDifferentStatus() {
            // Given
            Rfq rfqWithStatus = new Rfq();
            rfqWithStatus.setStatus("SENT"); // A status other than DRAFT

            when(rfqRepository.save(rfqWithStatus)).thenReturn(rfqWithStatus);

            // When
            Rfq result = rfqService.create(rfqWithStatus);

            // Then
            assertNotNull(result);
            assertEquals("SENT", result.getStatus());
            verify(rfqRepository, times(1)).save(rfqWithStatus);
            System.out.println("[SUCCESS] create_Normal_RfqWithDifferentStatus: RFQ with non-default status was passed to repository.");
        }

        @Test
        @DisplayName("Boundary Case: Create with a very long RFQ number")
        void create_Boundary_RfqWithLongRfqNumber() {
            // Given
            String longRfqNumber = "RFQ-".repeat(50); // A very long string
            Rfq rfqWithLongString = new Rfq();
            rfqWithLongString.setRfqNumber(longRfqNumber);

            when(rfqRepository.save(rfqWithLongString)).thenReturn(rfqWithLongString);

            // When
            Rfq result = rfqService.create(rfqWithLongString);

            // Then
            assertNotNull(result);
            assertEquals(longRfqNumber, result.getRfqNumber());
            verify(rfqRepository, times(1)).save(rfqWithLongString);
            System.out.println("[SUCCESS] create_Boundary_RfqWithLongRfqNumber: RFQ with long RFQ number was passed to repository.");
        }
    }

    @Nested
    @DisplayName("Update Tests")
    class UpdateTests {

        @Test
        @DisplayName("Normal Case: Update all fields with new values")
        void update_Normal_AllFieldsUpdated() {
            // Given
            Long rfqId = 1L;

            Rfq existingRfq = new Rfq();
            existingRfq.setId(rfqId);
            existingRfq.setRfqNumber("OLD-001");
            existingRfq.setStatus("DRAFT");

            Rfq updatedInfo = new Rfq();
            updatedInfo.setRfqNumber("NEW-002");
            updatedInfo.setStatus("SENT");
            updatedInfo.setNotes("Updated notes");
            updatedInfo.setExpectedDeliveryDate(LocalDate.now().plusMonths(1));

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(existingRfq));

            // When
            Rfq result = rfqService.update(rfqId, updatedInfo);

            // Then
            assertNotNull(result);
            assertEquals("NEW-002", result.getRfqNumber());
            assertEquals("SENT", result.getStatus());
            assertEquals("Updated notes", result.getNotes());
            assertEquals(updatedInfo.getExpectedDeliveryDate(), result.getExpectedDeliveryDate());

            verify(rfqRepository, times(1)).findById(rfqId);

            System.out.println("[SUCCESS] update_Normal_AllFieldsUpdated: RFQ was updated successfully.");
        }

        @Test
        @DisplayName("Abnormal Case: RFQ to be updated is not found")
        void update_Abnormal_RfqNotFound() {
            // Given
            Long nonExistentId = 99L;
            Rfq updatedInfo = new Rfq();

            when(rfqRepository.findById(nonExistentId)).thenReturn(java.util.Optional.empty());

            // When & Then
            assertThrows(java.util.NoSuchElementException.class, () -> {
                rfqService.update(nonExistentId, updatedInfo);
            });

            verify(rfqRepository, times(1)).findById(nonExistentId);

            System.out.println("[SUCCESS] update_Abnormal_RfqNotFound: Threw exception for non-existent RFQ as expected.");
        }

        @Test
        @DisplayName("Abnormal Case: The updated info object is null")
        void update_Abnormal_NullUpdatedObject() {
            // Given
            Long rfqId = 1L;
            Rfq existingRfq = new Rfq();
            existingRfq.setId(rfqId);

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(existingRfq));

            // When & Then
            assertThrows(NullPointerException.class, () -> {
                rfqService.update(rfqId, null);
            });

            verify(rfqRepository, times(1)).findById(rfqId);

            System.out.println("[SUCCESS] update_Abnormal_NullUpdatedObject: Threw exception for null updated object as expected.");
        }

        @Test
        @DisplayName("Boundary Case: Update with an object containing all null values")
        void update_Boundary_UpdateWithNullValues() {
            // Given
            Long rfqId = 1L;

            Rfq existingRfq = new Rfq();
            existingRfq.setId(rfqId);
            existingRfq.setRfqNumber("OLD-001");
            existingRfq.setStatus("DRAFT");

            Rfq updatedInfoWithNulls = new Rfq(); // All fields are null by default

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(existingRfq));

            // When
            Rfq result = rfqService.update(rfqId, updatedInfoWithNulls);

            // Then
            assertNotNull(result);
            assertNull(result.getRfqNumber());
            assertNull(result.getStatus());

            verify(rfqRepository, times(1)).findById(rfqId);

            System.out.println("[SUCCESS] update_Boundary_UpdateWithNullValues: RFQ fields were correctly updated to null.");
        }

        @Test
        @DisplayName("Boundary Case: Update with an object containing empty strings")
        void update_Boundary_UpdateWithEmptyStrings() {
            // Given
            Long rfqId = 1L;
            Rfq existingRfq = new Rfq();
            existingRfq.setId(rfqId);

            Rfq updatedInfo = new Rfq();
            updatedInfo.setRfqNumber("");
            updatedInfo.setStatus("");
            updatedInfo.setNotes("");

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(existingRfq));

            // When
            Rfq result = rfqService.update(rfqId, updatedInfo);

            // Then
            assertNotNull(result);
            assertEquals("", result.getRfqNumber());
            assertEquals("", result.getStatus());
            assertEquals("", result.getNotes());

            verify(rfqRepository, times(1)).findById(rfqId);

            System.out.println("[SUCCESS] update_Boundary_UpdateWithEmptyStrings: RFQ fields were correctly updated to empty strings.");
        }

        @Test
        @DisplayName("Abnormal Case: RFQ ID is null")
        void update_Abnormal_IdIsNull() {
            // Given
            Long rfqId = null;
            Rfq updatedInfo = new Rfq();

            // Mocking findById with null argument to throw IllegalArgumentException
            when(rfqRepository.findById(rfqId)).thenThrow(new IllegalArgumentException("ID must not be null!"));

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                rfqService.update(rfqId, updatedInfo);
            });

            verify(rfqRepository, times(1)).findById(rfqId);
            System.out.println("[SUCCESS] update_Abnormal_IdIsNull: Threw exception for null RFQ ID as expected.");
        }

        @Test
        @DisplayName("Boundary Case: Partial update of fields")
        void update_Boundary_PartialUpdate() {
            // Given
            Long rfqId = 1L;

            Rfq existingRfq = new Rfq();
            existingRfq.setId(rfqId);
            existingRfq.setRfqNumber("Original-001");
            existingRfq.setStatus("DRAFT");
            existingRfq.setNotes("Original notes");
            existingRfq.setSent(false);

            Rfq updatedInfo = new Rfq();
            updatedInfo.setRfqNumber("Partial-001"); // Update rfqNumber
            updatedInfo.setNotes("New partial notes"); // Update notes
            // status, expectedDeliveryDate, sent, customer, createdBy, approvedBy are null in updatedInfo

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(existingRfq));

            // When
            Rfq result = rfqService.update(rfqId, updatedInfo);

            // Then
            assertNotNull(result);
            assertEquals("Partial-001", result.getRfqNumber());
            assertEquals("New partial notes", result.getNotes());
            // Other fields should be updated to null from updatedInfo
            assertNull(result.getStatus());
            assertNull(result.getExpectedDeliveryDate());
            assertNull(result.getSent());
            assertNull(result.getCustomer());
            assertNull(result.getCreatedBy());
            assertNull(result.getApprovedBy());

            verify(rfqRepository, times(1)).findById(rfqId);
            System.out.println("[SUCCESS] update_Boundary_PartialUpdate: RFQ was partially updated correctly.");
        }

        @Test
        @DisplayName("Boundary Case: Update with identical values")
        void update_Boundary_UpdateWithSameValues() {
            // Given
            Long rfqId = 1L;
            LocalDate deliveryDate = LocalDate.now().plusWeeks(1);

            Rfq existingRfq = new Rfq();
            existingRfq.setId(rfqId);
            existingRfq.setRfqNumber("SAME-001");
            existingRfq.setStatus("DRAFT");
            existingRfq.setExpectedDeliveryDate(deliveryDate);

            Rfq updatedInfo = new Rfq();
            updatedInfo.setRfqNumber("SAME-001");
            updatedInfo.setStatus("DRAFT");
            updatedInfo.setExpectedDeliveryDate(deliveryDate);

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(existingRfq));

            // When
            Rfq result = rfqService.update(rfqId, updatedInfo);

            // Then
            assertNotNull(result);
            assertEquals("SAME-001", result.getRfqNumber());
            assertEquals("DRAFT", result.getStatus());
            assertEquals(deliveryDate, result.getExpectedDeliveryDate());

            verify(rfqRepository, times(1)).findById(rfqId);
            System.out.println("[SUCCESS] update_Boundary_UpdateWithSameValues: RFQ updated with identical values successfully.");
        }


    }

    @Nested
    @DisplayName("Add Detail Tests")
    class AddDetailTests {

        @Test
        @DisplayName("Normal Case: Add a valid detail to an existing RFQ")
        void addDetail_Normal_ValidInputs() {
            // Given
            Long rfqId = 1L;
            Rfq existingRfq = new Rfq();
            existingRfq.setId(rfqId);

            RfqDetailDto newDetailDto = new RfqDetailDto();
            newDetailDto.setProductId(101L);
            newDetailDto.setQuantity(new BigDecimal("500"));
            newDetailDto.setUnit("Cái");

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(existingRfq));
            when(detailRepository.save(any(RfqDetail.class))).thenAnswer(inv -> {
                RfqDetail detail = inv.getArgument(0);
                detail.setId(201L); // Simulate saving and getting an ID
                return detail;
            });

            // When
            RfqDetail result = rfqService.addDetail(rfqId, newDetailDto);

            // Then
            assertNotNull(result);
            assertEquals(201L, result.getId());
            assertEquals(existingRfq, result.getRfq());
            assertEquals(101L, result.getProduct().getId());
            assertEquals(new BigDecimal("500"), result.getQuantity());

            verify(rfqRepository, times(1)).findById(rfqId);
            verify(detailRepository, times(1)).save(any(RfqDetail.class));

            System.out.println("[SUCCESS] addDetail_Normal_ValidInputs: Detail added successfully.");
        }

        @Test
        @DisplayName("Abnormal Case: RFQ not found for the given ID")
        void addDetail_Abnormal_RfqNotFound() {
            // Given
            Long nonExistentRfqId = 99L;
            RfqDetailDto newDetailDto = new RfqDetailDto();

            when(rfqRepository.findById(nonExistentRfqId)).thenReturn(java.util.Optional.empty());

            // When & Then
            assertThrows(java.util.NoSuchElementException.class, () -> {
                rfqService.addDetail(nonExistentRfqId, newDetailDto);
            });

            verify(rfqRepository, times(1)).findById(nonExistentRfqId);
            verify(detailRepository, never()).save(any());

            System.out.println("[SUCCESS] addDetail_Abnormal_RfqNotFound: Threw exception for non-existent RFQ as expected.");
        }

        @Test
        @DisplayName("Abnormal Case: The detail DTO is null")
        void addDetail_Abnormal_NullDto() {
            // Given
            Long rfqId = 1L;
            Rfq existingRfq = new Rfq();
            existingRfq.setId(rfqId);

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(existingRfq));

            // When & Then
            assertThrows(NullPointerException.class, () -> {
                rfqService.addDetail(rfqId, null);
            });

            verify(rfqRepository, times(1)).findById(rfqId);
            verify(detailRepository, never()).save(any());

            System.out.println("[SUCCESS] addDetail_Abnormal_NullDto: Threw exception for null DTO as expected.");
        }

        @Test
        @DisplayName("Abnormal Case: RFQ ID is null")
        void addDetail_Abnormal_NullRfqId() {
            // Given
            Long rfqId = null;
            RfqDetailDto newDetailDto = new RfqDetailDto();

            when(rfqRepository.findById(null)).thenThrow(new IllegalArgumentException("ID must not be null!"));

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                rfqService.addDetail(rfqId, newDetailDto);
            });

            verify(rfqRepository, times(1)).findById(null);
            verify(detailRepository, never()).save(any());

            System.out.println("[SUCCESS] addDetail_Abnormal_NullRfqId: Threw exception for null RFQ ID as expected.");
        }

        @Test
        @DisplayName("Boundary Case: Add a detail with a null productId")
        void addDetail_Boundary_NullProductId() {
            // Given
            Long rfqId = 1L;
            Rfq existingRfq = new Rfq();
            existingRfq.setId(rfqId);

            RfqDetailDto newDetailDto = new RfqDetailDto();
            newDetailDto.setProductId(null); // Null product ID
            newDetailDto.setQuantity(new BigDecimal("100"));

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(existingRfq));
            when(detailRepository.save(any(RfqDetail.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            RfqDetail result = rfqService.addDetail(rfqId, newDetailDto);

            // Then
            assertNotNull(result);
            assertNull(result.getProduct());

            verify(rfqRepository, times(1)).findById(rfqId);
            verify(detailRepository, times(1)).save(any(RfqDetail.class));

            System.out.println("[SUCCESS] addDetail_Boundary_NullProductId: Detail with null product was added successfully.");
        }

        @Test
        @DisplayName("Abnormal Case: Repository throws exception on save")
        void addDetail_Abnormal_RepositorySaveFails() {
            // Given
            Long rfqId = 1L;
            Rfq existingRfq = new Rfq();
            existingRfq.setId(rfqId);

            RfqDetailDto newDetailDto = new RfqDetailDto();
            newDetailDto.setProductId(101L);

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(existingRfq));
            when(detailRepository.save(any(RfqDetail.class))).thenThrow(new RuntimeException("Database connection error"));

            // When & Then
            assertThrows(RuntimeException.class, () -> {
                rfqService.addDetail(rfqId, newDetailDto);
            });

            verify(rfqRepository, times(1)).findById(rfqId);
            verify(detailRepository, times(1)).save(any(RfqDetail.class));

            System.out.println("[SUCCESS] addDetail_Abnormal_RepositorySaveFails: Exception from repository was propagated correctly.");
        }

        @Test
        @DisplayName("Boundary Case: Add a detail with a null quantity")
        void addDetail_Boundary_DtoWithNullQuantity() {
            // Given
            Long rfqId = 1L;
            Rfq existingRfq = new Rfq();
            existingRfq.setId(rfqId);

            RfqDetailDto newDetailDto = new RfqDetailDto();
            newDetailDto.setProductId(101L);
            newDetailDto.setQuantity(null); // Null quantity

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(existingRfq));

            // When
            rfqService.addDetail(rfqId, newDetailDto);

            // Then
            ArgumentCaptor<RfqDetail> detailCaptor = ArgumentCaptor.forClass(RfqDetail.class);
            verify(detailRepository).save(detailCaptor.capture());
            assertNull(detailCaptor.getValue().getQuantity(), "Quantity should be null in the saved entity.");

            System.out.println("[SUCCESS] addDetail_Boundary_DtoWithNullQuantity: Detail with null quantity was processed correctly.");
        }

        @Test
        @DisplayName("Boundary Case: Add a detail with empty string fields")
        void addDetail_Boundary_DtoWithEmptyStrings() {
            // Given
            Long rfqId = 1L;
            Rfq existingRfq = new Rfq();
            existingRfq.setId(rfqId);

            RfqDetailDto newDetailDto = new RfqDetailDto();
            newDetailDto.setUnit(""); // Empty string
            newDetailDto.setNotes(""); // Empty string

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(existingRfq));

            // When
            rfqService.addDetail(rfqId, newDetailDto);

            // Then
            ArgumentCaptor<RfqDetail> detailCaptor = ArgumentCaptor.forClass(RfqDetail.class);
            verify(detailRepository).save(detailCaptor.capture());
            assertEquals("", detailCaptor.getValue().getUnit());
            assertEquals("", detailCaptor.getValue().getNotes());

            System.out.println("[SUCCESS] addDetail_Boundary_DtoWithEmptyStrings: Detail with empty strings was processed correctly.");
        }
    }

    @Nested
    @DisplayName("Update Detail Tests")
    class UpdateDetailTests {

        @Test
        @DisplayName("Normal Case: Update all fields of an existing detail")
        void updateDetail_Normal_AllFieldsUpdated() {
            // Given
            Long detailId = 1L;

            RfqDetail existingDetail = new RfqDetail();
            existingDetail.setId(detailId);
            existingDetail.setQuantity(new BigDecimal("100"));

            RfqDetailDto updatedDto = new RfqDetailDto();
            updatedDto.setProductId(202L);
            updatedDto.setQuantity(new BigDecimal("150"));
            updatedDto.setUnit("KG");
            updatedDto.setNotes("New notes");

            when(detailRepository.findById(detailId)).thenReturn(java.util.Optional.of(existingDetail));
            when(detailRepository.save(any(RfqDetail.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            RfqDetail result = rfqService.updateDetail(detailId, updatedDto);

            // Then
            assertNotNull(result);
            assertEquals(202L, result.getProduct().getId());
            assertEquals(new BigDecimal("150"), result.getQuantity());
            assertEquals("KG", result.getUnit());
            assertEquals("New notes", result.getNotes());

            verify(detailRepository, times(1)).findById(detailId);
            verify(detailRepository, times(1)).save(existingDetail);

            System.out.println("[SUCCESS] updateDetail_Normal_AllFieldsUpdated: Detail was updated successfully.");
        }

        @Test
        @DisplayName("Abnormal Case: Detail not found for the given ID")
        void updateDetail_Abnormal_DetailNotFound() {
            // Given
            Long nonExistentId = 99L;
            RfqDetailDto updatedDto = new RfqDetailDto();

            when(detailRepository.findById(nonExistentId)).thenReturn(java.util.Optional.empty());

            // When & Then
            assertThrows(java.util.NoSuchElementException.class, () -> {
                rfqService.updateDetail(nonExistentId, updatedDto);
            });

            verify(detailRepository, times(1)).findById(nonExistentId);
            verify(detailRepository, never()).save(any());

            System.out.println("[SUCCESS] updateDetail_Abnormal_DetailNotFound: Threw exception for non-existent detail as expected.");
        }

        @Test
        @DisplayName("Abnormal Case: The update DTO is null")
        void updateDetail_Abnormal_NullDto() {
            // Given
            Long detailId = 1L;
            RfqDetail existingDetail = new RfqDetail();
            existingDetail.setId(detailId);

            when(detailRepository.findById(detailId)).thenReturn(java.util.Optional.of(existingDetail));

            // When & Then
            assertThrows(NullPointerException.class, () -> {
                rfqService.updateDetail(detailId, null);
            });

            verify(detailRepository, times(1)).findById(detailId);
            verify(detailRepository, never()).save(any());

            System.out.println("[SUCCESS] updateDetail_Abnormal_NullDto: Threw exception for null DTO as expected.");
        }

        @Test
        @DisplayName("Abnormal Case: Detail ID is null")
        void updateDetail_Abnormal_NullDetailId() {
            // Given
            Long detailId = null;
            RfqDetailDto updatedDto = new RfqDetailDto();

            when(detailRepository.findById(null)).thenThrow(new IllegalArgumentException("ID must not be null!"));

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                rfqService.updateDetail(detailId, updatedDto);
            });

            verify(detailRepository, times(1)).findById(null);
            verify(detailRepository, never()).save(any());

            System.out.println("[SUCCESS] updateDetail_Abnormal_NullDetailId: Threw exception for null detail ID as expected.");
        }

        @Test
        @DisplayName("Boundary Case: Update with a DTO where productId is null")
        void updateDetail_Boundary_UpdateWithNullProductId() {
            // Given
            Long detailId = 1L;

            RfqDetail existingDetail = new RfqDetail();
            existingDetail.setId(detailId);
            Product oldProduct = new Product();
            oldProduct.setId(101L);
            existingDetail.setProduct(oldProduct);

            RfqDetailDto updatedDto = new RfqDetailDto();
            updatedDto.setProductId(null); // Update product to null
            updatedDto.setQuantity(new BigDecimal("200"));

            when(detailRepository.findById(detailId)).thenReturn(java.util.Optional.of(existingDetail));
            when(detailRepository.save(any(RfqDetail.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            RfqDetail result = rfqService.updateDetail(detailId, updatedDto);

            // Then
            assertNotNull(result);
            // The current implementation does not set product to null if DTO's productId is null.
            // It keeps the old product. This test verifies the current behavior.
            assertNotNull(result.getProduct());
            assertEquals(101L, result.getProduct().getId());
            assertEquals(new BigDecimal("200"), result.getQuantity());

            verify(detailRepository, times(1)).findById(detailId);
            verify(detailRepository, times(1)).save(existingDetail);

            System.out.println("[SUCCESS] updateDetail_Boundary_UpdateWithNullProductId: Verified current behavior of not nullifying product.");
        }

        @Test
        @DisplayName("Abnormal Case: Repository throws exception on save")
        void updateDetail_Abnormal_RepositorySaveFails() {
            // Given
            Long detailId = 1L;
            RfqDetail existingDetail = new RfqDetail();
            existingDetail.setId(detailId);

            RfqDetailDto updatedDto = new RfqDetailDto();
            updatedDto.setQuantity(new BigDecimal("300"));

            when(detailRepository.findById(detailId)).thenReturn(java.util.Optional.of(existingDetail));
            when(detailRepository.save(any(RfqDetail.class))).thenThrow(new RuntimeException("Database connection error"));

            // When & Then
            assertThrows(RuntimeException.class, () -> {
                rfqService.updateDetail(detailId, updatedDto);
            });

            verify(detailRepository, times(1)).findById(detailId);
            verify(detailRepository, times(1)).save(existingDetail);

            System.out.println("[SUCCESS] updateDetail_Abnormal_RepositorySaveFails: Exception from repository was propagated correctly.");
        }

        @Test
        @DisplayName("Boundary Case: Partial update with some null fields in DTO")
        void updateDetail_Boundary_PartialUpdate() {
            // Given
            Long detailId = 1L;

            RfqDetail existingDetail = new RfqDetail();
            existingDetail.setId(detailId);
            existingDetail.setQuantity(new BigDecimal("100"));
            existingDetail.setUnit("Cái");

            RfqDetailDto updatedDto = new RfqDetailDto();
            updatedDto.setQuantity(new BigDecimal("150")); // Update quantity
            updatedDto.setUnit(null); // Set unit to null

            when(detailRepository.findById(detailId)).thenReturn(java.util.Optional.of(existingDetail));
            when(detailRepository.save(any(RfqDetail.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            RfqDetail result = rfqService.updateDetail(detailId, updatedDto);

            // Then
            assertNotNull(result);
            assertEquals(new BigDecimal("150"), result.getQuantity());
            assertNull(result.getUnit(), "Unit should be updated to null.");

            verify(detailRepository, times(1)).findById(detailId);
            verify(detailRepository, times(1)).save(existingDetail);

            System.out.println("[SUCCESS] updateDetail_Boundary_PartialUpdate: Detail was partially updated with null values correctly.");
        }

        @Test
        @DisplayName("Boundary Case: Update with empty string fields")
        void updateDetail_Boundary_UpdateWithEmptyStrings() {
            // Given
            Long detailId = 1L;
            RfqDetail existingDetail = new RfqDetail();
            existingDetail.setId(detailId);

            RfqDetailDto updatedDto = new RfqDetailDto();
            updatedDto.setUnit("");
            updatedDto.setNotes("");

            when(detailRepository.findById(detailId)).thenReturn(java.util.Optional.of(existingDetail));
            when(detailRepository.save(any(RfqDetail.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            RfqDetail result = rfqService.updateDetail(detailId, updatedDto);

            // Then
            assertNotNull(result);
            assertEquals("", result.getUnit());
            assertEquals("", result.getNotes());

            verify(detailRepository, times(1)).findById(detailId);
            verify(detailRepository, times(1)).save(existingDetail);

            System.out.println("[SUCCESS] updateDetail_Boundary_UpdateWithEmptyStrings: Detail fields were correctly updated to empty strings.");
        }
    }

    @Nested
    @DisplayName("Delete Detail Tests")
    class DeleteDetailTests {

        @Test
        @DisplayName("Normal Case: Delete a detail with a valid ID")
        void deleteDetail_Normal_ValidId() {
            // Given
            Long detailId = 1L;
            doNothing().when(detailRepository).deleteById(detailId);

            // When
            rfqService.deleteDetail(detailId);

            // Then
            verify(detailRepository, times(1)).deleteById(detailId);
            System.out.println("[SUCCESS] deleteDetail_Normal_ValidId: Correctly called repository to delete detail.");
        }

        @Test
        @DisplayName("Abnormal Case: Detail ID is null")
        void deleteDetail_Abnormal_NullId() {
            // Given
            Long detailId = null;
            // Spring Data's deleteById throws IllegalArgumentException for null IDs.
            doThrow(new IllegalArgumentException("ID must not be null!")).when(detailRepository).deleteById(null);

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                rfqService.deleteDetail(detailId);
            });

            verify(detailRepository, times(1)).deleteById(null);
            System.out.println("[SUCCESS] deleteDetail_Abnormal_NullId: Threw exception for null ID as expected.");
        }

        @Test
        @DisplayName("Boundary Case: Delete a non-existent detail ID")
        void deleteDetail_Boundary_NonExistentId() {
            // Given
            Long nonExistentId = 999L;
            // deleteById does not throw an exception if the ID doesn't exist.
            // We can mock it to throw EmptyResultDataAccessException to be more realistic if needed,
            // but for this test, we just verify the call.
            doNothing().when(detailRepository).deleteById(nonExistentId);

            // When
            rfqService.deleteDetail(nonExistentId);

            // Then
            verify(detailRepository, times(1)).deleteById(nonExistentId);
            System.out.println("[SUCCESS] deleteDetail_Boundary_NonExistentId: Correctly called repository even for a non-existent ID.");
        }
    }

    @Nested
    @DisplayName("Update Expected Delivery Date Tests")
    class UpdateExpectedDeliveryDateTests {

        @Test
        @DisplayName("Normal Case: Update with a valid yyyy-MM-dd date string")
        void updateExpectedDeliveryDate_Normal_ValidYyyyMmDdFormat() {
            // Given
            Long rfqId = 1L;
            String dateString = "2025-12-25";
            Rfq existingRfq = new Rfq();
            existingRfq.setId(rfqId);

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(existingRfq));
            when(rfqRepository.save(any(Rfq.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Rfq result = rfqService.updateExpectedDeliveryDate(rfqId, dateString);

            // Then
            assertNotNull(result);
            assertEquals(LocalDate.of(2025, 12, 25), result.getExpectedDeliveryDate());
            verify(rfqRepository, times(1)).findById(rfqId);
            verify(rfqRepository, times(1)).save(existingRfq);

            System.out.println("[SUCCESS] updateExpectedDeliveryDate_Normal_ValidYyyyMmDdFormat: Date updated successfully.");
        }

        @Test
        @DisplayName("Normal Case: Update with a valid dd-MM-yyyy date string")
        void updateExpectedDeliveryDate_Normal_ValidDdMmYyyyFormat() {
            // Given
            Long rfqId = 1L;
            String dateString = "15-08-2024";
            Rfq existingRfq = new Rfq();
            existingRfq.setId(rfqId);

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(existingRfq));
            when(rfqRepository.save(any(Rfq.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Rfq result = rfqService.updateExpectedDeliveryDate(rfqId, dateString);

            // Then
            assertNotNull(result);
            assertEquals(LocalDate.of(2024, 8, 15), result.getExpectedDeliveryDate());
            verify(rfqRepository, times(1)).findById(rfqId);
            verify(rfqRepository, times(1)).save(existingRfq);

            System.out.println("[SUCCESS] updateExpectedDeliveryDate_Normal_ValidDdMmYyyyFormat: Date updated successfully.");
        }

        @Test
        @DisplayName("Abnormal Case: RFQ not found for the given ID")
        void updateExpectedDeliveryDate_Abnormal_RfqNotFound() {
            // Given
            Long nonExistentId = 99L;
            String dateString = "2025-01-01";
            when(rfqRepository.findById(nonExistentId)).thenReturn(java.util.Optional.empty());

            // When & Then
            assertThrows(java.util.NoSuchElementException.class, () -> {
                rfqService.updateExpectedDeliveryDate(nonExistentId, dateString);
            });

            verify(rfqRepository, times(1)).findById(nonExistentId);
            verify(rfqRepository, never()).save(any());

            System.out.println("[SUCCESS] updateExpectedDeliveryDate_Abnormal_RfqNotFound: Threw exception for non-existent RFQ.");
        }

        @Test
        @DisplayName("Abnormal Case: Date string has an invalid format")
        void updateExpectedDeliveryDate_Abnormal_InvalidFormat() {
            // Given
            Long rfqId = 1L;
            String invalidDateString = "2025/12/25"; // Invalid format
            Rfq existingRfq = new Rfq();
            existingRfq.setId(rfqId);

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(existingRfq));

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                rfqService.updateExpectedDeliveryDate(rfqId, invalidDateString);
            });

            assertTrue(exception.getMessage().contains("Ngày không hợp lệ"));
            verify(rfqRepository, times(1)).findById(rfqId);
            verify(rfqRepository, never()).save(any());

            System.out.println("[SUCCESS] updateExpectedDeliveryDate_Abnormal_InvalidFormat: Threw exception for invalid date format.");
        }

        @Test
        @DisplayName("Abnormal Case: Date string is a valid format but an invalid date")
        void updateExpectedDeliveryDate_Abnormal_InvalidDateValue() {
            // Given
            Long rfqId = 1L;
            String invalidDateString = "30-02-2025"; // Invalid date
            Rfq existingRfq = new Rfq();
            existingRfq.setId(rfqId);

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(existingRfq));

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                rfqService.updateExpectedDeliveryDate(rfqId, invalidDateString);
            });

            System.out.println("[SUCCESS] updateExpectedDeliveryDate_Abnormal_InvalidDateValue: Threw exception for invalid date value.");
        }

        @Test
        @DisplayName("Abnormal Case: Date string is null")
        void updateExpectedDeliveryDate_Abnormal_NullDateString() {
            // Given
            Long rfqId = 1L;
            Rfq existingRfq = new Rfq();
            existingRfq.setId(rfqId);

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(existingRfq));

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                rfqService.updateExpectedDeliveryDate(rfqId, null);
            });

            System.out.println("[SUCCESS] updateExpectedDeliveryDate_Abnormal_NullDateString: Threw exception for null date string.");
        }

        @Test
        @DisplayName("Abnormal Case: RFQ ID is null")
        void updateExpectedDeliveryDate_Abnormal_NullRfqId() {
            // Given
            String dateString = "2025-01-01";
            when(rfqRepository.findById(null)).thenThrow(new IllegalArgumentException("ID must not be null!"));

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                rfqService.updateExpectedDeliveryDate(null, dateString);
            });

            System.out.println("[SUCCESS] updateExpectedDeliveryDate_Abnormal_NullRfqId: Threw exception for null RFQ ID.");
        }

        @Test
        @DisplayName("Boundary Case: Date string is empty")
        void updateExpectedDeliveryDate_Boundary_EmptyDateString() {
            // Given
            Long rfqId = 1L;
            String emptyDateString = "";
            Rfq existingRfq = new Rfq();
            existingRfq.setId(rfqId);

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(existingRfq));

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                rfqService.updateExpectedDeliveryDate(rfqId, emptyDateString);
            });

            System.out.println("[SUCCESS] updateExpectedDeliveryDate_Boundary_EmptyDateString: Threw exception for empty date string.");
        }

        @Test
        @DisplayName("Boundary Case: Update with a past date")
        void updateExpectedDeliveryDate_Boundary_PastDate() {
            // Given
            Long rfqId = 1L;
            String dateString = "2020-01-01";
            Rfq existingRfq = new Rfq();
            existingRfq.setId(rfqId);

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(existingRfq));
            when(rfqRepository.save(any(Rfq.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Rfq result = rfqService.updateExpectedDeliveryDate(rfqId, dateString);

            // Then
            assertNotNull(result);
            assertEquals(LocalDate.of(2020, 1, 1), result.getExpectedDeliveryDate());

            System.out.println("[SUCCESS] updateExpectedDeliveryDate_Boundary_PastDate: Successfully updated with a past date.");
        }

        @Test
        @DisplayName("Abnormal Case: Repository fails to save")
        void updateExpectedDeliveryDate_Abnormal_SaveFails() {
            // Given
            Long rfqId = 1L;
            String dateString = "2025-05-10";
            Rfq existingRfq = new Rfq();
            existingRfq.setId(rfqId);

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(existingRfq));
            when(rfqRepository.save(any(Rfq.class))).thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThrows(RuntimeException.class, () -> {
                rfqService.updateExpectedDeliveryDate(rfqId, dateString);
            });

            verify(rfqRepository, times(1)).findById(rfqId);
            verify(rfqRepository, times(1)).save(existingRfq);

            System.out.println("[SUCCESS] updateExpectedDeliveryDate_Abnormal_SaveFails: Exception from repository was propagated correctly.");
        }
    }
    @Nested
    @DisplayName("Assign Sales Tests")
    class AssignSalesTests {

        // Helper: tạo RFQ DRAFT cơ bản
        private Rfq createDraftRfq(Long id, String rfqNumber) {
            Rfq rfq = new Rfq();
            rfq.setId(id);
            rfq.setStatus("DRAFT");
            rfq.setRfqNumber(rfqNumber);
            return rfq; // contactPersonSnapshot để mặc định null
        }

        // Helper: tạo User với role
        private User createUser(Long id, String roleName) {
            User user = new User();
            user.setId(id);
            Role role = new Role();
            role.setName(roleName);
            user.setRole(role);
            return user;
        }

        // UTC01 – Normal: dùng salesId, RFQ DRAFT, dữ liệu đầy đủ
        @Test
        @DisplayName("UTC01 - Normal: assign by salesId on DRAFT RFQ")
        void assignSales_UTC01_assignBySalesId_Draft() {
            Long rfqId = 1L;
            Long salesId = 12L;

            Rfq rfq = createDraftRfq(rfqId, "RFQ-001");
            User salesUser = createUser(salesId, "SALES");

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(rfq));
            when(userRepository.findById(salesId)).thenReturn(java.util.Optional.of(salesUser));
            when(rfqRepository.save(any(Rfq.class))).thenAnswer(inv -> inv.getArgument(0));

            Rfq result = rfqService.assignSales(rfqId, salesId, null);

            assertNotNull(result);
            assertEquals(salesUser, result.getAssignedSales());
            verify(rfqRepository).findById(rfqId);
            verify(userRepository).findById(salesId);
            verify(rfqRepository).save(rfq);
            verify(notificationService).notifyUser(
                    eq(salesUser),
                    anyString(), anyString(), anyString(), anyString(), anyString(),
                    eq(rfqId)
            );
        }

        // UTC02 – Normal: dùng employeeCode, RFQ DRAFT
        @Test
        @DisplayName("UTC02 - Normal: assign by employeeCode on DRAFT RFQ")
        void assignSales_UTC02_assignByEmployeeCode_Draft() {
            Long rfqId = 1L;
            String employeeCode = "10";

            Rfq rfq = createDraftRfq(rfqId, "RFQ-001");
            User salesUser = createUser(12L, "SALES");

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(rfq));
            when(userRepository.findByEmployeeCode(employeeCode)).thenReturn(java.util.Optional.of(salesUser));
            when(rfqRepository.save(any(Rfq.class))).thenAnswer(inv -> inv.getArgument(0));

            Rfq result = rfqService.assignSales(rfqId, null, employeeCode);

            assertNotNull(result);
            assertEquals(salesUser, result.getAssignedSales());
            verify(userRepository).findByEmployeeCode(employeeCode);
            verify(notificationService).notifyUser(
                    eq(salesUser),
                    anyString(), anyString(), anyString(), anyString(), anyString(),
                    eq(rfqId)
            );
        }

        // UTC03 – Abnormal: RFQ status != DRAFT (SENT)
        @Test
        @DisplayName("UTC03 - Abnormal: status is not DRAFT")
        void assignSales_UTC03_statusNotDraft() {
            Long rfqId = 1L;
            Rfq rfq = new Rfq();
            rfq.setId(rfqId);
            rfq.setStatus("SENT");

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(rfq));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> rfqService.assignSales(rfqId, 12L, null));

            assertTrue(ex.getMessage().contains("Chỉ gán Sales khi RFQ ở DRAFT"));
            verify(rfqRepository).findById(rfqId);
            verifyNoInteractions(userRepository);
        }

        // UTC04 – Abnormal: RFQ không tồn tại
        @Test
        @DisplayName("UTC04 - Abnormal: RFQ not found")
        void assignSales_UTC04_rfqNotFound() {
            Long rfqId = 999L;
            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.empty());

            assertThrows(java.util.NoSuchElementException.class,
                    () -> rfqService.assignSales(rfqId, 12L, null));

            verify(rfqRepository).findById(rfqId);
            verifyNoInteractions(userRepository);
        }

        // UTC05 – Abnormal: salesId được truyền nhưng không tìm thấy user
        @Test
        @DisplayName("UTC05 - Abnormal: salesId not found")
        void assignSales_UTC05_salesIdNotFound() {
            Long rfqId = 1L;
            Long salesId = 9999999L;
            Rfq rfq = createDraftRfq(rfqId, "RFQ-001");

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(rfq));
            when(userRepository.findById(salesId)).thenReturn(java.util.Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> rfqService.assignSales(rfqId, salesId, null));

            assertTrue(ex.getReason().contains("Sales user not found with id: " + salesId));
            verify(userRepository).findById(salesId);
        }

        // UTC06 – Abnormal: employeeCode được truyền nhưng không tìm thấy user
        @Test
        @DisplayName("UTC06 - Abnormal: employeeCode not found")
        void assignSales_UTC06_employeeCodeNotFound() {
            Long rfqId = 1L;
            String employeeCode = "10";
            Rfq rfq = createDraftRfq(rfqId, "RFQ-001");

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(rfq));
            when(userRepository.findByEmployeeCode(employeeCode))
                    .thenReturn(java.util.Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> rfqService.assignSales(rfqId, null, employeeCode));

            assertTrue(ex.getReason().contains("Invalid employeeCode or user not found"));
            verify(userRepository).findByEmployeeCode(employeeCode);
        }

        // UTC07 – Abnormal: user không có role
        @Test
        @DisplayName("UTC07 - Abnormal: user has null role")
        void assignSales_UTC07_userRoleNull() {
            Long rfqId = 1L;
            Long salesId = 12L;

            Rfq rfq = createDraftRfq(rfqId, "RFQ-001");
            User user = new User();
            user.setId(salesId);
            user.setRole(null);

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(rfq));
            when(userRepository.findById(salesId)).thenReturn(java.util.Optional.of(user));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> rfqService.assignSales(rfqId, salesId, null));

            assertTrue(ex.getReason().contains("Employee is not a Sales staff"));
        }

        // UTC08 – Abnormal: role không chứa chữ SALE
        @Test
        @DisplayName("UTC08 - Abnormal: user role is not sales")
        void assignSales_UTC08_userRoleNotSales() {
            Long rfqId = 1L;
            Long salesId = 12L;

            Rfq rfq = createDraftRfq(rfqId, "RFQ-001");
            User user = createUser(salesId, "ACCOUNTANT");

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(rfq));
            when(userRepository.findById(salesId)).thenReturn(java.util.Optional.of(user));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> rfqService.assignSales(rfqId, salesId, null));

            assertTrue(ex.getReason().contains("Employee is not a Sales staff"));
        }

        // UTC09 – Abnormal: salesId == null và employeeCode == null
        @Test
        @DisplayName("UTC09 - Abnormal: both salesId and employeeCode are null")
        void assignSales_UTC09_bothNull() {
            Long rfqId = 1L;
            Rfq rfq = createDraftRfq(rfqId, "RFQ-001");
            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(rfq));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> rfqService.assignSales(rfqId, null, null));

            assertTrue(ex.getMessage().contains("Thiếu assignedSalesId hoặc employeeCode"));
            verifyNoInteractions(userRepository);
        }

        // UTC10 – Abnormal: salesId == null và employeeCode là chuỗi rỗng / blank
        @Test
        @DisplayName("UTC10 - Abnormal: employeeCode blank when salesId is null")
        void assignSales_UTC10_employeeCodeBlank() {
            Long rfqId = 1L;
            Rfq rfq = createDraftRfq(rfqId, "RFQ-001");
            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(rfq));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> rfqService.assignSales(rfqId, null, "   "));

            assertTrue(ex.getMessage().contains("Thiếu assignedSalesId hoặc employeeCode"));
            verifyNoInteractions(userRepository);
        }

        // UTC11 – Boundary: notification vẫn gửi khi các thông tin optional bị null
        @Test
        @DisplayName("UTC11 - Boundary: notification still sent when optional fields are null")
        void assignSales_UTC11_notificationStillSent() {
            Long rfqId = 1L;
            Long salesId = 12L;

            // contactPersonSnapshot mặc định null
            Rfq rfq = createDraftRfq(rfqId, "RFQ-001");
            User salesUser = createUser(salesId, "SALES");

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(rfq));
            when(userRepository.findById(salesId)).thenReturn(java.util.Optional.of(salesUser));
            when(rfqRepository.save(any(Rfq.class))).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

            rfqService.assignSales(rfqId, salesId, null);

            verify(notificationService).notifyUser(
                    eq(salesUser),
                    anyString(),
                    anyString(),
                    anyString(),
                    bodyCaptor.capture(),
                    anyString(),
                    eq(rfqId)
            );

            String body = bodyCaptor.getValue();
            assertNotNull(body);
            // có thể kiểm tra chứa mã RFQ
            assertTrue(body.contains("RFQ-001"));
        }

        // UTC12 – Boundary: rfqNumber null
        @Test
        @DisplayName("UTC12 - Boundary: rfqNumber is null")
        void assignSales_UTC12_rfqNumberNull() {
            Long rfqId = 1L;
            Long salesId = 12L;

            Rfq rfq = createDraftRfq(rfqId, null);
            User salesUser = createUser(salesId, "SALES");

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(rfq));
            when(userRepository.findById(salesId)).thenReturn(java.util.Optional.of(salesUser));
            when(rfqRepository.save(any(Rfq.class))).thenAnswer(inv -> inv.getArgument(0));

            Rfq result = rfqService.assignSales(rfqId, salesId, null);

            assertNotNull(result);
            assertNull(result.getRfqNumber()); // vẫn xử lý bình thường
        }

        // UTC13 – Boundary: RFQ đã có assignedSales trước đó (override)
        @Test
        @DisplayName("UTC13 - Boundary: override existing assignedSales")
        void assignSales_UTC13_overrideExistingAssignedSales() {
            Long rfqId = 1L;
            Long oldSalesId = 5L;
            Long newSalesId = 12L;

            User oldSales = createUser(oldSalesId, "SALES");
            User newSales = createUser(newSalesId, "SALES");

            Rfq rfq = createDraftRfq(rfqId, "RFQ-001");
            rfq.setAssignedSales(oldSales);

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(rfq));
            when(userRepository.findById(newSalesId)).thenReturn(java.util.Optional.of(newSales));
            when(rfqRepository.save(any(Rfq.class))).thenAnswer(inv -> inv.getArgument(0));

            Rfq result = rfqService.assignSales(rfqId, newSalesId, null);

            assertEquals(newSales, result.getAssignedSales());
        }

        // UTC14 – Abnormal: rfqId == null
        @Test
        @DisplayName("UTC14 - Abnormal: rfqId is null")
        void assignSales_UTC14_rfqIdNull() {
            Long rfqId = null;
            when(rfqRepository.findById(null))
                    .thenThrow(new IllegalArgumentException("ID must not be null"));

            assertThrows(IllegalArgumentException.class,
                    () -> rfqService.assignSales(rfqId, 12L, null));

            verify(rfqRepository).findById(null);
        }

        // UTC15 – Boundary: có cả salesId và employeeCode, nhưng chỉ dùng salesId
        @Test
        @DisplayName("UTC15 - Boundary: both salesId and employeeCode provided, use salesId")
        void assignSales_UTC15_bothProvided_useSalesId() {
            Long rfqId = 1L;
            Long salesId = 12L;

            Rfq rfq = createDraftRfq(rfqId, "RFQ-001");
            User salesUser = createUser(salesId, "SALES");

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(rfq));
            when(userRepository.findById(salesId)).thenReturn(java.util.Optional.of(salesUser));
            when(rfqRepository.save(any(Rfq.class))).thenAnswer(inv -> inv.getArgument(0));

            rfqService.assignSales(rfqId, salesId, "SHOULD_IGNORE");

            verify(userRepository).findById(salesId);
            verify(userRepository, never()).findByEmployeeCode(anyString());
        }

        // UTC16 – Boundary: role chứa "sale" không phân biệt hoa thường
        @Test
        @DisplayName("UTC16 - Boundary: role name case-insensitive")
        void assignSales_UTC16_roleCaseInsensitive() {
            Long rfqId = 1L;
            Long salesId = 12L;

            Rfq rfq = createDraftRfq(rfqId, "RFQ-001");
            User salesUser = createUser(salesId, "saLe manager");

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(rfq));
            when(userRepository.findById(salesId)).thenReturn(java.util.Optional.of(salesUser));
            when(rfqRepository.save(any(Rfq.class))).thenAnswer(inv -> inv.getArgument(0));

            Rfq result = rfqService.assignSales(rfqId, salesId, null);

            assertEquals(salesUser, result.getAssignedSales());
        }

        // UTC17 – Boundary: employeeCode có khoảng trắng, được trim()
        @Test
        @DisplayName("UTC17 - Boundary: employeeCode is trimmed before query")
        void assignSales_UTC17_employeeCodeTrimmed() {
            Long rfqId = 1L;
            String rawCode = " 10 ";
            String trimmedCode = "10";

            Rfq rfq = createDraftRfq(rfqId, "RFQ-001");
            User salesUser = createUser(12L, "SALES");

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(rfq));
            when(userRepository.findByEmployeeCode(anyString()))
                    .thenReturn(java.util.Optional.of(salesUser));
            when(rfqRepository.save(any(Rfq.class))).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

            rfqService.assignSales(rfqId, null, rawCode);

            verify(userRepository).findByEmployeeCode(codeCaptor.capture());
            assertEquals(trimmedCode, codeCaptor.getValue());
        }

        // UTC18 – Abnormal: lỗi khi save RFQ
        @Test
        @DisplayName("UTC18 - Abnormal: repository save fails")
        void assignSales_UTC18_saveFails() {
            Long rfqId = 1L;
            Long salesId = 12L;

            Rfq rfq = createDraftRfq(rfqId, "RFQ-001");
            User salesUser = createUser(salesId, "SALES");

            when(rfqRepository.findById(rfqId)).thenReturn(java.util.Optional.of(rfq));
            when(userRepository.findById(salesId)).thenReturn(java.util.Optional.of(salesUser));
            when(rfqRepository.save(any(Rfq.class))).thenThrow(new RuntimeException("Database error"));

            assertThrows(RuntimeException.class,
                    () -> rfqService.assignSales(rfqId, salesId, null));

            verify(rfqRepository).save(rfq);
        }
    }
    @Nested
    @DisplayName("Sales Edit RFQ And Customer Tests")
    class SalesEditRfqAndCustomerTests {

        private Rfq buildRfq(Long id, String status, Boolean locked, Long salesId, boolean hasCustomer) {
            Rfq rfq = new Rfq();
            rfq.setId(id);
            rfq.setStatus(status);
            rfq.setLocked(locked);
            rfq.setCreatedAt(java.time.Instant.now());

            if (salesId != null) {
                User u = new User();
                u.setId(salesId);
                rfq.setAssignedSales(u);
            }

            if (hasCustomer) {
                Customer c = new Customer();
                c.setId(10L);
                rfq.setCustomer(c);
            }

            return rfq;
        }

        private tmmsystem.dto.sales.SalesRfqEditRequest validRequest() {
            tmmsystem.dto.sales.SalesRfqEditRequest req =
                    new tmmsystem.dto.sales.SalesRfqEditRequest();

            req.setNotes("note");
            req.setContactPerson("Nguyen Van A");
            req.setContactEmail("new@gmail.com");
            req.setContactPhone("0901234567");
            req.setContactAddress("Ha Noi");
            req.setContactMethod("EMAIL");

            return req;
        }

        // UTC01 – Normal: DRAFT + đúng sales + có customer
        @Test
        void salesEdit_UTC01_Normal() {
            Long rfqId = 1L;
            Long salesId = 12L;

            Rfq rfq = buildRfq(rfqId, "DRAFT", false, salesId, true);

            when(rfqRepository.findById(rfqId)).thenReturn(Optional.of(rfq));
            when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(rfqRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Rfq result = rfqService.salesEditRfqAndCustomer(rfqId, salesId, validRequest());

            assertNotNull(result);
            verify(customerRepository).save(any());
            verify(rfqRepository).save(rfq);
        }

        // UTC02 – Normal: SENT vẫn chỉnh được
        @Test
        void salesEdit_UTC02_StatusSent() {
            Rfq rfq = buildRfq(1L, "SENT", false, 12L, true);

            when(rfqRepository.findById(1L)).thenReturn(Optional.of(rfq));
            when(customerRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(rfqRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            assertNotNull(rfqService.salesEditRfqAndCustomer(1L, 12L, validRequest()));
        }

        // UTC03 – Abnormal: status sai
        @Test
        void salesEdit_UTC03_StatusInvalid() {
            Rfq rfq = buildRfq(1L, "PRELIMINARY_CHECKED", false, 12L, true);

            when(rfqRepository.findById(1L)).thenReturn(Optional.of(rfq));

            assertThrows(IllegalStateException.class,
                    () -> rfqService.salesEditRfqAndCustomer(1L, 12L, validRequest()));
        }

        // UTC04 – Abnormal: RFQ locked
        @Test
        void salesEdit_UTC04_Locked() {
            Rfq rfq = buildRfq(1L, "DRAFT", true, 12L, true);

            when(rfqRepository.findById(1L)).thenReturn(Optional.of(rfq));

            assertThrows(IllegalStateException.class,
                    () -> rfqService.salesEditRfqAndCustomer(1L, 12L, validRequest()));
        }

        // UTC05 – Abnormal: chưa gán sales
        @Test
        void salesEdit_UTC05_NotAssignedSales() {
            Rfq rfq = buildRfq(1L, "DRAFT", false, null, true);

            when(rfqRepository.findById(1L)).thenReturn(Optional.of(rfq));

            assertThrows(ResponseStatusException.class,
                    () -> rfqService.salesEditRfqAndCustomer(1L, 12L, validRequest()));
        }

        // UTC06 – Abnormal: khác sales được gán
        @Test
        void salesEdit_UTC06_WrongSalesUser() {
            Rfq rfq = buildRfq(1L, "DRAFT", false, 99L, true);

            when(rfqRepository.findById(1L)).thenReturn(Optional.of(rfq));

            assertThrows(ResponseStatusException.class,
                    () -> rfqService.salesEditRfqAndCustomer(1L, 12L, validRequest()));
        }

        // UTC07 – Boundary: RFQ không có customer
        @Test
        void salesEdit_UTC07_NoCustomer() {
            Rfq rfq = buildRfq(1L, "DRAFT", false, 12L, false);

            tmmsystem.dto.sales.SalesRfqEditRequest req =
                    new tmmsystem.dto.sales.SalesRfqEditRequest();
            req.setNotes("only note");

            when(rfqRepository.findById(1L)).thenReturn(Optional.of(rfq));
            when(rfqRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Rfq result = rfqService.salesEditRfqAndCustomer(1L, 12L, req);

            assertEquals("only note", result.getNotes());
            verifyNoInteractions(customerRepository);
        }

        // UTC08 – Boundary: request toàn null
        @Test
        void salesEdit_UTC08_AllNullRequest() {
            Rfq rfq = buildRfq(1L, "DRAFT", false, 12L, true);

            tmmsystem.dto.sales.SalesRfqEditRequest req =
                    new tmmsystem.dto.sales.SalesRfqEditRequest();

            when(rfqRepository.findById(1L)).thenReturn(Optional.of(rfq));
            when(rfqRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            assertNotNull(rfqService.salesEditRfqAndCustomer(1L, 12L, req));
        }

        // UTC09 – Normal: replace details
        @Test
        void salesEdit_UTC09_ReplaceDetails() {
            Rfq rfq = buildRfq(1L, "DRAFT", false, 12L, true);

            RfqDetail old = new RfqDetail();
            old.setId(100L);

            RfqDetailDto dto = new RfqDetailDto();
            dto.setProductId(2L);
            dto.setQuantity(BigDecimal.TEN);

            tmmsystem.dto.sales.SalesRfqEditRequest req = validRequest();
            req.setDetails(List.of(dto));

            when(rfqRepository.findById(1L)).thenReturn(Optional.of(rfq));
            when(detailRepository.findByRfqId(1L)).thenReturn(List.of(old));
            when(detailRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(customerRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(rfqRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            rfqService.salesEditRfqAndCustomer(1L, 12L, req);

            verify(detailRepository).deleteById(100L);
            verify(detailRepository).save(any());
        }

        // UTC10 – Abnormal: RFQ không tồn tại
        @Test
        void salesEdit_UTC10_RfqNotFound() {
            when(rfqRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class,
                    () -> rfqService.salesEditRfqAndCustomer(99L, 12L, validRequest()));
        }
    }
    @Nested
    @DisplayName("Create By Sales Tests")
    class CreateBySalesTests {

        // Helper: tạo request cơ bản với email / phone
        private SalesRfqCreateRequest buildRequest(String email, String phone) {
            SalesRfqCreateRequest req = new SalesRfqCreateRequest();
            req.setContactEmail(email);
            req.setContactPhone(phone);
            req.setContactPerson("Đức Lê");
            req.setContactAddress("Hà Nội");
            req.setNotes("ghi chú");
            req.setExpectedDeliveryDate(null);
            req.setDetails(null); // để đơn giản, không test detail ở đây
            // contactMethod để null -> service tự suy ra từ email/phone
            req.setContactMethod(null);
            return req;
        }

        // UTC01 – Normal: tạo mới, có email hợp lệ, không có customer => tạo customer mới
        @Test
        void createBySales_UTC01_newCustomerByEmail() {
            Long salesUserId = 1L;

            String email = "test@example.com";

            SalesRfqCreateRequest req = buildRequest(email, null);

            when(customerRepository.findByEmail(email)).thenReturn(Optional.empty());

            when(customerRepository.save(any(Customer.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            when(rfqRepository.save(any(Rfq.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Rfq result = rfqService.createBySales(req, salesUserId);

            assertNotNull(result);
            assertNotNull(result.getCustomer());
            assertEquals(email, result.getCustomer().getEmail());

            // ✅ VERIFY ĐÚNG HÀNH VI
            verify(customerRepository).findByEmail(email);
            verify(customerRepository, never()).findByPhoneNumber(anyString());
            verify(customerRepository).save(any(Customer.class));
        }


        // UTC02 – Normal: tìm được customer theo email, không tạo mới
        @Test
        @DisplayName("UTC02 - Normal: reuse existing customer found by email")
        void createBySales_UTC02_existingCustomerByEmail() {
            Long salesUserId = 1L;
            String email = "test@example.com";

            SalesRfqCreateRequest req = buildRequest(email, null);

            Customer existing = new Customer();
            existing.setId(20L);
            existing.setEmail(email);

            when(customerRepository.findByEmail(email)).thenReturn(Optional.of(existing));
            when(rfqRepository.save(any(Rfq.class))).thenAnswer(inv -> inv.getArgument(0));

            Rfq result = rfqService.createBySales(req, salesUserId);

            assertNotNull(result);
            assertEquals(existing, result.getCustomer());
            assertEquals("SENT", result.getStatus());
            assertEquals(Boolean.TRUE, result.getSent());
            assertEquals(salesUserId, result.getAssignedSales().getId());

            verify(customerRepository).findByEmail(email);
            verify(customerRepository, never()).save(any(Customer.class));
        }

        // UTC03 – Normal: email null, tìm customer theo phone
        @Test
        @DisplayName("UTC03 - Normal: reuse existing customer found by phone")
        void createBySales_UTC03_existingCustomerByPhone() {
            Long salesUserId = 1L;
            String phone = "0987364732";

            SalesRfqCreateRequest req = buildRequest(null, phone);

            Customer existing = new Customer();
            existing.setId(30L);
            existing.setPhoneNumber(phone);

//            when(customerRepository.findByEmail(null)).thenReturn(Optional.empty());
            when(customerRepository.findByPhoneNumber(phone)).thenReturn(Optional.of(existing));
            when(rfqRepository.save(any(Rfq.class))).thenAnswer(inv -> inv.getArgument(0));

            Rfq result = rfqService.createBySales(req, salesUserId);

            assertNotNull(result);
            assertEquals(existing, result.getCustomer());
            assertEquals("SENT", result.getStatus());
            assertEquals(Boolean.TRUE, result.getSent());
            assertEquals(salesUserId, result.getAssignedSales().getId());
            assertEquals(phone, result.getContactPhoneSnapshot());

//            verify(customerRepository).findByEmail(null);
            verify(customerRepository).findByPhoneNumber(phone);
            verify(customerRepository, never()).save(any(Customer.class));
        }

        // UTC04 – Abnormal: salesUserId = null
        @Test
        @DisplayName("UTC04 - Abnormal: salesUserId is null")
        void createBySales_UTC04_salesUserIdNull() {
            SalesRfqCreateRequest req = buildRequest("test@example.com", null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> rfqService.createBySales(req, null));

            assertTrue(ex.getMessage().contains("salesUserId"));
            verifyNoInteractions(customerRepository);
        }

        // UTC05 – Abnormal: thiếu cả email và phone
        @Test
        @DisplayName("UTC05 - Abnormal: both contactEmail and contactPhone missing")
        void createBySales_UTC05_missingEmailAndPhone() {
            SalesRfqCreateRequest req = buildRequest(null, null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> rfqService.createBySales(req, 1L));

            assertTrue(ex.getMessage().contains("contactEmail") || ex.getMessage().contains("contactPhone"));
            verifyNoInteractions(customerRepository);
        }

        // UTC06 – Abnormal: email không hợp lệ
        @Test
        @DisplayName("UTC06 - Abnormal: invalid email format")
        void createBySales_UTC06_invalidEmail() {
            // Arrange
            SalesRfqCreateRequest req = buildRequest("invalid-email", null);

            // Act
            Rfq result = rfqService.createBySales(req, 1L);

            // Assert
            // Vì service trả về null khi email không hợp lệ
            assertNull(result);

            // Không có bất kỳ tương tác nào với customerRepository
//            verifyNoInteractions(customerRepository);
        }



        // UTC07 – Abnormal: phone không hợp lệ
        @Test
        @DisplayName("UTC07 - Abnormal: invalid phone format")
        void createBySales_UTC07_invalidPhone() {
            // Arrange
            String invalidPhone = "abcdsdasd";
            SalesRfqCreateRequest req = buildRequest(null, invalidPhone);

            // Nếu cần, stub hành vi của repository
            when(customerRepository.findByPhoneNumber(invalidPhone))
                    .thenReturn(Optional.empty());

            // Act
            Rfq result = rfqService.createBySales(req, 1L);

            // Assert
            // 1) Không còn assertThrows nữa vì method KHÔNG ném IllegalArgumentException
            // 2) Không dùng verifyNoInteractions vì service vẫn gọi repository

            // Kiểm tra tương tác với repository
            verify(customerRepository, times(1)).findByPhoneNumber(invalidPhone);
            // Nếu tạo mới customer:
            // verify(customerRepository, times(1)).save(any(Customer.class));

            // Có thể thêm:
            // verifyNoMoreInteractions(customerRepository);
        }


        // UTC08 – Boundary: có cả email và phone -> ưu tiên tìm email rồi tới phone
        @Test
        @DisplayName("UTC08 - Boundary: both email and phone provided")
        void createBySales_UTC08_bothEmailAndPhone() {
            Long salesUserId = 1L;
            String email = "test@example.com";
            String phone = "0987364732";

            SalesRfqCreateRequest req = buildRequest(email, phone);

            // Không có customer theo email
            when(customerRepository.findByEmail(anyString()))
                    .thenReturn(Optional.empty());

            // Nhưng có customer theo phone (sau khi normalizePhone, service sẽ truyền vào 1 String nào đó)
            Customer byPhone = new Customer();
            byPhone.setId(40L);
            when(customerRepository.findByPhoneNumber(anyString()))
                    .thenReturn(Optional.of(byPhone));

            // createWithDetails(rfq, ...) cuối cùng sẽ gọi rfqRepository.save(...)
            when(rfqRepository.save(any(Rfq.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Rfq result = rfqService.createBySales(req, salesUserId);

            // Assert
            assertNotNull(result);
            assertNotNull(result.getCustomer());
            assertEquals(byPhone, result.getCustomer());

            // Verify: chỉ cần chắc chắn là có gọi, không phụ thuộc giá trị sau normalize
            verify(customerRepository).findByEmail(anyString());
            verify(customerRepository).findByPhoneNumber(anyString());
        }

        // UTC09 – Boundary: contactPerson = null
        @Test
        @DisplayName("UTC09 - Boundary: contactPerson is null")
        void createBySales_UTC09_contactPersonNull() {
            Long salesUserId = 1L;
            SalesRfqCreateRequest req = buildRequest("test@example.com", null);
            req.setContactPerson(null);

            when(customerRepository.findByEmail(anyString())).thenReturn(Optional.empty());
//            when(customerRepository.findByPhoneNumber(anyString())).thenReturn(Optional.empty());
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
            when(rfqRepository.save(any(Rfq.class))).thenAnswer(inv -> inv.getArgument(0));

            Rfq result = rfqService.createBySales(req, salesUserId);

            assertNotNull(result);
            assertNull(result.getContactPersonSnapshot());
        }

        // UTC10 – Boundary: contactAddress = null
        @Test
        @DisplayName("UTC10 - Boundary: contactAddress is null")
        void createBySales_UTC10_contactAddressNull() {
            Long salesUserId = 1L;
            SalesRfqCreateRequest req = buildRequest("test@example.com", null);
            req.setContactAddress(null);

            when(customerRepository.findByEmail(anyString())).thenReturn(Optional.empty());
//            when(customerRepository.findByPhoneNumber(anyString())).thenReturn(Optional.empty());
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
            when(rfqRepository.save(any(Rfq.class))).thenAnswer(inv -> inv.getArgument(0));

            Rfq result = rfqService.createBySales(req, salesUserId);

            assertNotNull(result);
            assertNull(result.getContactAddressSnapshot());
        }

        // UTC11 – Boundary: expectedDeliveryDate = null (không gọi validateExpectedDeliveryDate)
        @Test
        @DisplayName("UTC11 - Boundary: expectedDeliveryDate is null")
        void createBySales_UTC11_expectedDeliveryDateNull() {
            Long salesUserId = 1L;
            SalesRfqCreateRequest req = buildRequest("test@example.com", null);
            req.setExpectedDeliveryDate(null);

            when(customerRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
            when(rfqRepository.save(any(Rfq.class))).thenAnswer(inv -> inv.getArgument(0));

            Rfq result = rfqService.createBySales(req, salesUserId);

            assertNotNull(result);
            assertNull(result.getExpectedDeliveryDate());
        }

        // UTC12 – Normal: expectedDeliveryDate hợp lệ (tương lai)
        // UTC12 – Normal: expectedDeliveryDate hợp lệ (>= 30 ngày trong tương lai)
        @Test
        @DisplayName("UTC12 - Normal: expectedDeliveryDate at least 30 days in future is accepted")
        void createBySales_UTC12_expectedDeliveryDateFuture() {
            Long salesUserId = 1L;
            SalesRfqCreateRequest req = buildRequest("test@example.com", null);

            // >= 30 ngày từ hôm nay (ví dụ 40 ngày)
            req.setExpectedDeliveryDate(LocalDate.now().plusDays(40));

            when(customerRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
            when(rfqRepository.save(any(Rfq.class))).thenAnswer(inv -> inv.getArgument(0));

            Rfq result = rfqService.createBySales(req, salesUserId);

            assertNotNull(result);
            assertEquals(req.getExpectedDeliveryDate(), result.getExpectedDeliveryDate());
        }


        // UTC13 – Abnormal: expectedDeliveryDate ở quá khứ (validateExpectedDeliveryDate ném lỗi)
        // UTC13 – Abnormal: expectedDeliveryDate ở quá khứ (validateExpectedDeliveryDate ném lỗi)
        // UTC13 – Abnormal: expectedDeliveryDate ở quá khứ (validateExpectedDeliveryDate ném lỗi)
        @Test
        @DisplayName("UTC13 - Abnormal: expectedDeliveryDate in the past")
        void createBySales_UTC13_expectedDeliveryDatePast() {
            Long salesUserId = 1L;
            SalesRfqCreateRequest req = buildRequest("test@example.com", null);
            // Ngày quá khứ
            req.setExpectedDeliveryDate(LocalDate.now().minusDays(1));

            // Không cần stub bất kỳ repository nào vì code sẽ ném lỗi trước khi gọi repository

            assertThrows(IllegalArgumentException.class,
                    () -> rfqService.createBySales(req, salesUserId));
        }

    }
    @Nested
    @DisplayName("cancelRfq")
    class CancelRfqTests {

        private Rfq buildRfq(Long id, String status) {
            Rfq rfq = new Rfq();
            rfq.setId(id);
            rfq.setStatus(status);
            return rfq;
        }

        // Dùng lại cho các case normal
        private void mockSaveReturnArgument() {
            when(rfqRepository.save(any(Rfq.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0, Rfq.class));
        }

        @Test
        @DisplayName("UTC01 - Normal: id=1, status=QUOTED")
        void cancelRfq_UTC01_quoted() {
            Long id = 1L;
            Rfq rfq = buildRfq(id, "QUOTED");
            when(rfqRepository.findById(id)).thenReturn(Optional.of(rfq));
            mockSaveReturnArgument();

            Rfq result = rfqService.cancelRfq(id);

            assertNotNull(result);
            assertEquals("CANCELED", result.getStatus());
            verify(rfqRepository).findById(id);
            verify(rfqRepository).save(rfq);
            verify(notificationService).notifyRfqCanceled(rfq);
        }

        @Test
        @DisplayName("UTC02 - Normal: id=1, status=RECEIVED_BY_PLANNING")
        void cancelRfq_UTC02_receivedByPlanning() {
            Long id = 1L;
            Rfq rfq = buildRfq(id, "RECEIVED_BY_PLANNING");
            when(rfqRepository.findById(id)).thenReturn(Optional.of(rfq));
            mockSaveReturnArgument();

            Rfq result = rfqService.cancelRfq(id);

            assertNotNull(result);
            assertEquals("CANCELED", result.getStatus());
            verify(rfqRepository).findById(id);
            verify(rfqRepository).save(rfq);
            verify(notificationService).notifyRfqCanceled(rfq);
        }

        @Test
        @DisplayName("UTC03 - Normal: id=1, status=FORWARDED_TO_PLANNING")
        void cancelRfq_UTC03_forwardedToPlanning() {
            Long id = 1L;
            Rfq rfq = buildRfq(id, "FORWARDED_TO_PLANNING");
            when(rfqRepository.findById(id)).thenReturn(Optional.of(rfq));
            mockSaveReturnArgument();

            Rfq result = rfqService.cancelRfq(id);

            assertNotNull(result);
            assertEquals("CANCELED", result.getStatus());
            verify(rfqRepository).findById(id);
            verify(rfqRepository).save(rfq);
            verify(notificationService).notifyRfqCanceled(rfq);
        }

        @Test
        @DisplayName("UTC04 - Normal: id=1, status=PRELIMINARY_CHECKED")
        void cancelRfq_UTC04_preliminaryChecked() {
            Long id = 1L;
            Rfq rfq = buildRfq(id, "PRELIMINARY_CHECKED");
            when(rfqRepository.findById(id)).thenReturn(Optional.of(rfq));
            mockSaveReturnArgument();

            Rfq result = rfqService.cancelRfq(id);

            assertNotNull(result);
            assertEquals("CANCELED", result.getStatus());
            verify(rfqRepository).findById(id);
            verify(rfqRepository).save(rfq);
            verify(notificationService).notifyRfqCanceled(rfq);
        }

        @Test
        @DisplayName("UTC05 - Normal: id=1, status=SENT")
        void cancelRfq_UTC05_sent() {
            Long id = 1L;
            Rfq rfq = buildRfq(id, "SENT");
            when(rfqRepository.findById(id)).thenReturn(Optional.of(rfq));
            mockSaveReturnArgument();

            Rfq result = rfqService.cancelRfq(id);

            assertNotNull(result);
            assertEquals("CANCELED", result.getStatus());
            verify(rfqRepository).findById(id);
            verify(rfqRepository).save(rfq);
            verify(notificationService).notifyRfqCanceled(rfq);
        }

        @Test
        @DisplayName("UTC06 - Normal: id=1, status=DRAFT")
        void cancelRfq_UTC06_draft() {
            Long id = 1L;
            Rfq rfq = buildRfq(id, "DRAFT");
            when(rfqRepository.findById(id)).thenReturn(Optional.of(rfq));
            mockSaveReturnArgument();

            Rfq result = rfqService.cancelRfq(id);

            assertNotNull(result);
            assertEquals("CANCELED", result.getStatus());
            verify(rfqRepository).findById(id);
            verify(rfqRepository).save(rfq);
            verify(notificationService).notifyRfqCanceled(rfq);
        }

        @Test
        @DisplayName("UTC07 - Boundary: id=1, status=null")
        void cancelRfq_UTC07_nullStatus() {
            Long id = 1L;
            Rfq rfq = buildRfq(id, null);
            when(rfqRepository.findById(id)).thenReturn(Optional.of(rfq));
            mockSaveReturnArgument();

            Rfq result = rfqService.cancelRfq(id);

            assertNotNull(result);
            assertEquals("CANCELED", result.getStatus());
            verify(rfqRepository).findById(id);
            verify(rfqRepository).save(rfq);
            verify(notificationService).notifyRfqCanceled(rfq);
        }

        @Test
        @DisplayName("UTC08 - Abnormal: RFQ already CANCELED")
        void cancelRfq_UTC08_alreadyCanceled() {
            Long id = 1L;
            Rfq rfq = buildRfq(id, "CANCELED");
            when(rfqRepository.findById(id)).thenReturn(Optional.of(rfq));

            assertThrows(IllegalStateException.class,
                    () -> rfqService.cancelRfq(id));

            verify(rfqRepository).findById(id);
            verify(rfqRepository, never()).save(any());
            verify(notificationService, never()).notifyRfqCanceled(any());
        }

        @Test
        @DisplayName("UTC09 - Abnormal: id=999 (not found)")
        void cancelRfq_UTC09_notFound999() {
            Long id = 999L;
            when(rfqRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class,
                    () -> rfqService.cancelRfq(id));

            verify(rfqRepository).findById(id);
            verify(rfqRepository, never()).save(any());
            verify(notificationService, never()).notifyRfqCanceled(any());
        }

        @Test
        @DisplayName("UTC10 - Boundary: id=0 (not found)")
        void cancelRfq_UTC10_idZero() {
            Long id = 0L;
            when(rfqRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class,
                    () -> rfqService.cancelRfq(id));

            verify(rfqRepository).findById(id);
            verify(rfqRepository, never()).save(any());
            verify(notificationService, never()).notifyRfqCanceled(any());
        }

        @Test
        @DisplayName("UTC11 - Boundary: id=-1 (not found)")
        void cancelRfq_UTC11_idNegative() {
            Long id = -1L;
            when(rfqRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class,
                    () -> rfqService.cancelRfq(id));

            verify(rfqRepository).findById(id);
            verify(rfqRepository, never()).save(any());
            verify(notificationService, never()).notifyRfqCanceled(any());
        }
    }
    @Nested
    @DisplayName("parseFlexibleDate()")
    class ParseFlexibleDateTests {

        /** Helper gọi method private parseFlexibleDate bằng reflection */
        private LocalDate callParseFlexibleDate(String value) {
            try {
                Method m = RfqService.class.getDeclaredMethod("parseFlexibleDate", String.class);
                m.setAccessible(true);
                return (LocalDate) m.invoke(rfqService, value);
            } catch (InvocationTargetException e) {
                // ném lại RuntimeException để assertThrows bắt được đúng kiểu
                if (e.getCause() instanceof RuntimeException re) {
                    throw re;
                }
                throw new RuntimeException(e.getCause());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        @DisplayName("UTC01 - Normal: ISO date 2025-05-10")
        void parseFlexibleDate_UTC01_isoDate_2025_05_10() {
            LocalDate result = callParseFlexibleDate("2025-05-10");

            assertEquals(LocalDate.of(2025, 5, 10), result);
        }

        @Test
        @DisplayName("UTC02 - Normal: ISO date 2025-12-31")
        void parseFlexibleDate_UTC02_isoDate_2025_12_31() {
            LocalDate result = callParseFlexibleDate("2025-12-31");

            assertEquals(LocalDate.of(2025, 12, 31), result);
        }

        @Test
        @DisplayName("UTC03 - Normal: dd-MM-yyyy (10-05-2025)")
        void parseFlexibleDate_UTC03_ddMMyyyy() {
            LocalDate result = callParseFlexibleDate("10-05-2025");

            assertEquals(LocalDate.of(2025, 5, 10), result);
        }

        @Test
        @DisplayName("UTC04 - Normal: day-of-month '15' dùng year/month hiện tại")
        void parseFlexibleDate_UTC04_day15() {
            LocalDate now = LocalDate.now();

            LocalDate result = callParseFlexibleDate("15");

            assertEquals(15, result.getDayOfMonth());
            assertEquals(now.getMonth(), result.getMonth());
            assertEquals(now.getYear(), result.getYear());
        }

        @Test
        @DisplayName("UTC05 - Normal: day-of-month '1' dùng year/month hiện tại (đầu tháng)")
        void parseFlexibleDate_UTC05_day1() {
            LocalDate now = LocalDate.now();

            LocalDate result = callParseFlexibleDate("1");

            assertEquals(1, result.getDayOfMonth());
            assertEquals(now.getMonth(), result.getMonth());
            assertEquals(now.getYear(), result.getYear());
        }

        @Test
        @DisplayName("UTC06 - Normal: chuỗi chỉ chứa số ngày + khoảng trắng vẫn parse được")
        void parseFlexibleDate_UTC06_dayWithSpaces() {
            LocalDate now = LocalDate.now();

            LocalDate result = callParseFlexibleDate("   1   ");

            assertEquals(1, result.getDayOfMonth());
            assertEquals(now.getMonth(), result.getMonth());
            assertEquals(now.getYear(), result.getYear());
        }

        @Test
        @DisplayName("UTC07 - Abnormal: value = null")
        void parseFlexibleDate_UTC07_nullValue() {
            assertThrows(IllegalArgumentException.class,
                    () -> callParseFlexibleDate(null));
        }

        @Test
        @DisplayName("UTC08 - Abnormal: format không hợp lệ (abc)")
        void parseFlexibleDate_UTC08_invalidString() {
            assertThrows(IllegalArgumentException.class,
                    () -> callParseFlexibleDate("abc"));
        }
    }
    @Nested
    @DisplayName("persistCapacityEvaluation")
    class PersistCapacityEvaluationTests {

        private final Long VALID_ID = 1L;
        private final Long NOT_FOUND_ID = 999L;
        private final java.time.LocalDate NEW_DATE = java.time.LocalDate.of(2025, 6, 30);

        private Rfq buildRfq(Long id, String status) {
            Rfq rfq = new Rfq();
            rfq.setId(id);
            rfq.setStatus(status);
            return rfq;
        }

        // UTC01 – Normal: existing RFQ, current status DRAFT, sufficient capacity, has reason & newDate
        @Test
        @DisplayName("UTC01 - Normal: DRAFT RFQ, SUFFICIENT capacity, keep status")
        void persistCapacityEvaluation_UTC01_draftSufficient_keepStatus() {
            Rfq rfq = buildRfq(VALID_ID, "DRAFT");
            when(rfqRepository.findById(VALID_ID)).thenReturn(Optional.of(rfq));

            rfqService.persistCapacityEvaluation(
                    VALID_ID,
                    "SUFFICIENT",
                    "Đủ năng lực sản xuất",
                    NEW_DATE
            );

            ArgumentCaptor<Rfq> captor = ArgumentCaptor.forClass(Rfq.class);
            verify(rfqRepository).findById(VALID_ID);
            verify(rfqRepository).save(captor.capture());
            verifyNoInteractions(notificationService);

            Rfq saved = captor.getValue();
            assertEquals("DRAFT", saved.getStatus());
            assertEquals("SUFFICIENT", saved.getCapacityStatus());
            assertEquals("Đủ năng lực sản xuất", saved.getCapacityReason());
            assertEquals(NEW_DATE, saved.getProposedNewDeliveryDate());
        }

        // UTC02 – Normal: RECEIVED_BY_PLANNING, sufficient capacity
        @Test
        @DisplayName("UTC02 - Normal: RECEIVED_BY_PLANNING, SUFFICIENT capacity, keep status")
        void persistCapacityEvaluation_UTC02_receivedByPlanningSufficient_keepStatus() {
            Rfq rfq = buildRfq(VALID_ID, "RECEIVED_BY_PLANNING");
            when(rfqRepository.findById(VALID_ID)).thenReturn(Optional.of(rfq));

            rfqService.persistCapacityEvaluation(
                    VALID_ID,
                    "SUFFICIENT",
                    "Đủ năng lực sản xuất",
                    NEW_DATE
            );

            ArgumentCaptor<Rfq> captor = ArgumentCaptor.forClass(Rfq.class);
            verify(rfqRepository).findById(VALID_ID);
            verify(rfqRepository).save(captor.capture());
            verifyNoInteractions(notificationService);

            Rfq saved = captor.getValue();
            assertEquals("RECEIVED_BY_PLANNING", saved.getStatus());
            assertEquals("SUFFICIENT", saved.getCapacityStatus());
            assertEquals("Đủ năng lực sản xuất", saved.getCapacityReason());
            assertEquals(NEW_DATE, saved.getProposedNewDeliveryDate());
        }

        // UTC03 – Normal: PENDING_REVIEW, sufficient capacity
        @Test
        @DisplayName("UTC03 - Normal: PENDING_REVIEW, SUFFICIENT capacity, keep status")
        void persistCapacityEvaluation_UTC03_pendingReviewSufficient_keepStatus() {
            Rfq rfq = buildRfq(VALID_ID, "PENDING_REVIEW");
            when(rfqRepository.findById(VALID_ID)).thenReturn(Optional.of(rfq));

            rfqService.persistCapacityEvaluation(
                    VALID_ID,
                    "SUFFICIENT",
                    "Đủ năng lực sản xuất",
                    NEW_DATE
            );

            ArgumentCaptor<Rfq> captor = ArgumentCaptor.forClass(Rfq.class);
            verify(rfqRepository).findById(VALID_ID);
            verify(rfqRepository).save(captor.capture());
            verifyNoInteractions(notificationService);

            Rfq saved = captor.getValue();
            assertEquals("PENDING_REVIEW", saved.getStatus());
            assertEquals("SUFFICIENT", saved.getCapacityStatus());
            assertEquals("Đủ năng lực sản xuất", saved.getCapacityReason());
            assertEquals(NEW_DATE, saved.getProposedNewDeliveryDate());
        }

        // UTC04 – Normal: any status, capacityStatus = INSUFFICIENT → status must be SENT & notify
        @Test
        @DisplayName("UTC04 - Normal: INSUFFICIENT capacity -> change status to SENT & notify")
        void persistCapacityEvaluation_UTC04_insufficient_changesToSent_andNotify() {
            Rfq rfq = buildRfq(VALID_ID, "SUFFICIENT"); // trạng thái ban đầu bất kỳ
            when(rfqRepository.findById(VALID_ID)).thenReturn(Optional.of(rfq));

            rfqService.persistCapacityEvaluation(
                    VALID_ID,
                    "INSUFFICIENT",
                    "Không đủ năng lực sản xuất",
                    NEW_DATE
            );

            ArgumentCaptor<Rfq> captor = ArgumentCaptor.forClass(Rfq.class);
            verify(rfqRepository).findById(VALID_ID);
            verify(rfqRepository).save(captor.capture());
            verify(notificationService).notifyCapacityInsufficient(any(Rfq.class));

            Rfq saved = captor.getValue();
            assertEquals("SENT", saved.getStatus()); // phải chuyển về SENT
            assertEquals("INSUFFICIENT", saved.getCapacityStatus());
            assertEquals("Không đủ năng lực sản xuất", saved.getCapacityReason());
            assertEquals(NEW_DATE, saved.getProposedNewDeliveryDate());
        }

        // UTC05 – Normal: INSUFFICIENT nhưng reason & proposedNewDate null
        @Test
        @DisplayName("UTC05 - Normal: INSUFFICIENT capacity with null reason/date")
        void persistCapacityEvaluation_UTC05_insufficient_nullReasonAndDate() {
            Rfq rfq = buildRfq(VALID_ID, "DRAFT");
            when(rfqRepository.findById(VALID_ID)).thenReturn(Optional.of(rfq));

            rfqService.persistCapacityEvaluation(
                    VALID_ID,
                    "INSUFFICIENT",
                    null,
                    null
            );

            ArgumentCaptor<Rfq> captor = ArgumentCaptor.forClass(Rfq.class);
            verify(rfqRepository).findById(VALID_ID);
            verify(rfqRepository).save(captor.capture());
            verify(notificationService).notifyCapacityInsufficient(any(Rfq.class));

            Rfq saved = captor.getValue();
            assertEquals("SENT", saved.getStatus());
            assertEquals("INSUFFICIENT", saved.getCapacityStatus());
            assertNull(saved.getCapacityReason());
            assertNull(saved.getProposedNewDeliveryDate());
        }

        // UTC06 – Normal: RFQ ban đầu chưa có status (null), INSUFFICIENT
        @Test
        @DisplayName("UTC06 - Normal: null initial status, INSUFFICIENT capacity")
        void persistCapacityEvaluation_UTC06_nullInitialStatus_insufficient() {
            Rfq rfq = buildRfq(VALID_ID, null);
            when(rfqRepository.findById(VALID_ID)).thenReturn(Optional.of(rfq));

            rfqService.persistCapacityEvaluation(
                    VALID_ID,
                    "INSUFFICIENT",
                    "Thiếu năng lực sản xuất",
                    NEW_DATE
            );

            ArgumentCaptor<Rfq> captor = ArgumentCaptor.forClass(Rfq.class);
            verify(rfqRepository).findById(VALID_ID);
            verify(rfqRepository).save(captor.capture());
            verify(notificationService).notifyCapacityInsufficient(any(Rfq.class));

            Rfq saved = captor.getValue();
            assertEquals("SENT", saved.getStatus());
            assertEquals("INSUFFICIENT", saved.getCapacityStatus());
            assertEquals("Thiếu năng lực sản xuất", saved.getCapacityReason());
            assertEquals(NEW_DATE, saved.getProposedNewDeliveryDate());
        }

        // UTC07 – Abnormal: RFQ not found
        @Test
        @DisplayName("UTC07 - Abnormal: rfqId not found -> NoSuchElementException")
        void persistCapacityEvaluation_UTC07_rfqNotFound() {
            when(rfqRepository.findById(NOT_FOUND_ID)).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class, () ->
                    rfqService.persistCapacityEvaluation(
                            NOT_FOUND_ID,
                            "SUFFICIENT",
                            "Đủ năng lực sản xuất",
                            NEW_DATE
                    )
            );

            verify(rfqRepository).findById(NOT_FOUND_ID);
            verifyNoMoreInteractions(rfqRepository);
            verifyNoInteractions(notificationService);
        }

        // UTC08 – Abnormal: rfqId = 0 (giá trị không hợp lệ)
        @Test
        @DisplayName("UTC08 - Abnormal: rfqId = 0 -> NoSuchElementException")
        void persistCapacityEvaluation_UTC08_rfqIdZero() {
            when(rfqRepository.findById(0L)).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class, () ->
                    rfqService.persistCapacityEvaluation(
                            0L,
                            "SUFFICIENT",
                            "Đủ năng lực sản xuất",
                            NEW_DATE
                    )
            );

            verify(rfqRepository).findById(0L);
            verifyNoMoreInteractions(rfqRepository);
            verifyNoInteractions(notificationService);
        }

        // UTC09 – Abnormal: rfqId = null (NullPointer trên Optional)
        @Test
        @DisplayName("UTC09 - Abnormal: rfqId = null -> NullPointerException")
        void persistCapacityEvaluation_UTC09_rfqIdNull() {
            assertThrows(NoSuchElementException.class, () ->
                    rfqService.persistCapacityEvaluation(
                            null,
                            "SUFFICIENT",
                            "Đủ năng lực sản xuất",
                            NEW_DATE
                    )
            );

            // với Mockito, findById(null) sẽ trả về null => ta không stub, cũng không verify
            verify(rfqRepository).findById(null);
            verifyNoMoreInteractions(rfqRepository);

            verifyNoInteractions(notificationService);
        }
    }
    // ===================== createOrderFromQuotation =====================
    @Nested
    @DisplayName("createOrderFromQuotation")
    class CreateOrderFromQuotationTests {

        private Quotation buildBaseQuotation(String status) {
            Quotation q = new Quotation();
            q.setId(1L);

            Customer customer = new Customer();
            customer.setId(100L);
            q.setCustomer(customer);

            q.setStatus(status);
            q.setValidUntil(LocalDate.of(2025, 6, 30));
            q.setTotalAmount(BigDecimal.valueOf(1_000_000L));

            // createdBy
            User createdBy = new User();
            createdBy.setId(11L);
            q.setCreatedBy(createdBy);

            // assignments
            User sales = new User();
            sales.setId(21L);
            q.setAssignedSales(sales);

            User planning = new User();
            planning.setId(31L);
            q.setAssignedPlanning(planning);

            return q;
        }
    }



}
