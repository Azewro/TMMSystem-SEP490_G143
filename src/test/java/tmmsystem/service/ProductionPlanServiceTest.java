package tmmsystem.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import tmmsystem.dto.production_plan.*;
import tmmsystem.entity.*;
import tmmsystem.mapper.ProductionPlanMapper;
import tmmsystem.repository.*;
import tmmsystem.service.MachineSelectionService;
import tmmsystem.service.NotificationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductionPlanServiceTest {

    @Mock private ProductionPlanRepository planRepo;
    @Mock private ProductionPlanDetailRepository detailRepo;
    @Mock private ProductionPlanStageRepository stageRepo;
    @Mock private ContractRepository contractRepo;
    @Mock private UserRepository userRepo;
    @Mock private MachineRepository machineRepo;
    @Mock private ProductRepository productRepo;
    @Mock private ProductionOrderRepository poRepo;
    @Mock private ProductionOrderDetailRepository podRepo;
    @Mock private BomRepository bomRepo;
    @Mock private BomDetailRepository bomDetailRepo;
    @Mock private MaterialRepository materialRepo;
    @Mock private NotificationService notificationService;
    @Mock private ProductionPlanMapper mapper;
    @Mock private MachineSelectionService machineSelectionService;

    @InjectMocks
    private tmmsystem.service.ProductionPlanService service;

    @BeforeEach
    void setup() {}

    //=========================================================
    // createPlanFromContract
    //=========================================================
    @Nested
    class CreatePlanFromContractTests {

        @Test
        void createPlanFromContract_Normal_WithPlanCode() {
            CreateProductionPlanRequest req = new CreateProductionPlanRequest();
            req.setContractId(1L);
            req.setPlanCode("PP-TEST");
            req.setNotes("note");

            Contract contract = new Contract();
            contract.setId(1L);
            contract.setStatus("APPROVED");

            when(contractRepo.findById(1L)).thenReturn(Optional.of(contract));
            when(planRepo.findByContractId(1L)).thenReturn(List.of());
            when(planRepo.existsByPlanCode("PP-TEST")).thenReturn(false);

            // service.getCurrentUser() expects userRepo.findById(1L) - stub it
            when(userRepo.findById(1L)).thenReturn(Optional.of(new User()));

            when(planRepo.save(any(ProductionPlan.class))).thenAnswer(inv -> {
                ProductionPlan p = inv.getArgument(0);
                p.setId(100L);
                return p;
            });

            // IMPORTANT: specify the overloaded type explicitly to avoid ambiguity
            when(mapper.toDto(any(ProductionPlan.class)))
                    .thenReturn(new ProductionPlanDto());

            ProductionPlanDto out = service.createPlanFromContract(req);

            assertNotNull(out);
            verify(notificationService, times(1))
                    .notifyProductionPlanCreated(any(ProductionPlan.class));
        }

        @Test
        void createPlanFromContract_ContractNotFound() {
            CreateProductionPlanRequest req = new CreateProductionPlanRequest();
            req.setContractId(55L);

            when(contractRepo.findById(55L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> service.createPlanFromContract(req));
        }
    }

    //=========================================================
    // submitForApproval
    //=========================================================
    //=========================================================
    // submitForApproval  (UTC01 - UTC06)
    //=========================================================
    @Nested
    @DisplayName("submitForApproval tests (UTC01-UTC06)")
    class SubmitForApprovalTests {

        // UTC01 - Normal: id=1, valid notes
        @Test
        @DisplayName("UTC01 - Normal: id=1, valid notes")
        void submitForApproval_UTC01_Normal_Id1_ValidNotes() {
            Long planId = 1L;

            SubmitForApprovalRequest req = new SubmitForApprovalRequest();
            req.setNotes("Kế hoạch đã hoàn thiện, xin phê duyệt");

            ProductionPlan plan = new ProductionPlan();
            plan.setId(planId);
            plan.setStatus(ProductionPlan.PlanStatus.DRAFT);

            when(planRepo.findById(planId)).thenReturn(Optional.of(plan));
            when(planRepo.save(any(ProductionPlan.class))).thenAnswer(inv -> inv.getArgument(0));
            ProductionPlanDto dto = new ProductionPlanDto();
            when(mapper.toDto(any(ProductionPlan.class))).thenReturn(dto);

            ProductionPlanDto out = service.submitForApproval(planId, req);

            assertNotNull(out);
            assertEquals(ProductionPlan.PlanStatus.PENDING_APPROVAL, plan.getStatus());
            assertEquals("Kế hoạch đã hoàn thiện, xin phê duyệt", plan.getApprovalNotes());

            verify(notificationService, times(1))
                    .notifyProductionPlanSubmittedForApproval(any(ProductionPlan.class));
        }

        // UTC02 - Abnormal: id not found -> orElseThrow()
        @Test
        @DisplayName("UTC02 - Abnormal: id not found")
        void submitForApproval_UTC02_IdNotFound() {
            Long planId = 99999L;

            when(planRepo.findById(planId)).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class,
                    () -> service.submitForApproval(planId, new SubmitForApprovalRequest()));

            verify(planRepo).findById(planId);
            verify(notificationService, never()).notifyProductionPlanSubmittedForApproval(any());
        }

        // UTC03 - Abnormal: negative id -> treat as not found
        @Test
        @DisplayName("UTC03 - Abnormal: negative id")
        void submitForApproval_UTC03_IdNegative() {
            Long planId = -1L;

            when(planRepo.findById(planId)).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class,
                    () -> service.submitForApproval(planId, new SubmitForApprovalRequest()));

            verify(planRepo).findById(planId);
            verify(notificationService, never()).notifyProductionPlanSubmittedForApproval(any());
        }

        // UTC04 - Abnormal: null id -> expect exception
        @Test
        @DisplayName("UTC04 - Abnormal: null id")
        void submitForApproval_UTC04_IdNull() {
            SubmitForApprovalRequest req = new SubmitForApprovalRequest();
            req.setNotes("Kế hoạch đã hoàn thiện, xin phê duyệt");

            assertThrows(Exception.class, () -> service.submitForApproval(null, req));

            verify(notificationService, never()).notifyProductionPlanSubmittedForApproval(any());
        }

        // UTC05 - Normal: id=1, notes empty string
        @Test
        @DisplayName("UTC05 - Normal: notes empty string")
        void submitForApproval_UTC05_Id1_EmptyNotes() {
            Long planId = 1L;

            SubmitForApprovalRequest req = new SubmitForApprovalRequest();
            req.setNotes("");

            ProductionPlan plan = new ProductionPlan();
            plan.setId(planId);
            plan.setStatus(ProductionPlan.PlanStatus.DRAFT);

            when(planRepo.findById(planId)).thenReturn(Optional.of(plan));
            when(planRepo.save(any(ProductionPlan.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toDto(any(ProductionPlan.class))).thenReturn(new ProductionPlanDto());

            ProductionPlanDto out = service.submitForApproval(planId, req);

            assertNotNull(out);
            assertEquals(ProductionPlan.PlanStatus.PENDING_APPROVAL, plan.getStatus());
            assertEquals("", plan.getApprovalNotes());

            verify(notificationService, times(1)).notifyProductionPlanSubmittedForApproval(any(ProductionPlan.class));
        }

        // UTC06 - Normal: id=1, notes null
        @Test
        @DisplayName("UTC06 - Normal: notes null")
        void submitForApproval_UTC06_Id1_NotesNull() {
            Long planId = 1L;

            SubmitForApprovalRequest req = new SubmitForApprovalRequest();
            // notes left null

            ProductionPlan plan = new ProductionPlan();
            plan.setId(planId);
            plan.setStatus(ProductionPlan.PlanStatus.DRAFT);

            when(planRepo.findById(planId)).thenReturn(Optional.of(plan));
            when(planRepo.save(any(ProductionPlan.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toDto(any(ProductionPlan.class))).thenReturn(new ProductionPlanDto());

            ProductionPlanDto out = service.submitForApproval(planId, req);

            assertNotNull(out);
            assertEquals(ProductionPlan.PlanStatus.PENDING_APPROVAL, plan.getStatus());
            assertNull(plan.getApprovalNotes());

            verify(notificationService, times(1)).notifyProductionPlanSubmittedForApproval(any(ProductionPlan.class));
        }
    }



    //=========================================================
    // approvePlan + rejectPlan
    //=========================================================

}
