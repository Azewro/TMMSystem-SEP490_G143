package service;

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
import tmmsystem.entity.Customer;
import tmmsystem.entity.Product;
import tmmsystem.entity.Rfq;
import tmmsystem.entity.RfqDetail;
import tmmsystem.repository.RfqDetailRepository;
import tmmsystem.repository.RfqRepository;
import tmmsystem.service.NotificationService;
import tmmsystem.service.RfqService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RfqServiceTest {

    @Mock
    private RfqRepository rfqRepository;

    @Mock
    private RfqDetailRepository detailRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private RfqService rfqService;

    @Nested
    @DisplayName("Create With Details Tests")
    class CreateWithDetailsTests {

        @Test
        @DisplayName("Normal Case: Create RFQ with a single detail")
        void createWithDetails_Normal_WithOneDetail() {
            // Given
            Rfq rfq = new Rfq();
            rfq.setId(1L);

            RfqDetailDto detailDto = new RfqDetailDto();
            detailDto.setProductId(101L);
            detailDto.setQuantity(new BigDecimal("100"));

            when(rfqRepository.save(any(Rfq.class))).thenReturn(rfq);
            when(detailRepository.save(any(RfqDetail.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Rfq result = rfqService.createWithDetails(rfq, List.of(detailDto));

            // Then
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
            // Given
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
}
