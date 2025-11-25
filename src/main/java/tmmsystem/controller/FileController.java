package tmmsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tmmsystem.service.FileStorageService;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final FileStorageService storage;
    public FileController(FileStorageService storage) { this.storage = storage; }

    @Operation(summary = "Serve stored file by filename")
    @GetMapping(value = "/{filename}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> getFile(@Parameter(description = "Tên file") @PathVariable String filename) {
        try {
            byte[] bytes = storage.getFileByFilename(filename);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Upload ảnh lỗi QC", description = "QA sử dụng để upload ảnh lỗi, trả về URL có thể đính kèm vào kết quả kiểm tra")
    @PostMapping(value = "/qc/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadQcPhoto(
            @Parameter(description = "Ảnh lỗi (image/*)") @RequestParam("file") MultipartFile file,
            @Parameter(description = "ID công đoạn") @RequestParam(required = false) Long stageId,
            @Parameter(description = "ID QA user") @RequestParam(required = false) Long qcUserId) {
        try {
            String storedFileName = storage.uploadQcPhoto(file, stageId, qcUserId);
            String url = storage.buildPublicUrl(storedFileName);
            return ResponseEntity.ok(
                    Map.of("fileName", storedFileName,
                           "url", url));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}

