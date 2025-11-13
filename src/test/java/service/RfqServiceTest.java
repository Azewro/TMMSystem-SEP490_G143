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
}