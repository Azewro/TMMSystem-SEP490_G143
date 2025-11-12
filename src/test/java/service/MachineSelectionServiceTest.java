package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tmmsystem.entity.Machine;
import tmmsystem.entity.Product;
import tmmsystem.entity.ProductionPlanStage;
import tmmsystem.repository.*;
import tmmsystem.service.MachineSelectionService;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MachineSelectionServiceTest {

    @Mock
    private MachineRepository machineRepository;
    @Mock
    private MachineAssignmentRepository machineAssignmentRepository;
    @Mock
    private MachineMaintenanceRepository machineMaintenanceRepository;
    @Mock
    private ProductionPlanStageRepository productionPlanStageRepository;
    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private ProductionStageRepository productionStageRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private MachineSelectionService machineSelectionService;

    @Nested
    @DisplayName("Get Suitable Machines Parameter Tests")
    class GetSuitableMachinesTests {

        @Test
        @DisplayName("Normal Case: Valid inputs for a machine-based stage")
        void getSuitableMachines_Normal_ValidInputs() {
            // Given
            String stageType = "WEAVING";
            Long productId = 1L;
            BigDecimal requiredQuantity = new BigDecimal("100");
            LocalDateTime startTime = LocalDateTime.now().plusDays(1);
            LocalDateTime endTime = LocalDateTime.now().plusDays(2);

            Machine machine = new Machine();
            machine.setId(101L);
            machine.setCode("M-WEAVE-01");
            machine.setName("Máy dệt 01");
            machine.setType("WEAVING");
            machine.setStatus("AVAILABLE");
            machine.setSpecifications("{\"capacityPerHour\": {\"default\": 50}}");

            Product product = new Product();
            product.setId(productId);
            product.setName("Khăn tắm");

            when(machineRepository.findAll()).thenReturn(Collections.singletonList(machine));
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));
            when(machineMaintenanceRepository.findAll()).thenReturn(Collections.emptyList());
            when(productionPlanStageRepository.findAll()).thenReturn(Collections.emptyList());
            when(workOrderRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<MachineSelectionService.MachineSuggestionDto> suggestions = machineSelectionService.getSuitableMachines(stageType, productId, requiredQuantity, startTime, endTime);

            // Then
            assertNotNull(suggestions);
            assertFalse(suggestions.isEmpty());
            assertEquals(1, suggestions.size());
            MachineSelectionService.MachineSuggestionDto suggestion = suggestions.get(0);
            assertEquals(machine.getId(), suggestion.getMachineId());
            assertTrue(suggestion.isAvailable());
            System.out.println("[SUCCESS] getSuitableMachines_Normal_ValidInputs: Returned a suitable machine suggestion.");
        }

        @Test
        @DisplayName("Normal Case: Stage type is DYEING (Outsourced)")
        void getSuitableMachines_Normal_DyeingStage() {
            // Given
            String stageType = "DYEING";
            Long productId = 1L;
            BigDecimal requiredQuantity = new BigDecimal("500");
            LocalDateTime startTime = LocalDateTime.now().plusDays(1);
            LocalDateTime endTime = LocalDateTime.now().plusDays(2);

            // When
            List<MachineSelectionService.MachineSuggestionDto> suggestions = machineSelectionService.getSuitableMachines(stageType, productId, requiredQuantity, startTime, endTime);

            // Then
            assertNotNull(suggestions);
            assertEquals(1, suggestions.size());
            MachineSelectionService.MachineSuggestionDto suggestion = suggestions.get(0);
            assertNull(suggestion.getMachineId());
            assertEquals("OUTSOURCE-DYEING", suggestion.getMachineCode());
            System.out.println("[SUCCESS] getSuitableMachines_Normal_DyeingStage: Returned outsourced suggestion for DYEING stage.");
        }

        @Test
        @DisplayName("Normal Case: Stage type is PACKAGING (Manual)")
        void getSuitableMachines_Normal_PackagingStage() {
            // Given
            String stageType = "PACKAGING";
            Long productId = 1L;
            BigDecimal requiredQuantity = new BigDecimal("1000");
            LocalDateTime startTime = LocalDateTime.now().plusDays(1);
            LocalDateTime endTime = LocalDateTime.now().plusDays(2);

            // When
            List<MachineSelectionService.MachineSuggestionDto> suggestions = machineSelectionService.getSuitableMachines(stageType, productId, requiredQuantity, startTime, endTime);

            // Then
            assertNotNull(suggestions);
            assertEquals(1, suggestions.size());
            MachineSelectionService.MachineSuggestionDto suggestion = suggestions.get(0);
            assertNull(suggestion.getMachineId());
            assertEquals("MANUAL-PACKAGING", suggestion.getMachineCode());
            assertEquals(0, new BigDecimal("2").compareTo(suggestion.getEstimatedDurationHours())); // 1000 / 500 = 2 hours
            System.out.println("[SUCCESS] getSuitableMachines_Normal_PackagingStage: Returned manual suggestion for PACKAGING stage.");
        }

        @Test
        @DisplayName("Abnormal Case: Null stageType")
        void getSuitableMachines_Abnormal_NullStageType() {
            // Given
            Long productId = 1L;
            BigDecimal requiredQuantity = new BigDecimal("100");
            LocalDateTime startTime = LocalDateTime.now().plusDays(1);
            LocalDateTime endTime = LocalDateTime.now().plusDays(2);

            // When
            List<MachineSelectionService.MachineSuggestionDto> suggestions = machineSelectionService.getSuitableMachines(null, productId, requiredQuantity, startTime, endTime);

            // Then
            assertNotNull(suggestions);
            assertTrue(suggestions.isEmpty(), "Suggestions list should be empty for null stageType");
            System.out.println("[SUCCESS] getSuitableMachines_Abnormal_NullStageType: Returned empty list for null stageType as expected.");
        }

        @Test
        @DisplayName("Abnormal Case: Null productId")
        void getSuitableMachines_Abnormal_NullProductId() {
            // Given
            String stageType = "WEAVING";
            BigDecimal requiredQuantity = new BigDecimal("100");
            LocalDateTime startTime = LocalDateTime.now().plusDays(1);
            LocalDateTime endTime = LocalDateTime.now().plusDays(2);

            Machine machine = new Machine();
            machine.setType("WEAVING");
            machine.setStatus("AVAILABLE");
            machine.setSpecifications("{}");

            when(machineRepository.findAll()).thenReturn(Collections.singletonList(machine));

            // When
            List<MachineSelectionService.MachineSuggestionDto> suggestions = machineSelectionService.getSuitableMachines(stageType, null, requiredQuantity, startTime, endTime);

            // Then
            assertNotNull(suggestions);
            assertFalse(suggestions.isEmpty());
            // Should still return a suggestion but with zero capacity from product-specific calculation
            assertEquals(BigDecimal.ZERO, suggestions.get(0).getCapacityPerHour());
            System.out.println("[SUCCESS] getSuitableMachines_Abnormal_NullProductId: Handled null productId gracefully.");
        }

        @Test
        @DisplayName("Abnormal Case: Null requiredQuantity")
        void getSuitableMachines_Abnormal_NullRequiredQuantity() {
            // Given
            String stageType = "WEAVING";
            Long productId = 1L;
            LocalDateTime startTime = LocalDateTime.now().plusDays(1);
            LocalDateTime endTime = LocalDateTime.now().plusDays(2);

            // When & Then
            assertThrows(NullPointerException.class, () -> {
                machineSelectionService.getSuitableMachines(stageType, productId, null, startTime, endTime);
            });
            System.out.println("[SUCCESS] getSuitableMachines_Abnormal_NullRequiredQuantity: Threw exception for null requiredQuantity.");
        }

        @Test
        @DisplayName("Abnormal Case: Null preferredStartTime")
        void getSuitableMachines_Abnormal_NullStartTime() {
            // Given
            String stageType = "WEAVING";
            Long productId = 1L;
            BigDecimal requiredQuantity = new BigDecimal("100");
            LocalDateTime endTime = LocalDateTime.now().plusDays(2);

            // When & Then
            assertThrows(NullPointerException.class, () -> {
                machineSelectionService.getSuitableMachines(stageType, productId, requiredQuantity, null, endTime);
            });
            System.out.println("[SUCCESS] getSuitableMachines_Abnormal_NullStartTime: Threw exception for null startTime.");
        }

        @Test
        @DisplayName("Abnormal Case: Null preferredEndTime")
        void getSuitableMachines_Abnormal_NullEndTime() {
            // Given
            String stageType = "WEAVING";
            Long productId = 1L;
            BigDecimal requiredQuantity = new BigDecimal("100");
            LocalDateTime startTime = LocalDateTime.now().plusDays(1);

            // When & Then
            assertThrows(NullPointerException.class, () -> {
                machineSelectionService.getSuitableMachines(stageType, productId, requiredQuantity, startTime, null);
            });
            System.out.println("[SUCCESS] getSuitableMachines_Abnormal_NullEndTime: Threw exception for null endTime.");
        }

        @Test
        @DisplayName("Boundary Case: Empty stageType")
        void getSuitableMachines_Boundary_EmptyStageType() {
            // Given
            String stageType = "";
            Long productId = 1L;
            BigDecimal requiredQuantity = new BigDecimal("100");
            LocalDateTime startTime = LocalDateTime.now().plusDays(1);
            LocalDateTime endTime = LocalDateTime.now().plusDays(2);

            when(machineRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<MachineSelectionService.MachineSuggestionDto> suggestions = machineSelectionService.getSuitableMachines(stageType, productId, requiredQuantity, startTime, endTime);

            // Then
            assertNotNull(suggestions);
            assertTrue(suggestions.isEmpty());
            System.out.println("[SUCCESS] getSuitableMachines_Boundary_EmptyStageType: Returned no suggestions for empty stageType.");
        }

        @Test
        @DisplayName("Boundary Case: Zero requiredQuantity")
        void getSuitableMachines_Boundary_ZeroQuantity() {
            // Given
            String stageType = "CUTTING";
            Long productId = 1L;
            BigDecimal requiredQuantity = BigDecimal.ZERO;
            LocalDateTime startTime = LocalDateTime.now().plusDays(1);
            LocalDateTime endTime = LocalDateTime.now().plusDays(2);

            Machine machine = new Machine();
            machine.setType("CUTTING");
            machine.setStatus("AVAILABLE");
            machine.setSpecifications("{\"capacityPerHour\": {\"default\": 150}}");

            Product product = new Product();
            product.setId(productId);

            when(machineRepository.findAll()).thenReturn(Collections.singletonList(machine));
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));
            when(machineMaintenanceRepository.findAll()).thenReturn(Collections.emptyList());
            when(productionPlanStageRepository.findAll()).thenReturn(Collections.emptyList());
            when(workOrderRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<MachineSelectionService.MachineSuggestionDto> suggestions = machineSelectionService.getSuitableMachines(stageType, productId, requiredQuantity, startTime, endTime);

            // Then
            assertNotNull(suggestions);
            assertFalse(suggestions.isEmpty());
            assertEquals(0, BigDecimal.ZERO.compareTo(suggestions.get(0).getEstimatedDurationHours()));
            System.out.println("[SUCCESS] getSuitableMachines_Boundary_ZeroQuantity: Calculated zero duration for zero quantity.");
        }

        @Test
        @DisplayName("Boundary Case: Negative requiredQuantity")
        void getSuitableMachines_Boundary_NegativeQuantity() {
            // Given
            String stageType = "SEWING";
            Long productId = 1L;
            BigDecimal requiredQuantity = new BigDecimal("-100");
            LocalDateTime startTime = LocalDateTime.now().plusDays(1);
            LocalDateTime endTime = LocalDateTime.now().plusDays(2);

            Machine machine = new Machine();
            machine.setType("SEWING");
            machine.setStatus("AVAILABLE");
            machine.setSpecifications("{\"capacityPerHour\": {\"default\": 100}}");

            Product product = new Product();
            product.setId(productId);

            when(machineRepository.findAll()).thenReturn(Collections.singletonList(machine));
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));
            when(machineMaintenanceRepository.findAll()).thenReturn(Collections.emptyList());
            when(productionPlanStageRepository.findAll()).thenReturn(Collections.emptyList());
            when(workOrderRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<MachineSelectionService.MachineSuggestionDto> suggestions = machineSelectionService.getSuitableMachines(stageType, productId, requiredQuantity, startTime, endTime);

            // Then
            assertNotNull(suggestions);
            assertFalse(suggestions.isEmpty());
            assertTrue(suggestions.get(0).getEstimatedDurationHours().compareTo(BigDecimal.ZERO) < 0);
            System.out.println("[SUCCESS] getSuitableMachines_Boundary_NegativeQuantity: Calculated negative duration for negative quantity.");
        }

        @Test
        @DisplayName("Boundary Case: End time is before start time")
        void getSuitableMachines_Boundary_EndTimeBeforeStartTime() {
            // Given
            String stageType = "WEAVING";
            Long productId = 1L;
            BigDecimal requiredQuantity = new BigDecimal("100");
            LocalDateTime startTime = LocalDateTime.now().plusDays(2);
            LocalDateTime endTime = LocalDateTime.now().plusDays(1); // End before start

            Machine machine = new Machine();
            machine.setType("WEAVING");
            machine.setStatus("AVAILABLE");
            machine.setSpecifications("{\"capacityPerHour\": {\"default\": 50}}");

            Product product = new Product();
            product.setId(productId);

            when(machineRepository.findAll()).thenReturn(Collections.singletonList(machine));
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));
            when(machineMaintenanceRepository.findAll()).thenReturn(Collections.emptyList());
            when(productionPlanStageRepository.findAll()).thenReturn(Collections.emptyList());
            when(workOrderRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<MachineSelectionService.MachineSuggestionDto> suggestions = machineSelectionService.getSuitableMachines(stageType, productId, requiredQuantity, startTime, endTime);

            // Then
            assertNotNull(suggestions);
            assertFalse(suggestions.isEmpty());
            // The isTimeOverlap check should handle this, and the machine should still be considered available
            // as there are no conflicts.
            assertTrue(suggestions.get(0).isAvailable());
            System.out.println("[SUCCESS] getSuitableMachines_Boundary_EndTimeBeforeStartTime: Handled inverted time range correctly.");
        }

        @Test
        @DisplayName("Abnormal Case: Product not found for given productId")
        void getSuitableMachines_Abnormal_ProductNotFound() {
            // Given
            String stageType = "WEAVING";
            Long nonExistentProductId = 99L;
            BigDecimal requiredQuantity = new BigDecimal("100");
            LocalDateTime startTime = LocalDateTime.now().plusDays(1);
            LocalDateTime endTime = LocalDateTime.now().plusDays(2);

            Machine machine = new Machine();
            machine.setType("WEAVING");
            machine.setStatus("AVAILABLE");
            machine.setSpecifications("{\"capacityPerHour\": {\"default\": 50}}");

            when(machineRepository.findAll()).thenReturn(Collections.singletonList(machine));
            when(productRepository.findById(nonExistentProductId)).thenReturn(Optional.empty()); // Product not found
            when(machineMaintenanceRepository.findAll()).thenReturn(Collections.emptyList());
            when(productionPlanStageRepository.findAll()).thenReturn(Collections.emptyList());
            when(workOrderRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<MachineSelectionService.MachineSuggestionDto> suggestions = machineSelectionService.getSuitableMachines(stageType, nonExistentProductId, requiredQuantity, startTime, endTime);

            // Then
            assertNotNull(suggestions);
            assertFalse(suggestions.isEmpty());
            // Capacity should fall back to the machine's default since product-specific info is unavailable
            assertEquals(new BigDecimal("50"), suggestions.get(0).getCapacityPerHour());
            System.out.println("[SUCCESS] getSuitableMachines_Abnormal_ProductNotFound: Handled non-existent productId gracefully.");
        }

        @Test
        @DisplayName("Abnormal Case: Machine has malformed JSON specifications")
        void getSuitableMachines_Abnormal_MalformedMachineSpec() {
            // Given
            String stageType = "WEAVING";
            Long productId = 1L;
            BigDecimal requiredQuantity = new BigDecimal("100");
            LocalDateTime startTime = LocalDateTime.now().plusDays(1);
            LocalDateTime endTime = LocalDateTime.now().plusDays(2);

            Machine machine = new Machine();
            machine.setType("WEAVING");
            machine.setStatus("AVAILABLE");
            machine.setSpecifications("{\"capacityPerHour\": {\"default\": 50"); // Malformed JSON

            Product product = new Product();
            product.setId(productId);

            when(machineRepository.findAll()).thenReturn(Collections.singletonList(machine));
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));
            when(machineMaintenanceRepository.findAll()).thenReturn(Collections.emptyList());
            when(productionPlanStageRepository.findAll()).thenReturn(Collections.emptyList());
            when(workOrderRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<MachineSelectionService.MachineSuggestionDto> suggestions = machineSelectionService.getSuitableMachines(stageType, productId, requiredQuantity, startTime, endTime);

            // Then
            assertNotNull(suggestions);
            assertFalse(suggestions.isEmpty());
            // Should handle the JSON parsing error and likely default to zero capacity
            assertEquals(BigDecimal.ZERO, suggestions.get(0).getCapacityPerHour());
            System.out.println("[SUCCESS] getSuitableMachines_Abnormal_MalformedMachineSpec: Handled malformed machine specifications gracefully.");
        }

        @Test
        @DisplayName("Boundary Case: No machines of the required type exist")
        void getSuitableMachines_Boundary_NoMatchingMachineType() {
            // Given
            String stageType = "FINISHING";
            Long productId = 1L;
            BigDecimal requiredQuantity = new BigDecimal("100");
            LocalDateTime startTime = LocalDateTime.now().plusDays(1);
            LocalDateTime endTime = LocalDateTime.now().plusDays(2);

            Machine otherMachine = new Machine();
            otherMachine.setType("WEAVING"); // A different type

            when(machineRepository.findAll()).thenReturn(Collections.singletonList(otherMachine));

            // When
            List<MachineSelectionService.MachineSuggestionDto> suggestions = machineSelectionService.getSuitableMachines(stageType, productId, requiredQuantity, startTime, endTime);

            // Then
            assertNotNull(suggestions);
            assertTrue(suggestions.isEmpty());
            System.out.println("[SUCCESS] getSuitableMachines_Boundary_NoMatchingMachineType: Returned empty list as no machines matched the required type.");
        }
    }
    
        @Nested
        @DisplayName("Create Outsourced Stage Suggestion (Private Method) Tests")
        class CreateOutsourcedStageSuggestionTests {

            private Method method;

            @BeforeEach
            void setUp() throws NoSuchMethodException {
                method = MachineSelectionService.class.getDeclaredMethod("createOutsourcedStageSuggestion", String.class, Long.class, BigDecimal.class, LocalDateTime.class, LocalDateTime.class);
                method.setAccessible(true);
            }

            @Test
            @DisplayName("Normal Case: Valid inputs")
            void createOutsourcedStageSuggestion_Normal_ValidInputs() throws Exception {
                // Given
                String stageType = "DYEING";
                Long productId = 1L;
                BigDecimal requiredQuantity = new BigDecimal("500");
                LocalDateTime startTime = LocalDateTime.now().plusDays(1);
                LocalDateTime endTime = LocalDateTime.now().plusDays(2);

                // When
                List<MachineSelectionService.MachineSuggestionDto> result = (List<MachineSelectionService.MachineSuggestionDto>) method.invoke(machineSelectionService, stageType, productId, requiredQuantity, startTime, endTime);

                // Then
                assertNotNull(result);
                assertEquals(1, result.size());
                MachineSelectionService.MachineSuggestionDto suggestion = result.get(0);
                assertEquals("OUTSOURCE-DYEING", suggestion.getMachineCode());
                assertEquals(stageType, suggestion.getMachineType());
                assertTrue(suggestion.isAvailable());
                assertEquals(startTime, suggestion.getSuggestedStartTime());
                assertEquals(endTime.plusDays(1), suggestion.getSuggestedEndTime());
                System.out.println("[SUCCESS] createOutsourcedStageSuggestion_Normal_ValidInputs: Correctly created outsourced suggestion.");
            }

            @Test
            @DisplayName("Normal Case: Different stage type")
            void createOutsourcedStageSuggestion_Normal_DifferentStageType() throws Exception {
                // Given
                String stageType = "SPECIAL_TREATMENT";
                LocalDateTime startTime = LocalDateTime.now();
                LocalDateTime endTime = LocalDateTime.now().plusHours(8);

                // When
                List<MachineSelectionService.MachineSuggestionDto> result = (List<MachineSelectionService.MachineSuggestionDto>) method.invoke(machineSelectionService, stageType, 1L, BigDecimal.TEN, startTime, endTime);

                // Then
                assertNotNull(result);
                assertEquals(1, result.size());
                assertEquals(stageType, result.get(0).getMachineType(), "Machine type should match the input stageType.");
                System.out.println("[SUCCESS] createOutsourcedStageSuggestion_Normal_DifferentStageType: Correctly set machine type for a different stage.");
            }

            @Test
            @DisplayName("Abnormal Case: Null requiredQuantity")
            void createOutsourcedStageSuggestion_Abnormal_NullRequiredQuantity() {
                // Given
                String stageType = "DYEING";
                Long productId = 1L;
                LocalDateTime startTime = LocalDateTime.now().plusDays(1);
                LocalDateTime endTime = LocalDateTime.now().plusDays(2);

                // When & Then
                // This method does not use requiredQuantity, so it should not throw an exception.
                assertDoesNotThrow(() -> {
                    method.invoke(machineSelectionService, stageType, productId, null, startTime, endTime);
                }, "Method should handle null requiredQuantity gracefully as it is not used.");
                System.out.println("[SUCCESS] createOutsourcedStageSuggestion_Abnormal_NullRequiredQuantity: Handled null requiredQuantity without error.");
            }

            @Test
            @DisplayName("Abnormal Case: Null preferredStartTime")
            void createOutsourcedStageSuggestion_Abnormal_NullStartTime() {
                // Given
                String stageType = "DYEING";
                Long productId = 1L;
                BigDecimal requiredQuantity = new BigDecimal("500");
                LocalDateTime endTime = LocalDateTime.now().plusDays(2);

                // When & Then
                assertDoesNotThrow(() -> {
                    List<MachineSelectionService.MachineSuggestionDto> result = (List<MachineSelectionService.MachineSuggestionDto>) method.invoke(machineSelectionService, stageType, productId, requiredQuantity, null, endTime);
                    assertNull(result.get(0).getSuggestedStartTime(), "Suggested start time should be null when input is null.");
                }, "Method should handle null startTime gracefully.");
                System.out.println("[SUCCESS] createOutsourcedStageSuggestion_Abnormal_NullStartTime: Handled null startTime without error.");
            }

            @Test
            @DisplayName("Abnormal Case: Null preferredEndTime")
            void createOutsourcedStageSuggestion_Abnormal_NullEndTime() {
                // Given
                String stageType = "DYEING";
                Long productId = 1L;
                BigDecimal requiredQuantity = new BigDecimal("500");
                LocalDateTime startTime = LocalDateTime.now().plusDays(1);

                // When & Then
                Exception exception = assertThrows(Exception.class, () -> {
                    method.invoke(machineSelectionService, stageType, productId, requiredQuantity, startTime, null);
                });
                assertInstanceOf(NullPointerException.class, exception.getCause());
                System.out.println("[SUCCESS] createOutsourcedStageSuggestion_Abnormal_NullEndTime: Threw exception for null endTime.");
            }

            @Test
            @DisplayName("Abnormal Case: Null stageType")
            void createOutsourcedStageSuggestion_Abnormal_NullStageType() throws Exception {
                // Given
                LocalDateTime startTime = LocalDateTime.now();
                LocalDateTime endTime = LocalDateTime.now().plusDays(1);

                // When
                List<MachineSelectionService.MachineSuggestionDto> result = (List<MachineSelectionService.MachineSuggestionDto>) method.invoke(machineSelectionService, null, 1L, BigDecimal.TEN, startTime, endTime);

                // Then
                assertNotNull(result);
                assertFalse(result.isEmpty());
                assertNull(result.get(0).getMachineType(), "Machine type should be null for null stageType input.");
                System.out.println("[SUCCESS] createOutsourcedStageSuggestion_Abnormal_NullStageType: Handled null stageType correctly.");
            }

            @Test
            @DisplayName("Abnormal Case: Null productId")
            void createOutsourcedStageSuggestion_Abnormal_NullProductId() {
                // Given
                String stageType = "DYEING";
                LocalDateTime startTime = LocalDateTime.now();
                LocalDateTime endTime = LocalDateTime.now().plusDays(1);

                // When & Then
                assertDoesNotThrow(() -> {
                    method.invoke(machineSelectionService, stageType, null, BigDecimal.TEN, startTime, endTime);
                }, "Method should handle null productId gracefully as it is not used.");
                System.out.println("[SUCCESS] createOutsourcedStageSuggestion_Abnormal_NullProductId: Handled null productId without error.");
            }

            @Test
            @DisplayName("Boundary Case: Zero requiredQuantity")
            void createOutsourcedStageSuggestion_Boundary_ZeroQuantity() throws Exception {
                // Given
                String stageType = "DYEING";
                Long productId = 1L;
                BigDecimal requiredQuantity = BigDecimal.ZERO;
                LocalDateTime startTime = LocalDateTime.now().plusDays(1);
                LocalDateTime endTime = LocalDateTime.now().plusDays(2);

                // When
                List<MachineSelectionService.MachineSuggestionDto> result = (List<MachineSelectionService.MachineSuggestionDto>) method.invoke(machineSelectionService, stageType, productId, requiredQuantity, startTime, endTime);

                // Then
                assertNotNull(result);
                assertFalse(result.isEmpty());
                // The estimated duration is hardcoded, so it should not be affected by quantity.
                assertEquals(new BigDecimal("24"), result.get(0).getEstimatedDurationHours());
                System.out.println("[SUCCESS] createOutsourcedStageSuggestion_Boundary_ZeroQuantity: Correctly created suggestion for zero quantity.");
            }

            @Test
            @DisplayName("Boundary Case: Negative requiredQuantity")
            void createOutsourcedStageSuggestion_Boundary_NegativeQuantity() {
                // Given
                String stageType = "DYEING";
                BigDecimal negativeQuantity = new BigDecimal("-100");
                LocalDateTime startTime = LocalDateTime.now();
                LocalDateTime endTime = LocalDateTime.now().plusDays(1);

                // When & Then
                assertDoesNotThrow(() -> {
                    method.invoke(machineSelectionService, stageType, 1L, negativeQuantity, startTime, endTime);
                }, "Method should handle negative requiredQuantity gracefully as it is not used.");
                System.out.println("[SUCCESS] createOutsourcedStageSuggestion_Boundary_NegativeQuantity: Handled negative quantity without error.");
            }

            @Test
            @DisplayName("Boundary Case: Empty stageType")
            void createOutsourcedStageSuggestion_Boundary_EmptyStageType() throws Exception {
                // Given
                String stageType = "";
                LocalDateTime startTime = LocalDateTime.now();
                LocalDateTime endTime = LocalDateTime.now().plusDays(1);

                // When
                List<MachineSelectionService.MachineSuggestionDto> result = (List<MachineSelectionService.MachineSuggestionDto>) method.invoke(machineSelectionService, stageType, 1L, BigDecimal.TEN, startTime, endTime);

                // Then
                assertNotNull(result);
                assertFalse(result.isEmpty());
                assertEquals("", result.get(0).getMachineType(), "Machine type should be an empty string.");
                System.out.println("[SUCCESS] createOutsourcedStageSuggestion_Boundary_EmptyStageType: Handled empty stageType correctly.");
            }

            @Test
            @DisplayName("Boundary Case: Zero productId")
            void createOutsourcedStageSuggestion_Boundary_ZeroProductId() {
                // Given
                String stageType = "DYEING";
                Long productId = 0L;
                LocalDateTime startTime = LocalDateTime.now();
                LocalDateTime endTime = LocalDateTime.now().plusDays(1);

                // When & Then
                assertDoesNotThrow(() -> {
                    method.invoke(machineSelectionService, stageType, productId, BigDecimal.TEN, startTime, endTime);
                }, "Method should handle zero productId gracefully as it is not used.");
                System.out.println("[SUCCESS] createOutsourcedStageSuggestion_Boundary_ZeroProductId: Handled zero productId without error.");
            }

            @Test
            @DisplayName("Boundary Case: Negative productId")
            void createOutsourcedStageSuggestion_Boundary_NegativeProductId() {
                // Given
                String stageType = "DYEING";
                Long productId = -1L;
                LocalDateTime startTime = LocalDateTime.now();
                LocalDateTime endTime = LocalDateTime.now().plusDays(1);

                // When & Then
                assertDoesNotThrow(() -> {
                    method.invoke(machineSelectionService, stageType, productId, BigDecimal.TEN, startTime, endTime);
                }, "Method should handle negative productId gracefully as it is not used.");
                System.out.println("[SUCCESS] createOutsourcedStageSuggestion_Boundary_NegativeProductId: Handled negative productId without error.");
            }

            @Test
            @DisplayName("Boundary Case: End time is same as start time")
            void createOutsourcedStageSuggestion_Boundary_EndTimeSameAsStartTime() throws Exception {
                // Given
                String stageType = "DYEING";
                LocalDateTime time = LocalDateTime.now();

                // When
                List<MachineSelectionService.MachineSuggestionDto> result = (List<MachineSelectionService.MachineSuggestionDto>) method.invoke(machineSelectionService, stageType, 1L, BigDecimal.TEN, time, time);

                // Then
                assertNotNull(result);
                assertFalse(result.isEmpty());
                assertEquals(time.plusDays(1), result.get(0).getSuggestedEndTime(), "Suggested end time should be 1 day after the start time.");
                System.out.println("[SUCCESS] createOutsourcedStageSuggestion_Boundary_EndTimeSameAsStartTime: Correctly calculated end time when start and end are the same.");
            }
        }

    @Nested
    @DisplayName("Create Manual Stage Suggestion (Private Method) Tests")
    class CreateManualStageSuggestionTests {

        private Method method;

        @BeforeEach
        void setUp() throws NoSuchMethodException {
            method = MachineSelectionService.class.getDeclaredMethod("createManualStageSuggestion", String.class, Long.class, BigDecimal.class, LocalDateTime.class, LocalDateTime.class);
            method.setAccessible(true);
        }

        @Test
        @DisplayName("Normal Case: Valid inputs")
        void createManualStageSuggestion_Normal_ValidInputs() throws Exception {
            // Given
            String stageType = "PACKAGING";
            BigDecimal requiredQuantity = new BigDecimal("1500"); // Should take 3 hours
            LocalDateTime startTime = LocalDateTime.now().plusDays(1);

            // When
            List<MachineSelectionService.MachineSuggestionDto> result = (List<MachineSelectionService.MachineSuggestionDto>) method.invoke(machineSelectionService, stageType, 1L, requiredQuantity, startTime, null);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            MachineSelectionService.MachineSuggestionDto suggestion = result.get(0);
            assertEquals("MANUAL-PACKAGING", suggestion.getMachineCode());
            assertEquals(stageType, suggestion.getMachineType());
            assertEquals(0, new BigDecimal("3.00").compareTo(suggestion.getEstimatedDurationHours()));
            assertEquals(startTime.plusHours(3), suggestion.getSuggestedEndTime());
            System.out.println("[SUCCESS] createManualStageSuggestion_Normal_ValidInputs: Correctly created manual suggestion.");
        }

        @Test
        @DisplayName("Abnormal Case: Null requiredQuantity")
        void createManualStageSuggestion_Abnormal_NullRequiredQuantity() {
            // Given
            String stageType = "PACKAGING";
            LocalDateTime startTime = LocalDateTime.now();

            // When & Then
            Exception exception = assertThrows(Exception.class, () -> {
                method.invoke(machineSelectionService, stageType, 1L, null, startTime, null);
            });
            assertInstanceOf(NullPointerException.class, exception.getCause());
            System.out.println("[SUCCESS] createManualStageSuggestion_Abnormal_NullRequiredQuantity: Threw exception for null requiredQuantity.");
        }

        @Test
        @DisplayName("Abnormal Case: Null preferredStartTime")
        void createManualStageSuggestion_Abnormal_NullStartTime() {
            // Given
            String stageType = "PACKAGING";
            BigDecimal requiredQuantity = new BigDecimal("500");

            // When & Then
            Exception exception = assertThrows(Exception.class, () -> {
                method.invoke(machineSelectionService, stageType, 1L, requiredQuantity, null, null);
            });
            assertInstanceOf(NullPointerException.class, exception.getCause());
            System.out.println("[SUCCESS] createManualStageSuggestion_Abnormal_NullStartTime: Threw exception for null startTime.");
        }

        @Test
        @DisplayName("Boundary Case: Zero requiredQuantity")
        void createManualStageSuggestion_Boundary_ZeroQuantity() throws Exception {
            // Given
            String stageType = "PACKAGING";
            BigDecimal requiredQuantity = BigDecimal.ZERO;
            LocalDateTime startTime = LocalDateTime.now();

            // When
            List<MachineSelectionService.MachineSuggestionDto> result = (List<MachineSelectionService.MachineSuggestionDto>) method.invoke(machineSelectionService, stageType, 1L, requiredQuantity, startTime, null);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            MachineSelectionService.MachineSuggestionDto suggestion = result.get(0);
            assertEquals(0, BigDecimal.ZERO.compareTo(suggestion.getEstimatedDurationHours()));
            assertEquals(startTime, suggestion.getSuggestedEndTime(), "End time should be same as start time for zero duration.");
            System.out.println("[SUCCESS] createManualStageSuggestion_Boundary_ZeroQuantity: Correctly handled zero quantity.");
        }

        @Test
        @DisplayName("Boundary Case: Negative requiredQuantity")
        void createManualStageSuggestion_Boundary_NegativeQuantity() throws Exception {
            // Given
            String stageType = "PACKAGING";
            BigDecimal requiredQuantity = new BigDecimal("-500"); // Should be -1 hour
            LocalDateTime startTime = LocalDateTime.now();

            // When
            List<MachineSelectionService.MachineSuggestionDto> result = (List<MachineSelectionService.MachineSuggestionDto>) method.invoke(machineSelectionService, stageType, 1L, requiredQuantity, startTime, null);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            MachineSelectionService.MachineSuggestionDto suggestion = result.get(0);
            assertEquals(0, new BigDecimal("-1.00").compareTo(suggestion.getEstimatedDurationHours()));
            assertEquals(startTime.minusHours(1), suggestion.getSuggestedEndTime(), "End time should be before start time for negative duration.");
            System.out.println("[SUCCESS] createManualStageSuggestion_Boundary_NegativeQuantity: Correctly handled negative quantity.");
        }

        @Test
        @DisplayName("Abnormal Case: Null stageType")
        void createManualStageSuggestion_Abnormal_NullStageType() throws Exception {
            // Given
            BigDecimal requiredQuantity = new BigDecimal("500");
            LocalDateTime startTime = LocalDateTime.now();

            // When
            List<MachineSelectionService.MachineSuggestionDto> result = (List<MachineSelectionService.MachineSuggestionDto>) method.invoke(machineSelectionService, null, 1L, requiredQuantity, startTime, null);

            // Then
            assertNotNull(result);
            assertFalse(result.isEmpty());
            MachineSelectionService.MachineSuggestionDto suggestion = result.get(0);
            assertNull(suggestion.getMachineType(), "Machine type should be null when stageType is null.");
            assertEquals("MANUAL-PACKAGING", suggestion.getMachineCode(), "Machine code should be the default manual code.");
            System.out.println("[SUCCESS] createManualStageSuggestion_Abnormal_NullStageType: Handled null stageType gracefully.");
        }

        @Test
        @DisplayName("Abnormal Case: Null productId")
        void createManualStageSuggestion_Abnormal_NullProductId() {
            // Given
            String stageType = "PACKAGING";
            BigDecimal requiredQuantity = new BigDecimal("500");
            LocalDateTime startTime = LocalDateTime.now();

            // When & Then
            assertDoesNotThrow(() -> {
                method.invoke(machineSelectionService, stageType, null, requiredQuantity, startTime, null);
            }, "Method should handle null productId gracefully as it is not used.");
            System.out.println("[SUCCESS] createManualStageSuggestion_Abnormal_NullProductId: Handled null productId without error.");
        }

        @Test
        @DisplayName("Boundary Case: Quantity results in non-integer duration")
        void createManualStageSuggestion_Boundary_NonIntegerDuration() throws Exception {
            // Given
            String stageType = "PACKAGING";
            BigDecimal requiredQuantity = new BigDecimal("750"); // Should be 1.5 hours
            LocalDateTime startTime = LocalDateTime.now();

            // When
            List<MachineSelectionService.MachineSuggestionDto> result = (List<MachineSelectionService.MachineSuggestionDto>) method.invoke(machineSelectionService, stageType, 1L, requiredQuantity, startTime, null);

            // Then
            assertNotNull(result);
            assertFalse(result.isEmpty());
            MachineSelectionService.MachineSuggestionDto suggestion = result.get(0);
            assertEquals(0, new BigDecimal("1.50").compareTo(suggestion.getEstimatedDurationHours()));
            // Due to .intValue(), 1.5 becomes 1. The end time is only 1 hour later.
            assertEquals(startTime.plusHours(1), suggestion.getSuggestedEndTime(), "End time calculation should handle non-integer hours (current behavior truncates).");
            System.out.println("[SUCCESS] createManualStageSuggestion_Boundary_NonIntegerDuration: Correctly handled non-integer duration calculation.");
        }

        @Test
        @DisplayName("Boundary Case: Large requiredQuantity")
        void createManualStageSuggestion_Boundary_LargeQuantity() throws Exception {
            // Given
            String stageType = "PACKAGING";
            BigDecimal requiredQuantity = new BigDecimal("500000"); // 1000 hours
            LocalDateTime startTime = LocalDateTime.now();

            // When
            List<MachineSelectionService.MachineSuggestionDto> result = (List<MachineSelectionService.MachineSuggestionDto>) method.invoke(machineSelectionService, stageType, 1L, requiredQuantity, startTime, null);

            // Then
            assertNotNull(result);
            assertFalse(result.isEmpty());
            MachineSelectionService.MachineSuggestionDto suggestion = result.get(0);
            assertEquals(0, new BigDecimal("1000.00").compareTo(suggestion.getEstimatedDurationHours()));
            assertEquals(startTime.plusHours(1000), suggestion.getSuggestedEndTime());
            System.out.println("[SUCCESS] createManualStageSuggestion_Boundary_LargeQuantity: Correctly handled large quantity.");
        }

        @Test
        @DisplayName("Boundary Case: Empty stageType")
        void createManualStageSuggestion_Boundary_EmptyStageType() throws Exception {
            // Given
            String stageType = "";
            BigDecimal requiredQuantity = new BigDecimal("500");
            LocalDateTime startTime = LocalDateTime.now();

            // When
            List<MachineSelectionService.MachineSuggestionDto> result = (List<MachineSelectionService.MachineSuggestionDto>) method.invoke(machineSelectionService, stageType, 1L, requiredQuantity, startTime, null);

            // Then
            assertNotNull(result);
            assertFalse(result.isEmpty());
            MachineSelectionService.MachineSuggestionDto suggestion = result.get(0);
            assertEquals("", suggestion.getMachineType());
            assertEquals("MANUAL-", suggestion.getMachineCode(), "Machine code should be based on the empty stage type.");
            System.out.println("[SUCCESS] createManualStageSuggestion_Boundary_EmptyStageType: Handled empty stageType correctly.");
        }
    }

    @Nested
    @DisplayName("Calculate Machine Capacity (Private Method) Tests")
    class CalculateMachineCapacityTests {

        private Method method;

        @BeforeEach
        void setUp() throws NoSuchMethodException {
            method = MachineSelectionService.class.getDeclaredMethod("calculateMachineCapacity", Machine.class, Long.class, BigDecimal.class);
            method.setAccessible(true);
        }


        @Test
        @DisplayName("Normal Case: Fallback to default capacity for machine type")
        void calculateMachineCapacity_Normal_FallbackToDefault() throws Exception {
            // Given
            Machine machine = new Machine();
            machine.setType("WEAVING");
            machine.setSpecifications("{}"); // Empty specs, will trigger fallback

            Product product = new Product();
            product.setId(1L);
            product.setName("Sản phẩm mới");

            BigDecimal requiredQuantity = new BigDecimal("100");

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            // When
            MachineSelectionService.MachineCapacityInfo result = (MachineSelectionService.MachineCapacityInfo) method.invoke(machineSelectionService, machine, 1L, requiredQuantity);

            // Then
            assertNotNull(result);
            assertEquals(0, new BigDecimal("50").compareTo(result.getCapacityPerHour())); // Default for WEAVING
            assertEquals(0, new BigDecimal("2.00").compareTo(result.getEstimatedDurationHours())); // 100 / 50 = 2
            assertTrue(result.isCanHandleQuantity());
            System.out.println("[SUCCESS] calculateMachineCapacity_Normal_FallbackToDefault: Correctly fell back to default machine type capacity.");
        }

        @Test
        @DisplayName("Abnormal Case: Product not found")
        void calculateMachineCapacity_Abnormal_ProductNotFound() throws Exception {
            // Given
            Machine machine = new Machine();
            Long nonExistentProductId = 99L;
            BigDecimal requiredQuantity = new BigDecimal("100");

            when(productRepository.findById(nonExistentProductId)).thenReturn(Optional.empty());

            // When
            MachineSelectionService.MachineCapacityInfo result = (MachineSelectionService.MachineCapacityInfo) method.invoke(machineSelectionService, machine, nonExistentProductId, requiredQuantity);

            // Then
            assertNotNull(result);
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getCapacityPerHour()));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getEstimatedDurationHours()));
            assertFalse(result.isCanHandleQuantity());
            System.out.println("[SUCCESS] calculateMachineCapacity_Abnormal_ProductNotFound: Returned zero capacity for non-existent product.");
        }

        @Test
        @DisplayName("Abnormal Case: Null requiredQuantity")
        void calculateMachineCapacity_Abnormal_NullRequiredQuantity() {
            // Given
            Machine machine = new Machine();
            Product product = new Product();
            product.setId(1L);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            // When & Then
            Exception exception = assertThrows(Exception.class, () -> {
                method.invoke(machineSelectionService, machine, 1L, null);
            });
            assertInstanceOf(NullPointerException.class, exception.getCause());
            System.out.println("[SUCCESS] calculateMachineCapacity_Abnormal_NullRequiredQuantity: Threw exception for null requiredQuantity.");
        }

        @Test
        @DisplayName("Boundary Case: Zero requiredQuantity")
        void calculateMachineCapacity_Boundary_ZeroQuantity() throws Exception {
            // Given
            Machine machine = new Machine();
            machine.setSpecifications("{\"capacityPerHour\": {\"default\": 50}}");
            Product product = new Product();
            product.setId(1L);
            BigDecimal requiredQuantity = BigDecimal.ZERO;

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            // When
            MachineSelectionService.MachineCapacityInfo result = (MachineSelectionService.MachineCapacityInfo) method.invoke(machineSelectionService, machine, 1L, requiredQuantity);

            // Then
            assertNotNull(result);
            assertEquals(0, new BigDecimal("50").compareTo(result.getCapacityPerHour()));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getEstimatedDurationHours()));
            assertTrue(result.isCanHandleQuantity());
            System.out.println("[SUCCESS] calculateMachineCapacity_Boundary_ZeroQuantity: Correctly calculated zero duration for zero quantity.");
        }

        @Test
        @DisplayName("Abnormal Case: Malformed JSON in specifications")
        void calculateMachineCapacity_Abnormal_MalformedSpecJson() throws Exception {
            // Given
            Machine machine = new Machine();
            machine.setType("WEAVING");
            machine.setSpecifications("{\"capacityPerHour\": {\"default\": 50, "); // Malformed JSON

            Product product = new Product();
            product.setId(1L);
            product.setName("Khăn tắm");

            BigDecimal requiredQuantity = new BigDecimal("100");

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            // When
            MachineSelectionService.MachineCapacityInfo result = (MachineSelectionService.MachineCapacityInfo) method.invoke(machineSelectionService, machine, 1L, requiredQuantity);

            // Then
            assertNotNull(result);
            // Should fallback to default capacity for WEAVING type
            assertEquals(0, new BigDecimal("50").compareTo(result.getCapacityPerHour()));
            System.out.println("[SUCCESS] calculateMachineCapacity_Abnormal_MalformedSpecJson: Handled malformed JSON by falling back to default capacity.");
        }

        @Test
        @DisplayName("Abnormal Case: Null productId")
        void calculateMachineCapacity_Abnormal_NullProductId() throws Exception {
            // Given
            Machine machine = new Machine();
            BigDecimal requiredQuantity = new BigDecimal("100");

            // When
            MachineSelectionService.MachineCapacityInfo result = (MachineSelectionService.MachineCapacityInfo) method.invoke(machineSelectionService, machine, null, requiredQuantity);

            // Then
            assertNotNull(result);
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getCapacityPerHour()));
            assertFalse(result.isCanHandleQuantity());
            System.out.println("[SUCCESS] calculateMachineCapacity_Abnormal_NullProductId: Returned zero capacity for null productId.");
        }

        @Test
        @DisplayName("Abnormal Case: Null machine type for fallback")
        void calculateMachineCapacity_Abnormal_NullMachineType() {
            // Given
            Machine machine = new Machine();
            machine.setType(null); // Null type
            machine.setSpecifications("{}"); // Will trigger fallback

            Product product = new Product();
            product.setId(1L);
            BigDecimal requiredQuantity = new BigDecimal("100");

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            // When & Then
            Exception exception = assertThrows(Exception.class, () -> {
                method.invoke(machineSelectionService, machine, 1L, requiredQuantity);
            });
            // The switch statement on a null object throws NPE
            assertInstanceOf(NullPointerException.class, exception.getCause());
            System.out.println("[SUCCESS] calculateMachineCapacity_Abnormal_NullMachineType: Threw exception when falling back with a null machine type.");
        }

        @Test
        @DisplayName("Boundary Case: Quantity exceeds daily capacity")
        void calculateMachineCapacity_Boundary_QuantityExceedsDailyCapacity() throws Exception {
            // Given
            Machine machine = new Machine();
            machine.setSpecifications("{\"capacityPerHour\": {\"default\": 50}}"); // 50/hr -> 400/day
            Product product = new Product();
            product.setId(1L);
            BigDecimal requiredQuantity = new BigDecimal("500"); // Exceeds 400

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            // When
            MachineSelectionService.MachineCapacityInfo result = (MachineSelectionService.MachineCapacityInfo) method.invoke(machineSelectionService, machine, 1L, requiredQuantity);

            // Then
            assertNotNull(result);
            assertFalse(result.isCanHandleQuantity(), "canHandleQuantity should be false when quantity exceeds daily capacity.");
            System.out.println("[SUCCESS] calculateMachineCapacity_Boundary_QuantityExceedsDailyCapacity: Correctly flagged that quantity cannot be handled in a day.");
        }

        @Test
        @DisplayName("Boundary Case: Spec key not found")
        void calculateMachineCapacity_Boundary_SpecKeyNotFound() throws Exception {
            // Given
            Machine machine = new Machine();
            machine.setType("SEWING");
            // Valid JSON, but no key for "khăn mặt" or "default"
            machine.setSpecifications("{\"capacityPerHour\": {\"khăn tắm\": 120}}");

            Product product = new Product();
            product.setId(1L);
            product.setName("Khăn mặt");

            BigDecimal requiredQuantity = new BigDecimal("200");

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            // When
            MachineSelectionService.MachineCapacityInfo result = (MachineSelectionService.MachineCapacityInfo) method.invoke(machineSelectionService, machine, 1L, requiredQuantity);

            // Then
            assertNotNull(result);
            // Should fallback to default capacity for SEWING type
            assertEquals(0, new BigDecimal("100").compareTo(result.getCapacityPerHour()));
            System.out.println("[SUCCESS] calculateMachineCapacity_Boundary_SpecKeyNotFound: Correctly fell back to default when spec key was not found.");
        }
    }
    }
