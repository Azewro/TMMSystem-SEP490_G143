package tmmsystem.dto.qc;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class TechnicalDefectDto {
    private Long defectId;
    private String lotCode; // Mã lô từ ProductionLot
    private String productName; // Tên sản phẩm
    private String size; // Kích thước
    private String stageName; // Tên công đoạn (Cuồng mắc, Dệt, Nhuộm, ...)
    private String stageCode; // Mã công đoạn
    private String severity; // MINOR, MAJOR, CRITICAL
    private String severityLabel; // Lỗi nhẹ, Lỗi nặng, Lỗi nghiêm trọng
    private String status; // pending, resolved
    private String statusLabel; // Chờ xử lý, Đã xử lý
    private Instant sentAt; // Thời gian gửi (từ QcInspection.inspectedAt)
    private String sentAtFormatted; // Thời gian gửi định dạng dd/MM/yyyy cho frontend
    private String defectDescription; // Mô tả lỗi từ QcDefect
    private String defectType; // Loại lỗi từ QcDefect
    private Long stageId; // ID của ProductionStage
    private Long inspectionId; // ID của QcInspection
}

