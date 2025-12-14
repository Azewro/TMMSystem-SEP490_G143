package tmmsystem.service;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tmmsystem.entity.*;
import tmmsystem.repository.StageTrackingRepository;
import tmmsystem.repository.StagePauseLogRepository;
import tmmsystem.repository.OutsourcingTaskRepository;
import tmmsystem.service.ExecutionService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutionServiceTest {

    @Mock
    private StageTrackingRepository trackingRepo;

    @Mock
    private StagePauseLogRepository pauseRepo;

    @Mock
    private OutsourcingTaskRepository outsourcingRepo;

    @InjectMocks
    private ExecutionService service;

    // =========================================================
    // createTracking() – các test trước đó
    // =========================================================
    @Test
    void createTracking_Normal_Start() {
        StageTracking e = new StageTracking();

        ProductionStage stage = new ProductionStage();
        stage.setId(1L);
        e.setProductionStage(stage);

        User operator = new User();
        operator.setId(11L);
        e.setOperator(operator);

        e.setAction("START");
        e.setQuantityCompleted(null); // cho đơn giản, không cần kiểu số chính xác
        e.setNotes("Bắt đầu công đoạn dệt");

        when(trackingRepo.save(any())).thenAnswer(inv -> {
            StageTracking saved = inv.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        StageTracking result = service.createTracking(e);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("START", result.getAction());
        verify(trackingRepo, times(1)).save(e);
    }

    @Test
    void createTracking_NoteOnly() {
        StageTracking e = new StageTracking();

        ProductionStage stage = new ProductionStage();
        stage.setId(1L);
        e.setProductionStage(stage);

        User operator = new User();
        operator.setId(11L);
        e.setOperator(operator);

        e.setAction("NOTE");
        e.setQuantityCompleted(null);
        e.setNotes("Ghi chú tiến độ");

        when(trackingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StageTracking result = service.createTracking(e);

        assertNotNull(result);
        assertEquals("NOTE", result.getAction());
        assertNull(result.getQuantityCompleted());
        verify(trackingRepo, times(1)).save(e);
    }

    @Test
    void createTracking_EndCompleted() {
        StageTracking e = new StageTracking();

        ProductionStage stage = new ProductionStage();
        stage.setId(1L);
        e.setProductionStage(stage);

        User operator = new User();
        operator.setId(11L);
        e.setOperator(operator);

        e.setAction("END");
        e.setQuantityCompleted(null);
        e.setNotes("Kết thúc công đoạn");

        when(trackingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StageTracking result = service.createTracking(e);

        assertNotNull(result);
        assertEquals("END", result.getAction());
        verify(trackingRepo, times(1)).save(e);
    }

    @Test
    void createTracking_NoStage() {
        StageTracking e = new StageTracking();

        User operator = new User();
        operator.setId(11L);
        e.setOperator(operator);

        e.setAction("START");
        e.setQuantityCompleted(null);
        e.setNotes("Thiếu stage");

        when(trackingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StageTracking result = service.createTracking(e);

        assertNotNull(result);
        assertNull(result.getProductionStage());
        verify(trackingRepo, times(1)).save(e);
    }

    @Test
    void createTracking_RepositoryThrows() {
        StageTracking e = new StageTracking();
        e.setAction("START");

        when(trackingRepo.save(any()))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class,
                () -> service.createTracking(e));
    }

    // =========================================================
    // createPause()
    // =========================================================
    @Test
    void createPause_Normal() {
        StagePauseLog e = new StagePauseLog();

        ProductionStage stage = new ProductionStage();
        stage.setId(1L);
        e.setProductionStage(stage);

        User pausedBy = new User();
        pausedBy.setId(99L);
        e.setPausedBy(pausedBy);

        User resumedBy = new User();
        resumedBy.setId(100L);
        e.setResumedBy(resumedBy);

        e.setPauseReason("Sửa chữa bảo trì");
        e.setPauseNotes("Máy dừng do bảo trì");
        e.setPausedAt(Instant.now());
        e.setResumedAt(Instant.now());
        e.setDurationMinutes(60);

        when(pauseRepo.save(any())).thenAnswer(inv -> {
            StagePauseLog saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        StagePauseLog result = service.createPause(e);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Sửa chữa bảo trì", result.getPauseReason());
        assertEquals(60, result.getDurationMinutes());
        verify(pauseRepo, times(1)).save(e);
    }

    @Test
    void createPause_NoResumedYet() {
        StagePauseLog e = new StagePauseLog();

        ProductionStage stage = new ProductionStage();
        stage.setId(1L);
        e.setProductionStage(stage);

        User pausedBy = new User();
        pausedBy.setId(99L);
        e.setPausedBy(pausedBy);

        e.setPauseReason("Mất điện");
        e.setPauseNotes("Máy dừng do mất điện");
        e.setPausedAt(Instant.now());
        e.setResumedAt(null);
        e.setDurationMinutes(null);

        when(pauseRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StagePauseLog result = service.createPause(e);

        assertNotNull(result);
        assertEquals("Mất điện", result.getPauseReason());
        assertNull(result.getResumedAt());
        verify(pauseRepo, times(1)).save(e);
    }

    @Test
    void createPause_RepositoryThrows() {
        StagePauseLog e = new StagePauseLog();
        e.setPauseReason("Lỗi bất thường");

        when(pauseRepo.save(any()))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class,
                () -> service.createPause(e));
    }

    // =========================================================
    // updatePause()
    // =========================================================
    @Test
    void updatePause_Normal() {
        StagePauseLog existing = new StagePauseLog();
        existing.setId(1L);

        StagePauseLog upd = new StagePauseLog();
        ProductionStage stage = new ProductionStage();
        stage.setId(1L);
        upd.setProductionStage(stage);

        User pausedBy = new User();
        pausedBy.setId(99L);
        upd.setPausedBy(pausedBy);

        User resumedBy = new User();
        resumedBy.setId(100L);
        upd.setResumedBy(resumedBy);

        upd.setPauseReason("Sửa chữa bảo trì");
        upd.setPauseNotes("Máy dừng do bảo trì");
        upd.setPausedAt(Instant.now());
        upd.setResumedAt(Instant.now());
        upd.setDurationMinutes(45);

        when(pauseRepo.findById(1L)).thenReturn(Optional.of(existing));

        StagePauseLog result = service.updatePause(1L, upd);

        assertSame(existing, result);
        assertEquals("Sửa chữa bảo trì", result.getPauseReason());
        assertEquals(45, result.getDurationMinutes());
        assertEquals(resumedBy, result.getResumedBy());
    }

    @Test
    void updatePause_NotFound() {
        when(pauseRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.updatePause(99L, new StagePauseLog()));
    }

    // =========================================================
    // createOutsourcing()
    // =========================================================
    @Test
    void createOutsourcing_Normal() {
        OutsourcingTask e = new OutsourcingTask();

        ProductionStage stage = new ProductionStage();
        stage.setId(1L);
        e.setProductionStage(stage);

        e.setVendorName("Cty Dệt Như Ý");
        e.setDeliveryNoteNumber("DN-001");
        // các trường số để null cho an toàn kiểu dữ liệu
        e.setStatus("SENT");
        e.setNotes("Tạo mới outsourcing task thành công");

        when(outsourcingRepo.save(any())).thenAnswer(inv -> {
            OutsourcingTask saved = inv.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        OutsourcingTask result = service.createOutsourcing(e);

        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals("Cty Dệt Như Ý", result.getVendorName());
        assertEquals("SENT", result.getStatus());
        verify(outsourcingRepo, times(1)).save(e);
    }

    @Test
    void createOutsourcing_MinimalData() {
        OutsourcingTask e = new OutsourcingTask();
        e.setVendorName("Vendor A");

        when(outsourcingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OutsourcingTask result = service.createOutsourcing(e);

        assertNotNull(result);
        assertEquals("Vendor A", result.getVendorName());
        verify(outsourcingRepo, times(1)).save(e);
    }

    @Test
    void createOutsourcing_RepositoryThrows() {
        OutsourcingTask e = new OutsourcingTask();
        e.setVendorName("Vendor Err");

        when(outsourcingRepo.save(any()))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class,
                () -> service.createOutsourcing(e));
    }

    // =========================================================
    // updateOutsourcing()
    // =========================================================
    @Test
    void updateOutsourcing_Normal() {
        OutsourcingTask existing = new OutsourcingTask();
        existing.setId(1L);

        OutsourcingTask upd = new OutsourcingTask();
        ProductionStage stage = new ProductionStage();
        stage.setId(1L);
        upd.setProductionStage(stage);
        upd.setVendorName("Cty Dệt Như Ý");
        upd.setDeliveryNoteNumber("DN-001");
        upd.setStatus("CREATED");
        upd.setNotes("Cập nhật outsourcing task thành công");

        User creator = new User();
        creator.setId(7L);
        upd.setCreatedBy(creator);

        when(outsourcingRepo.findById(1L)).thenReturn(Optional.of(existing));

        OutsourcingTask result = service.updateOutsourcing(1L, upd);

        assertSame(existing, result);
        assertEquals("Cty Dệt Như Ý", result.getVendorName());
        assertEquals("DN-001", result.getDeliveryNoteNumber());
        assertEquals("CREATED", result.getStatus());
        assertEquals(creator, result.getCreatedBy());
    }

    @Test
    void updateOutsourcing_NotFound() {
        when(outsourcingRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.updateOutsourcing(99L, new OutsourcingTask()));
    }
}
