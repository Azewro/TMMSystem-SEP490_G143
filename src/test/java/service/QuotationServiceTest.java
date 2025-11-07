package service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tmmsystem.entity.Customer;
import tmmsystem.entity.Quotation;
import tmmsystem.entity.Rfq;
import tmmsystem.repository.QuotationRepository;
import tmmsystem.service.QuotationService;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuotationServiceTest {

    @Mock
    private QuotationRepository quotationRepository;

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

        @Test
        @DisplayName("Normal Case: Should update quotation successfully")
        void updateQuotation_Normal_Success() {
            // Given
            Long quotationId = 1L;
            Quotation existingQuotation = new Quotation();
            existingQuotation.setId(quotationId);
            existingQuotation.setQuotationNumber("QUO-OLD-001");
            existingQuotation.setStatus("DRAFT");
            existingQuotation.setTotalAmount(new BigDecimal("100.00"));

            Quotation updatedInfo = new Quotation();
            updatedInfo.setQuotationNumber("QUO-NEW-002");
            updatedInfo.setStatus("SENT");
            updatedInfo.setTotalAmount(new BigDecimal("200.00"));
            updatedInfo.setValidUntil(LocalDate.now().plusDays(15));

            when(quotationRepository.findById(quotationId)).thenReturn(java.util.Optional.of(existingQuotation));

            // When
            Quotation updatedQuotation = quotationService.update(quotationId, updatedInfo);

            // Then
            assertNotNull(updatedQuotation);
            assertEquals("QUO-NEW-002", updatedQuotation.getQuotationNumber(), "Quotation number should be updated.");
            assertEquals("SENT", updatedQuotation.getStatus(), "Status should be updated.");
            assertEquals(0, new BigDecimal("200.00").compareTo(updatedQuotation.getTotalAmount()), "Total amount should be updated.");
            assertEquals(LocalDate.now().plusDays(15), updatedQuotation.getValidUntil(), "Valid until date should be updated.");
            System.out.println("[SUCCESS] updateQuotation_Normal_Success: Quotation updated successfully.");
            System.out.println(">> Updated Quotation: " + updatedQuotation);
        }

        @Test
        @DisplayName("Abnormal Case: Quotation not found")
        void updateQuotation_Abnormal_NotFound() {
            // Given
            Long nonExistentId = 99L;
            Quotation updatedInfo = new Quotation();
            when(quotationRepository.findById(nonExistentId)).thenReturn(java.util.Optional.empty());

            // When & Then
            assertThrows(java.util.NoSuchElementException.class, () -> {
                quotationService.update(nonExistentId, updatedInfo);
            }, "Should throw NoSuchElementException when quotation is not found.");
            System.out.println("[SUCCESS] updateQuotation_Abnormal_NotFound: Failed as expected. Quotation not found.");
        }

        @Test
        @DisplayName("Boundary Case: Update with null values")
        void updateQuotation_Boundary_NullValues() {
            // Given
            Long quotationId = 1L;
            Quotation existingQuotation = new Quotation();
            existingQuotation.setId(quotationId);
            existingQuotation.setQuotationNumber("QUO-EXISTING-001");

            Quotation updatedInfoWithNulls = new Quotation();
            updatedInfoWithNulls.setQuotationNumber(null);
            updatedInfoWithNulls.setCustomer(null);
            updatedInfoWithNulls.setStatus(null);

            when(quotationRepository.findById(quotationId)).thenReturn(java.util.Optional.of(existingQuotation));

            // When
            Quotation updatedQuotation = quotationService.update(quotationId, updatedInfoWithNulls);

            // Then
            assertNotNull(updatedQuotation);
            assertNull(updatedQuotation.getQuotationNumber(), "Quotation number should be updated to null.");
            assertNull(updatedQuotation.getCustomer(), "Customer should be updated to null.");
            assertNull(updatedQuotation.getStatus(), "Status should be updated to null.");
            System.out.println("[SUCCESS] updateQuotation_Boundary_NullValues: Quotation updated with null values.");
            System.out.println(">> Updated Quotation: " + updatedQuotation);
        }

        @Test
        @DisplayName("Boundary Case: Update with empty string for quotation number")
        void updateQuotation_Boundary_EmptyQuotationNumber() {
            // Given
            Long quotationId = 1L;
            Quotation existingQuotation = new Quotation();
            existingQuotation.setId(quotationId);
            existingQuotation.setQuotationNumber("QUO-EXISTING-001");

            Quotation updatedInfo = new Quotation();
            updatedInfo.setQuotationNumber("");

            when(quotationRepository.findById(quotationId)).thenReturn(java.util.Optional.of(existingQuotation));

            // When
            Quotation updatedQuotation = quotationService.update(quotationId, updatedInfo);

            // Then
            assertNotNull(updatedQuotation);
            assertEquals("", updatedQuotation.getQuotationNumber(), "Quotation number should be updated to an empty string.");
            System.out.println("[SUCCESS] updateQuotation_Boundary_EmptyQuotationNumber: Quotation number updated to empty string.");
            System.out.println(">> Updated Quotation: " + updatedQuotation);
        }

        @Test
        @DisplayName("Boundary Case: Update with negative total amount")
        void updateQuotation_Boundary_NegativeTotalAmount() {
            // Given
            Long quotationId = 1L;
            Quotation existingQuotation = new Quotation();
            existingQuotation.setId(quotationId);
            existingQuotation.setTotalAmount(new BigDecimal("1000.00"));

            Quotation updatedInfo = new Quotation();
            updatedInfo.setTotalAmount(new BigDecimal("-50.00"));

            when(quotationRepository.findById(quotationId)).thenReturn(java.util.Optional.of(existingQuotation));

            // When
            Quotation updatedQuotation = quotationService.update(quotationId, updatedInfo);

            // Then
            assertNotNull(updatedQuotation);
            assertTrue(updatedQuotation.getTotalAmount().compareTo(BigDecimal.ZERO) < 0, "Total amount should be updated to a negative value.");
            System.out.println("[SUCCESS] updateQuotation_Boundary_NegativeTotalAmount: Total amount updated to a negative value.");
            System.out.println(">> Updated Quotation: " + updatedQuotation);
        }
    }
}
