package tmmsystem.service;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tmmsystem.entity.ProductionOrder;
import tmmsystem.entity.ProductionStage;
import tmmsystem.entity.StagePauseLog;
import tmmsystem.entity.User;
import tmmsystem.repository.*;
import tmmsystem.service.NotificationService;
import tmmsystem.service.ProductionPlanService;
import tmmsystem.service.ProductionService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductionServiceTest {

    @Mock private ProductionOrderRepository poRepo;
    @Mock private ProductionOrderDetailRepository podRepo;
    @Mock private TechnicalSheetRepository techRepo;
    @Mock private WorkOrderRepository woRepo;
    @Mock private WorkOrderDetailRepository wodRepo;
    @Mock private ProductionStageRepository stageRepo;
    @Mock private UserRepository userRepo;
    @Mock private NotificationService notificationService;
    @Mock private ContractRepository contractRepository;
    @Mock private BomRepository bomRepository;
    @Mock private ProductionOrderDetailRepository productionOrderDetailRepository;
    @Mock private ProductionPlanService productionPlanService;

    @InjectMocks
    private ProductionService service;

    // ============================================================
    //                  🟦  GET PO (getPO)
    // ============================================================
    @Nested
    @DisplayName("getPO Tests")
    class GetPOTests {

        @Test
        @DisplayName("UTC01 – Normal – id = 1 → HTTP 200")
        void getPO_Normal() {
            ProductionOrder po = new ProductionOrder();
            po.setId(1L);

            when(poRepo.findById(1L)).thenReturn(Optional.of(po));

            ProductionOrder result = service.findPO(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            verify(poRepo, times(1)).findById(1L);
        }

        @Test
        @DisplayName("UTC02 – Abnormal – id = 999 → NOT FOUND → throw")
        void getPO_NotFound() {
            when(poRepo.findById(999L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> service.findPO(999L));

            assertTrue(ex.getMessage().toLowerCase().contains("not"));
            verify(poRepo, times(1)).findById(999L);
        }

        @Test
        @DisplayName("UTC03 – Boundary – id = -1 → NOT FOUND → throw")
        void getPO_NegativeId() {
            when(poRepo.findById(-1L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> service.findPO(-1L));
        }

        @Test
        @DisplayName("UTC04 – Boundary – id = null → throw exception")
        void getPO_NullId() {
            assertThrows(RuntimeException.class, () -> service.findPO(null));
        }
    }

    // ============================================================
    //                  🟩  LIST PO (findAllPO)
    // ============================================================
    @Nested
    @DisplayName("listPO Tests")
    class ListPOTests {

        @Test
        @DisplayName("UTC01 – Normal – return list<PO>")
        void listPO_Normal() {
            ProductionOrder p1 = new ProductionOrder();
            ProductionOrder p2 = new ProductionOrder();

            when(poRepo.findAll()).thenReturn(List.of(p1, p2));

            List<ProductionOrder> result = service.findAllPO();

            assertNotNull(result);
            assertEquals(2, result.size());
            verify(poRepo, times(1)).findAll();
        }

        @Test
        @DisplayName("UTC02 – Boundary – empty list")
        void listPO_Empty() {
            when(poRepo.findAll()).thenReturn(List.of());

            List<ProductionOrder> result = service.findAllPO();

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(poRepo, times(1)).findAll();
        }
    }


}
